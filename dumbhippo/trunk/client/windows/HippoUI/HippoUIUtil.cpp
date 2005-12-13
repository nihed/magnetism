/* HippoUIUtil.cpp: Some useful string and URL manipulation functions
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "StdAfx.h"
#include "HippoUIUtil.h"

void 
HippoUIUtil::encodeQueryString(HippoBSTR                   &url, 
                               const HippoArray<HippoBSTR> &paramNames, 
                               const HippoArray<HippoBSTR> &paramValues)
{
    url = L"?";
    bool first = TRUE;

    assert(paramNames.length() == paramValues.length());

    for (unsigned int i = 0; i < paramNames.length(); i++) {
        if (i > 0)
            url.Append(L"&");
        url.Append(paramNames[i]);
        url.Append(L"=");

        WCHAR encoded[1024] = {0}; 
        DWORD len = sizeof(encoded)/sizeof(encoded[0]);

        if (!SUCCEEDED (UrlEscape(const_cast<HippoBSTR&>(paramValues[i]), encoded, &len, URL_ESCAPE_UNSAFE | URL_ESCAPE_SEGMENT_ONLY)))
            return;
        url.Append(encoded);
    }
}

void 
HippoUIUtil::splitString(const HippoBSTR       &str, 
                         WCHAR                  separator, 
                         HippoArray<HippoBSTR> &result) 
{
    const WCHAR *strData = str.m_str;
    
    while (true) {
        const WCHAR *item = wcschr(strData, separator);
        if (!item)
            break;

        unsigned len = item - strData > UINT_MAX ? UINT_MAX : item - strData;
        result.append(HippoBSTR(len, strData));
        strData = item + 1;
    }

    result.append(HippoBSTR(strData));
}
