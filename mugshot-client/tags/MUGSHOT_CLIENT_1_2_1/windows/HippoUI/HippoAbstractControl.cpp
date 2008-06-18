/* HippoAbstractControl.cpp: a "widget" (wrapper around an HWND)
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"

#include "HippoAbstractControl.h"
#include <hippo/hippo-canvas-item.h>

HippoAbstractControl::HippoAbstractControl()
    : lastWidthRequest_(0), lastHeightRequest_(0), parent_(0),
      hresizable_(true), vresizable_(true),
      requestChangedSinceRequest_(true), requestChangedSinceAllocate_(true), insideAllocation_(false),
      canvasItem_(NULL)
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
        parent->markRequestChanged();
    }
    if (parent_) {
        parent_->markRequestChanged();
        parent_->Release();
    }
    parent_ = parent;
    setCreateWithParent(parent_);
    
    if (parent_) {
        if (parent_->isCreated())
            create();
        if (parent_->isShowing())
            show(false);
    }

    // for now we don't markRequestChanged on the control itself,
    // since changing parent in theory doesn't affect that ...
}

void
HippoAbstractControl::setResizable(HippoOrientation orientation,
                                   bool             value)
{
    if (orientation == HIPPO_ORIENTATION_VERTICAL) {
        if (value == vresizable_)
            return;
        vresizable_ = value;
    } else {
        if (value == hresizable_)
            return;
        hresizable_ = value;
    }
    markRequestChanged();
}

void
HippoAbstractControl::setCanvasItem(GObject *item)
{
    canvasItem_ = item;
    if (canvasItem_ && requestChangedSinceAllocate_) {
        // sync up the request changed flag on the item
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(canvasItem_));
    }
}

bool
HippoAbstractControl::create()
{
    bool result;

    if (isCreated())
        return true;

    if (ui_ == NULL && parent_) {
        setUI(parent_->ui_);
    }

    ensureRequestAndAllocation(); // get our default size

    result = HippoAbstractWindow::create();

    createChildren();

    return result;
}

void
HippoAbstractControl::createChildren()
{
    // intended to be overrided, base class does nothing
}

void
HippoAbstractControl::show(bool activate)
{
    if (!create()) {
        g_warning("failed to create control");
        return;
    }
    ensureRequestAndAllocation();         // get our size right
    showChildren();                       // show our children
    HippoAbstractWindow::show(activate);  // actually show
}

void
HippoAbstractControl::showChildren()
{
    // intended to be overrided, base class does nothing
}

void
HippoAbstractControl::sizeAllocate(const HippoRectangle *rect)
{
#if 0
    g_debug("SIZING: sizeAllocate %p %s to %d,%d %dx%d from %d,%d %dx%d requestChanged = %d",
        window_, HippoUStr(getClassName()).c_str(),
        rect->x, rect->y, rect->width, rect->height,
        getX(), getY(), getWidth(), getHeight(),
        requestChangedSinceAllocate_);
#endif

    HippoRectangle old;

    getClientArea(&old);

    // if the control's request hasn't changed (no re-request was queued) 
    // then a size allocation can be short-circuited when nothing has 
    // been modified. Otherwise, the control is owed at least one allocation.
    //
    // This short-circuit is required to avoid recursive size allocate
    // since moveResizeWindow generates a WM_SIZE/WM_MOVE which would 
    // potentially re-allocate.
    if (!requestChangedSinceAllocate_ && hippo_rectangle_equal(rect, &old)) {
        return;
    }

    if (insideAllocation_) {
        g_warning("%s recursively size allocated",
                HippoUStr(getClassName()).c_str());
        return;
    }

    if (parent_ && parent_->requestChangedSinceAllocate_) {
        // Parent is going to get re-requested/allocated anyways, so wait for that
        return;
    }

    requestChangedSinceAllocate_ = false;
    insideAllocation_ = true;

    moveResizeWindow(rect->x, rect->y, rect->width, rect->height);

    if (requestChangedSinceAllocate_) {
        g_warning("%s changed its size request inside moveResizeWindow()",
                HippoUStr(getClassName()).c_str());
        // try to avoid the infinite loop
        requestChangedSinceAllocate_ = false;
    }

    bool sizeChanged = (getWidth() != old.width || getHeight() != old.height);

    // children get allocated in here
    onSizeAllocated();

    if (requestChangedSinceAllocate_) {
        g_warning("%s changed its size request inside onSizeAllocated()",
                HippoUStr(getClassName()).c_str());
        // try to avoid the infinite loop
        requestChangedSinceAllocate_ = false;
    }

    if (sizeChanged)
        invalidate(0, 0, getWidth(), getHeight());

    insideAllocation_ = false;
}

void
HippoAbstractControl::sizeAllocate(int x, int y, int width, int height)
{
    HippoRectangle rect = { x, y, width, height };
    sizeAllocate(&rect);
}

void
HippoAbstractControl::sizeAllocate(int width, int height)
{
    sizeAllocate(getX(), getY(), width, height);
}

void
HippoAbstractControl::onSizeAllocated()
{
    // just a callback, doesn't do anything in base class
}

void
HippoAbstractControl::markRequestChanged()
{
#if 0
    g_debug("SIZING: markRequestChanged %p %s",
        window_, HippoUStr(getClassName()).c_str());
#endif

    if (insideAllocation_) {
        g_warning("%s tried to change its size request inside size allocate",
                HippoUStr(getClassName()).c_str());
        return;
    }

    if (!requestChangedSinceAllocate_) {
        requestChangedSinceAllocate_ = true;
        requestChangedSinceRequest_ = true;
        // send it up to the parent, the topmost parent is supposed to 
        // do an idle handler then allocate all its children. This 
        // also preserves the invariant that if a child has changed its
        // request, the parent also has
        if (parent_)
            parent_->markRequestChanged();
        if (canvasItem_)
            hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(canvasItem_));
        onRequestChanged();
    }
}

void 
HippoAbstractControl::onRequestChanged()
{
    // callback, does nothing in this base class
}

void
HippoAbstractControl::ensureRequestAndAllocation()
{
#if 0
    g_debug("SIZING: ensureRequestAndAllocation requestChanged = %d %p %s",
        requestChangedSinceAllocate_, window_, HippoUStr(getClassName()).c_str());
#endif

    if (!requestChangedSinceAllocate_)
        return;

    if (insideAllocation_) {
        g_warning("control %s doing ensureRequestAndAllocation from inside allocation",
                HippoUStr(getClassName()).c_str());
    }

    if (parent_) {
        if (insideAllocation_) {
            g_warning("parent %s is inside allocation when child %s is doing ensureRequestAndAllocation",
                    HippoUStr(parent_->getClassName()).c_str(), HippoUStr(getClassName()).c_str());
        }

        if (!parent_->requestChangedSinceAllocate_) {
            g_warning("child %s request has been marked changed but parent %s request has not",
                HippoUStr(getClassName()).c_str(), HippoUStr(parent_->getClassName()).c_str());
        }
        // this should result in ourselves being allocated
        parent_->ensureRequestAndAllocation();
        if (requestChangedSinceAllocate_) {
            g_warning("%s was not allocated by parent",
                HippoUStr(getClassName()).c_str());
        }
    } else {
        // we are either a toplevel or an orphan
        int w = getWidthRequest();
        int h = getHeightRequest(w);

        int oldW = getWidth();
        int oldH = getHeight();

        int newW, newH;

        if (isHResizable()) {
            newW = MAX(w, oldW);
        } else {
            newW = w;
        }

        if (isVResizable()) {
            newH = MAX(h, oldH);
        } else {
            newH = h;
        }

        sizeAllocate(newW, newH);

        if (requestChangedSinceAllocate_) {
            g_warning("%s size allocation did not work?",
                HippoUStr(getClassName()).c_str());
        }
    }
}

void
HippoAbstractControl::onMoveResizeMessage(const HippoRectangle *newClientArea)
{
    // FIXME this should just queue a sizeAllocate, so we get some compression
    // of WM_SIZE handling - WM_PAINT gets compressed by Windows, but WM_SIZE apparently 
    // does not. We may need to split queuing this from queuing a request changed (or
    // at least it might be easier to do so).
    // Anyway, for now it's not actively a big problem that we know of, so leaving it.
    sizeAllocate(newClientArea);
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
    requestChangedSinceRequest_ = false;
    return h;
}

void
HippoAbstractControl::getLastRequest(int *width_p, 
                                     int *height_p)
{
    if (requestChangedSinceRequest_) {
        g_warning("%s asked for its last request while its request was invalid",
                HippoUStr(getClassName()).c_str());
    }

    if (width_p)
        *width_p = lastWidthRequest_;
    if (height_p)
        *height_p = lastHeightRequest_;
}

bool
HippoAbstractControl::handleNotification(UINT notification)
{
    // No notifications for the base class; child classes can override

    return false;
}


bool
HippoAbstractControl::processMessage(UINT   message,
                                     WPARAM wParam,
                                     LPARAM lParam)
{
    if (HippoAbstractWindow::processMessage(message, wParam, lParam))
        return true;

    switch (message) {
        case WM_PAINT:
            // subclasses should chain up to this, then paint
            ensureRequestAndAllocation();
            return false;

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

                // don't be deceived by mmi->ptMaxSize which is the maximized size, not the maximum size

                if (!hresizable_) {
                    mmi->ptMaxTrackSize.x = mmi->ptMinTrackSize.x;
                }

                if (!vresizable_) {
                    mmi->ptMaxTrackSize.y = mmi->ptMinTrackSize.y;
                }

                return true;
            }
            break;
    }

    return false;
}
