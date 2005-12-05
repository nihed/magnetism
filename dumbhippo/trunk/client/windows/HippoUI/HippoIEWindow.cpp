#include "StdAfx.h"
#include ".\hippoiewindow.h"
#include "HippoUI.h"

static const WCHAR *CLASS_NAME = L"HippoIEWindow";

void
HippoIEWindow::HippoIEWindowIECallback::onError(WCHAR *errText)
{
    this->win_->ui_->debugLogW(L"HippoIEWindow error: %s", errText);
}

HippoIEWindow::HippoIEWindow(HippoUI *ui, WCHAR *title, int width, int height, WCHAR *src, IDispatch *external, HippoIEWindowCallback *cb)
{
    ui_ = ui;
    cb_ = cb;
    instance_ = GetModuleHandle(NULL);
    registerClass();
    if (title == NULL)
        title = L"Loading...";
    window_ = CreateWindow(CLASS_NAME, title, WS_OVERLAPPED | WS_MINIMIZEBOX | WS_SYSMENU, 
                           CW_USEDEFAULT, CW_USEDEFAULT, width, height, 
                           NULL, NULL, instance_, NULL);
    hippoSetWindowData<HippoIEWindow>(window_, this);
    ieCb_ = new HippoIEWindowIECallback(this);
    ie_ = new HippoIE(window_, src, ieCb_, external);
    SetFocus(window_);
    created_ = FALSE;
    ui_->registerWindowMsgHook(window_, this);
}

HippoIEWindow::~HippoIEWindow(void)
{
    ui_->unregisterWindowMsgHook(window_);
    DestroyWindow(window_);
    ie_->Release();
    delete ieCb_;
}

HippoIE *
HippoIEWindow::getIE()
{
    return ie_;
}

void
HippoIEWindow::show() 
{
    if (!created_) {
        ie_->create();
        created_ = true;
    }
    AnimateWindow(window_, 400, AW_BLEND);
}

void
HippoIEWindow::hide()
{
    AnimateWindow(window_, 400, AW_BLEND | AW_HIDE);
}

bool
HippoIEWindow::registerClass()
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

bool
HippoIEWindow::hookMessage(MSG *msg)
{
    if ((msg->message >= WM_KEYFIRST && msg->message <= WM_KEYLAST))
    {
        HippoPtr<IWebBrowser> browser(ie_->getBrowser());
        HippoQIPtr<IOleInPlaceActiveObject> active(ie_->getBrowser());
        HRESULT res = active->TranslateAccelerator(msg);
        return res == S_OK;
    }
    return FALSE;
}

bool
HippoIEWindow::processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{

    switch (message) 
    {
    case WM_CLOSE:
        if (cb_ == NULL || cb_->onClose(window_))
            hide();
        return true;
    case WM_ACTIVATE:
        {
        HippoQIPtr<IOleInPlaceActiveObject> active(ie_->getBrowser());
        if (active)
            active->OnDocWindowActivate(wParam == WA_ACTIVE);
        return true;
        }
    default:
        return false;
    }
}

LRESULT CALLBACK 
HippoIEWindow::windowProc(HWND   window,
                        UINT   message,
                        WPARAM wParam,
                        LPARAM lParam)
{
    HippoIEWindow *win = hippoGetWindowData<HippoIEWindow>(window);
    if (win) {
        if (win->processMessage(message, wParam, lParam))
            return 0;
    }
    return DefWindowProc(window, message, wParam, lParam);
}
