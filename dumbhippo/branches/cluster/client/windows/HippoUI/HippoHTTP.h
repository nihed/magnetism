/* HippoHTTP.h: Wrapper class around WinINET for HTTP operation
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <wininet.h>
#include <new>

class HippoHTTPAsyncHandler
{
public:
    virtual void handleError(HRESULT result) = 0;
    virtual void handleGotSize(long responseSize) { };
    virtual void handleContentType(WCHAR *mimetype, WCHAR *charset) { };
    virtual void handleBytesRead(void *responseData, long responseBytes) { };
    virtual void handleComplete(void *responseData, long responseBytes) = 0;
};

class HippoHTTP
{
public:
    HippoHTTP(void);
    ~HippoHTTP(void);

    void doGet(WCHAR *url, bool useCache, HippoHTTPAsyncHandler *handler);
    void doPost(WCHAR *url, WCHAR *contentType, void *requestInput, long len, HippoHTTPAsyncHandler *handler);
    void doMultipartFormPost(WCHAR *url, HippoHTTPAsyncHandler *handler, WCHAR *name, bool binary, void *data, ...);

    void shutdown(void);
private:
    HINTERNET inetHandle_;

    bool parseURL(WCHAR *url, BSTR *host, INTERNET_PORT *port, BSTR *target) throw (std::bad_alloc);
    bool writeAllToStream(IStream *stream, void *data, ULONG bytesTotal, HippoHTTPAsyncHandler *handler);
    bool writeToStreamAsUTF8(IStream *stream,
                             WCHAR   *str,
                             HippoHTTPAsyncHandler *handler);
    bool writeToStreamAsUTF8Printf(IStream *stream,
                                     WCHAR   *fmt,
                                     HippoHTTPAsyncHandler *handler,
                                     ...);
    void doAsync(WCHAR                 *host, 
                 INTERNET_PORT          port, 
                 WCHAR                 *op, 
                 WCHAR                 *target, 
                 bool                   useCache, 
                 WCHAR                 *contentType, 
                 void                  *requestInput, 
                 long                   len, 
                 HippoHTTPAsyncHandler *handler);
};
