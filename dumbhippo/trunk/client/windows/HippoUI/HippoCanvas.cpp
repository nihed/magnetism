/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoCanvas.cpp: a control that contains a canvas item
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"
#include <windowsx.h> // GET_X_LPARAM seems to be in here, though I'm not sure it's the right file to include
#include "HippoCanvas.h"
#include "HippoScrollbar.h"
#include "HippoImageFactory.h"

#include <cairo-win32.h>

// pangowin32 wants to define STRICT itself
#undef STRICT
#include <pango/pangowin32.h>
#define STRICT
#include <pango/pangocairo.h>

typedef struct _HippoCanvasContextWinClass HippoCanvasContextWinClass;

#define HIPPO_TYPE_CANVAS_CONTEXT_WIN              (hippo_canvas_context_win_get_type ())
#define HIPPO_CANVAS_CONTEXT_WIN(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_CONTEXT_WIN, HippoCanvasContextWin))
#define HIPPO_CANVAS_CONTEXT_WIN_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_CONTEXT_WIN, HippoCanvasContextWinClass))
#define HIPPO_IS_CANVAS_CONTEXT_WIN(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_CONTEXT_WIN))
#define HIPPO_IS_CANVAS_CONTEXT_WIN_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_CONTEXT_WIN))
#define HIPPO_CANVAS_CONTEXT_WIN_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_CONTEXT_WIN, HippoCanvasContextWinClass))

static GType                  hippo_canvas_context_win_get_type               (void) G_GNUC_CONST;
static HippoCanvasContextWin* hippo_canvas_context_win_new                    (HippoCanvas *canvas);
static void                   hippo_canvas_context_win_update_pango           (HippoCanvasContextWin *context_win,
                                                                               cairo_t               *cr);
static void                   hippo_canvas_context_win_create_controls        (HippoCanvasContextWin *context_win);
static void                   hippo_canvas_context_win_show_controls          (HippoCanvasContextWin *context_win);

HippoCanvas::HippoCanvas()
    : canvasWidthReq_(0), canvasHeightReq_(0), canvasX_(0), canvasY_(0), hscrollNeeded_(false), vscrollNeeded_(false),
      hscrollbarPolicy_(HIPPO_SCROLLBAR_NEVER), vscrollbarPolicy_(HIPPO_SCROLLBAR_NEVER),
      containsMouse_(false), pointer_(HIPPO_CANVAS_POINTER_UNSET)
{
    HippoCanvasContextWin *context;

    setClassName(L"HippoCanvasClass");
    setClassStyle(CS_HREDRAW | CS_VREDRAW);
    setTitle(L"Canvas");

    context = hippo_canvas_context_win_new(this);
    context_ = context;
    g_object_unref((void*) context); // lose the extra reference
    g_assert(HIPPO_IS_CANVAS_CONTEXT(context_));

    hscroll_ = new HippoScrollbar();
    hscroll_->Release(); // lose extra ref
    hscroll_->setOrientation(HIPPO_ORIENTATION_HORIZONTAL);

    vscroll_ = new HippoScrollbar();
    vscroll_->Release();
    
    hscroll_->setParent(this);
    vscroll_->setParent(this);
}


void
HippoCanvas::onRootRequestChanged()
{
    markRequestChanged();
}

void
HippoCanvas::onRootPaintNeeded(const HippoRectangle *damage_box)
{
    int cx, cy;

    getCanvasOrigin(&cx, &cy);
    invalidate(cx + damage_box->x, cy + damage_box->y, damage_box->width, damage_box->height);
}

void
HippoCanvas::setRoot(HippoCanvasItem *item)
{
    if (root_ == item)
        return;

    rootRequestChanged_.disconnect();
    rootPaintNeeded_.disconnect();

    root_ = item;
    if (item) {
        rootRequestChanged_.connect(G_OBJECT(item), "request-changed",
            slot(this, &HippoCanvas::onRootRequestChanged));
        rootPaintNeeded_.connect(G_OBJECT(item), "paint-needed",
            slot(this, &HippoCanvas::onRootPaintNeeded));
        if (isCreated()) {
            g_assert(HIPPO_IS_CANVAS_CONTEXT(context_));
            hippo_canvas_item_set_context(item, HIPPO_CANVAS_CONTEXT(context_));
        }
    }
    markRequestChanged();
}

void
HippoCanvas::setScrollbarPolicy(HippoOrientation     orientation,
                                HippoScrollbarPolicy policy)
{
    if (orientation == HIPPO_ORIENTATION_VERTICAL) {
        if (policy == vscrollbarPolicy_)
            return;

        vscrollbarPolicy_ = policy;
    } else {
        if (policy == hscrollbarPolicy_)
            return;

        hscrollbarPolicy_ = policy;
    }

    markRequestChanged();
}

void
HippoCanvas::createChildren()
{
    hscroll_->create();
    vscroll_->create();

    // this should register any embedded controls, set their parents,
    // which as a side effect should create them all
    if (root_ != (HippoCanvasItem*) NULL) {
        g_assert(HIPPO_IS_CANVAS_CONTEXT(context_));
        hippo_canvas_item_set_context(root_, HIPPO_CANVAS_CONTEXT(context_));
    }
}

void 
HippoCanvas::showChildren()
{
    // we don't call updateScrollbars here because 
    // it does a lot of extra work, in particular
    // recursively allocating all canvas items.
    // so we just show these if they are already 
    // known to be needed. The toplevel window control
    // normally shows children, then does a resize cycle,
    // then shows itself, which will sort things out.
    if (hscrollNeeded_)
        hscroll_->show(false);
    if (vscrollNeeded_)
        vscroll_->show(false);

    hippo_canvas_context_win_show_controls(context_);
}

int
HippoCanvas::computeChildWidthRequest()
{
    if (root_ != (HippoCanvasItem*) NULL) {
        return hippo_canvas_item_get_width_request(root_);
    } else {
        return 0;
    }
}

int
HippoCanvas::computeChildHeightRequest(int forWidth)
{
    if (root_ != (HippoCanvasItem*) NULL) {
        return hippo_canvas_item_get_height_request(root_, forWidth);
    } else {
        return 0;
    }
}

void
HippoCanvas::clearCachedChildHeights()
{
    for (int hscrollbar = 0; hscrollbar <= 1; hscrollbar++)
        for (int vscrollbar = 0; vscrollbar <= 1; vscrollbar++)
            childHeightReq_[hscrollbar][vscrollbar] = -1;
}

int
HippoCanvas::getWidthRequestImpl()
{
    childWidthReq_ = computeChildWidthRequest();
    clearCachedChildHeights();
    
    int baseWidth;
    switch (hscrollbarPolicy_) {
    case HIPPO_SCROLLBAR_NEVER:
        baseWidth = childWidthReq_;
        break;
    case HIPPO_SCROLLBAR_AUTOMATIC:
        baseWidth = MIN(childWidthReq_, hscroll_->getWidthRequest());
        break;
    case HIPPO_SCROLLBAR_ALWAYS:
        baseWidth = hscroll_->getWidthRequest();
        break;
    }

    // We have to be careful about a pathology here - in the case where we have
    // an automatic vertical scrollbar, it logically makes sense to allow
    // resizing to the width of the child without a scrollbar, and have that
    // force off the scrollbar, but the practical consequences of that are
    // unattractive.... the window might suddenly become hundreds of thousands
    // of pixels high. So we always include an automatic vertical scrollbar
    // in our minimum width. (We could do a bit better: if 
    // getChildHeightRequest(baseWidth) is less than the minimum
    // scrollbar height, then we know that we will never need a vertical scrollbar.)

    switch (vscrollbarPolicy_) {
    case HIPPO_SCROLLBAR_NEVER:
        canvasWidthReq_ = baseWidth;
        break;
    case HIPPO_SCROLLBAR_AUTOMATIC:
    case HIPPO_SCROLLBAR_ALWAYS:
        canvasWidthReq_ = baseWidth + vscroll_->getWidthRequest();
        break;
    }

    return canvasWidthReq_;
}

// Computes the child's height requisition for the current value of 'currentWidth_' and the
// given scrolllbar combination, if the combination is impossible, we return G_MAXINT, but
// we really are never supposed to hit that code path.
int
HippoCanvas::getChildHeightRequest(bool hscrollbar, bool vscrollbar)
{
    if (childHeightReq_[hscrollbar][vscrollbar] != -1)
        return childHeightReq_[hscrollbar][vscrollbar];

    int newValue;

    if (hscrollbar) {
        if (vscrollbar) {
            newValue = computeChildHeightRequest(MAX(childWidthReq_, currentWidth_ - vscroll_->getWidthRequest()));
        } else {
            newValue = computeChildHeightRequest(MAX(childWidthReq_, currentWidth_));
        }
    } else {
        int availableWidth = currentWidth_;
        if (vscrollbar)
            availableWidth -= vscroll_->getWidthRequest();
        
        if (availableWidth >= childWidthReq_)
            newValue = computeChildHeightRequest(availableWidth);
        else
            newValue = G_MAXINT;
    }

    childHeightReq_[hscrollbar][vscrollbar] = newValue;

    return newValue;
}

// Computes our height requisition for the current value of 'currentWidth_' and the
// given scrolllbar combination. If the combination is impossible, we return
// G_MAXINT
int
HippoCanvas::getCanvasHeightRequest(bool hscrollbar, bool vscrollbar)
{
    if (hscrollbar) {
        if (vscrollbar) {
            return vscroll_->getHeightRequest(vscroll_->getWidthRequest()) + hscroll_->getHeightRequest(100);
        } else {
            return getChildHeightRequest(hscrollbar, vscrollbar) + hscroll_->getHeightRequest(100);
        }
    } else {
        int availableWidth = currentWidth_;
        if (vscrollbar)
            availableWidth -= vscroll_->getWidthRequest();

        if (availableWidth < childWidthReq_)
            return G_MAXINT;

        if (vscrollbar)
            return vscroll_->getHeightRequest(vscroll_->getWidthRequest());
        else
            return getChildHeightRequest(hscrollbar, vscrollbar);
    }
}

int
HippoCanvas::getHeightRequestImpl(int forWidth)
{
    if (forWidth != currentWidth_) {
        clearCachedChildHeights();
        currentWidth_ = forWidth;
    }
    
    int minHeightRequest = G_MAXINT;

    // If the width passed in is smaller than the minimum possible width, pretend
    // we got that width
    if (forWidth < canvasWidthReq_)
        forWidth = canvasWidthReq_;

    // Go through all possibilities for scrollbars allowed by the current scrollbar
    // policy and find the one with the minimum required height for this width
    bool found = false;
    for (int hscrollbar = 0; hscrollbar <= 1; hscrollbar++) {
        if (!hscrollbar && hscrollbarPolicy_ == HIPPO_SCROLLBAR_ALWAYS)
            continue;
        if (hscrollbar && hscrollbarPolicy_ == HIPPO_SCROLLBAR_NEVER)
            continue;
            
        for (int vscrollbar = 0; vscrollbar <= 1; vscrollbar++) {
            if (!vscrollbar && vscrollbarPolicy_ == HIPPO_SCROLLBAR_ALWAYS)
                continue;
            if (vscrollbar && vscrollbarPolicy_ == HIPPO_SCROLLBAR_NEVER)
                continue;

            int heightRequest = getCanvasHeightRequest(hscrollbar != 0, vscrollbar != 0);
            if (heightRequest < minHeightRequest) {
                found = true;
                minHeightRequest = heightRequest;
            }
        }
    }
    
    if (!found) {
        // This should not happen if our logic is correct
        g_warning("HippoCanvas::getHeightRequestImpl didn't find a possible scrollbar combination!");
        canvasHeightReq_ = 100;
    } else {
        canvasHeightReq_ = minHeightRequest;
    }
    
    return canvasHeightReq_;
}

bool
HippoCanvas::tryAllocate(bool hscrollbar, bool vscrollbar)
{
    int w = getWidth();
    int h = getHeight();

    // If we get called with something other than the 'forWidth' passed to getHeightRequestImpl()
    // we need to redo that
    if (w != currentWidth_)
        getHeightRequestImpl(w);

    // If we get called with something smaller than our minimum size, just allocate as if we
    // had our minimum size
    if (w < canvasWidthReq_)
        w = canvasWidthReq_;
    if (h < canvasHeightReq_)
        h = canvasHeightReq_;

    // Compute scrollbar sizes
    int vWidth = vscrollbar ? vscroll_->getWidthRequest() : 0;
    int hWidth = hscrollbar ? w - vWidth : 0;
    int hHeight = hscrollbar ? hscroll_->getHeightRequest(hWidth) : 0;
    int vHeight = vscrollbar ? h - hHeight : 0;

    // See if this scrollbar combination is a possibility
    if (!hscrollbar) {
        if (w - vWidth < childWidthReq_)
            return false;
    }
    
    int childHeightRequest = getChildHeightRequest(hscrollbar, vscrollbar);
    if (!vscrollbar) {
        if (h - hHeight < childHeightRequest)
            return false;
    }

    // OK, it's possible

    hscrollNeeded_ = hscrollbar;
    vscrollNeeded_ = vscrollbar;

    // Compute the size we are going to allocate our child
    
    int childWidthAlloc;
    if (hscrollbar)
        childWidthAlloc = MAX(childWidthReq_, w - vWidth);
    else
        childWidthAlloc = w - vWidth;

    int childHeightAlloc;
    if (vscrollbar)
        childHeightAlloc = MAX(childHeightRequest, h - hHeight);
    else
        childHeightAlloc = h - hHeight;
    
#if 0
        g_debug("updating scrollbars %d x %d h=%d v=%d",
        w, h, hscrollNeeded_, vscrollNeeded_);
#endif

    // hide if needed, then resize, then show if needed,
    // means less flashing

    if (!vscrollNeeded_) {
        vscroll_->hide();
    }
    if (!hscrollNeeded_) {
        hscroll_->hide();
    }

    if (vscrollNeeded_) {
        //g_debug("setting size of vscrollbar to %d,%d %dx%d", w - vWidth, 0, vWidth, vHeight);
        vscroll_->setBounds(0, childHeightAlloc, vHeight);
        vscroll_->sizeAllocate(w - vWidth, 0, vWidth, vHeight);
        if (isShowing())
            vscroll_->show(false);
    } else {
        // needs to get an allocation to maintain invariants
        vscroll_->sizeAllocate(0, 0, 0, 0);
    }

    if (hscrollNeeded_) {
        //g_debug("setting size of hscrollbar %d,%d %dx%d", 0, h - hHeight, hWidth, hHeight);
        hscroll_->setBounds(0, childWidthAlloc, hWidth);
        hscroll_->sizeAllocate(0, h - hHeight, hWidth, hHeight);
        if (isShowing())
            hscroll_->show(false);
    } else {
        // needs to get an allocation to maintain invariants
        hscroll_->sizeAllocate(0, 0, 0, 0);
    }
    
    if (root_ != (HippoCanvasItem*) NULL) {
        hippo_canvas_item_allocate(root_, childWidthAlloc, childHeightAlloc);
    }

    return true;
}

void
HippoCanvas::onSizeAllocated()
{
    // Go through all possibilities for scrollbars allowed by the current scrollbar
    // policy and use the first one that is possible. (Note that this is different
    // from getHeightRequestImpl(int forWidth) where we need to find the possible
    // variant with the minimum required height, so we examine all possibilities)
    for (int hscrollbar = 0; hscrollbar <= 1; hscrollbar++) {
        if (!hscrollbar && hscrollbarPolicy_ == HIPPO_SCROLLBAR_ALWAYS)
            continue;
        if (hscrollbar && hscrollbarPolicy_ == HIPPO_SCROLLBAR_NEVER)
            continue;
            
        for (int vscrollbar = 0; vscrollbar <= 1; vscrollbar++) {
            if (!vscrollbar && vscrollbarPolicy_ == HIPPO_SCROLLBAR_ALWAYS)
                continue;
            if (vscrollbar && vscrollbarPolicy_ == HIPPO_SCROLLBAR_NEVER)
                continue;
            
            if (tryAllocate(hscrollbar != 0, vscrollbar != 0))
                return;
        }
    }

    // This should not happen if our logic is correct
    g_warning("HippoCanvas::onSizeAllocated  didn't find a possible scrollbar combination!");
}

void
HippoCanvas::getCanvasOrigin(int *x_p, int *y_p)
{
    if (hscrollNeeded_)
        *x_p = - canvasX_;
    else
        *x_p = 0;
    if (vscrollNeeded_)
        *y_p = - canvasY_;
    else
        *y_p = 0;
}

void
HippoCanvas::getViewport(RECT *rect_p)
{
    rect_p->left = 0;
    rect_p->top = 0;
    rect_p->right = rect_p->left + getWidth() - (vscrollNeeded_ ? vscroll_->getWidth() : 0);
    rect_p->bottom = rect_p->top + getHeight() - (hscrollNeeded_ ? hscroll_->getHeight() : 0);
}

void
HippoCanvas::scrollTo(int newX, int newY)
{
    g_return_if_fail(hscrollbarPolicy_ != HIPPO_SCROLLBAR_NEVER || vscrollbarPolicy_ != HIPPO_SCROLLBAR_NEVER);

    int dx = hscrollNeeded_ ? canvasX_ - newX : 0;
    int dy = vscrollNeeded_ ? canvasY_ - newY : 0;

    if (dx == 0 && dy == 0)
        return;

    canvasX_ = newX;
    canvasY_ = newY;

#define SMOOTH_SCROLL_TIME 5000

    RECT viewport;
    getViewport(&viewport);
    ScrollWindowEx(window_, 
                   dx, dy,
                   &viewport, // portion of client area to scroll
                   &viewport, // clip region (do not modify bits outside it)
                   NULL,      // return for invalid region
                   NULL,      // return for invalid rectangle
                   // the HIWORD of the flags is the smooth scroll time
                   SW_INVALIDATE | SW_SCROLLCHILDREN | SW_ERASE);

                   // SW_SMOOTHSCROLL does not appear to work; some people on 
                   // the internet say it's because it only supports scrolling
                   // the entire window. If so, we would need to create a viewport 
                   // subwindow in order to use it. We probably want a viewport 
                   // subwindow and not just a whole-canvas subwindow, since
                   // 32-bit coords seem sketchy on Windows, e.g. not sure how
                   // to get motion events outside of signed 16-bit.
                   // SW_SMOOTHSCROLL | (SMOOTH_SCROLL_TIME << 16));
}

void
HippoCanvas::updatePointer(int rootItemX, int rootItemY)
{
    HippoCanvasPointer newPointer = HIPPO_CANVAS_POINTER_UNSET;

    if (root_ != (HippoCanvasItem*) NULL && containsMouse_) {
        newPointer = hippo_canvas_item_get_pointer(root_, rootItemX, rootItemY);
    }

    /* this ensures we always go unset->default or default->unset on enter/leave, 
     * so we change the pointer from e.g. the resize arrow on the window border
     */
    if (containsMouse_ && newPointer == HIPPO_CANVAS_POINTER_UNSET)
        newPointer = HIPPO_CANVAS_POINTER_DEFAULT;

    g_assert((containsMouse_ && newPointer != HIPPO_CANVAS_POINTER_UNSET) ||
             (!containsMouse_ && newPointer == HIPPO_CANVAS_POINTER_UNSET));

    if (newPointer != pointer_) {
        HCURSOR hcursor = NULL;
        switch (newPointer) {
            case HIPPO_CANVAS_POINTER_UNSET:
                hcursor = LoadCursor(NULL, IDC_ARROW);
                break;
            case HIPPO_CANVAS_POINTER_DEFAULT:
                hcursor = LoadCursor(NULL, IDC_ARROW);
                break;
            case HIPPO_CANVAS_POINTER_HAND:
                hcursor = LoadCursor(NULL, IDC_HAND);
                break;
        }
        if (hcursor == NULL) {
            g_warning("Failed to load mouse cursor");
            return;
        }
        SetCursor(hcursor);
        pointer_ = newPointer;
    }
}

// returns true if mouse is inside client area
bool
HippoCanvas::getMouseCoords(LPARAM lParam, int *x_p, int *y_p)
{
    int cx, cy;
    getCanvasOrigin(&cx, &cy);
    int mx = GET_X_LPARAM(lParam);
    int my = GET_Y_LPARAM(lParam);

    bool outsideClient = (mx < 0 || my < 0 || mx >= getWidth() || my >= getHeight());

    *x_p = mx - cx;
    *y_p = my - cy;

    return !outsideClient;
}

void
HippoCanvas::onMouseDown(int button, WPARAM wParam, LPARAM lParam)
{
    if (root_ != (HippoCanvasItem*) NULL) {
        int x, y;
        if (getMouseCoords(lParam, &x, &y)) {
            hippo_canvas_item_emit_button_press_event(root_,
                x, y, button,
                0, 0, 0);
        }
    }
}

void
HippoCanvas::onMouseUp(int button, WPARAM wParam, LPARAM lParam)
{
    if (root_ != (HippoCanvasItem*) NULL) {
        int x, y;
        getMouseCoords(lParam, &x, &y); // don't check return value - emit even if outside the area
        hippo_canvas_item_emit_button_release_event(root_,
                x, y, button,
                0, 0, 0);
    }
}

void
HippoCanvas::onMouseMove(WPARAM wParam, LPARAM lParam)
{
    int x, y;
    if (!getMouseCoords(lParam, &x, &y))
        return;

    bool entered;

    entered = false;
    if (!containsMouse_) {
        // request a WM_MOUSELEAVE message
        TRACKMOUSEEVENT tme;
        tme.cbSize = sizeof(tme);
        tme.dwFlags = TME_LEAVE;
        tme.hwndTrack = window_;
        if (TrackMouseEvent(&tme) != 0) {
            containsMouse_ = true;
            entered = true;
        } else {
            g_warning("Failed to track mouse leave");
            return;
        }
    }

    g_assert(containsMouse_);

    if (root_ != (HippoCanvasItem*) NULL) {
        if (entered) {
            hippo_canvas_item_emit_motion_notify_event(root_,
                x, y, HIPPO_MOTION_DETAIL_ENTER);
        }
        hippo_canvas_item_emit_motion_notify_event(root_,
            x, y, HIPPO_MOTION_DETAIL_WITHIN);
    }

    updatePointer(x, y);
}

void
HippoCanvas::onMouseLeave(WPARAM wParam, LPARAM lParam)
{
    if (!containsMouse_)
        return;

    containsMouse_ = false;

    if (root_ != (HippoCanvasItem*) NULL) {
        // FIXME Windows does not provide coordinates here, so we'd have to save
        // the last move event coordinates or request the current coords or something
        // like that
        hippo_canvas_item_emit_motion_notify_event(root_,
                0, 0, HIPPO_MOTION_DETAIL_LEAVE);
    }

    updatePointer(-1, -1);
}

void
HippoCanvas::onPaint(WPARAM wParam, LPARAM lParam)
{
    RECT region;
    if (GetUpdateRect(window_, &region, true)) {

        int regionWidth = region.right - region.left;
        int regionHeight = region.bottom - region.top;

#if 0
        g_debug("SIZING: %p paint region %d,%d %dx%d",
                window_, region.left, region.top,
                regionWidth, regionHeight);
#endif

        // go ahead and request/resize if necessary, so we paint the right thing
        ensureRequestAndAllocation();

        PAINTSTRUCT paint;
        HDC hdc = BeginPaint(window_, &paint);

        //g_debug("paint.fErase=%d", paint.fErase);

        cairo_surface_t *surface = cairo_win32_surface_create(hdc);
        cairo_surface_t *buffer = cairo_surface_create_similar(surface,
            CAIRO_CONTENT_COLOR, regionWidth, regionHeight);
        cairo_t *cr = cairo_create(buffer);
        hippo_canvas_context_win_update_pango(context_, cr);

        // make the buffer's coordinates look like the real coordinates
        cairo_translate(cr, - region.left, - region.top);

        // Paint a background rectangle to the buffer
        cairo_rectangle(cr, region.left, region.top, regionWidth, regionHeight);
        cairo_clip(cr);
        
        // FIXME not the right background color (on linux it's the default gtk background)
        // should use system color, maybe GetThemeSysColorBrush is right. Note that 
        // this rectangle draws the little corner between the scrollbars in 
        // addition to the viewport background.
        hippo_cairo_set_source_rgba32(cr, 0xffffffff);
        cairo_paint(cr);

        // Draw canvas item to the buffer
        if (root_ != (HippoCanvasItem*) NULL) {
            RECT viewport;
            HippoRectangle viewport_hippo;
            HippoRectangle region_hippo;

            getViewport(&viewport);
            
            hippo_rectangle_from_rect(&viewport_hippo, &viewport);
            hippo_rectangle_from_rect(&region_hippo, &region);

            if (hippo_rectangle_intersect(&viewport_hippo, &region_hippo, &region_hippo)) {
                // we have to clip so we don't draw outside the viewport - the canvas
                // doesn't have its own window
                cairo_save(cr);
                cairo_rectangle(cr, region_hippo.x, region_hippo.y, region_hippo.width, region_hippo.height);
                cairo_clip(cr);

                int x, y;
                getCanvasOrigin(&x, &y);
                hippo_canvas_item_process_paint(root_, cr, &region_hippo, x, y);

                cairo_restore(cr);
            }
        }
        
        // pop the update region clip and the translation off the buffer
        cairo_destroy(cr);

        // Copy the buffer to the window
        cairo_t *window_cr = cairo_create(surface);
        cairo_rectangle(window_cr, region.left, region.top, regionWidth, regionHeight);
        cairo_clip(window_cr);
        cairo_set_source_surface(window_cr, buffer, region.left, region.top);
        cairo_paint(window_cr);
        cairo_destroy(window_cr);

        cairo_surface_destroy(buffer);
        cairo_surface_destroy(surface);

        EndPaint(window_, &paint);
    }
}

bool
HippoCanvas::processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    if (HippoAbstractControl::processMessage(message, wParam, lParam))
        return true;

    switch (message) {
        case WM_PAINT:
            onPaint(wParam, lParam);
            return false;
        case WM_HSCROLL:
            if (hscrollNeeded_) {
                int newX = hscroll_->handleScrollMessage(message, wParam, lParam);
                scrollTo(newX, canvasY_);
            }
            return true;
        case WM_VSCROLL:
            if (vscrollNeeded_) {
                int newY = vscroll_->handleScrollMessage(message, wParam, lParam);
                scrollTo(canvasX_, newY);
            }
            return true;
        case WM_SETCURSOR:
            // kill DefWindowProc setting the cursor, so we are the only 
            // ones that do (in the mouse move handler)
            return true;
        case WM_LBUTTONDOWN:
            onMouseDown(1, wParam, lParam);
            return true;
        case WM_LBUTTONUP:
            onMouseUp(1, wParam, lParam);
            return true;
        case WM_MBUTTONDOWN:
            onMouseDown(2, wParam, lParam);
            return true;
        case WM_MBUTTONUP:
            onMouseUp(2, wParam, lParam);
            return true;
        case WM_RBUTTONDOWN:
            onMouseDown(3, wParam, lParam);
            return true;
        case WM_RBUTTONUP:
            onMouseUp(3, wParam, lParam);
            return true;
        case WM_MOUSEMOVE:
            onMouseMove(wParam, lParam);
            return true;
        case WM_MOUSELEAVE:
            onMouseLeave(wParam, lParam);
            return true;
    }

    return false;
}

typedef struct
{
    HippoCanvasItem *item;
    HippoAbstractControl *control;
} RegisteredControlItem;

static void hippo_canvas_context_win_init       (HippoCanvasContextWin             *canvas_win);
static void hippo_canvas_context_win_class_init (HippoCanvasContextWinClass        *klass);
static void hippo_canvas_context_win_dispose    (GObject                 *object);
static void hippo_canvas_context_win_finalize   (GObject                 *object);
static void hippo_canvas_context_win_iface_init (HippoCanvasContextIface *klass);


static void hippo_canvas_context_win_set_property (GObject      *object,
                                                   guint         prop_id,
                                                   const GValue *value,
                                                   GParamSpec   *pspec);
static void hippo_canvas_context_win_get_property (GObject      *object,
                                                   guint         prop_id,
                                                   GValue       *value,
                                                   GParamSpec   *pspec);

static PangoLayout*     hippo_canvas_context_win_create_layout          (HippoCanvasContext *context);
static cairo_surface_t* hippo_canvas_context_win_load_image             (HippoCanvasContext *context,
                                                                         const char         *image_name);
static guint32          hippo_canvas_context_win_get_color              (HippoCanvasContext *context,
                                                                         HippoStockColor     color);
static void             hippo_canvas_context_win_register_widget_item   (HippoCanvasContext *context,
                                                                         HippoCanvasItem    *item);
static void             hippo_canvas_context_win_unregister_widget_item (HippoCanvasContext *context,
                                                                         HippoCanvasItem    *item);
static void             hippo_canvas_context_win_translate_to_widget    (HippoCanvasContext *context,
                                                                         HippoCanvasItem    *item,
                                                                         int                *x_p,
                                                                         int                *y_p);

struct _HippoCanvasContextWin {
    GObject parent;

    HippoCanvas *canvas;
    HippoCanvasPointer pointer;
    GSList *control_items;
    PangoContext *pango;
};

struct _HippoCanvasContextWinClass {
    GObjectClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasContextWin, hippo_canvas_context_win, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTEXT,
                                              hippo_canvas_context_win_iface_init));

static void
hippo_canvas_context_win_init(HippoCanvasContextWin *canvas_win)
{
    PangoFontDescription *desc;

    canvas_win->pointer = HIPPO_CANVAS_POINTER_UNSET;
    /* canvas_win->pango = pango_win32_get_context(); */
    PangoCairoFontMap *font_map = (PangoCairoFontMap*) pango_cairo_font_map_get_default();
    canvas_win->pango = pango_cairo_font_map_create_context(font_map);
    g_object_unref((void*) font_map);

    desc = pango_font_description_new();
    // Note that this matches the web font in our site.css 
    // We only set Arial instead of Arial, sans-serif because
    // pango cairo doesn't like a font list here.
    pango_font_description_set_family_static(desc, "Arial");
    // FIXME on my laptop (Visual Studio 2005) this crashes Pango with a g_error()
    //pango_context_set_font_description(canvas_win->pango, desc);
    pango_font_description_free(desc);
}

static void
hippo_canvas_context_win_class_init(HippoCanvasContextWinClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    
    object_class->set_property = hippo_canvas_context_win_set_property;
    object_class->get_property = hippo_canvas_context_win_get_property;

    object_class->dispose = hippo_canvas_context_win_dispose;
    object_class->finalize = hippo_canvas_context_win_finalize;
}

static void
hippo_canvas_context_win_iface_init (HippoCanvasContextIface *klass)
{
    klass->create_layout = hippo_canvas_context_win_create_layout;
    klass->load_image = hippo_canvas_context_win_load_image;
    klass->get_color = hippo_canvas_context_win_get_color;
    klass->register_widget_item = hippo_canvas_context_win_register_widget_item;
    klass->unregister_widget_item = hippo_canvas_context_win_unregister_widget_item;
    klass->translate_to_widget = hippo_canvas_context_win_translate_to_widget;
}

static void
hippo_canvas_context_win_dispose(GObject *object)
{
    HippoCanvasContextWin *canvas_win = HIPPO_CANVAS_CONTEXT_WIN(object);
    
    if (canvas_win->pango) {
        g_object_unref((void*)canvas_win->pango);
        canvas_win->pango = NULL;
    }

    G_OBJECT_CLASS(hippo_canvas_context_win_parent_class)->dispose(object);
}

static void
hippo_canvas_context_win_finalize(GObject *object)
{
    /* HippoCanvasContextWin *canvas_win = HIPPO_CANVAS_CONTEXT_WIN(object); */

    G_OBJECT_CLASS(hippo_canvas_context_win_parent_class)->finalize(object);
}

static void
hippo_canvas_context_win_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasContextWin *canvas_win;

    canvas_win = HIPPO_CANVAS_CONTEXT_WIN(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_context_win_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasContextWin *canvas_win;

    canvas_win = HIPPO_CANVAS_CONTEXT_WIN (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}
static PangoLayout*
hippo_canvas_context_win_create_layout(HippoCanvasContext *context)
{
    HippoCanvasContextWin *canvas_win;
    PangoLayout *layout;

    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), NULL);

    canvas_win = HIPPO_CANVAS_CONTEXT_WIN(context);

    layout = pango_layout_new(canvas_win->pango);

    return layout;
}

static cairo_surface_t*
hippo_canvas_context_win_load_image(HippoCanvasContext *context,
                                    const char         *image_name)
{
    /* HippoCanvasContextWin *canvas_win = HIPPO_CANVAS_CONTEXT_WIN(context); */

    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), NULL);

    cairo_surface_t *surface = hippo_image_factory_get(image_name);
    if (surface)
        cairo_surface_reference(surface);
    return surface;
}

#if 0
static guint32
convert_color(GdkColor *gdk_color)
{
    guint32 rgba;
    
    rgba = gdk_color->red / 256;
    rgba <<= 8;
    rgba &= gdk_color->green / 256;
    rgba <<= 8;
    rgba &= gdk_color->blue / 256;
    rgba <<= 8;
    rgba &= 0xff; /* alpha */

    return rgba;
}
#endif

static guint32
hippo_canvas_context_win_get_color(HippoCanvasContext *context,
                                   HippoStockColor     color)
{
    HippoCanvasContextWin *canvas_win = HIPPO_CANVAS_CONTEXT_WIN(context);
    
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), 0);

    switch (color) {
    case HIPPO_STOCK_COLOR_BG_NORMAL:
        // FIXME use real system color - GetThemeColor?
        return 0x777777ff;
    case HIPPO_STOCK_COLOR_BG_PRELIGHT:
        // FIXME use real system color
        return 0x999999ff;
    }

    g_warning("unknown stock color %d", color);
    return 0;
}

static void
clear_control(RegisteredControlItem *citem)
{
    if (citem->control) {
        citem->control->hide();
        citem->control->setParent(NULL);
        citem->control->setCanvasItem(NULL);
        citem->control->Release();
        citem->control = NULL;
    }
}

static void
update_control(HippoCanvasContextWin   *canvas_win,
               RegisteredControlItem   *citem)
{
    HippoAbstractControl *new_control;

    new_control = NULL;
    g_object_get(G_OBJECT(citem->item), "control", &new_control, NULL);

    if (new_control == citem->control)
        return;
    
    if (new_control) {
        new_control->AddRef();
        new_control->setParent(canvas_win->canvas);
        new_control->setCanvasItem(G_OBJECT(citem->item));
    }
    
    clear_control(citem);
    
    citem->control = new_control;
}

static void
on_item_control_changed(HippoCanvasItem *item,
                       GParamSpec      *arg,
                       void            *data)
{
    HippoCanvasContextWin *canvas_win = HIPPO_CANVAS_CONTEXT_WIN(data);
    RegisteredControlItem *citem;
    GSList *link;
    
    citem = NULL;
    for (link = canvas_win->control_items;
         link != NULL;
         link = link->next) {
        citem = (RegisteredControlItem*) link->data;
        if (citem->item == item) {
            update_control(canvas_win, citem);
            return;
        }
    }

    g_warning("got control changed for an unregistered control item");
}

static void
add_control_item(HippoCanvasContextWin     *canvas_win,
                 HippoCanvasItem *item)
{
    RegisteredControlItem *citem = g_new0(RegisteredControlItem, 1);

    citem->item = item;
    g_object_ref(citem->item);    
    canvas_win->control_items = g_slist_prepend(canvas_win->control_items, citem);

    update_control(canvas_win, citem);
    
    g_signal_connect(G_OBJECT(item), "notify::control",
                     G_CALLBACK(on_item_control_changed),
                     canvas_win);
}

static void
remove_control_item(HippoCanvasContextWin     *canvas_win,
                    HippoCanvasItem *item)
{
    RegisteredControlItem *citem;
    GSList *link;
    
    citem = NULL;
    for (link = canvas_win->control_items;
         link != NULL;
         link = link->next) {
        citem = (RegisteredControlItem*) link->data;
        if (citem->item == item)
            break;
    }
    if (link == NULL) {
        g_warning("removing a not-registered control item");
        return;
    }

    canvas_win->control_items = g_slist_remove(canvas_win->control_items, citem);
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(citem->item),
                                         G_CALLBACK(on_item_control_changed),
                                         canvas_win);
    clear_control(citem);
    g_object_unref(citem->item);
    g_free(citem);
}

static void
hippo_canvas_context_win_register_widget_item(HippoCanvasContext *context,
                                              HippoCanvasItem    *item)
{
    HippoCanvasContextWin *canvas_win = HIPPO_CANVAS_CONTEXT_WIN(context);

    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));

    add_control_item(canvas_win, item);
}

static void
hippo_canvas_context_win_unregister_widget_item (HippoCanvasContext *context,
                                                 HippoCanvasItem    *item)
{
    HippoCanvasContextWin *canvas_win = HIPPO_CANVAS_CONTEXT_WIN(context);

    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));

    remove_control_item(canvas_win, item);
}

static void
hippo_canvas_context_win_translate_to_widget(HippoCanvasContext *context,
                                             HippoCanvasItem    *item,
                                             int                *x_p,
                                             int                *y_p)
{
    HippoCanvasContextWin *canvas_win;

    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));

    canvas_win = HIPPO_CANVAS_CONTEXT_WIN(context);

    /* convert coords of root canvas item to coords of
     * the HWND
     */
    int cx, cy;
    canvas_win->canvas->getCanvasOrigin(&cx, &cy);

    if (x_p)
        *x_p += cx;
    if (y_p)
        *y_p += cy;
}

static HippoCanvasContextWin*
hippo_canvas_context_win_new(HippoCanvas *canvas)
{
    HippoCanvasContextWin *context_win;

    context_win = HIPPO_CANVAS_CONTEXT_WIN(g_object_new(HIPPO_TYPE_CANVAS_CONTEXT_WIN, NULL));
    g_assert(HIPPO_IS_CANVAS_CONTEXT(context_win));

    // don't ref it, since it owns us, it would create a cycle
    context_win->canvas = canvas;
    
    return context_win;
}

static void
hippo_canvas_context_win_update_pango(HippoCanvasContextWin *context_win,
                                      cairo_t               *cr)
{
    pango_cairo_update_context(cr, context_win->pango);
}

static void
hippo_canvas_context_win_create_controls(HippoCanvasContextWin *context_win)
{
    RegisteredControlItem *citem;
    GSList *link;
    
    citem = NULL;
    for (link = context_win->control_items;
         link != NULL;
         link = link->next) {
        citem = (RegisteredControlItem*) link->data;
        if (citem->control) {
            if (!citem->control->create())
                g_warning("Failed to create canvas control");
        }
    }
}

static void
hippo_canvas_context_win_show_controls(HippoCanvasContextWin *context_win)
{
    RegisteredControlItem *citem;
    GSList *link;
    
    citem = NULL;
    for (link = context_win->control_items;
         link != NULL;
         link = link->next) {
        citem = (RegisteredControlItem*) link->data;
        if (citem->control) {
            citem->control->show(false);
        }
    }
}
