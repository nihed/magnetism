/* HippoAbstractControl.cpp: a "widget" (wrapper around an HWND)
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"

#include "HippoAbstractControl.h"
#include <glib.h>

HippoAbstractControl::HippoAbstractControl()
    : lastWidthRequest_(0), lastHeightRequest_(0), parent_(0)
{
    setWindowStyle(WS_CHILD);
}

void
HippoAbstractControl::setParent(HippoAbstractControl *parent)
{
    if (parent_ == parent)
        return;

    if (parent) {
        parent->AddRef();
    }
    if (parent_) {
        parent_->queueResize();
        parent_->Release();
    }
    parent_ = parent;
    setCreateWithParent(parent_);
    queueResize();
}

bool
HippoAbstractControl::create()
{
    bool result;

    result = HippoAbstractWindow::create();

    return result;
}

void
HippoAbstractControl::show(bool activate)
{
    HippoAbstractWindow::show(activate);
}

void
HippoAbstractControl::queueResize()
{
    // send it up to the parent, the topmost parent is supposed to 
    // do an idle handler then moveResize all its children
    if (parent_)
        parent_->queueResize();
}

int
HippoAbstractControl::getWidthRequest()
{
    int w = getWidthRequestImpl();
    lastWidthRequest_ = w;
    return w;
}

int
HippoAbstractControl::getHeightRequest(int forWidth)
{
    int h = getHeightRequestImpl(forWidth);
    lastHeightRequest_ = h;
    return h;
}

int
HippoAbstractControl::getWidthRequestImpl()
{
    // default is to return current window size
    return getWidth();
}

int
HippoAbstractControl::getHeightRequestImpl(int forWidth)
{
    // default is to return current window size
    return getHeight();
}


void
HippoAbstractControl::getLastRequest(int *width_p, 
                                     int *height_p)
{
    if (width_p)
        *width_p = lastWidthRequest_;
    if (height_p)
        *height_p = lastHeightRequest_;
}

bool
HippoAbstractControl::processMessage(UINT   message,
                                     WPARAM wParam,
                                     LPARAM lParam)
{
    if (HippoAbstractWindow::processMessage(message, wParam, lParam))
        return true;

    switch (message) {
        case WM_GETMINMAXINFO:
            if (parent_ == NULL) {
                // Override the minimum width/height of the window if it's a toplevel
                MINMAXINFO *mmi = (MINMAXINFO*) lParam;
                // according to docs, the structure is supposed to be initialized with the system defaults
                mmi->ptMinTrackSize.x = MAX(lastWidthRequest_, mmi->ptMinTrackSize.x);
                mmi->ptMinTrackSize.y = MAX(lastHeightRequest_, mmi->ptMinTrackSize.y);
                //g_debug("setting min size to %dx%d", mmi->ptMinTrackSize.x, mmi->ptMinTrackSize.y);
                return true;
            }
            break;
    }

    return false;
}
