/* HippoRegistrar.h: Utility class for registering stuff in the windows registry
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include <limits.h>
#include <stdarg.h>
#include <strsafe.h>

#include "HippoUtil.h"
#include "HippoRegistrar.h"

HippoRegistrar::HippoRegistrar(const WCHAR *dllName)
{
    modulePath_ = NULL;

    HINSTANCE module = GetModuleHandleW(dllName);
    if (!module)
        return;

    WCHAR modulePath[MAX_PATH];
    HRESULT hr = GetModuleFileName(module, modulePath, MAX_PATH);
    if (FAILED(hr))
        return;

    modulePath_ = (WCHAR *)malloc(sizeof(WCHAR) * (lstrlenW(modulePath) + 1));
    if (!modulePath)
        return;

    StringCchCopy(modulePath_, lstrlen(modulePath) + 1, modulePath);
}

HippoRegistrar::~HippoRegistrar()
{
    free(modulePath_);
}

WCHAR *
HippoRegistrar::getModulePath()
{
    return modulePath_;
}

HRESULT
HippoRegistrar::registerTypeLib()
{
    HippoPtr<ITypeLib> typeLib;
    HRESULT hr;

    if (!modulePath_)
        return E_OUTOFMEMORY;

    hr = LoadTypeLib(modulePath_, &typeLib);
    if (!SUCCEEDED (hr)) 
        return hr;

    hr = RegisterTypeLib(typeLib, modulePath_, NULL);

    return hr;
}

HRESULT
HippoRegistrar::registerClassImplCategories(const CLSID &classID, 
                                            ULONG        cCategories,
                                            CATID        categories[])
{   
    HippoPtr<ICatRegister> catRegister;
    HRESULT hr;

    CoInitialize(NULL);

    hr = CoCreateInstance(CLSID_StdComponentCategoriesMgr,
                          NULL,
                          CLSCTX_ALL,
                          IID_ICatRegister,
                          (LPVOID *)&catRegister);

    if (SUCCEEDED (hr))
        hr = catRegister->RegisterClassImplCategories(classID, cCategories, 
                                                      categories);

    CoUninitialize();

    return hr;
}

// Set a key, using a printf string to define the subkey
static HRESULT
setValuePrintf(HKEY         key,
               const WCHAR *subkeyFormat,
               const WCHAR *value,
               const WCHAR *data,
               ...)
{
    va_list vap;
    WCHAR subkey[MAX_PATH];
    size_t len;
    LONG result;
    HKEY newKey;

    va_start(vap, data);
    StringCchVPrintf(subkey, MAX_PATH, subkeyFormat, vap);
    va_end(vap);

    result = RegCreateKeyEx(key, subkey, NULL, NULL, 
                            REG_OPTION_NON_VOLATILE, KEY_WRITE, NULL,
                            &newKey, NULL);
    if (result != ERROR_SUCCESS)
        return E_FAIL;

    len = lstrlen(data);
    if (sizeof(WCHAR) * (len + 1) > UINT_MAX)
        return E_OUTOFMEMORY;

    result = RegSetValueEx(newKey, value, NULL, REG_SZ,
                           (const BYTE *)data, (DWORD)sizeof(WCHAR) * (len + 1));
    if (result != ERROR_SUCCESS)
        return E_FAIL;

    RegCloseKey(newKey);

    /* We could replace the above with usage of the Shell utility function:
    result = SHSetValueW(key, subkey, value, 
                       REG_SZ, 
                       (const void *)data, (DWORD)sizeof(WCHAR) * (len + 1)); */

    return S_OK;
}

// Delete a value under a key, using a printf string to define the subkey
static HRESULT
deleteValuePrintf(HKEY         key,
                  const WCHAR *subkeyFormat,
                  const WCHAR *value,
                  ...)
{
    va_list vap;
    WCHAR subkey[MAX_PATH];
    LONG result;
    HKEY newKey;

    va_start(vap, value);
    StringCchVPrintf(subkey, MAX_PATH, subkeyFormat, vap);
    va_end(vap);

    result = RegOpenKeyEx(key, subkey, 0,KEY_WRITE, &newKey);
    // We assume that if we can't open the key, we don't need to delete the value
    if (result != ERROR_SUCCESS)
        return ERROR_SUCCESS;

    result = RegDeleteValue(newKey, value);
    if (result != ERROR_SUCCESS)
        return E_FAIL;

    RegCloseKey(newKey);

    return S_OK;
}

// Delete a key, using a printf string to define the subkey
static HRESULT
deleteKeyPrintf(HKEY         key,
               const WCHAR *subkeyFormat,
               ...)
{
    va_list vap;
    WCHAR subkey[MAX_PATH];
    LONG result;
    HKEY newKey;

    va_start(vap, subkeyFormat);
    StringCchVPrintf(subkey, MAX_PATH, subkeyFormat, vap);
    va_end(vap);

    result = RegOpenKeyEx(key, subkey, 0,KEY_WRITE, &newKey);
    // We assume that if we can't open the key, we don't need to delete the value
    if (result != ERROR_SUCCESS)
        return ERROR_SUCCESS;
    RegCloseKey(newKey);

    result = RegDeleteKey(key, subkey);
    if (result != ERROR_SUCCESS)
        return E_FAIL;

    return S_OK;
}

HRESULT
HippoRegistrar::registerInprocServer(const CLSID &classID,
                                     const WCHAR *title)
{
    WCHAR *classStr;
    HRESULT hr;

    hr = StringFromIID(classID, &classStr);
    if (FAILED(hr))
        return hr;

    hr = setValuePrintf(HKEY_CLASSES_ROOT, 
                        L"CLSID\\%ls", 
                        NULL, title,
                        classStr);
    if (FAILED(hr))
        goto failed;

    hr = setValuePrintf(HKEY_CLASSES_ROOT, 
                        L"CLSID\\%ls\\InprocServer32", 
                        NULL, modulePath_,
                        classStr);
    if (FAILED(hr))
        goto failed;

    hr = setValuePrintf(HKEY_CLASSES_ROOT,
                        L"CLSID\\%ls\\InprocServer32", 
                        L"ThreadingModel", L"Apartment",
                        classStr); 

failed:
    CoTaskMemFree(classStr);

    return hr;
}

HRESULT 
HippoRegistrar::registerBrowserHelperObject(const CLSID &classID,
                                            const WCHAR *title)
{
    WCHAR *classStr;
    HRESULT hr;

    hr = StringFromIID(classID, &classStr);
    if (FAILED(hr))
        return hr;

    // The value we set here isn't used, but it's useful for debugging
    // to identify our GUID among other BHO's.
    hr = setValuePrintf(HKEY_LOCAL_MACHINE,
                        L"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Browser Helper Objects\\%ls", 
                        NULL, title,
                        classStr); 

    CoTaskMemFree(classStr);

    return hr;
}

HRESULT
HippoRegistrar::registerStartupProgram(const WCHAR *key,
                                       const WCHAR *commandline)
{
    return setValuePrintf(HKEY_CURRENT_USER,
                          L"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run", 
                          key, commandline);         
}

HRESULT
HippoRegistrar::unregisterStartupProgram(const WCHAR *key)
{
    return deleteValuePrintf(HKEY_CURRENT_USER,
                             L"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run", 
                             key);
}

HRESULT
HippoRegistrar::registerGlobalShellCtxMenu(const CLSID &classID,
                                           const WCHAR *title)
{
    WCHAR *classStr;
    HRESULT hr;

    hr = StringFromIID(classID, &classStr);
    if (FAILED(hr))
        return hr;

    hr = setValuePrintf(HKEY_CLASSES_ROOT,
        L"*\\shellex\\ContextMenuHandlers\\%ls", 
        NULL, classStr,
        title); 

    CoTaskMemFree(classStr);

    return hr;
}

HRESULT
HippoRegistrar::unregisterGlobalShellCtxMenu(const WCHAR *title)
{
    HRESULT hr;
    hr = deleteKeyPrintf(HKEY_CLASSES_ROOT,
        L"*\\shellex\\ContextMenuHandlers\\%ls", title); 
    return hr;
}