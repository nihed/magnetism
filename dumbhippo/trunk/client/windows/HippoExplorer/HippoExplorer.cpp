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

// Definitions of GUIDs
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include <shlguid.h>
#include "Guid.h"
#pragma data_seg()

HINSTANCE dllInstance;
UINT dllRefCount;

extern "C" BOOL WINAPI
DllMain(HINSTANCE hInstance, 
        DWORD     dwReason, 
        LPVOID    lpReserved)
{
    switch(dwReason)
    {
       case DLL_PROCESS_ATTACH:
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

    if (!IsEqualCLSID(classID, CLSID_HippoExplorerBar) &&
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
    
    return S_OK;
}
