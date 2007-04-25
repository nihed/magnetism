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

//
// NOTE: Thread-safety issues in this code are rather complex; please make
// sure you understand what's going on before you make changes. In outline:
//
//  - We start an asynchronous WinINet operation
//  - WinINet makes callbacks back to us *from random threads*. In these 
//    callbacks, we read data and progress through a state machine, defined
//    by the ResponseState enumeration.
//  - When there is anything we need to report to the handler (we've read
//    a chunk of data, an error has occurred, etc), we queue that to the
//    main thread using the "response idle"
//  - In the response idle, we look at the current state, report things
//    as necessary to the user, and record what we've reported so that
//    the next time the response idle is run, we don't report them again.
//  - The response idle is also responsible for freeing the HippoHTTPContext
//    object, but needs to wait to do this until the HINTERNET handles
//    are actually closed, which may occur asynchronously after we've
//    finished other handling.
//
// Since the WinINet callbacks and the response idle happen in different
// threads, member variables accessed from both, such as responseState_
// need to be protected with a critical section.
//
// Things are made a little more complex by the fact that we have to do
// things differently depending on whether the HTTP server sends us a size
// in advance. If we have the size in advance, we can allocate a buffer
// for the entire response that will be static and we can pass pointers
// to this buffer to the response handler. If we don't have the size
// in advance, we'll have to realloc the buffer bigger, so we have to wait
// until the end to report.
//
// (The point of the incremental reporting is for the upgrader; on a slow
// or intermittent connection, we might only download part of the download
// before we lose the connection, and we'd like to save that to disk so
// we can pick up where we left off. Since we don't have the code to 
// resume a partial download, it doesn't *actually* do us much good, but
// that's the idea, and the resume code could be added later.)
//

// Downloads are to a temporary memory buffer, so we need to limit the size
static const long MAX_DOWNLOAD_SIZE = 16 * 1024 * 1024; // 16MB

class HippoHTTPContext
{
public:
    enum ResponseState {
        RESPONSE_STATE_STARTING, // Waiting for a server response
        RESPONSE_STATE_HEADERS,  // Parsing information from the server headers
        RESPONSE_STATE_READING,  // Reading data
        RESPONSE_STATE_COMPLETE, // Got all the request data
        RESPONSE_STATE_ERROR,    // Got an error
        RESPONSE_STATE_DONE      // Everything done but freeing the context
    };

    HINTERNET connectionHandle_;
    HINTERNET requestOpenHandle_;

    HippoBSTR op_;
    HippoBSTR target_;
    HippoBSTR contentType_;

    void *inputData_;
    long inputLen_;

    // These are set before the first time the response idle is called and not changed,
    // so are safe to access from the response idle
    long responseSize_;
    HippoBSTR responseContentType_;
    HippoBSTR responseContentCharset_;

    // If responseSize_ >= 0, then responseBuffer_ is allocated in advance, and we can
    // incrementally feed stuff to the handler in the response idle. If responseSize_ == -1,
    // then responseBuffer_ may be realloc'ed, so we have to wait until the download
    // is complete before the response idle can read from responseBuffer_.
    void *responseBuffer_;
    long responseBufferSize_;

    // -----------------------------------------------------------------------------
    // These following items are shared between the read thread and response idle,
    //  and must be accessed from within the critical section.
    CRITICAL_SECTION criticalSection_;

    ResponseState responseState_;
    long responseBytesRead_;
    unsigned responseIdle_;
    HRESULT responseError_;

    // Used by the response idle to track what has been reported
    bool reportedHeaders_;
    long responseBytesSeen_;

    // -----------------------------------------------------------------------------

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
    void onHandleClose(HINTERNET handle);
};


void
HippoHTTPContext::doResponseIdle()
{
    HippoHTTPContext::ResponseState state;

    // We figure out what we need to do inside the critical section, then leave
    // the critical section to actually make the callbacks to the handler
    EnterCriticalSection(&criticalSection_);
    state = responseState_;

    // On error or receiving all the data, we're done with all our callbacks,
    // be we might still be called again later to delete the object after
    // the handles are asynchronously closed, so mark that state
    if (state == RESPONSE_STATE_ERROR || state == RESPONSE_STATE_COMPLETE)
        responseState_ = RESPONSE_STATE_DONE;

    // Is it OK to go ahaead and free the connection
    bool handlesClosed = connectionHandle_ == NULL && requestOpenHandle_ == NULL;

    bool oldReportedHeaders = reportedHeaders_;
    reportedHeaders_ = true;

    long oldResponseBytesSeen = responseBytesSeen_;
    long newResponseBytesSeen = responseBytesSeen_ = responseBytesRead_;

    responseIdle_ = 0;
    LeaveCriticalSection(&criticalSection_);

    // -------------------------------------------------------------------

    if (state == RESPONSE_STATE_DONE) {
        if (handlesClosed)
            delete this;
        return;
    }
    
    if (state == RESPONSE_STATE_ERROR) {
        handler_->handleError(responseError_);
        if (handlesClosed)
            delete this;
        return;
    }

    if (!oldReportedHeaders)
        handler_->handleContentType(responseContentType_, responseContentCharset_);

    // We can only give incremental updates if we had the content size in advance,
    // since otherwise responseBuffer_ may be reallocated under our feet; more
    // obviously, if we don't have the content size in advance, we have to delay
    // reporting it to the end
    if (responseSize_ >= 0) {
        if (!oldReportedHeaders)
            handler_->handleGotSize(responseSize_);

        if (oldResponseBytesSeen != newResponseBytesSeen) {
            handler_->handleBytesRead((char *)responseBuffer_ + oldResponseBytesSeen, 
                                      newResponseBytesSeen - oldResponseBytesSeen);
        }
    }

    if (state == RESPONSE_STATE_COMPLETE) {
        // If we couldn't give an incremental update, give it at the end
        if (responseSize_ < 0) {
            handler_->handleGotSize(responseBytesRead_);
            handler_->handleBytesRead(responseBuffer_, responseBytesRead_);
        }

        handler_->handleComplete(responseBuffer_, responseBytesRead_);
        if (handlesClosed)
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
    // Closing handles is asynchronous, we'll get a status update
    // with a status of INTERNET_STATUS_HANDLE_CLOSING when it happens.

    if (requestOpenHandle_)
        InternetCloseHandle(requestOpenHandle_);
    if (connectionHandle_)
        InternetCloseHandle(connectionHandle_);
}

void 
HippoHTTPContext::onHandleClose(HINTERNET handle)
{
    EnterCriticalSection(&criticalSection_);
    if (handle == connectionHandle_) {
        connectionHandle_ = NULL;
    } else if (handle == requestOpenHandle_) {
        requestOpenHandle_ = NULL;
    }

    if (connectionHandle_ == NULL && requestOpenHandle_ == NULL)
        ensureResponseIdle();
    LeaveCriticalSection(&criticalSection_);
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
        if (responseSize_ >= 0) {
            toRead = responseSize_ - responseBytesRead_;
        } else {
            toRead = MIN(MAX_DOWNLOAD_SIZE - responseBytesRead_, 4096);
        }
        void *readLocation;

        if (toRead > bytesAvailable)
            toRead = bytesAvailable;

        if (((long)toRead + responseBytesRead_) > responseBufferSize_) {
            // No need to guard against overflow because of clamp to MAX_DOWNLOAD_SIZE
            responseBufferSize_ *= 2;
            responseBuffer_ = realloc(responseBuffer_, responseBufferSize_);
        }

        readLocation = ((char*)responseBuffer_) + responseBytesRead_;

        LeaveCriticalSection(&criticalSection_);

        if (responseBuffer_ == NULL) {
            enqueueError(E_OUTOFMEMORY);
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
            if (responseSize_ >= 0) // Only incremental report when we know the size in advance
                ensureResponseIdle();
            LeaveCriticalSection(&criticalSection_);

            if (bytesRead == 0)
                break;
        }
    }

    closeHandles();

    EnterCriticalSection(&criticalSection_);
    responseState_ = HippoHTTPContext::RESPONSE_STATE_COMPLETE;
    ensureResponseIdle();
    LeaveCriticalSection(&criticalSection_);

    return;
}


static void
handleCompleteRequest(HINTERNET ictx, HippoHTTPContext *ctx, DWORD status, LPVOID statusInfo, 
                      DWORD statusLength)
{
    EnterCriticalSection(&(ctx->criticalSection_));
    HippoHTTPContext::ResponseState state = ctx->responseState_;

    if (state == HippoHTTPContext::RESPONSE_STATE_STARTING) {
        ctx->responseState_ = HippoHTTPContext::RESPONSE_STATE_HEADERS;
    }

    LeaveCriticalSection(&(ctx->criticalSection_));

    if (state == HippoHTTPContext::RESPONSE_STATE_HEADERS) {
        // our assumption is that COMPLETE_REQUEST callbacks all occur serialized
        // from a single thread. If we get a COMPLETE_REQUEST callback while we
        // are processing the headers, that assumption has been violated.
        // Ignoring such a call is probably right, since it's not clear what
        // it would mean or what we should do ... if state == HEADERS, we
        // are going to go ahead and try to read data in any case.

        hippoDebugLogW(L"WARNING: handleCompleteRequest() called again while reading headers");
        return;
    }

    if (state != HippoHTTPContext::RESPONSE_STATE_STARTING &&
        state != HippoHTTPContext::RESPONSE_STATE_READING) {
        // It seems to sometimes happen that handleCompleteRequest gets called after
        // we've closed the handles in an error case. 
        // (

        hippoDebugLogW(L"handleCompleteRequest() called while cleaning up, ignoring");
        return;
    }

    if (state == HippoHTTPContext::RESPONSE_STATE_STARTING) {
        DWORD statusCode;
        DWORD statusCodeSize = sizeof(statusCode);

        if (!HttpQueryInfo(ctx->requestOpenHandle_, HTTP_QUERY_STATUS_CODE | HTTP_QUERY_FLAG_NUMBER,
            (void *)&statusCode, &statusCodeSize, 0) || statusCode != 200)
        {
            ctx->enqueueError(E_FAIL);
            return;
        }

        WCHAR responseSize[80];
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
            else if (ctx->responseSize_ > MAX_DOWNLOAD_SIZE)
                ctx->responseSize_ = MAX_DOWNLOAD_SIZE;
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
                ctx->responseContentCharset_ = contentPtr + wcslen(charsetDelim);
            } else {
                ctx->responseContentType_ = responseContentType;
                ctx->responseContentCharset_ = defaultCharset;
            }
        }
        ctx->responseBytesRead_ = 0;
        ctx->responseBuffer_ = malloc (ctx->responseBufferSize_);
        ctx->responseState_ = HippoHTTPContext::RESPONSE_STATE_READING;
        EnterCriticalSection(&(ctx->criticalSection_));
        ctx->ensureResponseIdle(); // To report the content type and maybe size
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
            ctx->onHandleClose(ictx);
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
    context->reportedHeaders_ = false;
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
