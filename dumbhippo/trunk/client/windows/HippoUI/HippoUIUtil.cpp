/* HippoUIUtil.cpp: Some useful string and URL manipulation functions
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"
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

static const char hex[] = "0123456789ABCDEF";

/* Note: this doesn't take into account the different forms of escaping that
 * are needed for different parts of an URL; see RFC-2396 */
static BSTR
escapeUriString (const WCHAR        *string, 
                 UnsafeCharacterSet  mask)
{
#define ACCEPTABLE(a) ((a)>=32 && (a)<128 && (acceptable[(a)-32] & use_mask))

    const char *p;
    char *q;
    char *result;
    int c;
    int unacceptable;
    UnsafeCharacterSet use_mask;

    // Get UTF-8 version, we want to encode the parameters in that character set
    HippoUStr strUtf(string);

    unacceptable = 0;
    use_mask = mask;
    for (p = strUtf.c_str(); *p != '\0'; p++) {
        c = (unsigned char) *p;
        if (!ACCEPTABLE(c)) 
            unacceptable++;
    }

    result = (char *)malloc (sizeof(char) * (p - strUtf.c_str() + unacceptable * 2 + 1));
    if (!result)
        return NULL;

    use_mask = mask;
    for (q = result, p = strUtf.c_str(); *p != '\0'; p++) {
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

    HippoBSTR resultWide;
    resultWide.setUTF8(result);
    return ::SysAllocString(resultWide.m_str);
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
        BSTR encoded = escapeUriString(paramValues[i].m_str, UNSAFE_ALL);
        if (!encoded)
            continue;

        if (i > 0)
            url.Append(L"&");
        url.Append(paramNames[i]);
        url.Append(L"=");
        url.Append(encoded);

        ::SysFreeString(encoded);
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

// It seems to help Windows get a backtrace if we 
// trigger G_IS_OBJECT failure here instead of 
// inside g_object_ref
void
HippoGObjectRefcounter::ref(GObject *object)
{
    if (!G_IS_OBJECT(object))
        G_BREAKPOINT();
    g_object_ref(object);
}

void
HippoGObjectRefcounter::unref(GObject *object)
{
    if (!G_IS_OBJECT(object))
        G_BREAKPOINT();
    g_object_unref(object);
}

void
hippo_rectangle_from_rect(HippoRectangle *hippo_rect, RECT *windows_rect)
{
    hippo_rect->x = windows_rect->left;
    hippo_rect->y = windows_rect->top;
    hippo_rect->width = windows_rect->right - windows_rect->left;
    hippo_rect->height = windows_rect->bottom - windows_rect->top;
}

static bool
windowVisibleAtPoint(HWND  window,
                     POINT point)
{
    HWND atPoint = WindowFromPoint(point);
    if (!atPoint)
        return FALSE;

    // The root here isn't the X root window (the entire screen), but the toplevel window
    HWND root = GetAncestor(atPoint, GA_ROOT);

    return root == window;
}

bool 
hippoWindowIsOnscreen(HWND window)
{
    // We consider a window to be partially visible (and hence "onscreen") if
    // any of its four corners are visible

    RECT rect;

    if (!GetWindowRect(window, &rect))
        return FALSE;

    POINT point;

    point.x = rect.left;
    point.y = rect.top;
    if (windowVisibleAtPoint(window, point))
        return true;

    point.x = rect.right - 1;
    if (windowVisibleAtPoint(window, point))
        return true;
    
    point.y = rect.bottom - 1;
    if (windowVisibleAtPoint(window, point))
        return true;
     
    point.x = rect.left;
    if (windowVisibleAtPoint(window, point))
        return true;

    return FALSE;
}

bool
hippoWindowIsActive(HWND window)
{
    // The active window isn't actually what we are interested in here, since what we
    // want to know for the Mugshot browser window is whether when the user clicks on
    // the mugshot icon, we should raise the window or instead hide it. And when the
    // user clicks on the mugshot icon, the panel is the active window. So, what
    // we check here is if our window is the top window that isn't WS_EX_TOPMOST.
    //
    // In other uses, we could just do a simple check with GetWindowInfo(), but
    // we'll just always do the same thing for now.


    HWND top = GetTopWindow(NULL);
    while (top) {
        if (top == window)
            return TRUE;

        WINDOWINFO windowInfo;
        memset(&windowInfo, 0, sizeof(windowInfo));
        windowInfo.cbSize = sizeof(windowInfo);
        if (!GetWindowInfo(top, &windowInfo))
            return FALSE;

        if ((windowInfo.dwExStyle & WS_EX_TOPMOST) == 0)
            return FALSE;

        top = GetNextWindow(top, GW_HWNDNEXT);
    }

    return FALSE;
}
