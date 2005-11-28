/* HippoRegKey: Utility class for loading or saving to a single registry key
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx.h"
#include <limits.h>
#include "HippoUtil.h"
#include "HippoRegKey.h"

HippoRegKey::HippoRegKey(HKEY         parentKey,
                         const WCHAR *subKey,
                         bool         writable)
{
    key_ = NULL;
    if (writable) {
        RegCreateKeyEx(HKEY_CURRENT_USER, 
                       subKey,
                       NULL, NULL, 
                       REG_OPTION_NON_VOLATILE, KEY_READ | KEY_WRITE, NULL,
                       &key_, NULL);
    } else {
        RegOpenKeyEx(HKEY_CURRENT_USER, 
                     subKey,
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
HippoRegKey::saveString(const WCHAR *valueName, 
                        BSTR         str)
{
    if (!key_)
        return false;

    if (str) {
        unsigned int len = ::SysStringLen(str);
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
