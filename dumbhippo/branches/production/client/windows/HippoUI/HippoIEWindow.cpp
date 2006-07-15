/* HippoIEWindow.cpp: Toplevel window with an embedded IE web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "StdAfx.h"
#include "HippoIEWindow.h"
#include "HippoUI.h"

static const WCHAR *CLASS_NAME = L"HippoIEWindow";

HippoIEWindow::HippoIEWindow(WCHAR *src, HippoIEWindowCallback *cb)
{
    cb_ = cb;

    setAnimate(true);
    setWindowStyle(WS_OVERLAPPED | WS_MINIMIZEBOX | WS_SYSMENU);
    setClassName(CLASS_NAME);
    setTitle(L"Loading...");
    setURL(src);
}

HippoIEWindow::~HippoIEWindow()
{
}

void
HippoIEWindow::onClose(bool fromScript) 
{
    if (cb_ == NULL || cb_->onClose())
        hide();
}

void
HippoIEWindow::onDocumentComplete()
{
    if (cb_)
        cb_->onDocumentComplete();
}
