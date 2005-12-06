#pragma once

#include "stdafx.h"
#include <HippoUtil.h>
#include <HippoArray.h>

class HippoUIUtil
{
public:
    static void encodeQueryString(HippoBSTR &url, HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues);
    static void splitString(HippoBSTR str, WCHAR separator, HippoArray<HippoBSTR> &result);
};
