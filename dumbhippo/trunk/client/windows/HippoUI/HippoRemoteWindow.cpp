#include "StdAfx.h"
#include "HippoUI.h"
#include ".\hipporemotewindow.h"
#include <wininet.h>

HippoRemoteWindow::HippoRemoteWindow(HippoUI *ui, WCHAR *title, HippoIEWindowCallback *ieCb)
{
    ui_ = ui;
    title_ = title;
    ieCb_ = ieCb;
}

HippoRemoteWindow::~HippoRemoteWindow(void)
{
    delete ieWindow_;
}

void
HippoRemoteWindow::navigate(WCHAR *url)
{
    ieWindow_ = new HippoIEWindow(ui_, title_, 500, 600, url, NULL, ieCb_);
    ieWindow_->show();
}

void
HippoRemoteWindow::showShare(WCHAR *urlToShare, WCHAR *titleOfShare)
{
    HippoBSTR shareURL;

    if (!SUCCEEDED (ui_->getRemoteURL(HippoBSTR(L"sharelink"), &shareURL)))
        ui_->debugLogW(L"out of memory");
    if (!SUCCEEDED (shareURL.Append(L"?next=close&url=")))
        ui_->debugLogW(L"out of memory");

    wchar_t encoded[1024] = {0}; 
    DWORD len = sizeof(encoded)/sizeof(encoded[0]);

    if (!SUCCEEDED (UrlEscape(urlToShare, encoded, &len, URL_ESCAPE_UNSAFE | URL_ESCAPE_SEGMENT_ONLY)))
        ui_->debugLogW(L"out of memory");
    if (!SUCCEEDED (shareURL.Append(encoded)))
        ui_->debugLogW(L"out of memory");

    if (!SUCCEEDED (shareURL.Append(L"&title=")))
        ui_->debugLogW(L"out of memory");

    encoded[0] = 0;
    len = sizeof(encoded)/sizeof(encoded[0]);
    if (!SUCCEEDED (UrlEscape(titleOfShare, encoded, &len, URL_ESCAPE_UNSAFE | URL_ESCAPE_SEGMENT_ONLY)))
        ui_->debugLogW(L"out of memory");
    if (!SUCCEEDED (shareURL.Append(encoded)))
        ui_->debugLogW(L"out of memory");

    navigate(shareURL);
}

void
HippoRemoteWindow::showSignin()
{
    HippoBSTR signinUrl;
    ui_->getRemoteURL(L"signin", &signinUrl);
    navigate(signinUrl);
}

HippoIE *
HippoRemoteWindow::getIE()
{
    return ieWindow_->getIE();
}