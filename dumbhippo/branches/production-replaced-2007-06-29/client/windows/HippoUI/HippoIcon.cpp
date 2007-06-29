/* HippoIcon.cpp: notification icon
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx-hippoui.h"
#include <strsafe.h>
#include "HippoIcon.h"
#include "HippoUI.h"
#include <HippoUtil.h>
#include <HippoUtil_h.h>
#include "Resource.h"
#include "Guid.h"

// Note that timeout values are clamped between 10 and 30 seconds
// by current versions of Windows

// Timeout for balloons for new posts (30 seconds)
static const int NEW_POST_NOTIFY_TIMEOUT = 30 * 1000;

// Timeout for ballons from someone clicking on an existing post (10 seconds)
static const int CLICKED_ON_NOTIFY_TIMEOUT = 10 * 1000;

HippoIcon::HippoIcon()
{
    ignoreNextClick_ = false;
}

HippoIcon::~HippoIcon()
{
}

void
HippoIcon::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoIcon::create(HWND window)            
{
    NOTIFYICONDATA notifyIconData = { 0 };
    HINSTANCE instance = GetModuleHandle(NULL);
   
    window_ = window;
    
    notifyMessage_ = RegisterWindowMessage(TEXT("HippoNotifyMessage"));
    taskbarCreatedMessage_ = RegisterWindowMessage(TEXT("TaskbarCreated"));

    registerWithTaskbar();

    return true;
}

void
HippoIcon::destroy(void)
{
    NOTIFYICONDATA notifyIconData = { 0 };

    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = window_;
    notifyIconData.uID = 0;
   
    Shell_NotifyIcon(NIM_DELETE, &notifyIconData);
}

void
HippoIcon::setIcon(HICON icon)
{
    icon_ = icon;
}

void
HippoIcon::registerWithTaskbar()
{
    NOTIFYICONDATA notifyIconData = { 0 };
    HINSTANCE instance = GetModuleHandle(NULL);
   
    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = window_;
    notifyIconData.uID = 0;
    notifyIconData.uFlags = NIF_ICON | NIF_MESSAGE;
    notifyIconData.uCallbackMessage = notifyMessage_;
    notifyIconData.hIcon = icon_;

    if (tip_.m_str != 0) {
        notifyIconData.uFlags |= NIF_TIP;
        StringCchCopyW(notifyIconData.szTip, 
                       sizeof(notifyIconData.szTip) / sizeof(notifyIconData.szTip[0]),
                       tip_.m_str);
    }

    Shell_NotifyIcon(NIM_ADD, &notifyIconData);
}

void
HippoIcon::updateIcon(HICON icon)
{       
    setIcon(icon);
    NOTIFYICONDATA notifyIconData = { 0 };
    notifyIconData.uID = 0;
    notifyIconData.hWnd = window_;
    notifyIconData.uFlags = NIF_ICON;
    notifyIconData.hIcon = icon_;

    Shell_NotifyIcon(NIM_MODIFY, &notifyIconData);
}

void
HippoIcon::updateTip(const WCHAR *tip)
{
    tip_ = tip;

    NOTIFYICONDATA notifyIconData = { 0 };
    notifyIconData.uID = 0;
    notifyIconData.hWnd = window_;
    notifyIconData.uFlags = NIF_TIP;
    StringCchCopyW(notifyIconData.szTip, 
                   sizeof(notifyIconData.szTip) / sizeof(notifyIconData.szTip[0]),
                   tip);
    Shell_NotifyIcon(NIM_MODIFY, &notifyIconData);
}

bool
HippoIcon::processMessage(UINT   message,
                          WPARAM wParam,
                          LPARAM lParam)
                          
{
    taskbarCreatedMessage_ = RegisterWindowMessage(TEXT("TaskbarCreated"));

    if (message == notifyMessage_) {
        switch (lParam) {
        case WM_LBUTTONDOWN:
        case NIN_SELECT:
            ui_->showMenu(TPM_LEFTBUTTON);
            break;
        case WM_RBUTTONDOWN:
        case WM_CONTEXTMENU:
            ui_->showMenu(TPM_RIGHTBUTTON);
            break;
        case NIN_BALLOONSHOW:
            break;
        case NIN_BALLOONUSERCLICK:
            break;
        case NIN_BALLOONHIDE:
        case NIN_BALLOONTIMEOUT:
            break;
        }
        return true;
    } else if (message == taskbarCreatedMessage_) {
        registerWithTaskbar();
        return true;
    }

    return false;
}
