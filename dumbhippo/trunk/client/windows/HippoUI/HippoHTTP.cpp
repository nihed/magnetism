#include "StdAfx.h"
#include ".\hippohttp.h"
#include <glib.h>
#include <wininet.h>
#include <HippoUtil.h>

typedef enum {
        HIPPO_HTTP_STATE_CONNECTING,
        HIPPO_HTTP_STATE_REQUESTING,
        HIPPO_HTTP_STATE_OPERATING,
        HIPPO_HTTP_STATE_COMPLETE,
        HIPPO_HTTP_STATE_ERROR
} HippoHTTPState; 

typedef struct
{
    HippoHTTPState state;

    HINTERNET requestOpenHandle;

    HippoBSTR op;
    HippoBSTR target;
    HippoBSTR contentType;

    void *inputData;
    long inputLen;
    long responseSize;
    INTERNET_BUFFERS requestOutputBuffer;

    HippoHTTPAsyncHandler *handler;
    void *userData;
} HippoHTTPContext;

static gboolean
idleInvokeComplete(gpointer data)
{
    HippoHTTPContext *ctx = (HippoHTTPContext*)data;

    ctx->handler->handleComplete(NULL);

    delete ctx;

    return FALSE;
}

static void
handleHandleCreated(HINTERNET ictx, HippoHTTPContext *ctx)
{
    switch (ctx->state) {
        case HIPPO_HTTP_STATE_CONNECTING:
            {
            ctx->state = HIPPO_HTTP_STATE_REQUESTING;
            if (!HttpOpenRequest(ictx, ctx->op, ctx->target, NULL, NULL, NULL,
                                INTERNET_FLAG_NO_AUTH | INTERNET_FLAG_NO_UI | INTERNET_FLAG_CACHE_IF_NET_FAIL,
                                (DWORD_PTR) ctx)) {
                ctx->state = HIPPO_HTTP_STATE_ERROR;
                ctx->handler->handleError(GetLastError());
            }
            }
            break;
        case HIPPO_HTTP_STATE_REQUESTING:
            {
            ctx->requestOpenHandle = ictx;
            if (ctx->contentType) {
                WCHAR buf[1024];    
                StringCchPrintf(buf, sizeof(buf)/sizeof(buf[0]), L"Content-Type: %s\r\n", ctx->contentType);
                HttpAddRequestHeaders(ctx->requestOpenHandle, buf, -1, HTTP_ADDREQ_FLAG_REPLACE);
            }
            ctx->state = HIPPO_HTTP_STATE_OPERATING;
            if (!HttpSendRequest(ctx->requestOpenHandle, NULL, 0, ctx->inputData, ctx->inputLen)) {
                ctx->state = HIPPO_HTTP_STATE_ERROR;
                ctx->handler->handleError(GetLastError());
            }
            }
            break;
        default:
            g_assert (FALSE);
            break;
    }
}

static void
handleCompleteRequest(HINTERNET ictx, HippoHTTPContext *ctx, DWORD status, LPVOID statusInfo, 
                      DWORD statusLength)
{
    switch (ctx->state) 
    {
    case HIPPO_HTTP_STATE_CONNECTING:
        assert (FALSE);
        break;
    case HIPPO_HTTP_STATE_REQUESTING: 
        assert (FALSE);
        break;
    case HIPPO_HTTP_STATE_OPERATING:
        {
            DWORD headerIndex = 0;
            DWORD responseDatumSize = sizeof(ctx->responseSize);
            if (ctx->responseSize < 0) {
                if (!HttpQueryInfo(ctx->requestOpenHandle, HTTP_QUERY_CONTENT_LENGTH,
                                   &(ctx->responseSize), &responseDatumSize, 
                                   &headerIndex)) {
                                        ctx->state = HIPPO_HTTP_STATE_ERROR;
                                        ctx->handler->handleError(GetLastError());
                                        break;
                                   }
                ctx->requestOutputBuffer.dwStructSize = sizeof(ctx->requestOutputBuffer);
                ctx->requestOutputBuffer.dwBufferLength = (2 * ctx->responseSize) + 1;
                ctx->requestOutputBuffer.dwBufferTotal = 0;
                ctx->requestOutputBuffer.lpvBuffer = malloc (ctx->requestOutputBuffer.dwBufferLength);
            } else {
                while (ctx->requestOutputBuffer.dwBufferLength != 0) {
                    BOOL status = InternetReadFileEx(ctx->requestOpenHandle, &(ctx->requestOutputBuffer), 
                                                     IRF_ASYNC, (DWORD_PTR) ctx);
                    if(!status && GetLastError() == ERROR_IO_PENDING)
                        break;
                }
            }
            if (ctx->requestOutputBuffer.dwBufferLength == 0) {
                char * buf = (char*) ctx->requestOutputBuffer.lpvBuffer;
                buf[ctx->requestOutputBuffer.dwBufferTotal] = 0;
                ctx->state = HIPPO_HTTP_STATE_COMPLETE;
            }
        }
        break;
    case HIPPO_HTTP_STATE_COMPLETE:
        {
        }
        break;
    case HIPPO_HTTP_STATE_ERROR:
        break;
    }
}

static void CALLBACK
asyncStatusUpdate(HINTERNET ictx, DWORD_PTR uctx, DWORD status, LPVOID statusInfo, 
                  DWORD statusLength)
{
    HippoHTTPContext *ctx = (HippoHTTPContext*)uctx;

    switch (status) {
        case INTERNET_STATUS_CLOSING_CONNECTION:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_CONNECTED_TO_SERVER:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_CONNECTING_TO_SERVER:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_CONNECTION_CLOSED:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_HANDLE_CLOSING:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_HANDLE_CREATED:
            {
                INTERNET_ASYNC_RESULT *res = (INTERNET_ASYNC_RESULT *) statusInfo;
                handleHandleCreated((HINTERNET) (res->dwResult), ctx);
            }
            break;
        case INTERNET_STATUS_INTERMEDIATE_RESPONSE:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_NAME_RESOLVED:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_RECEIVING_RESPONSE:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_RESPONSE_RECEIVED:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_REDIRECT:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_REQUEST_COMPLETE:
            handleCompleteRequest(ictx, ctx, status, statusInfo, statusLength);
            break;
        case INTERNET_STATUS_REQUEST_SENT:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_RESOLVING_NAME:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_SENDING_REQUEST:
            ctx->handler->handleError(0);
            break;
        case INTERNET_STATUS_STATE_CHANGE:
            ctx->handler->handleError(0);
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
HippoHTTP::doAsync(WCHAR *host, INTERNET_PORT port, WCHAR *op, WCHAR *target, WCHAR *contentType, void *requestInput, long len, HippoHTTPAsyncHandler *handler, void *data)
{
    HippoHTTPContext *context;

    context = new HippoHTTPContext;
    ZeroMemory(context, sizeof(*context));
    context->handler = handler;
    context->userData = data;
    context->op = op;
    context->target = target;
    context->inputData = requestInput;
    context->inputLen = len;
    
    context->state = HIPPO_HTTP_STATE_CONNECTING;
    InternetConnect(inetHandle_, host, port, NULL, NULL, INTERNET_SERVICE_HTTP, 0, (DWORD_PTR) context);
}
