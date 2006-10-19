/* HippoToolTip.cpp: tooltip window
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"
#include <hippo/hippo-graphics.h>

#include "HippoToolTip.h"
#include <glib.h>

HippoToolTip::HippoToolTip()
{
    setSelfSizing(true);

    // standard Windows control
    setClassName(TOOLTIPS_CLASS);

    // NOPREFIX means don't parse ampersand as for menu items
    // ALWAYSTIP means the tip can appear on unfocused windows, but 
    // since we show/hide it manually this is irrelevant anyway
    // The WS_POPUP and WS_EX_TOOLWINDOW styles are implied by the class, 
    // but we put them in so HippoAbstractWindow knows about them
    setWindowStyle(WS_POPUP | WS_EX_TOOLWINDOW | TTS_NOPREFIX | TTS_ALWAYSTIP);

    forArea_.x = 0;
    forArea_.y = 0;
    forArea_.width = 0;
    forArea_.height = 0;
}

bool
HippoToolTip::create()
{
    g_assert(forWindow_ != NULL);

    if (!isCreated()) {
        if (!HippoAbstractWindow::create())
            return false;

        g_assert(window_ != NULL);

        TOOLINFO ti;
        getTrackingToolInfo(&ti);

        if (!SendMessage(window_, TTM_ADDTOOL, 0, (LPARAM)&ti)) {
            hippoDebugLogW(L"Failed to send TTM_ADDTOOL");
            return false;
        }

        // set a width, which leads to line wrapping (otherwise you can get hugely wide tips).
        // the wrapping is like Pango WRAP_WORD not WRAP_CHAR i.e. you must have whitespace
        if (!SendMessage(window_, TTM_SETMAXTIPWIDTH, 0, 300)) {
            hippoDebugLogW(L"Failed to set max tooltip width");
            return false;
        }
    }

    return true;
}

void
HippoToolTip::setForWindow(HWND forWindow)
{
    g_assert(!isCreated());

    forWindow_ = forWindow;
}

void
HippoToolTip::getTrackingToolInfo(TOOLINFO *ti)
{
    g_assert(window_ != NULL);

    memset(ti, 0, sizeof(TOOLINFO));

    ti->cbSize = sizeof(TOOLINFO);
    ti->uFlags = TTF_TRACK | TTF_ABSOLUTE;
    ti->hwnd = forWindow_;
    // we use the forWindow_ for this, but don't specify uFlags=TTF_IDISHWND
    // which means Windows thinks the tooltip goes with ti->rect instead
    // of with a window. TTF_IDISHWND will lead to ignoring ti->rect.
    // However, since ti->rect and ti->hwnd are ignored anyway with TTF_TRACK,
    // it doesn't even matter.
    ti->uId = (UINT_PTR) forWindow_;
    // name of text resource to get from instance, or TEXTCALLBACK to use 
    // TTN_GETDISPINFO, or if hinst is NULL, a pointer to a string.
    // we say callback here but then set it to a string later.
    ti->lpszText = LPSTR_TEXTCALLBACK;
    // instance to get the text resource from
    ti->hinst  = NULL;
    // With TTF_TRACK afaict this is just ignored. It would be 
    // convenient if it were not, but I can't make it do anything.
    ti->rect.left = ti->rect.top = ti->rect.bottom = ti->rect.right = 0;
}

void
HippoToolTip::activate(const HippoRectangle *for_area,
                       const char           *text)
{
    TOOLINFO ti;

    hippoDebugLogU("Activating tooltip '%s' for area %d,%d %dx%d window %p",
        text, for_area->x, for_area->y, for_area->width, for_area->height, forWindow_);

    forArea_ = *for_area;
    text_.setUTF8(text);

    if (!create())
        return;

    g_assert(window_ != NULL);

    getTrackingToolInfo(&ti); // init the hwnd and uId fields

     // needs to be NULL or LPSTR_TEXTCALLBACK will be taken as a buffer to fill, afaict
    ti.lpszText = NULL;
    if (!SendMessage(window_, TTM_GETTOOLINFO, 0, (LPARAM) &ti)) {
        hippoDebugLogW(L"Failed to send TTM_GETTOOLINFO");
        return;
    }

    if (ti.hwnd != forWindow_) {
        hippoDebugLogW(L"Got hwnd %p expecting %p for tooltip window", ti.hwnd, forWindow_);
    }

    // the docs imply we could only set ti.lpszText here if it were guaranteed
    // <80 chars, but this seems to work fine with long strings. If it 
    // ever does not, the fix would be to have the forWindow_ reply to the 
    // TTN_GETDISPINFO notification.
    ti.lpszText = text_.m_str; // LPSTR_TEXTCALLBACK;
    ti.hinst = NULL; // interpret lpszText as a string instead of resource name

    // return value of this is meaningless, don't try to use it
    // to detect success/failure
    SendMessage(window_, TTM_SETTOOLINFO, 0, (LPARAM) &ti);

    // Now position tooltip in screen coordinates
    POINT where;
    where.x = for_area->x;
    where.y = for_area->y + for_area->height;
    MapWindowPoints(forWindow_, NULL, &where, 1);
    SendMessage(window_, TTM_TRACKPOSITION, 0, MAKELONG(where.x, where.y));

    // raise (even above "on top" windows)
    SetWindowPos(window_,
                 HWND_TOPMOST,
                 0, 0, 0, 0,
                 SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);

    if (!SendMessage(window_, TTM_TRACKACTIVATE, (WPARAM)TRUE, (LPARAM)&ti)) {
        hippoDebugLogW(L"Failed to send TTM_TRACKACTIVATE to activate");
        return;
    }

    markShowing(true);
}

void
HippoToolTip::deactivate()
{
    TOOLINFO ti;

    if (!isShowing())
        return;

    hippoDebugLogW(L"Deactivating tooltip");

    getTrackingToolInfo(&ti);

    if (!SendMessage(window_, TTM_TRACKACTIVATE, (WPARAM)FALSE, (LPARAM)&ti)) {
        hippoDebugLogW(L"Failed to send TTM_TRACKACTIVATE to deactivate");
        return;
    }

    markShowing(false);
}

void
HippoToolTip::show(bool activate)
{
    // this does not make sense
    hippoDebugLogW(L"Showing a tooltip without giving its text or location");
}

void
HippoToolTip::hide()
{
    deactivate();
}
