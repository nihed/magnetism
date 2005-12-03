#pragma once
#include "hippoiewindow.h"

class HippoUI;

class HippoRemoteWindow
{
public:
    HippoRemoteWindow(HippoUI *ui, WCHAR *title, HippoIEWindowCallback *ieCb);
    void navigate(WCHAR *url);
    void showShare(WCHAR *urlToShare, WCHAR *titleOfShare);
    void showSignin();
    ~HippoRemoteWindow(void);

    HippoIE *getIE();

private:
    HippoUI *ui_;
    HippoBSTR title_;
    HippoIEWindowCallback *ieCb_;
    HippoIEWindow *ieWindow_;
};
