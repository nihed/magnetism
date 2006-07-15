/* HippoLogWindow.cpp: Debug log window
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx.h"
#include <HippoUtil.h>
#include "HippoLogWindow.h"
#include "resource.h"

#include <glib.h>

static const int MAX_LOADSTRING = 100;
static const TCHAR *CLASS_NAME = TEXT("HippoLogWindowClass");

static HippoLogWindow *theLogWindow = NULL;

HippoLogWindow::HippoLogWindow()
{
    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    editWindow_ = NULL;
}

HippoLogWindow::~HippoLogWindow()
{
}

void
HippoLogWindow::setBigIcon(HICON bigIcon)
{
    bigIcon_ = bigIcon;
}

void
HippoLogWindow::setSmallIcon(HICON smallIcon)
{
    smallIcon_ = smallIcon;
}


bool
HippoLogWindow::create()
{
    if (!registerClass())
        return false;

    if (!createWindow())
        return false;

    theLogWindow = this;

    return true;
}

void
HippoLogWindow::destroy()
{
    theLogWindow = NULL;
    DestroyWindow(window_);
}

void
HippoLogWindow::show()
{
    ShowWindow(window_, SW_SHOW);
    SetActiveWindow(window_); // In case it was already visible
}

void
HippoLogWindow::hide()
{
    ShowWindow(window_, SW_HIDE);
}

void
HippoLogWindow::logString(WCHAR *str)
{
    SYSTEMTIME localtime;
    WCHAR timebuf[20];

    if (!editWindow_)
        return;

    GetLocalTime(&localtime);
    StringCchPrintfW(timebuf, sizeof(timebuf) / sizeof(timebuf[0]), 
                     L"%02d:%02d:%02d ", localtime.wHour, localtime.wMinute, localtime.wSecond);

    SendMessage(editWindow_, EM_SCROLLCARET, (WPARAM)-1, (LPARAM)-1);
    SendMessage(editWindow_, EM_REPLACESEL, (WPARAM)FALSE /* not undoable */, (LPARAM)timebuf);
    SendMessage(editWindow_, EM_REPLACESEL, (WPARAM)FALSE /* not undoable */, (LPARAM)str);
    SendMessage(editWindow_, EM_REPLACESEL, (WPARAM)FALSE /* not undoable */, (LPARAM)L"\r\n");
    SendMessage(editWindow_, EM_SCROLLCARET, 0, 0);
}

bool
HippoLogWindow::registerClass()
{
    WNDCLASSEX wcex;

    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style          = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc    = windowProc;
    wcex.cbClsExtra     = 0;
    wcex.cbWndExtra     = 0;
    wcex.hInstance      = instance_;
    wcex.hIcon          = bigIcon_;
    wcex.hCursor        = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = NULL;
    wcex.lpszClassName  = CLASS_NAME;
    wcex.hIconSm        = smallIcon_;

    return RegisterClassEx(&wcex) != 0;
}

bool
HippoLogWindow::createWindow(void)
{
    WCHAR title[MAX_LOADSTRING];
    LoadString(instance_, IDS_APP_TITLE, title, MAX_LOADSTRING);

    window_ = CreateWindow(CLASS_NAME, title, WS_OVERLAPPEDWINDOW,
                           CW_USEDEFAULT, CW_USEDEFAULT, 400, 500, 
                           NULL, NULL, instance_, NULL);
    if (!window_)
        return false;

    hippoSetWindowData<HippoLogWindow>(window_, this);

    editWindow_ = CreateWindow(L"EDIT", NULL,
                               (WS_CHILD | WS_VISIBLE | WS_VSCROLL | 
                                ES_LEFT | ES_MULTILINE | ES_READONLY),
                               0, 0, 0, 0,  // Size will be set later
                               window_, NULL, instance_, NULL);

    return true;
}

bool
HippoLogWindow::processMessage(UINT   message,
                               WPARAM wParam,
                               LPARAM lParam)
{
    switch (message) 
    {
    case WM_CLOSE:
        hide();
        return true;
    case WM_SIZE:
        MoveWindow(editWindow_, 0, 0, LOWORD(lParam), HIWORD(lParam), TRUE);
        return true;
    default:
        return false;
    }
}

LRESULT CALLBACK 
HippoLogWindow::windowProc(HWND   window,
                           UINT   message,
                           WPARAM wParam,
                           LPARAM lParam)
{
    HippoLogWindow *logWindow = hippoGetWindowData<HippoLogWindow>(window);
    if (logWindow) {
        if (logWindow->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}
