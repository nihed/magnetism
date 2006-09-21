/* HippoAbstractWindow.cpp: Base class for toplevel windows that embed a web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"

#include "HippoUI.h"
#include <HippoUtil.h>
#include "HippoAbstractIEWindow.h"

HippoAbstractIEWindow::HippoAbstractIEWindow()
{
    ie_ = NULL;
    ieCallback_ = new HippoAbstractWindowIECallback(this);
}

void
HippoAbstractIEWindow::destroy()
{
    HippoAbstractWindow::destroy();

    assert(window_ == NULL);
    assert(ie_ == NULL);

    delete ieCallback_;
}

void
HippoAbstractIEWindow::setApplication(IDispatch *application)
{
    application_ = application;
}

void 
HippoAbstractIEWindow::setURL(const HippoBSTR &url)
{
    url_ = url;   
}

HippoBSTR
HippoAbstractIEWindow::getURL()
{
    return url_;
}

void 
HippoAbstractIEWindow::initializeIE()
{
}

void 
HippoAbstractIEWindow::initializeBrowser()
{
}

void
HippoAbstractIEWindow::onDocumentComplete()
{
}

void 
HippoAbstractIEWindow::HippoAbstractWindowIECallback::onClose()
{
    abstractWindow_->onClose(true);
}

void 
HippoAbstractIEWindow::HippoAbstractWindowIECallback::onDocumentComplete()
{
    abstractWindow_->onDocumentComplete();
}

void 
HippoAbstractIEWindow::HippoAbstractWindowIECallback::launchBrowser(const HippoBSTR &url)
{
    abstractWindow_->ui_->launchBrowser(url.m_str);
}

bool
HippoAbstractIEWindow::HippoAbstractWindowIECallback::isOurServer(const HippoBSTR &host)
{
    char *serverHostU;
    int port;
    HippoPlatform *platform;

    platform = abstractWindow_->ui_->getPlatform();
    hippo_platform_get_web_host_port(platform, &serverHostU, &port);

    HippoBSTR serverHost = HippoBSTR::fromUTF8(serverHostU);
    g_free(serverHostU);

    return host == serverHost;
}

HRESULT 
HippoAbstractIEWindow::HippoAbstractWindowIECallback::getToplevelBrowser(const IID &ifaceID, void **toplevelBrowser)
{
    return E_UNEXPECTED;
}

bool
HippoAbstractIEWindow::embedIE(void)
{
    ie_ = HippoIE::create(window_, getURL(), ieCallback_, application_);
    ie_->setThreeDBorder(false);
    initializeIE();
    ie_->embedBrowser();
    browser_ = ie_->getBrowser();

    initializeBrowser();

    return true;
}

bool
HippoAbstractIEWindow::create(void)
{
    // chain up 
    if (!HippoAbstractWindow::create())
        return false;

    if (!embedIE()) {
        hippoDebugLogW(L"Failed to embed IE");
        return false;
    }
    return true;
}

void
HippoAbstractIEWindow::onWindowDestroyed(void)
{
    // chain up 
    HippoAbstractWindow::onWindowDestroyed();

    if (ie_) {
        ie_->shutdown();
        ie_ = NULL;
    }
}

HippoIE *
HippoAbstractIEWindow::getIE()
{
    return ie_;
}

bool
HippoAbstractIEWindow::hookMessage(MSG *msg)
{
    // chain up
    if (HippoAbstractWindow::hookMessage(msg))
        return true;

    if ((msg->message >= WM_KEYFIRST && msg->message <= WM_KEYLAST))
    {
        HippoPtr<IWebBrowser> browser(ie_->getBrowser());
        HippoQIPtr<IOleInPlaceActiveObject> active(ie_->getBrowser());
        HRESULT res = active->TranslateAccelerator(msg);
        return res == S_OK;
    }
    return false;
}

bool
HippoAbstractIEWindow::processMessage(UINT   message,
                                      WPARAM wParam,
                                      LPARAM lParam)
{
    // chain up 
    if (HippoAbstractWindow::processMessage(message, wParam, lParam))
        return true;

    switch (message) 
    {
    case WM_ACTIVATE:
        {
            // It's not completely clear that this is necessary
            HippoQIPtr<IOleInPlaceActiveObject> active(ie_->getBrowser());
            if (active)
                active->OnFrameWindowActivate(LOWORD(wParam) != WA_INACTIVE);
            return true;
        }
    case WM_SIZE:
        if (ie_) {
            RECT rect = { 0, 0, LOWORD(lParam), HIWORD(lParam) };
            ie_->resize(&rect);
        }
        HippoAbstractWindow::processMessage(message, wParam, lParam);
        return true;
    default:
        return false;
    }
}
