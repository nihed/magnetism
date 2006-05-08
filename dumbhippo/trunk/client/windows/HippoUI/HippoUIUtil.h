/* HippoUIUtil.h: Some useful string and URL manipulation functions
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include "stdafx.h"
#include <HippoUtil.h>
#include <HippoArray.h>
#include <glib.h>

class HippoUIUtil
{
public:
    /**
     * Append a query string to an url.
     * @param url         the base url; will be modified by appending the query string
     * @param paramNames  the parameter names to append
     * @param paramValues the parameter values to append; must correspond one-to-one with paramNames
     */
    static void encodeQueryString(HippoBSTR                   &url, 
                                  const HippoArray<HippoBSTR> &paramNames, 
                                  const HippoArray<HippoBSTR> &paramValues);
    /**
     * Splits a string on a separator character; if there are N occurrences
     * of the separator in the string, the result will have N+1 items.
     *
     * @param str       the string to split
     * @param separator the separator character
     * @param result    an array into which to store the result; the results are simply
     *                  appended to this without clearing it before hand
     */
    static void splitString(const HippoBSTR       &str, 
                            WCHAR                  separator, 
                            HippoArray<HippoBSTR> &result);
};

// Basically just a wrapper for g_utf16_to_utf8(), think three times before extending it
class HippoUStr 
{
public:
    HippoUStr(const HippoBSTR &bstr) {
        if (bstr.m_str)       
            str = g_utf16_to_utf8((gunichar2 *)bstr.m_str, SysStringLen(bstr.m_str), NULL, NULL, NULL);
        else
            str = NULL;
    }

    HippoUStr(WCHAR *wstr) {
        if (wstr)
            str = g_utf16_to_utf8((gunichar2 *)wstr, -1, NULL, NULL, NULL);
        else
            str = NULL;
    }

    HippoUStr(WCHAR *wstr, int len) {
        if (wstr)
            str = g_utf16_to_utf8((gunichar2 *)wstr, len, NULL, NULL, NULL);
        else
            str = NULL;
    }

    ~HippoUStr() {
        g_free(str);
    }

    const char *c_str() const {
        return str;
    }

    // get a g_malloc allocated string and unset this one;
    // must be g_free'd 
    char *steal() {
        char *tmp = str;
        str = NULL;
        return tmp;
    }

private:
    HippoUStr(const HippoUStr &other) {}
    HippoUStr operator=(const HippoUStr &other) {}

    char *str;
};