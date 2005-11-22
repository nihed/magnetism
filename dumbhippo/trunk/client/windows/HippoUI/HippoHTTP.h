#pragma once

#include "stdafx.h"
#include <wininet.h>

class HippoHTTPAsyncHandler
{
public:
    virtual void handleError(HRESULT result) = 0;
    virtual void handleComplete(BSTR *responseText) = 0;
};

class HippoHTTP
{
public:
    HippoHTTP(void);
    ~HippoHTTP(void);

    void doAsync(WCHAR *host, INTERNET_PORT port, WCHAR *op, WCHAR *target, WCHAR *contentType, void *requestInput, long len, HippoHTTPAsyncHandler *handler, void *data);

    void shutdown(void);
private:
    HINTERNET inetHandle_;
};
