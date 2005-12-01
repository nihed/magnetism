#pragma once

#include "stdafx.h"
#include <HippoUtil.h>

// Right now this is a collection of utility functions for IE, later
// it will likely morph into a generic embedding class.
class HippoIE
{
public:
    HippoIE(void);
    ~HippoIE(void);
    static HRESULT invokeJavascript(HippoPtr<IWebBrowser2> browser, WCHAR * funcName, VARIANT *invokeResult, int nargs, ...);
    static HRESULT invokeJavascript(HippoPtr<IWebBrowser2> browser, WCHAR * funcName, VARIANT *invokeResult, int nargs, va_list args);
};
