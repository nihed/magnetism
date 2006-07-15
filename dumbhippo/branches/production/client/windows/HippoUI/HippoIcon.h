/* HippoIcon.h: notification icon
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>

class HippoUI;

class HippoIcon
{
public:
    HippoIcon();
    ~HippoIcon();

    void setUI(HippoUI *ui);
    void setIcon(HICON icon);
    void updateIcon(HICON icon);
    void updateTip(const WCHAR *tip);
    bool create(HWND window);
    void destroy();

    UINT getMessage();
    void processMessage(WPARAM wParam,
                        LPARAM lParam);
                        
private:
    void showMenu(UINT buttonFlag);

    HippoUI *ui_;
    HWND window_; // XXX should eliminate in favor of getter on HippoUI
    HWND popupWindow_;
    UINT message_;
    HICON icon_;

    // When the user clicks on us with a ballon tip, we get *first* a NIN_BALLOONUSERCLICK
    // then a WM_[LR]BUTTONDOWN. We want to ignore the second to avoid going to the
    // web page *and* showing the menu. This sucks, since it looks like you'd get the
    // menu. Maybe we should always show the menu and have going to the link be an option
    // on it. Or could call GetCursorPos() and try to guess what the user clicked on.
    bool ignoreNextClick_;
};
