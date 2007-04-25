/* HippoIEWindow.cpp: Toplevel window with an embedded IE web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"
#include "HippoIEWindow.h"
#include "HippoUI.h"

static const WCHAR *CLASS_NAME = L"HippoIEWindow";

HippoIEWindow::HippoIEWindow(WCHAR *src, HippoIEWindowCallback *cb)
{
    cb_ = cb;

    setWindowStyle(WS_OVERLAPPED | WS_MINIMIZEBOX | WS_SYSMENU | WS_CAPTION);
    setClassName(CLASS_NAME);
    setTitle(L"Loading...");
    setURL(src);
    setDefaultSize(600, 400);
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
