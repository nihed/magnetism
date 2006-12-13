/* HippoScrollbar.cpp: scrollbar control
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"
#include <hippo/hippo-graphics.h>

#include "HippoScrollbar.h"
#include <glib.h>

HippoScrollbar::HippoScrollbar()
    : orientation_(HIPPO_ORIENTATION_HORIZONTAL), minPos_(0), maxPos_(0), pageSize_(0), widthReq_(0), heightReq_(0)
{
    // standard Windows control
    setClassName(L"SCROLLBAR");

    // we defaulted to orientation horizontal, change to 
    // vertical (the default) to trigger setting window style,
    // widthReq_ and heightReq_
    setOrientation(HIPPO_ORIENTATION_VERTICAL);
}

bool
HippoScrollbar::create()
{
    if (!HippoAbstractControl::create())
        return false;

    syncBounds();

    return true;
}

void
HippoScrollbar::onSizeChanged()
{
}

void
HippoScrollbar::setOrientation(HippoOrientation orientation)
{
    if (orientation_ == orientation)
        return;

    orientation_ = orientation;
    setWindowStyle(WS_CHILD | (orientation_ == HIPPO_ORIENTATION_VERTICAL ? 
                   SBS_VERT : SBS_HORZ));

    if (orientation_ == HIPPO_ORIENTATION_VERTICAL) {
        widthReq_ = GetSystemMetrics(SM_CXVSCROLL); // width of vscrollbar
        heightReq_ = GetSystemMetrics(SM_CYVSCROLL) * 2 + 5; // height of two scroll arrows plus arbitrary 5 for the bar
    } else {
        widthReq_ = GetSystemMetrics(SM_CXHSCROLL) * 2 + 5; // width of two scroll arrows plus arbitrary 5 for the bar
        heightReq_ = GetSystemMetrics(SM_CYHSCROLL); // height of hscrollbar
    }

    // g_debug("scrollbar widthReq_ %d heightReq_ %d", widthReq_, heightReq_);
}

void
HippoScrollbar::setBounds(int minPos,
                          int maxPos,
                          int pageSize)
{
    if (minPos_ == minPos && maxPos_ == maxPos && pageSize_ == pageSize)
        return;
    minPos_ = minPos;
    maxPos_ = maxPos;
    pageSize_ = pageSize;
    syncBounds();
}

void
HippoScrollbar::syncBounds()
{
    if (isCreated()) {
        SCROLLINFO si;
        si.cbSize = sizeof(si);
        si.fMask = SIF_DISABLENOSCROLL | SIF_PAGE | SIF_RANGE;
        si.nMin = minPos_;
        si.nMax = maxPos_;
        si.nPage = pageSize_;
        SetScrollInfo(window_, SB_CTL, &si, true);
    }
}

int
HippoScrollbar::handleScrollMessage(UINT   message,
                                    WPARAM wParam,
                                    LPARAM lParam)
{
    switch (message) {
        case WM_MOUSEWHEEL:
            return handleWheelMessage(message, wParam, lParam);
        case WM_HSCROLL:
        case WM_VSCROLL:
            return handleDragMessage(message, wParam, lParam);
        default:
            return 0;
    }
}

int
HippoScrollbar::handleWheelMessage(UINT   message,
                                   WPARAM wParam,
                                   LPARAM lParam)
{
    g_return_val_if_fail(message == WM_MOUSEWHEEL, 0);

    /* delta/WHEEL_DELTA is the number of "clicks" of scrolling.
     * it's negative to scroll down.
     */
    int delta = GET_WHEEL_DELTA_WPARAM(wParam);
    int increment;
    if (delta < 0) {
        increment = SB_LINEDOWN;
        delta = - delta; // fix it to always be positive
    } else {
        increment = SB_LINEUP;
    }

    SCROLLINFO si;
    si.cbSize = sizeof(si);
    si.fMask = SIF_POS;
    GetScrollInfo(window_, SB_CTL, &si);

    int currentPos = si.nPos;
    int newPos = getScrollIncrement(increment, delta / (double) WHEEL_DELTA, currentPos);

    if (newPos != currentPos) {
        si.fMask = SIF_POS;
        si.nPos = newPos;
        SetScrollInfo(window_, SB_CTL, &si, true);
    }

    return newPos;
}

int
HippoScrollbar::handleDragMessage(UINT   message,
                                  WPARAM wParam,
                                  LPARAM lParam)
{
    g_return_val_if_fail((message == WM_HSCROLL && orientation_ == HIPPO_ORIENTATION_HORIZONTAL) ||
                         (message == WM_VSCROLL && orientation_ == HIPPO_ORIENTATION_VERTICAL), 0);

    // Note, Windows packs a 16-bit scroll position into the message 
    // params, but we want to get the 32-bit position instead with GetScrollInfo

    // The "track position" is where the user moved the bar, and the 
    // "position" is where we set it. If we don't set it, then user movements
    // have no effect (the scrollbar will "bounce back" on mouse release).

    SCROLLINFO si;
    si.cbSize = sizeof(si);
    si.fMask = SIF_POS | SIF_TRACKPOS;
    GetScrollInfo(window_, SB_CTL, &si);

    int currentPos = si.nPos;
    int currentTrackPos = si.nTrackPos;
    int newPos = currentPos;

    switch (LOWORD(wParam)) {
    case SB_PAGEDOWN:
    case SB_LINEUP:
    case SB_LINEDOWN:
    case SB_PAGEUP:
        newPos = getScrollIncrement(LOWORD(wParam), 1.0, currentPos);
        break;
    case SB_THUMBPOSITION:
        // this is an update when we set the scroll position ourselves
        break;
    case SB_THUMBTRACK:
        newPos = currentTrackPos;
        clampPosition(&newPos);
        break;
    default:
        break;
    }

    if (newPos != currentPos) {
        si.fMask = SIF_POS;
        si.nPos = newPos;
        SetScrollInfo(window_, SB_CTL, &si, true);
    }

    return newPos;
}

void
HippoScrollbar::clampPosition(int *posPtr)
{
    int newPos = *posPtr;

    if (newPos > (maxPos_ - pageSize_ - 1))
        newPos = maxPos_ - pageSize_ - 1;

    if (newPos < 0) 
        newPos = 0;

    *posPtr = newPos;
}

int
HippoScrollbar::getScrollIncrement(int increment, double count, int currentPos)
{
    int newPos = currentPos;

    switch (increment) {
    case SB_PAGEUP:
        newPos -= MAX((int) (pageSize_ * 0.9 * count), 1);
        break;
    case SB_PAGEDOWN:
        newPos += MAX((int) (pageSize_ * 0.9 * count), 1);
        break;
    case SB_LINEUP:
        newPos -= MAX((int) (pageSize_ * 0.1 * count), 1);
        break;
    case SB_LINEDOWN:
        newPos += MAX((int) (pageSize_ * 0.1 * count), 1);
        break;
    case SB_THUMBPOSITION:
    case SB_THUMBTRACK:
    default:
        g_warning("invalid scroll increment type");
        break;
    }

    clampPosition(&newPos);

    return newPos;
}

bool 
HippoScrollbar::processMessage(UINT   message,
                               WPARAM wParam,
                               LPARAM lParam)
{
    g_warning("We aren't expecting our window proc to be called on common control class SCROLLBAR");
    return HippoAbstractControl::processMessage(message, wParam, lParam);
}

int
HippoScrollbar::getWidthRequestImpl()
{
    return widthReq_;
}

int
HippoScrollbar::getHeightRequestImpl(int forWidth)
{
    return heightReq_;
}
