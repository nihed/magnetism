/* HippoUI.h: Debug log window
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

class HippoLogWindow
{
public:
    HippoLogWindow();
    ~HippoLogWindow();

    void setBigIcon(HICON bigIcon);
    void setSmallIcon(HICON smallIcon);

    bool create();
    void destroy();
    void show();
    void hide();
    void logString(WCHAR *str);

private:
    bool registerClass();
    bool createWindow();

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);

private:
    HINSTANCE instance_;
    HICON bigIcon_;
    HICON smallIcon_;
    HWND window_;
    HWND editWindow_;
};
