/* HippoExplorerBar.cpp: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 
 **/

#include <HippoStdAfx.h>
#include <HippoUtil.h>
#include <HippoUtil_i.c>

#include "hippo-com-ipc-hub.h"

HINSTANCE dllInstance;

extern "C" BOOL WINAPI
DllMain(HINSTANCE hInstance, 
        DWORD     dwReason, 
        LPVOID    lpReserved)
{
    switch(dwReason)
    {
    case DLL_PROCESS_ATTACH:
       dllInstance = hInstance;

       HippoComIpcHub::startup(hInstance); // Initialize criticial section, etc.

       break;
    case DLL_PROCESS_DETACH:
        HippoComIpcHub::shutdown(); // Clean up resources
        break;
    }
       
    return TRUE;
}                                 
