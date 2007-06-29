/* HippoRegKey: Utility class for loading or saving to a single registry key
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx-hippoutil.h"
#include <limits.h>
#include <stdarg.h>
#include "HippoUtil.h"
#include "HippoRegKey.h"

HippoRegKey::HippoRegKey(HKEY         parentKey,
                         const WCHAR *subkeyFormat,
                         bool         writable,
                         ...)
{
    WCHAR subkey[MAX_PATH];
    va_list vap;

    va_start(vap, writable);
    StringCchVPrintf(subkey, MAX_PATH, subkeyFormat, vap);
    va_end(vap);

    key_ = NULL;
    if (writable) {
        LONG result = RegCreateKeyEx(parentKey, 
                                     subkey,
                                     NULL, NULL, 
                                     REG_OPTION_NON_VOLATILE, KEY_READ | KEY_WRITE, NULL,
                                     &key_, NULL);
    } else {
        RegOpenKeyEx(parentKey, 
                     subkey,
                     0, KEY_READ, 
                     &key_);
    }
}

HippoRegKey::~HippoRegKey(void)
{
    if (key_)
        RegCloseKey(key_);
}

 bool 
 HippoRegKey::loadString(const WCHAR *valueName,
                         BSTR        *str)
 {
    if (!key_)
        return false;

    long result;
    BYTE buf[1024];
    DWORD bufSize = sizeof(buf) / sizeof(buf[0]);
    DWORD type;

    result = RegQueryValueEx(key_, valueName, NULL, 
                             &type, buf, &bufSize);
    if (result == ERROR_SUCCESS && type == REG_SZ) {
        HippoBSTR((WCHAR *)buf).CopyTo(str);
        return true;
    } else {
        return false;
    }
 }

 bool 
 HippoRegKey::loadBool(const WCHAR *valueName,
                       bool        *value)
 {
    if (!key_)
        return false;

    long result;
    DWORD tmp;
    DWORD bufSize = sizeof(DWORD);
    DWORD type;

    result = RegQueryValueEx(key_, valueName, NULL, 
                             &type, (BYTE *)&tmp, &bufSize);
    if (result == ERROR_SUCCESS && type == REG_DWORD) {
        *value = tmp != 0;
        return true;
    } else {
        return false;
    }
 }

bool 
HippoRegKey::loadLong(const WCHAR *valueName,
                      long        *value)
 {
    if (!key_)
        return false;

    long result;
    DWORD tmp;
    DWORD bufSize = sizeof(DWORD);
    DWORD type;

    result = RegQueryValueEx(key_, valueName, NULL, 
                             &type, (BYTE *)&tmp, &bufSize);
    if (result == ERROR_SUCCESS && type == REG_DWORD) {
        *value = (long)tmp;
        return true;
    } else {
        return false;
    }
 }
 
bool 
HippoRegKey::loadBinary(const WCHAR *valueName,
                        BYTE       **data,
                        DWORD       *dataLength)
{
    if (!key_)
        return false;

    long result;
    DWORD bufSize = 0;
    DWORD type;
    BYTE *buf;

    result = RegQueryValueEx(key_, valueName, NULL, 
                             &type, NULL, &bufSize);
    if ((result != ERROR_SUCCESS && result != ERROR_MORE_DATA) || type != REG_BINARY)
        return false;

    buf = (BYTE *)CoTaskMemAlloc(bufSize);
    if (!buf)
        return false;

    result = RegQueryValueEx(key_, valueName, NULL, 
        &type, buf, &bufSize);
    if (result != ERROR_SUCCESS || type != REG_BINARY) {
        CoTaskMemFree(buf);
        return false;
    }

    *data = buf;
    *dataLength = bufSize;

    return true;
}

bool 
HippoRegKey::saveString(const WCHAR *valueName, 
                        const WCHAR *str)
{
    if (!key_)
        return false;

    if (str) {
        size_t len = wcslen(str);
        if (sizeof(WCHAR) * (len + 1) > UINT_MAX)
            return false;

        return RegSetValueEx(key_, valueName, NULL, REG_SZ,
                             (const BYTE *)str, (DWORD)sizeof(WCHAR) * (len + 1)) == ERROR_SUCCESS;
    } else {
        return RegDeleteValue(key_, valueName) == ERROR_SUCCESS;
    }
}

bool 
HippoRegKey::saveBool(const WCHAR *valueName, 
                      bool         value)
{
    if (!key_)
        return false;

    DWORD tmp = value;

    return RegSetValueEx(key_, valueName, NULL, REG_DWORD,
                         (const BYTE *)&tmp, sizeof(DWORD)) == ERROR_SUCCESS;

}

bool 
HippoRegKey::saveLong(const WCHAR *valueName, 
                      long         value)
{
    if (!key_)
        return false;

    DWORD tmp = (DWORD)value;

    return RegSetValueEx(key_, valueName, NULL, REG_DWORD,
                         (const BYTE *)&tmp, sizeof(DWORD)) == ERROR_SUCCESS;

}

bool 
HippoRegKey::saveBinary(const WCHAR *valueName,
                        const BYTE  *data,
                        const DWORD  dataLength)
{
    if (!key_)
        return false;

    if (data && dataLength > 0) {
        return RegSetValueEx(key_, valueName, NULL, REG_BINARY,
                             data, dataLength) == ERROR_SUCCESS;
    } else {
        return RegDeleteValue(key_, valueName) == ERROR_SUCCESS;
    }
}
