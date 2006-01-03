/* HippoUIUtil.cpp: Some useful string and URL manipulation functions
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "StdAfx.h"
#include "HippoUIUtil.h"

// The following is adapted from glib/glib/gutils.c:g_escape_uri_string()
// Copyright 2000, Red Hat, Inc. Licensed under the terms of the GNU
// Lesser General Public License.
//
// We don't use UrlEscape() because that can't be made to escape '+',
// and Java interprets + as ' ' in a query string.

typedef enum {
    UNSAFE_ALL        = 0x1,  /* Escape all unsafe characters   */
    UNSAFE_ALLOW_PLUS = 0x2,  /* Allows '+'  */
    UNSAFE_PATH       = 0x8,  /* Allows '/', '&', '=', ':', '@', '+', '$' and ',' */
    UNSAFE_HOST       = 0x10, /* Allows '/' and ':' and '@' */
    UNSAFE_SLASHES    = 0x20  /* Allows all characters except for '/' and '%' */
} UnsafeCharacterSet;

static const unsigned char acceptable[96] = {
    /* A table of the ASCII chars from space (32) to DEL (127) */
    /*      !    "    #    $    %    &    '    (    )    *    +    ,    -    .    / */ 
    0x00,0x3F,0x20,0x20,0x28,0x00,0x2C,0x3F,0x3F,0x3F,0x3F,0x2A,0x28,0x3F,0x3F,0x1C,
    /* 0    1    2    3    4    5    6    7    8    9    :    ;    <    =    >    ? */
    0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x38,0x20,0x20,0x2C,0x20,0x20,
    /* @    A    B    C    D    E    F    G    H    I    J    K    L    M    N    O */
    0x38,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
    /* P    Q    R    S    T    U    V    W    X    Y    Z    [    \    ]    ^    _ */
    0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x20,0x20,0x20,0x20,0x3F,
    /* `    a    b    c    d    e    f    g    h    i    j    k    l    m    n    o */
    0x20,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
    /* p    q    r    s    t    u    v    w    x    y    z    {    |    }    ~  DEL */
    0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x20,0x20,0x20,0x3F,0x20
};

static const WCHAR hex[] = L"0123456789ABCDEF";

/* Note: this doesn't take into account the different forms of escaping that
 * are needed for different parts of an URL; see RFC-2396 */
static WCHAR *
escapeUriString (const WCHAR        *string, 
                 UnsafeCharacterSet  mask)
{
#define ACCEPTABLE(a) ((a)>=32 && (a)<128 && (acceptable[(a)-32] & use_mask))

    const WCHAR *p;
    WCHAR *q;
    WCHAR *result;
    int c;
    int unacceptable;
    UnsafeCharacterSet use_mask;

    unacceptable = 0;
    use_mask = mask;
    for (p = string; *p != '\0'; p++) {
        c = (unsigned char) *p;
        if (!ACCEPTABLE(c)) 
            unacceptable++;
    }

    result = (WCHAR *)malloc (sizeof(WCHAR) * (p - string + unacceptable * 2 + 1));
    if (!result)
        return NULL;

    use_mask = mask;
    for (q = result, p = string; *p != '\0'; p++) {
        c = (unsigned char) *p;

        if (!ACCEPTABLE(c))
        {
            *q++ = '%'; /* means hex coming */
            *q++ = hex[c >> 4];
            *q++ = hex[c & 15];
        }
        else
            *q++ = *p;
    }

    *q = '\0';

    return result;
}

void 
HippoUIUtil::encodeQueryString(HippoBSTR                   &url, 
                               const HippoArray<HippoBSTR> &paramNames, 
                               const HippoArray<HippoBSTR> &paramValues)
{
    url = L"?";
    bool first = TRUE;

    assert(paramNames.length() == paramValues.length());

    for (unsigned int i = 0; i < paramNames.length(); i++) {
        WCHAR *encoded = escapeUriString(paramValues[i].m_str, UNSAFE_ALL);
        if (!encoded)
            continue;

        if (i > 0)
            url.Append(L"&");
        url.Append(paramNames[i]);
        url.Append(L"=");
        url.Append(encoded);

        free(encoded);
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
