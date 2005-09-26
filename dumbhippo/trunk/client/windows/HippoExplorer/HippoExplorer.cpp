/* HippoExplorerBar.cpp: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 
 **/

#include "stdafx.h"
#include "ClassFactory.h"
#include "Globals.h"

#include <HippoUtil.h>
#include <HippoUtil_i.c>
#include <HippoRegistrar.h>
#include "HippoExplorer_h.h"
#include "HippoExplorer_i.c"

// Definitions of GUIDs
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include <shlguid.h>
#include "Guid.h"
#pragma data_seg()

HINSTANCE dllInstance;
UINT dllRefCount;

static bool
isExplorer(void)
{
    static const WCHAR *suffix = L"\\explorer.exe";

    HMODULE process = GetModuleHandle(NULL);

    WCHAR modulePath[MAX_PATH];
    HRESULT hr = GetModuleFileName(process, modulePath, MAX_PATH);
    if (FAILED(hr))
	return false;

    _wcslwr(modulePath);
    size_t len = wcslen(modulePath);

    return (len >= wcslen(suffix) &&
	    wcscmp(modulePath + len - wcslen(suffix), suffix) == 0);
}

extern "C" BOOL WINAPI
DllMain(HINSTANCE hInstance, 
        DWORD     dwReason, 
        LPVOID    lpReserved)
{
    switch(dwReason)
    {
    case DLL_PROCESS_ATTACH:
       /* For efficiency reasons (and to make it easier to load new versions
        * when debugging), we want to avoid being loaded into Windows Explorer;
	* browser helper objects will normally be loaded there, but we are
	* only interested in web browsing.
        */
       if (isExplorer())
	   return FALSE;

       dllInstance = hInstance;
       break;
    }
       
    return TRUE;
}                                 

STDAPI 
DllCanUnloadNow(void)
{
    return dllRefCount > 0 ? S_FALSE : S_OK;
}


STDAPI 
DllGetClassObject(const CLSID &classID, 
                  const IID   &ifaceID,
                  void       **result)
{
    HRESULT hr;

    if (!IsEqualCLSID(classID, CLSID_HippoEmbed) &&
	!IsEqualCLSID(classID, CLSID_HippoExplorerBar) &&
	!IsEqualCLSID(classID, CLSID_HippoTracker)) {
	return CLASS_E_CLASSNOTAVAILABLE;
    }
       
    ClassFactory *classFactory = new ClassFactory(classID);
    if (!classFactory) {
        *result = NULL;
	return E_OUTOFMEMORY;
    }
       
    hr = classFactory->QueryInterface(ifaceID, result);
    classFactory->Release();

    return hr;
}

STDAPI 
DllRegisterServer(void)
{
    HippoRegistrar registrar(TEXT("HippoExplorer.dll"));
    HRESULT hr;
    CATID catids[1];

    hr  = registrar.registerTypeLib();
    if (FAILED(hr))
	return hr;

    hr = registrar.registerInprocServer(CLSID_HippoExplorerBar,
			                TEXT("Hi&ppo Bar"));
    if (FAILED(hr))
	return hr;

    catids[0] = CATID_CommBand;
    hr = registrar.registerClassImplCategories(CLSID_HippoExplorerBar,
	                                       1, catids);
    if (FAILED (hr))
	return hr;

    hr = registrar.registerInprocServer(CLSID_HippoTracker,
			                TEXT("Hippo Tracker"));
    if (FAILED(hr))
	return hr;
    
    hr = registrar.registerInprocServer(CLSID_HippoEmbed,
			                TEXT("Hippo Embed"));
    if (FAILED(hr))
	return hr;

    return S_OK;
}
