#include "StdAfx.h"
#include ".\hippohttp.h"
#include <glib.h>
#include <wininet.h>
#include <HippoUtil.h>

class HippoHTTPContext
{
public:
    HINTERNET requestOpenHandle;

    HippoBSTR op;
    HippoBSTR target;
    HippoBSTR contentType;

    void *inputData;
    long inputLen;

    long responseSize;
    long responseBytesRead;
    void *responseBuffer;

    HANDLE readerThread;
    DWORD readerThreadId;

    HippoHTTPAsyncHandler *handler;

    ~HippoHTTPContext() {
        if (responseBuffer)
            free(responseBuffer);
    }
};

static gboolean
idleEmitComplete(gpointer data)
{
    HippoHTTPContext *ctx = (HippoHTTPContext*)data;

    ctx->handler->handleComplete(ctx->responseBuffer, ctx->responseBytesRead);

    delete ctx;

    return FALSE;
}

typedef struct {
    HRESULT res;
    HippoHTTPContext *ctx;
} HippoIdleEmitErrorContext;

static gboolean
idleEmitError(gpointer data)
{
    HippoIdleEmitErrorContext *ctx = (HippoIdleEmitErrorContext*)data;
    ctx->ctx->handler->handleError(ctx->res);
    delete ctx->ctx;
    delete ctx;
    return FALSE;
}

static void
enqueueError(HippoHTTPContext *ctx, HRESULT err)
{
    HippoIdleEmitErrorContext *ictx = new HippoIdleEmitErrorContext;
    ictx->ctx = ctx;
    ictx->res = err;
    g_idle_add (idleEmitError, ictx);
}

static void
readerThreadMain(void *threadContext)
{
    HippoHTTPContext *ctx = (HippoHTTPContext*) threadContext;
    while ((ctx->responseSize - ctx->responseBytesRead) != 0) {
        DWORD bytesRead;
        if (!InternetReadFile(ctx->requestOpenHandle, ((char*)ctx->responseBuffer) + ctx->responseBytesRead, 
                              ctx->responseSize - ctx->responseBytesRead,
                              &bytesRead)) {
            if (GetLastError() != ERROR_IO_PENDING) {
                enqueueError(ctx, GetLastError());
                return;
            }
        }
        ctx->responseBytesRead += bytesRead;
        if (bytesRead == 0)
            break;
    }
    if (ctx->responseSize - ctx->responseBytesRead != 0) { 
        // Invalid Content-Length
        ctx->responseSize = ctx->responseBytesRead;
    }
    g_idle_add (idleEmitComplete, ctx);
    return;
}

static void
handleCompleteRequest(HINTERNET ictx, HippoHTTPContext *ctx, DWORD status, LPVOID statusInfo, 
                      DWORD statusLength)
{
    WCHAR responseSize[80];
    long maxSize = 5 * 1024 * 1024;
    DWORD responseDatumSize = sizeof(responseSize);
    if (ctx->responseSize < 0) {
        if (!HttpQueryInfo(ctx->requestOpenHandle, HTTP_QUERY_CONTENT_LENGTH,
            &responseSize, &responseDatumSize, 
            NULL)) {
                enqueueError(ctx, GetLastError());
                return;
            }
            ctx->responseSize = wcstoul(responseSize, NULL, 10);
            if (ctx->responseSize < 0)
                ctx->responseSize = 0;
            else if (ctx->responseSize > maxSize)
                ctx->responseSize = maxSize;
            ctx->responseBytesRead = 0;
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

void
HippoHTTP::doAsync(WCHAR *host, INTERNET_PORT port, WCHAR *op, WCHAR *target, WCHAR *contentType, void *requestInput, long len, HippoHTTPAsyncHandler *handler)
{
    HippoHTTPContext *context;

    context = new HippoHTTPContext;
    ZeroMemory(context, sizeof(*context));
    context->handler = handler;
    context->op = op;
    context->target = target;
    context->responseSize = -1;
    context->inputData = requestInput;
    context->inputLen = len;
    
    HINTERNET handle = InternetConnect(inetHandle_, host, port, NULL, NULL, INTERNET_SERVICE_HTTP, 0, (DWORD_PTR) context);

    context->requestOpenHandle = HttpOpenRequest(handle, context->op, context->target, NULL, NULL, NULL,
                                                 INTERNET_FLAG_NO_AUTH | INTERNET_FLAG_NO_UI | INTERNET_FLAG_CACHE_IF_NET_FAIL,
                                                 (DWORD_PTR) context);
    if (!context->requestOpenHandle) {
        context->handler->handleError(GetLastError());
        return;
    }

    if (context->contentType) {
        WCHAR buf[1024];    
        StringCchPrintf(buf, sizeof(buf)/sizeof(buf[0]), L"Content-Type: %s\r\n", context->contentType);
        HttpAddRequestHeaders(context->requestOpenHandle, buf, -1, HTTP_ADDREQ_FLAG_REPLACE);
    }

    if (!HttpSendRequest(context->requestOpenHandle, NULL, 0, context->inputData, context->inputLen) &&
        GetLastError() != ERROR_IO_PENDING) {
        context->handler->handleError(GetLastError());
    }
}
