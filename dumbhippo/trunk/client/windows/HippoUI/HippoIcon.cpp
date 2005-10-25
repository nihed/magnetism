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
        ui_->showMenu(TPM_LEFTBUTTON);
        break;
    case WM_RBUTTONDOWN:
    case WM_CONTEXTMENU:
        ui_->showMenu(TPM_RIGHTBUTTON);
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
HippoIcon::showURL(const WCHAR *senderName,
				   const WCHAR *url,
		   const WCHAR *title,
		   const WCHAR *description)
{
    currentURL_ = url;

    NOTIFYICONDATA notifyIconData = { 0 };

    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = window_;
    notifyIconData.uID = 0;
    notifyIconData.uFlags = NIF_INFO;
    const size_t infoLen = sizeof(notifyIconData.szInfo) / sizeof(notifyIconData.szInfo[0]);

	StringCchCopy(notifyIconData.szInfo, infoLen, TEXT(""));
	if (senderName) {
	StringCchCat(notifyIconData.szInfo, infoLen, senderName);
	StringCchCat(notifyIconData.szInfo, infoLen, TEXT("\n"));
    StringCchCat(notifyIconData.szInfo, infoLen, url);
	}
    if (description) {
	StringCchCat(notifyIconData.szInfo, infoLen, TEXT("\n"));
	StringCchCat(notifyIconData.szInfo, infoLen, description);
    }
    if (StringCchCat(notifyIconData.szInfo, infoLen, TEXT("\n(click to win)")) == STRSAFE_E_INSUFFICIENT_BUFFER)
	StringCchCopy(notifyIconData.szInfo + infoLen - 4, 4, TEXT("..."));

    notifyIconData.uTimeout = 10 * 1000; // 10 seconds
    const size_t titleLen = sizeof(notifyIconData.szInfoTitle) / sizeof(notifyIconData.szInfoTitle[0]);
	StringCchCopy(notifyIconData.szInfoTitle, titleLen, TEXT("New link"));
    if (title) {
	StringCchCat(notifyIconData.szInfoTitle, titleLen, TEXT(": "));
	StringCchCat(notifyIconData.szInfoTitle, titleLen, title);
    }
    notifyIconData.dwInfoFlags = NIIF_USER;
   
    Shell_NotifyIcon(NIM_MODIFY, &notifyIconData);
}
