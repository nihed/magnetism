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
    ieWindow_ = new HippoIEWindow(ui_, title_, url, NULL, ieCb_);
    ieWindow_->moveResize(CW_DEFAULT, CW_DEFAULT, 500, 600);
    ieWindow_->show();
}

void
HippoRemoteWindow::showShare(WCHAR *urlToShare, WCHAR *titleOfShare)
{
    showShare(urlToShare, titleOfShare, L"url");
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
    if (!SUCCEEDED (ui_->getRemoteURL(HippoBSTR(L"sharelink"), &shareURL))) {
        ui_->debugLogW(L"out of memory");
        return;
    }
            
    shareURL.Append(queryString);

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