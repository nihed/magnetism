/* HippoUIUtil.h: Some useful string and URL manipulation functions
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include "stdafx.h"
#include <HippoUtil.h>
#include <HippoArray.h>

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
