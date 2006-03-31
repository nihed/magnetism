/* HippoRemoteWindow.cpp: Wrapper class around HippoIEWindow with application specific
 * knowledge about particular DumbHippo URLs.
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "StdAfx.h"
#include "HippoUI.h"
#include "HippoUIUtil.h"
#include "HippoRemoteWindow.h"
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
    ieWindow_ = new HippoIEWindow(url, ieCb_);
    ieWindow_->setTitle(title_);
    ieWindow_->setUI(ui_);
    ieWindow_->setApplication(ui_);
    ieWindow_->create();
    ieWindow_->moveResize(CW_DEFAULT, CW_DEFAULT, 600, 600);
    ieWindow_->show();
}

void
HippoRemoteWindow::setForegroundWindow()
{
    if (ieWindow_)
        ieWindow_->setForegroundWindow();
}

void
HippoRemoteWindow::showShare(WCHAR *urlToShare, WCHAR *titleOfShare)
{
    showShare(urlToShare, titleOfShare, L"url");
    ieWindow_->moveResize(CW_DEFAULT, CW_DEFAULT, 550, 400);
}

void
HippoRemoteWindow::showShare(WCHAR *urlToShare, WCHAR *titleOfShare, WCHAR *shareType)
{
    HippoArray<HippoBSTR> queryParamNames;
    HippoArray<HippoBSTR> queryParamValues;

    queryParamNames.append(HippoBSTR(L"next"));
    queryParamValues.append(HippoBSTR(L"close"));
    queryParamNames.append(HippoBSTR(L"url"));
    queryParamValues.append(HippoBSTR(urlToShare));
    queryParamNames.append(HippoBSTR(L"title"));
    queryParamValues.append(HippoBSTR(titleOfShare));
    queryParamNames.append(HippoBSTR(L"shareType"));
    queryParamValues.append(HippoBSTR(shareType)); 

    HippoBSTR queryString;
    HippoUIUtil::encodeQueryString(queryString, queryParamNames, queryParamValues);

    HippoBSTR shareURL;
    ui_->getRemoteURL(HippoBSTR(L"sharelink"), &shareURL);
            
    shareURL.Append(queryString);

    navigate(shareURL);
}

void
HippoRemoteWindow::showSignin()
{
    HippoBSTR signinUrl;
    ui_->getRemoteURL(L"who-are-you?next=close", &signinUrl);
    navigate(signinUrl);
}

HippoIE *
HippoRemoteWindow::getIE()
{
    return ieWindow_->getIE();
}