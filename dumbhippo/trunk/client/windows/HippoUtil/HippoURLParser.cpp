#include "stdafx.h"
#include <HippoURLParser.h>

HippoURLParser::HippoURLParser(const HippoBSTR &url)
{
    ZeroMemory(&components_, sizeof(components_));
    components_.dwStructSize = sizeof(components_);

    // The case where lpszHostName is NULL and dwHostNameLength is non-0 means
    // to return pointers into the passed in URL along with lengths. The 
    // specific non-zero value is irrelevant
    components_.dwHostNameLength = 1;
    components_.dwUserNameLength = 1;
    components_.dwPasswordLength = 1;
    components_.dwUrlPathLength = 1;
    components_.dwExtraInfoLength = 1;

    ok_ = !!InternetCrackUrl(url.m_str, 0, 0, &components_);
}

INTERNET_SCHEME
HippoURLParser::getScheme()
{
    if (!ok_)
        return INTERNET_SCHEME_UNKNOWN;

    return components_.nScheme;
}

INTERNET_PORT
HippoURLParser::getPort()
{
    if (!ok_)
        return 80; // Random

    return components_.nPort;
}

HippoBSTR
HippoURLParser::getHostName()
{
    if (!ok_)
        return HippoBSTR();

    return HippoBSTR(components_.dwHostNameLength, components_.lpszHostName);
}

HippoBSTR
HippoURLParser::getUrlPath()
{
    if (!ok_)
        return HippoBSTR();

    return HippoBSTR(components_.dwUrlPathLength, components_.lpszUrlPath);
}

HippoBSTR
HippoURLParser::getExtraInfo()
{
    if (!ok_)
        return HippoBSTR();

    return HippoBSTR(components_.dwExtraInfoLength, components_.lpszExtraInfo);
}

