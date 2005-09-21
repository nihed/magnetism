/* HippoExplorerBar.cpp: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 *
 * Partially based on MSDN BandObjs sample:
 *  Copyright 1997 Microsoft Corporation.  All Rights Reserved.
 **/

#include "stdafx.h"
#include <ole2.h>
#include <comcat.h>
#include <olectl.h>
#include <strsafe.h>
#include "ClsFact.h"
#include <HippoUtil_i.c>

/**************************************************************************
   GUID stuff
**************************************************************************/

// This part is only done once.
// If you need to use the GUID in another file, just include Guid.h.
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include <shlguid.h>
#include "Guid.h"
#pragma data_seg()

extern "C" BOOL WINAPI DllMain(HINSTANCE, DWORD, LPVOID);
BOOL RegisterServer(CLSID, LPTSTR);
BOOL RegisterComCat(CLSID, CATID);

HINSTANCE   g_hInst;
UINT        g_DllRefCount;
HRESULT     hr;

extern "C" BOOL WINAPI
DllMain(HINSTANCE hInstance, 
        DWORD     dwReason, 
        LPVOID    lpReserved)
{
    switch(dwReason)
    {
       case DLL_PROCESS_ATTACH:
          g_hInst = hInstance;
          break;
    
       case DLL_PROCESS_DETACH:
          break;
    }
       
    return TRUE;
}                                 

STDAPI 
DllCanUnloadNow(void)
{
    return (g_DllRefCount ? S_FALSE : S_OK);
}


STDAPI 
DllGetClassObject(REFCLSID rclsid, 
                  REFIID   riid, 
                  LPVOID  *ppReturn)
{
    *ppReturn = NULL;
    
    // If this classid is not supported, return the proper error code.
    if (!IsEqualCLSID(rclsid, CLSID_HippoExplorerBar) &&
	!IsEqualCLSID(rclsid, CLSID_HippoTracker))
       return CLASS_E_CLASSNOTAVAILABLE;
       
    // Create a CClassFactory object and check it for validity.
    CClassFactory *pClassFactory = new CClassFactory(rclsid);
    if(NULL == pClassFactory)
       return E_OUTOFMEMORY;
       
    // Get the QueryInterface return for our return value
    HRESULT hResult = pClassFactory->QueryInterface(riid, ppReturn);
    
    // Call Release to decrement the ref count - creating the object set the ref
    // count to 1 and QueryInterface incremented it. Since it's only being used
    // externally, the ref count should only be 1.
    pClassFactory->Release();
    
    // Return the result from QueryInterface.
    return hResult;
}


STDAPI 
DllRegisterServer(void)
{
    if(!RegisterServer(CLSID_HippoExplorerBar, 
                       TEXT("Hi&ppo Bar")))
       return SELFREG_E_CLASS;
    
    if(!RegisterComCat(CLSID_HippoExplorerBar, CATID_CommBand))
       return SELFREG_E_CLASS;

    if(!RegisterServer(CLSID_HippoTracker, 
                       TEXT("Hippo Tracker")))
       return SELFREG_E_CLASS;
    
    return S_OK;
}


typedef struct{
   HKEY  hRootKey;
   LPTSTR szSubKey;        // TCHAR szSubKey[MAX_PATH];
   LPTSTR lpszValueName;
   LPTSTR szData;          // TCHAR szData[MAX_PATH];
}DOREGSTRUCT, *LPDOREGSTRUCT;

BOOL 
RegisterServer(CLSID clsid, LPTSTR lpszTitle)
{
    int      i;
    HKEY     hKey;
    LRESULT  lResult;
    DWORD    dwDisp;
    TCHAR    szSubKey[MAX_PATH];
    TCHAR    szCLSID[MAX_PATH];
    TCHAR    szModule[MAX_PATH];
    LPWSTR   pwsz;
    DWORD    retval;

    // Get the CLSID in string form.
    StringFromIID(clsid, &pwsz);

    if(pwsz)
    {
        #ifdef UNICODE
            hr = StringCchCopyW(szCLSID, MAX_PATH, pwsz);
            // TODO: Add error handling for hr here.
        #else
            WideCharToMultiByte( CP_ACP,
                                 0,
                                 pwsz,
                                 -1,
                                 szCLSID,
                                 MAX_PATH * sizeof(TCHAR),
                                 NULL,
                                 NULL);
        #endif

        // Free the string.
        LPMALLOC pMalloc;
        CoGetMalloc(1, &pMalloc);
        pMalloc->Free(pwsz);
        pMalloc->Release();
    }
    
    // Get the handle of the DLL.
    g_hInst = GetModuleHandle(TEXT("HippoExplorer.DLL"));

    // Get this app's path and file name.
    retval = GetModuleFileName(g_hInst, szModule, MAX_PATH);
    // TODO: Add error handling to check return value for success/failure
    //       before using szModule.

    DOREGSTRUCT ClsidEntries[ ] = {HKEY_CLASSES_ROOT,      
                                   TEXT("CLSID\\%38s"),            
                                   NULL,                   
                                   lpszTitle,
                                   HKEY_CLASSES_ROOT,      
                                   TEXT("CLSID\\%38s\\InprocServer32"), 
                                   NULL,  
                                   szModule,
                                   HKEY_CLASSES_ROOT,      
                                   TEXT("CLSID\\%38s\\InprocServer32"), 
                                   TEXT("ThreadingModel"), 
                                   TEXT("Apartment"),
                                   NULL,                
                                   NULL,
                                   NULL,
                                   NULL};

    //register the CLSID entries
    for(i = 0; ClsidEntries[i].hRootKey; i++)
    {
        //create the sub key string - for this case, insert the file extension
        hr = StringCchPrintf(szSubKey, 
                             MAX_PATH, 
                             ClsidEntries[i].szSubKey, 
                             szCLSID);
        // TODO: Add error handling code here to check the hr return value.
 
        lResult = RegCreateKeyEx(ClsidEntries[i].hRootKey,
                                 szSubKey,
                                 0,
                                 NULL,
                                 REG_OPTION_NON_VOLATILE,
                                 KEY_WRITE,
                                 NULL,
                                 &hKey,
                                 &dwDisp);
   
        if(NOERROR == lResult)
        {
            TCHAR szData[MAX_PATH];
			size_t length;

            // If necessary, create the value string.
            hr = StringCchPrintf(szData, 
                                 MAX_PATH, 
                                 ClsidEntries[i].szData, 
                                 szModule);
            // TODO: Add error handling code here to check the hr return value.

            hr = StringCchLength(szData, MAX_PATH, &length);
            // TODO: Add error handling code here to check the hr return value.
   
            lResult = RegSetValueEx(hKey,
                                    ClsidEntries[i].lpszValueName,
                                    0,
                                    REG_SZ,
                                    (LPBYTE)szData,
                                    (length + 1) * sizeof(TCHAR));
      
            RegCloseKey(hKey);
        }
        else
            return FALSE;
    }

    return TRUE;
}

BOOL 
RegisterComCat(CLSID clsid, CATID CatID)
{
    ICatRegister   *pcr;
    HRESULT        hr = S_OK ;
        
    CoInitialize(NULL);
    
    hr = CoCreateInstance(CLSID_StdComponentCategoriesMgr, 
                          NULL, 
                          CLSCTX_INPROC_SERVER, 
                          IID_ICatRegister, 
                          (LPVOID*)&pcr);
    
    if(SUCCEEDED(hr))
    {
       hr = pcr->RegisterClassImplCategories(clsid, 1, &CatID);
    
       pcr->Release();
    }
            
    CoUninitialize();
    
    return SUCCEEDED(hr);
}

