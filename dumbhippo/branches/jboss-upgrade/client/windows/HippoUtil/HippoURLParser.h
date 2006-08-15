/* HippoURLParser.cpp: Convenient wrapper around InternetCrackUrl
 *
 * Copyright Red Hat, Inc. 2006
 */
#pragma once

#include <HippoUtil.h>
#include <wininet.h>

#ifdef BUILDING_HIPPO_UTIL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif

class DLLEXPORT HippoURLParser {
public:
    HippoURLParser(const HippoBSTR &url);

    bool ok() { return ok_; }
    INTERNET_SCHEME getScheme();
    INTERNET_PORT getPort();
    HippoBSTR getHostName();
    HippoBSTR getUrlPath();
    HippoBSTR getExtraInfo();

private:
    bool ok_;
    URL_COMPONENTS components_;
};
