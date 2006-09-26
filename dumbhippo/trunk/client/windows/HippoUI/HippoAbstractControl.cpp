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
            // Override the minimum width/height of the window if it's a toplevel
            if (parent_ == NULL) {
                RECT windowArea_;
                RECT clientArea_;
                GetWindowRect(window_, &windowArea_);
                GetClientRect(window_, &clientArea_);
                int hrequest = lastWidthRequest_ + (windowArea_.right - windowArea_.left) - (clientArea_.right - clientArea_.left);
                int vrequest = lastHeightRequest_ + (windowArea_.bottom - windowArea_.top) - (clientArea_.bottom - clientArea_.top);
                MINMAXINFO *mmi = (MINMAXINFO*) lParam;
                // according to docs, the structure is supposed to be initialized with the system defaults.
                // The min/max track sizes include the window frame, not just client area.
                mmi->ptMinTrackSize.x = MAX(hrequest, mmi->ptMinTrackSize.x);
                mmi->ptMinTrackSize.y = MAX(vrequest, mmi->ptMinTrackSize.y);
                //g_debug("setting min size to %dx%d", mmi->ptMinTrackSize.x, mmi->ptMinTrackSize.y);
                return true;
            }
            break;
    }

    return false;
}
