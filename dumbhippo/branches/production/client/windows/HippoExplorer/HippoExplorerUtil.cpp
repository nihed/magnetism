/* HippoExplorerUtil.cpp: Common utility functions for HippoExplorer.h
 *
 * Copyright Red Hat, Inc. 2006
 */

#include "stdafx.h"
#include "HippoExplorerUtil.h"

// "SUFFIX" by itself or "<foo>.SUFFIX" will be allowed. We might want to consider 
// changing things so that the control can only be used from *exactly* the web
// servers specified in the preferences. (You'd have to check for either the
// normal or the debug servers.)
static const WCHAR *ALLOWED_HOST_SUFFIXES[] = { L"dumbhippo.com", L"mugshot.org" };
static const int ALLOWED_HOST_SUFFIX_COUNT = sizeof(ALLOWED_HOST_SUFFIXES) / sizeof(ALLOWED_HOST_SUFFIXES[0]);

bool 
hippoVerifyGuid(const HippoBSTR &guid)
{
    WCHAR *p;

    // Contents are alphanumeric (we don't generate a,e,i,o,u,E,I,O,U in our
    // GUID's at the moment, but there is no harm in allowing them)
    for (p = guid.m_str; *p; p++) {
        if (!((*p >= '0' && *p <= '9') ||
              (*p >= 'A' && *p <= 'Z') ||
              (*p >= 'a' && *p <= 'z')))
            return false;
    }

    // Length is 14
    if (p - guid.m_str != 14) 
        return false;

    return true;
}

bool 
hippoIsOurServer(const HippoBSTR &host)
{
    unsigned int hostLength = host.Length();
    
    for (int i = 0; i < ALLOWED_HOST_SUFFIX_COUNT; i++) {
        const WCHAR *allowedHostSuffix = ALLOWED_HOST_SUFFIXES[i];
        size_t allowedHostLength = wcslen(allowedHostSuffix);
        if (hostLength < allowedHostLength)
            continue;

        // check for "SUFFIX" or "<foo>.SUFFIX"
        if (wcsncmp(host.m_str + hostLength - allowedHostLength,
                    allowedHostSuffix,
                    allowedHostLength) != 0)
            continue;

        if (hostLength > allowedHostLength && 
            *(host.m_str + hostLength - allowedHostLength - 1) != '.')
            continue;

        return true;
    }

    return false;
}
