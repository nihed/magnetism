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

static HRESULT
loadTypeInfoFromTypelib(ITypeLib *typeLib, va_list vap)
{
    HRESULT hr = S_OK;

    while (true) {
        const GUID *guid = va_arg(vap, const GUID *);
        if (!guid)
            return hr;

        ITypeInfo **info = va_arg(vap, ITypeInfo **);
        hr = typeLib->GetTypeInfoOfGuid(*guid, info);
        if (!SUCCEEDED(hr))
            return hr;
    }
}

static void
clearArgs(va_list vap)
{
    while (true) {
        const GUID *guid = va_arg(vap, const GUID *);
        if (!guid)
            return;
        ITypeInfo **info = va_arg(vap, ITypeInfo **);
        *info = NULL;
    }
}

static void
freeArgs(va_list vap)
{
    while (true) {
        const GUID *guid = va_arg(vap, const GUID *);
        if (!guid)
            return;
        ITypeInfo **info = va_arg(vap, ITypeInfo **);
        if (*info) {
            (*info)->Release();
            *info = NULL;
        }
    }
}

HRESULT 
hippoLoadTypeInfo(const WCHAR *libraryName, ...)
{
    HippoPtr<ITypeLib> typeLib;
    va_list vap;
    HRESULT hr;            

    HMODULE module = GetModuleHandle(libraryName);
    if (!module)
        return E_FAIL;

    WCHAR moduleFile[MAX_PATH];
    DWORD length = GetModuleFileName(module, moduleFile, MAX_PATH);
    if (length == 0 || length > MAX_PATH - 1)
        return E_FAIL;

    va_start(vap, libraryName);
    clearArgs(vap);
    va_end(vap);

    hr = LoadTypeLib(moduleFile, &typeLib);
    if (FAILED(hr))
        goto out;

    va_start(vap, libraryName);
    hr = loadTypeInfoFromTypelib(typeLib, vap);
    va_end(vap);

out:
    if (FAILED(hr)) {
        va_start(vap, libraryName);
        freeArgs(vap);
        va_end(vap);

        hippoDebugDialog(L"Failed to load type info from %ls (%x)", libraryName, hr);
    }

    return hr;
}

HRESULT 
hippoLoadRegTypeInfo(const GUID    &libraryId, 
                     unsigned short majorVersion, 
                     unsigned short minorVersion 
                     ...)
{
    HippoPtr<ITypeLib> typeLib;
    va_list vap;
    HRESULT hr;
    
    va_start(vap, minorVersion);
    clearArgs(vap);
    va_end(vap);

    hr = LoadRegTypeLib(libraryId, 
                        majorVersion, minorVersion,
                        0,    /* LCID */
                        &typeLib);
    if (FAILED(hr))
        goto out;
    
    va_start(vap, minorVersion);
    hr = loadTypeInfoFromTypelib(typeLib, vap);
    va_end(vap);

out:
    if (FAILED(hr)) {
        va_start(vap, minorVersion);
        freeArgs(vap);
        va_end(vap);

        hippoDebugLogW(L"Failed to load type info (%x)", hr);
    }

    return hr;
}

void
hippoDebugDialog(WCHAR *format, ...)
{
    WCHAR buf[1024];
    va_list vap;
    va_start(vap, format);
    StringCchVPrintfW(buf, sizeof(buf) / sizeof(buf[0]), format, vap);
    va_end(vap);
    MessageBoxW(NULL, buf, L"Hippo Debug", MB_OK);
}

void 
hippoDebugLastErr(WCHAR *fmt, ...) 
{
    HippoBSTR str;
    HippoBSTR errstr;
    WCHAR buf[1024];
    HRESULT res = GetLastError();
    va_list vap;
    va_start(vap, fmt);
    StringCchVPrintfW(buf, sizeof(buf) / sizeof(buf[0]), fmt, vap);
    va_end(vap);
    str.Append(buf);
    hippoHresultToString(res, errstr);
    str.Append(errstr);
    MessageBoxW(NULL, str, L"Hippo Debug", MB_OK);
}

void 
hippoDebugLogW(const WCHAR *format, ...) 
{
    WCHAR buf[1024];
    va_list vap;
    va_start (vap, format);
    StringCchVPrintfW(buf, sizeof(buf) / sizeof(buf[0]) - 2, format, vap);
    va_end (vap);

    StringCchCatW(buf, sizeof(buf) / sizeof(buf[0]) - 2, L"\r\n");

    OutputDebugStringW(buf);
}

void 
hippoDebugLogU(const char *format, ...)
{
    char buf[1024];
    va_list vap;
    va_start (vap, format);
    StringCchVPrintfA(buf, sizeof(buf) / sizeof(buf[0]) - 2, format, vap);
    va_end (vap);

    StringCchCatA(buf, sizeof(buf) / sizeof(buf[0]) - 2, "\r\n");

    HippoBSTR strW;
    strW.setUTF8(buf);

    OutputDebugStringW(strW);
}
