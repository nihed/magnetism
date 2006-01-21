/* HippoChatWindow.cpp: Window displaying a chat room for a post
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include <mshtml.h>
#include "exdisp.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include "HippoUI.h"
#include "HippoIE.h"
#include <HippoUtil.h>
#include "HippoChatWindow.h"
#include "Guid.h"

static const TCHAR *CLASS_NAME = TEXT("HippoChatWindowClass");
static const int BASE_WIDTH = 600;
static const int BASE_HEIGHT = 600;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

HippoChatWindow::HippoChatWindow(void)
{
    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    ie_ = NULL;

    ieCallback_ = new HippoChatWindowIECallback(this);
}

HippoChatWindow::~HippoChatWindow(void)
{
    DestroyWindow(window_);

    delete ieCallback_;
}

void 
HippoChatWindow::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoChatWindow::createWindow(void)
{
    window_ = CreateWindow(CLASS_NAME, L"Hippo Chat", WS_OVERLAPPEDWINDOW,
                           CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                           NULL, NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    hippoSetWindowData<HippoChatWindow>(window_, this);

    return true;
}

void HippoChatWindow::HippoChatWindowIECallback::onDocumentComplete()
{
    chatWindow_->ui_->debugLogW(L"HippoChatWindow document complete");
}

void
HippoChatWindow::HippoChatWindowIECallback::onError(WCHAR *text) 
{
    chatWindow_->ui_->debugLogW(L"HippoIE error: %s", text);
}

bool
HippoChatWindow::embedIE(void)
{
    RECT rect;
    GetClientRect(window_,&rect);
    HippoBSTR srcURL;
    ui_->getRemoteURL(L"chatwindow?postId=", &srcURL);
    srcURL.Append(postId_);
    ie_ = new HippoIE(window_, srcURL, ieCallback_, NULL);
    ie_->setThreeDBorder(false);

    ie_->create();
    browser_ = ie_->getBrowser();

    return true;
}

bool
HippoChatWindow::create(void)
{
    if (window_ != NULL) {
        return true;
    }
    if (!registerClass()) {
        ui_->debugLogW(L"Failed to register window class");
        return false;
    }
    if (!createWindow()) {
        ui_->debugLogW(L"Failed to create window");
        return false;
    }
    if (!embedIE()) {
        ui_->debugLogW(L"Failed to embed IE");
        return false;
    }
    return true;
}

bool
HippoChatWindow::registerClass()
{
    WNDCLASSEX wcex;

    ZeroMemory(&wcex, sizeof(WNDCLASSEX));
    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = windowProc;
    wcex.cbClsExtra = 0;
    wcex.cbWndExtra = 0;
    wcex.hInstance  = instance_;
    wcex.hCursor    = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = NULL;
    wcex.lpszClassName  = CLASS_NAME;

    if (RegisterClassEx(&wcex) == 0) {
        if (GetClassInfoEx(instance_, CLASS_NAME, &wcex) != 0)
            return true;
        return false;
    }
    return true;
}

void
HippoChatWindow::show(void) 
{   
    ui_->debugLogW(L"doing ChatWindow show");
    if (!ShowWindow(window_, SW_SHOW))
        ui_->logLastError(L"Failed to invoke ShowWindow");
    if (!RedrawWindow(window_, NULL, NULL, RDW_UPDATENOW))
        ui_->logLastError(L"Failed to invoke RedrawWindow");
    if (!BringWindowToTop(window_))
        ui_->logLastError(L"Failed to invoke BringWindowToTop");
}

void 
HippoChatWindow::setForegroundWindow()
{
    if (window_)
        SetForegroundWindow(window_);
}

void 
HippoChatWindow::setPostId(BSTR postId)
{
    postId_ = postId;
}

BSTR
HippoChatWindow::getPostId()
{
    return postId_.m_str;
}

bool
HippoChatWindow::processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    switch (message) 
    {
    case WM_CLOSE:
        ui_->onChatWindowClosed(this);
        return true;
    case WM_SIZE:
        {
            RECT rect = { 0, 0, LOWORD(lParam), HIWORD(lParam) };
            hippoDebugLogW(L"Now CLIENT is %d %d", rect.right, rect.bottom);
            ie_->resize(&rect);
            return true;
        }
    default:
        return false;
    }
}

LRESULT CALLBACK 
HippoChatWindow::windowProc(HWND   window,
                            UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    HippoChatWindow *chatWindow = hippoGetWindowData<HippoChatWindow>(window);
    if (chatWindow) {
        if (chatWindow->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}
