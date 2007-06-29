// HippoUtil.cpp : Defines the entry point for the application.
//

#include "stdafx-hippoutil.h"
#include <ole2.h>
#include "HippoUtil.h"
#include "HippoRegistrar.h"

// Generated code for HippoUtil.idl
#include "HippoUtil_i.c"

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
    HippoRegistrar registrar(L"HippoUtil.dll");
    return registrar.registerTypeLib();
}
