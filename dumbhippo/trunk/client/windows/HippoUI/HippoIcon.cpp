/* HippoIcon.cpp: notification icon
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx.h"
#include <strsafe.h>
#include "HippoIcon.h"
#include "HippoUI.h"
#include "Resource.h"

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
    
    menu_ = LoadMenu(instance, MAKEINTRESOURCE(IDR_NOTIFY));
    message_ = RegisterWindowMessage(TEXT("HippoNotifyMessage"));

    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = window_;
    notifyIconData.uID = 0;
    notifyIconData.uFlags = NIF_ICON | NIF_MESSAGE;
    notifyIconData.uCallbackMessage = message_;
    notifyIconData.hIcon = icon_;

    Shell_NotifyIcon(NIM_ADD, &notifyIconData);

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

UINT
HippoIcon::getMessage()
{
    return message_;
}

void 
HippoIcon::processMessage(WPARAM wParam,
			  LPARAM lParam)
		          
{
    switch (lParam) {
    case WM_LBUTTONDOWN:
    case NIN_SELECT:
	if (ignoreNextClick_) {
	    ignoreNextClick_ = false;
	    return;
	}
        showMenu(TPM_LEFTBUTTON);
        break;
    case WM_RBUTTONDOWN:
    case WM_CONTEXTMENU:
        showMenu(TPM_RIGHTBUTTON);
        break;
    case NIN_BALLOONSHOW:
	break;
    case NIN_BALLOONUSERCLICK:
        ignoreNextClick_ = true;
	ui_->showURL(currentURL_);
        break;
    case NIN_BALLOONHIDE:
    case NIN_BALLOONTIMEOUT:
	break;
    }
    
}

// XP SP2 addition
#ifndef NIIF_USER
#define NIIF_USER 4
#endif

void
HippoIcon::showURL(const WCHAR *url)
{
    currentURL_ = url;

    WCHAR menubuf[64];

    StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Share "));
    StringCchCat(menubuf, sizeof(menubuf) / sizeof(TCHAR) - 5, url);
    StringCchCat(menubuf, sizeof(menubuf) / sizeof(TCHAR) - 5, TEXT("..."));
    StringCchCopy(menubuf + sizeof(menubuf) / sizeof(TCHAR) - 6, 6, TEXT("[...]"));

    ModifyMenu(menu_, IDM_SHARE, MF_BYCOMMAND | MF_STRING, 
	       IDM_SHARE, menubuf);

    NOTIFYICONDATA notifyIconData = { 0 };

    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = window_;
    notifyIconData.uID = 0;
    notifyIconData.uFlags = NIF_INFO;
    StringCchCopy(notifyIconData.szInfo, sizeof(notifyIconData.szInfo) / sizeof(TCHAR), url);
    StringCchCat(notifyIconData.szInfo, sizeof(notifyIconData.szInfo) / sizeof(TCHAR),
                 TEXT("\n(click to view)"));
    notifyIconData.szInfo[sizeof(notifyIconData.szInfo) - 1] = '\0';
    notifyIconData.uTimeout = 10 * 1000; // 10 seconds
    StringCchCopy(notifyIconData.szInfoTitle, sizeof(notifyIconData.szInfoTitle) / sizeof(TCHAR), TEXT("New Link"));
    notifyIconData.dwInfoFlags = NIIF_USER;
   
    Shell_NotifyIcon(NIM_MODIFY, &notifyIconData);
}

void
HippoIcon::showMenu(UINT buttonFlag)
{
    POINT pt;
    HMENU popupMenu;

    // We:
    //  - Set the foreground window to our (non-shown) window so that clicking
    //    away elsewhere works
    //  - Send the dummy event to force a context switch to our app
    // See Microsoft knowledgebase Q135788

    GetCursorPos(&pt);
    popupMenu = GetSubMenu(menu_, 0);

    SetForegroundWindow(window_);
    TrackPopupMenu(popupMenu, buttonFlag, pt.x, pt.y, 0, window_, NULL);

    PostMessage(window_, WM_NULL, 0, 0);
}
