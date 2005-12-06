/* HippoRemoteWindow.h: Wrapper class around HippoIEWindow with application specific
 * knowledge about particular DumbHippo URLs.
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once
#include "HippoIEWindow.h"

class HippoUI;

/* Utility class for displaying remote URLs on the server, includes
   special functions for signin and link sharing */
class HippoRemoteWindow
{
public:
    HippoRemoteWindow(HippoUI *ui, WCHAR *title, HippoIEWindowCallback *ieCb);
    void navigate(WCHAR *url);
    void showShare(WCHAR *urlToShare, WCHAR *titleOfShare);
    void showShare(WCHAR *urlToShare, WCHAR *titleOfShare, WCHAR *shareType);
    void showSignin();
    ~HippoRemoteWindow(void);

    HippoIE *getIE();

private:
    HippoUI *ui_;
    HippoBSTR title_;
    HippoIEWindowCallback *ieCb_;
    HippoIEWindow *ieWindow_;
};
