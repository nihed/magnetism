/* HippoRegKey: Utility class for loading or saving to a single registry key
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#ifdef BUILDING_HIPPO_UTIL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif

class DLLEXPORT HippoRegKey
{
public:
    HippoRegKey(HKEY         parentKey,
                const WCHAR *subkeyFormat,
                bool         writable,
                ...);
    ~HippoRegKey(void);

    bool loadString(const WCHAR *valueName,
                    BSTR        *str);
    bool loadBool(const WCHAR *valueName,
                  bool        *result);
    bool loadLong(const WCHAR *valueName,
                  long        *result);
    bool loadBinary(const WCHAR *valueName,
                    BYTE       **data,
                    DWORD       *dataLength);
    bool saveString(const WCHAR *valueName, 
                    const WCHAR *str);
    bool saveBool(const WCHAR *valueName, 
                  bool         value);
    bool saveLong(const WCHAR *valueName, 
                  long         value);
    bool saveBinary(const WCHAR *valueName,
                    const BYTE  *data,
                    const DWORD  dataLength);

private:
    HKEY key_;
};