/* HippoExplorerBar.cpp: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 
 **/

#include "stdafx-hippoexplorer.h"
#include "ClassFactory.h"
#include "Globals.h"
#include "Resource.h"

#include <HippoUtil.h>
#include <HippoUtil_i.c>
#include <HippoRegistrar.h>
#include <HippoRegKey.h>
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

    if (!IsEqualCLSID(classID, CLSID_HippoChatControl) &&
        !IsEqualCLSID(classID, CLSID_HippoEmbed) &&
        !IsEqualCLSID(classID, CLSID_HippoExplorerBar) &&
        !IsEqualCLSID(classID, CLSID_HippoToolbarAction) &&
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

static HRESULT
registerToolbarAction(HippoRegistrar *registrar)
{
    WCHAR iconPath[MAX_PATH];
    WCHAR *extensionStr = NULL;
    WCHAR *classStr = NULL;
    HRESULT hr;

    // See http://msdn.microsoft.com/workshop/browser/ext/tutorials/button.asp; Note that the
    // the Icon/HotIcon strings there are buggy - there must be no comma after the space
    // to point to an embedded resource

#define CHECK_BOOL(expr)        \
    do {                        \
        if (!expr) {            \
            hr = E_FAIL;        \
            goto out;           \
        }                       \
    } while (0)
    
    hr = registrar->registerInprocServer(CLSID_HippoToolbarAction,
                                         TEXT("Mugshot Toolbar Action"));
    if (FAILED(hr))
        return hr;

    hr = StringFromIID(GUID_HippoToolbarButton, &extensionStr);
    if (FAILED(hr))
        return hr;

    HippoRegKey key(HKEY_LOCAL_MACHINE, L"SOFTWARE\\Microsoft\\Internet Explorer\\Extensions\\%s", TRUE,
                    extensionStr);
    CHECK_BOOL(key.saveString(L"Default Visible", L"Yes"));
    CHECK_BOOL(key.saveString(L"ButtonText", L"Share Link"));
    CHECK_BOOL(key.saveString(L"MenuText", L"Share Link..."));
    CHECK_BOOL(key.saveString(L"MenuStatusBar", L"Share the current web page via Mugshot"));

    // Note that HotIcon / Icon here are windows concepts corresponding to grayscale and prelighted/color
    // icons, they have nothing to do with our system's idea of "hotness" (and in fact right now 
    // we're just lazy and use the same icon for both)

    hr = StringCchPrintf(iconPath, MAX_PATH, L"%s,%d", registrar->getModulePath(), IDI_LINKSWARM);
    if (FAILED(hr))
        goto out;
    CHECK_BOOL(key.saveString(L"HotIcon", iconPath));

    hr = StringCchPrintf(iconPath, MAX_PATH, L"%s,%d", registrar->getModulePath(), IDI_LINKSWARM);
    if (FAILED(hr))
        goto out;
    CHECK_BOOL(key.saveString(L"Icon", iconPath));

    CHECK_BOOL(key.saveString(L"CLSID", L"{1FBA04EE-3024-11d2-8F1F-0000F87ABD16}"));
    
    hr = StringFromIID(CLSID_HippoToolbarAction, &classStr);
    if (FAILED(hr))
        goto out;
    CHECK_BOOL(key.saveString(L"ClsidExtension", classStr));

#undef CHECK_BOOL

out:
    if (classStr)
        CoTaskMemFree(classStr);
    if (extensionStr)
        CoTaskMemFree(extensionStr);

    return hr;
}

STDAPI 
DllRegisterServer(void)
{
    HippoRegistrar registrar(TEXT("HippoExplorer.dll"));
    HRESULT hr;

    hr  = registrar.registerTypeLib();
    if (FAILED(hr))
        return hr;

    hr = registrar.registerInprocServer(CLSID_HippoExplorerBar,
                                        TEXT("&Mugshot Bar"));
    if (FAILED(hr))
        return hr;

    CLSID catids[1];
    catids[0] = CATID_CommBand;
    hr = registrar.registerClassImplCategories(CLSID_HippoExplorerBar,
                                               1, catids);
    if (FAILED (hr))
        return hr;

    /* This forces a rescan of (horizontal) explorer bar categories. See comments in Components.wxs */
    RegDeleteKey(HKEY_CLASSES_ROOT, L"Component Categories\\{00021494-0000-0000-C000-000000000046}\\Enum");
    RegDeleteKey(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Discardable\\PostSetup\\Component Categories\\{00021494-0000-0000-C000-000000000046}\\Enum");

    hr = registrar.registerInprocServer(CLSID_HippoTracker,
                                        TEXT("Mugshot Tracker"));
    if (FAILED(hr))
        return hr;

    hr = registrar.registerBrowserHelperObject(CLSID_HippoTracker,
                                               TEXT("Mugshot Tracker"));
    if (FAILED(hr))
        return hr;
    
    hr = registrar.registerInprocServer(CLSID_HippoChatControl,
                                        TEXT("Mugshot Chat Control"));
    if (FAILED(hr))
        return hr;

    hr = registrar.registerInprocServer(CLSID_HippoEmbed,
                                        TEXT("Mugshot Embed"));
    if (FAILED(hr))
        return hr;

    registerToolbarAction(&registrar);

    return S_OK;
}
