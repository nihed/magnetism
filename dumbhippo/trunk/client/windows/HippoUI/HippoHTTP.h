#pragma once

#include "stdafx.h"
#include <wininet.h>

class HippoHTTPAsyncHandler
{
public:
    virtual void handleError(HRESULT result) = 0;
    virtual void handleBytesRead(void *responseData, long responseBytes) = 0;
    virtual void handleComplete(void *responseData, long responseBytes) = 0;
    
};

class HippoHTTP
{
public:
    HippoHTTP(void);
    ~HippoHTTP(void);

    void doGet(WCHAR *url, WCHAR *contentType, HippoHTTPAsyncHandler *handler);
    void doPost(WCHAR *url, WCHAR *contentType, void *requestInput, long len, HippoHTTPAsyncHandler *handler);

    void shutdown(void);
private:
    HINTERNET inetHandle_;

    bool parseURL(WCHAR *url, BSTR *host, INTERNET_PORT *port, BSTR *target);
    void doAsync(WCHAR *host, INTERNET_PORT port, WCHAR *op, WCHAR *target, WCHAR *contentType, void *requestInput, long len, HippoHTTPAsyncHandler *handler);
};
