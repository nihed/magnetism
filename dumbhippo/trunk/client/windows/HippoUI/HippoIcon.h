/* HippoIcon.h: notification icon
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

class HippoIcon
{
public:
    HippoIcon();
    ~HippoIcon();

    void setIcon(HICON icon);
    bool create(HWND window);
    void destroy();

    UINT getMessage();
    void processMessage(WPARAM wParam,
		        LPARAM lParam);
	                
    void showURL(const WCHAR *url);

private:
    void showMenu(UINT buttonFlag);

    
    HWND window_;
    UINT message_;
    HMENU menu_;
    HICON icon_;

    // When the user clicks on us with a ballon tip, we get *first* a NIN_BALLOONUSERCLICK
    // then a WM_[LR]BUTTONDOWN. We want to ignore the second to avoid going to the
    // web page *and* showing the menu. This sucks, since it looks like you'd get the
    // menu. Maybe we should always show the menu and have going to the link be an option
    // on it. Or could call GetCursorPos() and try to guess what the user clicked on.
    bool ignoreNextClick_;
};
