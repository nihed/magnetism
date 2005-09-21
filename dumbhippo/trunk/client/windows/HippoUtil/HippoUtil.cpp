// HippoUtil.cpp : Defines the entry point for the application.
//

#include "stdafx.h"
#include <ole2.h>
#include "HippoUtil.h"

/**************************************************************************
   GUID stuff
**************************************************************************/

// This part is only done once.
// If you need to use the GUID in another file, just include Guid.h.
#if 0
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include "Guid.h"
#pragma data_seg()
#endif

extern "C" BOOL WINAPI DllMain(HINSTANCE, DWORD, LPVOID);

HINSTANCE   g_hInst;
UINT        g_DllRefCount;

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
DllRegisterServer(void)
{   
    /* The documentation seems to claim that regsrv32 will automatically 
     * register any typelib embedded in the DLL, but that seems not to be
     * the case, so do it manually. It may be that the documentation is
     * referring to the ATL-generated standard DllRegisterServer.
     */
    WCHAR    szModule[MAX_PATH];
    HRESULT hr;
    ITypeLib *pTypeLib = NULL;
    
    g_hInst = GetModuleHandle(TEXT("HippoUtil.DLL"));

    hr = GetModuleFileNameW(g_hInst, szModule, MAX_PATH);
    if (!SUCCEEDED (hr))
	return hr;

    if (sizeof(WCHAR) != sizeof(OLECHAR))
	return -1;
   
    hr = LoadTypeLib(szModule, &pTypeLib);
    if (!SUCCEEDED (hr)) 
	return hr;

    hr = RegisterTypeLib(pTypeLib, (OLECHAR *)szModule, NULL);

    pTypeLib->Release();

    return hr;
}