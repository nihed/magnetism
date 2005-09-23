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
};
