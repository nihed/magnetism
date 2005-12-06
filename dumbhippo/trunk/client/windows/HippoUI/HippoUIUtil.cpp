#include "StdAfx.h"
#include ".\hippouiutil.h"

void 
HippoUIUtil::encodeQueryString(HippoBSTR &url, HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues)
{
    url = L"?";
    bool first = TRUE;

    for (unsigned int i = 0; i < paramNames.length(); i++) {
        if (i > 0)
            url.Append(L"&");
        url.Append(paramNames[i]);
        url.Append(L"=");

        WCHAR encoded[1024] = {0}; 
        DWORD len = sizeof(encoded)/sizeof(encoded[0]);

        if (!SUCCEEDED (UrlEscape(paramValues[i], encoded, &len, URL_ESCAPE_UNSAFE | URL_ESCAPE_SEGMENT_ONLY)))
            return;
        url.Append(encoded);
    }
}

void 
HippoUIUtil::splitString(HippoBSTR str, WCHAR separator, HippoArray<HippoBSTR> &result) {
    WCHAR *strData = str.m_str;
    WCHAR *item = wcschr(strData, separator);

    while (item) {
        size_t count = item - strData;
        HippoBSTR subStr(count, L"");
        wcsncpy(subStr.m_str, item, count);
        result.append(subStr);
        strData = item + 1;
        item = wcschr(strData, separator);
    }
    result.append(HippoBSTR(strData));
}