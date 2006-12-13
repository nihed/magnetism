/* HippoHTTP.cpp: Wrapper class around WinINET for HTTP operation
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"
#include "HippoHTTP.h"
#include <glib.h>
#include <wininet.h>
#include "HippoUIUtil.h"
#include <HippoUtil.h>
#include "HippoLogWindow.h"

class HippoHTTPContext
{
public:
    enum ResponseState {
        RESPONSE_STATE_STARTING,
        RESPONSE_STATE_READING,
        RESPONSE_STATE_DONE,
        RESPONSE_STATE_ERROR
    };

    HINTERNET connectionHandle_;
    HINTERNET requestOpenHandle_;

    HippoBSTR op_;
    HippoBSTR target_;
    HippoBSTR contentType_;

    void *inputData_;
    long inputLen_;

    long responseSize_;
    HippoBSTR responseContentType_;
    HippoBSTR responseContentCharset_;

    // -----------------------------------------------------------------------------
    // These following items are shared between the read thread and response idle
    CRITICAL_SECTION criticalSection_;

    ResponseState responseState_;
    bool seenResponseSize_;
    bool seenResponseContentType_;
    long responseBytesRead_;
    long responseBytesSeen_;
    unsigned responseIdle_;
    HRESULT responseError_;

    // -----------------------------------------------------------------------------

    void *responseBuffer_;
    long responseBufferSize_;

    HippoHTTPAsyncHandler *handler_;

    ~HippoHTTPContext() {
        DeleteCriticalSection(&criticalSection_);
        if (responseBuffer_)
            free(responseBuffer_);
    }

    void ensureResponseIdle();
    void doResponseIdle();
    void enqueueError(HRESULT error);
    void readData();
    void closeHandles();
};


void
HippoHTTPContext::doResponseIdle()
{
    HippoHTTPContext::ResponseState state;

    EnterCriticalSection(&criticalSection_);
    state = responseState_;
    bool oldSeenResponseSize = seenResponseSize_;
    seenResponseSize_ = true;
    long oldResponseBytesSeen = responseBytesSeen_;
    long newResponseBytesSeen = responseBytesSeen_ = responseBytesRead_;
    responseIdle_ = 0;
    LeaveCriticalSection(&criticalSection_);

    if (state == RESPONSE_STATE_ERROR) {
        handler_->handleError(responseError_);
        delete this;
        return;
    }
    
    if (!oldSeenResponseSize) {
        handler_->handleGotSize(responseSize_);
        handler_->handleContentType(responseContentType_, responseContentCharset_);
    }

    if (oldResponseBytesSeen != newResponseBytesSeen) {
        handler_->handleBytesRead((char *)responseBuffer_ + oldResponseBytesSeen, 
                                  newResponseBytesSeen - oldResponseBytesSeen);
    }

    if (state == RESPONSE_STATE_DONE) {
        handler_->handleComplete(responseBuffer_, responseBytesRead_);
        delete this;
    }
}

static gboolean
responseIdle(gpointer data)
{
    HippoHTTPContext *ctx = (HippoHTTPContext*)data;

    ctx->doResponseIdle();

    return FALSE;
}

void
HippoHTTPContext::ensureResponseIdle()
{
    if (!responseIdle_)
        responseIdle_ = g_idle_add (responseIdle, this);
}

void
HippoHTTPContext::closeHandles()
{
    InternetCloseHandle(requestOpenHandle_);
    requestOpenHandle_ = NULL;
    InternetCloseHandle(connectionHandle_);
    connectionHandle_ = NULL;
}

void
HippoHTTPContext::enqueueError(HRESULT error)
{
    closeHandles();

    EnterCriticalSection(&criticalSection_);
    responseError_ = error;
    responseState_ = RESPONSE_STATE_ERROR;
    ensureResponseIdle();
    LeaveCriticalSection(&criticalSection_);
}

void
HippoHTTPContext::readData()
{
    if (!requestOpenHandle_) {
        hippoDebugLogW(L"readData called with closed handle");
        return;
    }

    while (responseSize_ == -1 || (responseSize_ - responseBytesRead_) != 0) {
        DWORD bytesRead;

        // Things seem to work badly if we call neglect to call InternetQueryDataAvailable() 
        // before trying to read
        DWORD bytesAvailable = 0;
        if (!InternetQueryDataAvailable(requestOpenHandle_, &bytesAvailable, 0, 0)) {
            // ERROR_IO_PENDING means no data currently available; return and we'll get
            // called again later
            if (GetLastError() != ERROR_IO_PENDING)
                enqueueError(GetLastError());

            return;
        }

        EnterCriticalSection(&criticalSection_);

        DWORD toRead;
        if (responseSize_ > 0) {
            toRead = responseSize_ - responseBytesRead_;
        } else {
            toRead = 4096;
        }
        void *readLocation;
        HRESULT allocResult;

        if (toRead > bytesAvailable)
            toRead = bytesAvailable;

        if (((long)toRead + responseBytesRead_) > responseBufferSize_) {
            responseBuffer_ = realloc(responseBuffer_, responseBufferSize_ *= 2);
            allocResult = GetLastError();
        }

        readLocation = ((char*)responseBuffer_) + responseBytesRead_;

        LeaveCriticalSection(&criticalSection_);

        if (responseBuffer_ == NULL) {
            enqueueError(allocResult);
            return;
        }

        if (!InternetReadFile(requestOpenHandle_,
                              readLocation, toRead, &bytesRead)) 
        {    
            // ERROR_IO_PENDING really shouldn't happen here since 
            // InternetQueryDataAvailable told us there was data available
            // before.
            if (GetLastError() != ERROR_IO_PENDING)
                enqueueError(GetLastError());

            return;
        } else {
            EnterCriticalSection(&criticalSection_);
            char *current = (char*)responseBuffer_;
            responseBytesRead_ += bytesRead;
            ensureResponseIdle();
            LeaveCriticalSection(&criticalSection_);

            if (bytesRead == 0)
                break;
        }
    }

    closeHandles();

    EnterCriticalSection(&criticalSection_);
    if (responseSize_ < 0 || responseSize_ - responseBytesRead_ != 0) { 
        // Missing or invalid Content-Length
        responseSize_ = responseBytesRead_;
    }
    responseState_ = HippoHTTPContext::RESPONSE_STATE_DONE;
    ensureResponseIdle();
    LeaveCriticalSection(&criticalSection_);

    return;
}


static void
handleCompleteRequest(HINTERNET ictx, HippoHTTPContext *ctx, DWORD status, LPVOID statusInfo, 
                      DWORD statusLength)
{
    if (ctx->responseBuffer_ == NULL) {
        DWORD statusCode;
        DWORD statusCodeSize = sizeof(statusCode);

        if (!HttpQueryInfo(ctx->requestOpenHandle_, HTTP_QUERY_STATUS_CODE | HTTP_QUERY_FLAG_NUMBER,
            (void *)&statusCode, &statusCodeSize, 0) || statusCode != 200)
        {
            ctx->enqueueError(E_FAIL);
            return;
        }

        WCHAR responseSize[80];
        static const long MAX_SIZE = 16 * 1024 * 1024;
        DWORD responseDatumSize = sizeof(responseSize);
        WCHAR responseContentType[120];
        DWORD responseContentTypeSize = sizeof(responseContentType);

        if (!HttpQueryInfo(ctx->requestOpenHandle_, HTTP_QUERY_CONTENT_LENGTH,
            &responseSize, &responseDatumSize, 
            NULL)) 
        {
            ctx->responseSize_ = -1;
            ctx->responseBufferSize_ = 4096;
        } else {
            ctx->responseSize_ = wcstoul(responseSize, NULL, 10);
            if (ctx->responseSize_ < 0)
                ctx->responseSize_ = 0;
            else if (ctx->responseSize_ > MAX_SIZE)
                ctx->responseSize_ = MAX_SIZE;
            ctx->responseBufferSize_ = ctx->responseSize_;
        }

        ctx->responseContentType_ = NULL;
        ctx->responseContentCharset_ = NULL;
        if (HttpQueryInfo(ctx->requestOpenHandle_, HTTP_QUERY_CONTENT_TYPE,
            &responseContentType, &responseContentTypeSize, 
            NULL))
        {
            WCHAR *defaultCharset = L"UTF-8";
            WCHAR *charsetDelim = L"; charset=";
            WCHAR *contentPtr = wcsstr(responseContentType, charsetDelim);
            if (contentPtr) {
                ctx->responseContentType_ = HippoBSTR(contentPtr - responseContentType, responseContentType);
                ctx->responseContentCharset_ = contentPtr + wcslen(charsetDelim) + 1;
            } else {
                ctx->responseContentType_ = responseContentType;
                ctx->responseContentCharset_ = defaultCharset;
            }
        }
        ctx->responseBytesRead_ = 0;
        ctx->responseBuffer_ = malloc (ctx->responseBufferSize_);
        ctx->responseState_ = HippoHTTPContext::RESPONSE_STATE_READING;
        EnterCriticalSection(&(ctx->criticalSection_));
        ctx->ensureResponseIdle(); // To report that we have the size
        LeaveCriticalSection(&(ctx->criticalSection_));
    }

    ctx->readData();
}

static void CALLBACK
asyncStatusUpdate(HINTERNET ictx, DWORD_PTR uctx, DWORD status, LPVOID statusInfo, 
                  DWORD statusLength)
{
    HippoHTTPContext *ctx = (HippoHTTPContext*)uctx;

    switch (status) {
        case INTERNET_STATUS_CLOSING_CONNECTION:
            break;
        case INTERNET_STATUS_CONNECTED_TO_SERVER:
            break;
        case INTERNET_STATUS_CONNECTING_TO_SERVER:
            break;
        case INTERNET_STATUS_CONNECTION_CLOSED:
            break;
        case INTERNET_STATUS_HANDLE_CLOSING:
            break;
        case INTERNET_STATUS_HANDLE_CREATED:
            break;
        case INTERNET_STATUS_INTERMEDIATE_RESPONSE:
            break;
        case INTERNET_STATUS_NAME_RESOLVED:
            break;
        case INTERNET_STATUS_RECEIVING_RESPONSE:
            break;
        case INTERNET_STATUS_RESPONSE_RECEIVED:
            break;
        case INTERNET_STATUS_REDIRECT:
            break;
        case INTERNET_STATUS_REQUEST_COMPLETE:
            handleCompleteRequest(ictx, ctx, status, statusInfo, statusLength);
            break;
        case INTERNET_STATUS_REQUEST_SENT:
            break;
        case INTERNET_STATUS_RESOLVING_NAME:
            break;
        case INTERNET_STATUS_SENDING_REQUEST:
            break;
        case INTERNET_STATUS_STATE_CHANGE:
            break;
    }
}

HippoHTTP::HippoHTTP(void)
{
    inetHandle_ = InternetOpen(L"Mugshot Client/1.0", INTERNET_OPEN_TYPE_PRECONFIG,
                               NULL, NULL, INTERNET_FLAG_ASYNC);
    InternetSetStatusCallback(inetHandle_, asyncStatusUpdate);
}

HippoHTTP::~HippoHTTP(void)
{
    shutdown();
}

void
HippoHTTP::shutdown(void)
{
    InternetCloseHandle(inetHandle_);
}

bool
HippoHTTP::parseURL(WCHAR         *url, 
                    BSTR          *hostReturn,
                    INTERNET_PORT *portReturn, 
                    BSTR          *targetReturn)
{
    URL_COMPONENTS components;
    ZeroMemory(&components, sizeof(components));
    components.dwStructSize = sizeof(components);

    // The case where lpszHostName is NULL and dwHostNameLength is non-0 means
    // to return pointers into the passed in URL along with lengths. The 
    // specific non-zero value is irrelevant
    components.dwHostNameLength = 1;
    components.dwUserNameLength = 1;
    components.dwPasswordLength = 1;
    components.dwUrlPathLength = 1;
    components.dwExtraInfoLength = 1;

    if (!InternetCrackUrl(url, 0, 0, &components))
        return false;

    if (components.nScheme != INTERNET_SCHEME_HTTP && components.nScheme != INTERNET_SCHEME_HTTPS)
        return false;

    HippoBSTR host(components.dwHostNameLength, components.lpszHostName);
    host.CopyTo(hostReturn);

    *portReturn = components.nPort;

    // We don't care about the division between the path and the query string
    HippoBSTR target(components.lpszUrlPath);
    target.CopyTo(targetReturn);

    return true;
}

void
HippoHTTP::doGet(WCHAR                 *url, 
                 bool                   useCache,
                 HippoHTTPAsyncHandler *handler)
{
    HippoBSTR host;
    INTERNET_PORT port;
    HippoBSTR target;

    if (!parseURL(url, &host, &port, &target))
        return;

    doAsync(host, port, L"GET", target, useCache, NULL, NULL, 0, handler);
}

void
HippoHTTP::doPost(WCHAR                 *url, 
                  WCHAR                 *contentType, 
                  void                  *requestInput, 
                  long                   len, 
                  HippoHTTPAsyncHandler *handler)
{
    HippoBSTR host;
    INTERNET_PORT port;
    HippoBSTR target;

    if (!parseURL(url, &host, &port, &target))
        return;

    doAsync(host, port, L"POST", target, false, contentType, requestInput, len, handler);
}

bool
HippoHTTP::writeAllToStream(IStream *stream, void *data, ULONG bytesTotal, HippoHTTPAsyncHandler *handler)
{
    ULONG bytesWritten = 0;
    HRESULT res;
    while (((res = stream->Write(((char*)data) + bytesWritten, bytesTotal, &bytesWritten)) == S_OK)
        && bytesWritten > 0) {
        bytesTotal -= bytesWritten;
    }
    if (FAILED(res)) {
        handler->handleError(res);
        return false;
    }
    return true;
}

bool
HippoHTTP::writeToStreamAsUTF8(IStream *stream,
                               WCHAR   *str,
                               HippoHTTPAsyncHandler *handler)
{

    HippoUStr utf(str);
    long bytesTotal = (long) strlen(utf.c_str());
    bool ret = writeAllToStream(stream, (void *)utf.c_str(), bytesTotal, handler);
    return ret;
}

bool
HippoHTTP::writeToStreamAsUTF8Printf(IStream *stream,
                                     WCHAR   *fmt,
                                     HippoHTTPAsyncHandler *handler,
                                     ...)
{
    va_list args;
    va_start(args, handler);
    long bufSize = 1024;
    WCHAR *buf = (WCHAR*)g_malloc(bufSize);
    HRESULT res;

    while ((res = StringCchVPrintf(buf, bufSize/2, fmt, args)) == STRSAFE_E_INSUFFICIENT_BUFFER) {
        bufSize *= 2;
        buf = (WCHAR*)g_realloc(buf, bufSize);
    }
    bool ret = writeToStreamAsUTF8(stream, buf, handler);

    g_free (buf);
    va_end(args);
    
    return ret;
}

void
HippoHTTP::doMultipartFormPost(WCHAR     *url,
                               HippoHTTPAsyncHandler *handler,
                               WCHAR     *name,
                               bool       binary,
                               void      *data,
                               ...)
{
    va_list args;

    va_start (args, data);

    HippoBSTR boundary(L"dhform----------boundary--");
    int suffix = rand();
    WCHAR suffixBuf[120];

    StringCchPrintf(suffixBuf, sizeof(suffixBuf)/sizeof(suffixBuf[0]), L"%d", suffix);
    boundary.Append(suffixBuf);

    IStream *formBuf;
    CreateStreamOnHGlobal(NULL,TRUE,&formBuf);
    
    if (!writeToStreamAsUTF8Printf(formBuf, L"--%s", handler, boundary))
        return;
    if (!writeToStreamAsUTF8(formBuf, L"\r\n", handler))
        return;
    while (name) {    
        if (!writeToStreamAsUTF8(formBuf, L"Content-Disposition: form-data; name=\"", handler))
            break;
        if (!writeToStreamAsUTF8(formBuf, name, handler))
            break;
        if (!writeToStreamAsUTF8(formBuf, L"\"", handler))
            break;
        if (binary) {
            const ULONG dataLen = va_arg(args, ULONG);
            const WCHAR *contentType = va_arg(args, WCHAR*);
            const WCHAR *filename = va_arg(args, WCHAR*);

            if (!writeToStreamAsUTF8Printf(formBuf, L"; filename=\"%s\"\r\nContent-Type: %s\r\n\r\n", handler,
                                           filename, contentType))
                break;
            if (!writeAllToStream(formBuf, data, dataLen, handler))
                break;

        } else {
            if (!writeToStreamAsUTF8Printf(formBuf, L"\r\n\r\n%s", handler, (WCHAR*)data))
                break;
        }
        if (!writeToStreamAsUTF8Printf(formBuf, L"\r\n--%s", handler, boundary))
            break;
        name = va_arg(args, WCHAR *);
        if (name) {
            binary = va_arg(args, bool);
            data = va_arg(args, void *);
        } else {
            if (!writeToStreamAsUTF8(formBuf, L"--", handler))
                break;
        }
        if (!writeToStreamAsUTF8(formBuf, L"\r\n", handler))
            break;
    }
    
    va_end (args);

    WCHAR contentTypeBuf[1024];
    StringCchPrintfW(contentTypeBuf, sizeof(contentTypeBuf)/sizeof(contentTypeBuf[0]),
                     L"multipart/form-data; boundary=%s", boundary);
    void *buf;
    ULONG size;
    {
        HGLOBAL hg = NULL;
        STATSTG formBufStats;
        HRESULT res;

        GetHGlobalFromStream(formBuf, &hg);
        buf = GlobalLock(hg);
        res = formBuf->Stat(&formBufStats, 0);
        if (FAILED(res)) {
            handler->handleError(res);
            return;
        }
        size = (ULONG) formBufStats.cbSize.LowPart;
    }

    doPost(url, contentTypeBuf, buf, size, handler);
}

void
HippoHTTP::doAsync(WCHAR                 *host, 
                   INTERNET_PORT          port, 
                   WCHAR                 *op,
                   WCHAR                 *target, 
                   bool                   useCache,
                   WCHAR                 *contentType, 
                   void                  *requestInput,
                   long                   len, 
                   HippoHTTPAsyncHandler *handler)
{
    HippoHTTPContext *context;

    context = new HippoHTTPContext;
    ZeroMemory(context, sizeof(*context));
    context->handler_ = handler;
    context->op_ = op;
    context->target_ = target;
    context->contentType_ = contentType;
    context->responseState_ = HippoHTTPContext::RESPONSE_STATE_STARTING;
    context->responseSize_ = -1;
    context->seenResponseSize_ = false;
    context->responseBuffer_ = NULL;
    context->inputData_ = requestInput;
    context->inputLen_ = len;
    InitializeCriticalSection(&(context->criticalSection_));
    
    context->connectionHandle_ = InternetConnect(inetHandle_, host, port, NULL, NULL, 
                                                 INTERNET_SERVICE_HTTP, 0, (DWORD_PTR) context);
    if (!context->connectionHandle_) {
        context->enqueueError(GetLastError());
        return;
    }

    // Asking the user for authentication would be weird in most cases, and shouldn't
    // happen anyways. We'd rather just get an error.
    DWORD flags = INTERNET_FLAG_NO_AUTH | INTERNET_FLAG_NO_UI;

    // RELOAD means to ignore any cached content; NO_CACHE_WRITE means don't cache
    // the result either. We use these when downloading new versions of the client;
    // since caching isn't useful, and might conceivably cause a bad download
    // to be sticky and not work on the next retry.
    if (!useCache) 
        flags |= INTERNET_FLAG_RELOAD | INTERNET_FLAG_NO_CACHE_WRITE;

    context->requestOpenHandle_ = HttpOpenRequest(context->connectionHandle_, context->op_, context->target_, 
                                                  NULL, NULL, NULL,
                                                  flags, (DWORD_PTR) context);
    if (!context->requestOpenHandle_) {
        context->enqueueError(GetLastError());
        return;
    }

    if (context->contentType_) {
        WCHAR buf[1024];    
        StringCchPrintf(buf, sizeof(buf)/sizeof(buf[0]), L"Content-Type: %s\r\n", context->contentType_);
        HttpAddRequestHeaders(context->requestOpenHandle_, buf, -1, HTTP_ADDREQ_FLAG_ADD_IF_NEW);
    }

    if (!HttpSendRequest(context->requestOpenHandle_, NULL, 0, context->inputData_, context->inputLen_)) {
        // Normally when our request is sent to the web, we get an IO_PENDING error here
        if (GetLastError() != ERROR_IO_PENDING)
            context->enqueueError(GetLastError());

        return;
    }

    // If HttpSendRequest returned true, that means that the data is being served from the
    // local cache, so go straight on to reading
    handleCompleteRequest(context->requestOpenHandle_, context, 
                          INTERNET_STATUS_REQUEST_COMPLETE, NULL, 0); // last two parameters don't matter
}
