/* HippoHTTP.cpp: Wrapper class around WinINET for HTTP operation
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "StdAfx.h"
#include "HippoHTTP.h"
#include <glib.h>
#include <wininet.h>
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

    HINTERNET connectionHandle;
    HINTERNET requestOpenHandle;

    HippoBSTR op;
    HippoBSTR target;
    HippoBSTR contentType;

    void *inputData;
    long inputLen;

    long responseSize;

    // -----------------------------------------------------------------------------
    // These following items are shared between the read thread and response idle
    CRITICAL_SECTION criticalSection_;
    bool criticalSectionInitialized_;

    ResponseState responseState_;
    long responseBytesRead_;
    long responseBytesSeen_;
    unsigned responseIdle_;
    HRESULT responseError_;

    // -----------------------------------------------------------------------------

    void *responseBuffer;

    HANDLE readerThread;
    DWORD readerThreadId;

    HippoHTTPAsyncHandler *handler;

    ~HippoHTTPContext() {
        if (criticalSectionInitialized_)
            DeleteCriticalSection(&criticalSection_);
        if (responseBuffer)
            free(responseBuffer);
    }

    void ensureResponseIdle();
    void doResponseIdle();
    void enqueueError(HRESULT error);
};


void
HippoHTTPContext::doResponseIdle()
{
    HippoHTTPContext::ResponseState state;
    
    if (criticalSectionInitialized_)
        EnterCriticalSection(&criticalSection_);
    state = responseState_;
    long oldResponseBytesSeen = responseBytesSeen_;
    long newResponseBytesSeen = responseBytesSeen_ = responseBytesRead_;
    responseIdle_ = 0;
    if (criticalSectionInitialized_)
        LeaveCriticalSection(&criticalSection_);

    switch (state) {
    case RESPONSE_STATE_READING:
        handler->handleBytesRead((char *)responseBuffer + oldResponseBytesSeen, 
                                 newResponseBytesSeen - oldResponseBytesSeen);
        break;
    case RESPONSE_STATE_DONE:
        handler->handleComplete(responseBuffer, responseBytesRead_);
        delete this;
        break;
    case RESPONSE_STATE_ERROR:
        handler->handleError(responseError_);
        delete this;
        break;
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
HippoHTTPContext::enqueueError(HRESULT error)
{
    InternetCloseHandle(requestOpenHandle);
    requestOpenHandle = NULL;
    InternetCloseHandle(connectionHandle);
    connectionHandle = NULL;

    if (criticalSectionInitialized_)
        EnterCriticalSection(&criticalSection_);
    responseError_ = error;
    responseState_ = RESPONSE_STATE_ERROR;
    ensureResponseIdle();
    if (criticalSectionInitialized_)
        LeaveCriticalSection(&criticalSection_);
}

static void
readerThreadMain(void *threadContext)
{
    HippoHTTPContext *ctx = (HippoHTTPContext*) threadContext;

    InitializeCriticalSection(&ctx->criticalSection_);
    ctx->criticalSectionInitialized_ = true;

    ctx->responseState_ = HippoHTTPContext::RESPONSE_STATE_READING;
        
    while ((ctx->responseSize - ctx->responseBytesRead_) != 0) {
        DWORD bytesRead;

        if (!ctx->requestOpenHandle) {
            hippoDebugLogW(L"Reader thread with closed handle!");
            return;
        }

        // Things seem to work badly if we call neglect to call InternetQueryDataAvailable() 
        // before trying to read
        DWORD bytesAvailable;
        while (TRUE) {
            if (InternetQueryDataAvailable(ctx->requestOpenHandle, &bytesAvailable, 0, 0)) {
                break;
            } else {
                if (GetLastError() != ERROR_IO_PENDING)
                    goto error;
                Sleep(500); // 0.5 seconds
            }
        }

        EnterCriticalSection(&ctx->criticalSection_);

        DWORD toRead = ctx->responseSize - ctx->responseBytesRead_;
        void *readLocation;

        readLocation = ((char*)ctx->responseBuffer) + ctx->responseBytesRead_;
        if (toRead > bytesAvailable)
            toRead = bytesAvailable;

        LeaveCriticalSection(&ctx->criticalSection_);

        if (!InternetReadFile(ctx->requestOpenHandle,
                              readLocation, toRead, &bytesRead)) 
        {    
            if (GetLastError() != ERROR_IO_PENDING)
                goto error;

            // This really should never happen, since InternetQueryDataAvailable()
            // has told us that data *is* available
            Sleep(500);
        } else {
            EnterCriticalSection(&ctx->criticalSection_);
            ctx->responseBytesRead_ += bytesRead;
            ctx->ensureResponseIdle();
            LeaveCriticalSection(&ctx->criticalSection_);

            if (bytesRead == 0)
                break;
        }
    }

    InternetCloseHandle(ctx->requestOpenHandle);
    ctx->requestOpenHandle = NULL;
    InternetCloseHandle(ctx->connectionHandle);
    ctx->connectionHandle = NULL;

    EnterCriticalSection(&ctx->criticalSection_);
    if (ctx->responseSize - ctx->responseBytesRead_ != 0) { 
        // Invalid Content-Length
        ctx->responseSize = ctx->responseBytesRead_;
    }
    ctx->responseState_ = HippoHTTPContext::RESPONSE_STATE_DONE;
    ctx->ensureResponseIdle();
    LeaveCriticalSection(&ctx->criticalSection_);

    return;

error:    
    ctx->enqueueError(GetLastError());
}


static void
handleCompleteRequest(HINTERNET ictx, HippoHTTPContext *ctx, DWORD status, LPVOID statusInfo, 
                      DWORD statusLength)
{
    if (ctx->responseSize < 0) {
        WCHAR responseSize[80];
        static const long MAX_SIZE = 16 * 1024 * 1024;
        DWORD responseDatumSize = sizeof(responseSize);

        if (!HttpQueryInfo(ctx->requestOpenHandle, HTTP_QUERY_CONTENT_LENGTH,
            &responseSize, &responseDatumSize, 
            NULL)) 
        {
            InternetCloseHandle(ctx->requestOpenHandle);
            ctx->requestOpenHandle = NULL;
            InternetCloseHandle(ctx->connectionHandle);
            ctx->connectionHandle = NULL;
            ctx->responseBytesRead_ = 0;
            ctx->responseBuffer = NULL;
            ctx->responseState_ = HippoHTTPContext::RESPONSE_STATE_DONE;
            ctx->ensureResponseIdle();
            return;
        }
        ctx->responseSize = wcstoul(responseSize, NULL, 10);
        if (ctx->responseSize < 0)
            ctx->responseSize = 0;
        else if (ctx->responseSize > MAX_SIZE)
            ctx->responseSize = MAX_SIZE;
        ctx->responseBytesRead_ = 0;
        ctx->responseBuffer = malloc (ctx->responseSize);
        ctx->readerThread = CreateThread(NULL, 0,
            (LPTHREAD_START_ROUTINE)readerThreadMain, (void *) ctx, 0,
            &ctx->readerThreadId);
    }
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
    inetHandle_ = InternetOpen(L"DumbHippo Client/1.0", INTERNET_OPEN_TYPE_PRECONFIG,
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
    if (FAILED(host.CopyTo(hostReturn)))
        return false;

    *portReturn = components.nPort;

    // We don't care about the division between the path and the query string
    HippoBSTR target(components.lpszUrlPath);
    if (FAILED(target.CopyTo(targetReturn)))
        return false;

    return true;
}

void
HippoHTTP::doGet(WCHAR                 *url, 
                 HippoHTTPAsyncHandler *handler)
{
    HippoBSTR host;
    INTERNET_PORT port;
    HippoBSTR target;

    if (!parseURL(url, &host, &port, &target))
        return;

    doAsync(host, port, L"GET", target, NULL, NULL, 0, handler);
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

    doAsync(host, port, L"POST", target, contentType, requestInput, len, handler);
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

    char *utf = g_utf16_to_utf8(str, -1, NULL, NULL, NULL);
    long bytesTotal = (long) strlen(utf);
    bool ret = writeAllToStream(stream, utf, bytesTotal, handler);
    g_free (utf);
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
                   WCHAR                 *contentType, 
                   void                  *requestInput, 
                   long                   len, 
                   HippoHTTPAsyncHandler *handler)
{
    HippoHTTPContext *context;

    context = new HippoHTTPContext;
    ZeroMemory(context, sizeof(*context));
    context->handler = handler;
    context->op = op;
    context->target = target;
    context->contentType = contentType;
    context->responseState_ = HippoHTTPContext::RESPONSE_STATE_STARTING;
    context->responseSize = -1;
    context->inputData = requestInput;
    context->inputLen = len;
    
    context->connectionHandle = InternetConnect(inetHandle_, host, port, NULL, NULL, 
                                                INTERNET_SERVICE_HTTP, 0, (DWORD_PTR) context);
    if (!context->connectionHandle) {
        context->enqueueError(GetLastError());
        return;
    }

    context->requestOpenHandle = HttpOpenRequest(context->connectionHandle, context->op, context->target, 
                                                 NULL, NULL, NULL,
                                                 INTERNET_FLAG_NO_AUTH | INTERNET_FLAG_NO_UI | INTERNET_FLAG_CACHE_IF_NET_FAIL,
                                                 (DWORD_PTR) context);
    if (!context->requestOpenHandle) {
        context->enqueueError(GetLastError());
        return;
    }

    if (context->contentType) {
        WCHAR buf[1024];    
        StringCchPrintf(buf, sizeof(buf)/sizeof(buf[0]), L"Content-Type: %s\r\n", context->contentType);
        HttpAddRequestHeaders(context->requestOpenHandle, buf, -1, HTTP_ADDREQ_FLAG_ADD_IF_NEW);
    }

    if (!HttpSendRequest(context->requestOpenHandle, NULL, 0, context->inputData, context->inputLen) &&
        GetLastError() != ERROR_IO_PENDING) 
    {
        context->enqueueError(GetLastError());
        return;
    }
}
