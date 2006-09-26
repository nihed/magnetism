/* HippoCanvas.cpp: a control that contains a canvas item
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"

#include "HippoCanvas.h"
#include "HippoScrollbar.h"

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

HippoCanvas::HippoCanvas()
    : canvasWidthReq_(0), canvasHeightReq_(0), canvasX_(0), canvasY_(0), hscrollNeeded_(false), vscrollNeeded_(false),
      hscrollable_(false), vscrollable_(false)
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
HippoCanvas::setRoot(HippoCanvasItem *item)
{
    if (root_ == item)
        return;

    root_ = item;
    if (item) {
        if (isCreated()) {
            g_assert(HIPPO_IS_CANVAS_CONTEXT(context_));
            hippo_canvas_item_set_context(item, HIPPO_CANVAS_CONTEXT(context_));
        }
    }
    queueResize();
}

void
HippoCanvas::setScrollable(HippoOrientation orientation,
                           bool             value)
{
    if (orientation == HIPPO_ORIENTATION_VERTICAL) {
        if (value == vscrollable_)
            return;

        vscrollable_ = value;
    } else {
        if (value == hscrollable_)
            return;

        hscrollable_ = value;
    }

    queueResize();
}

bool
HippoCanvas::create()
{
    bool result;
    
    result = HippoAbstractControl::create();
    if (!result)
        return false;
    
    hscroll_->create();
    vscroll_->create();

    if (root_ != (HippoCanvasItem*) NULL) {
        g_assert(HIPPO_IS_CANVAS_CONTEXT(context_));
        hippo_canvas_item_set_context(root_, HIPPO_CANVAS_CONTEXT(context_));
        queueResize();
    }

    return result;
}

void 
HippoCanvas::show(bool activate)
{
    if (!create()) // because scrollbars will need us created
        return;

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

    HippoAbstractControl::show(activate);
}

int
HippoCanvas::getWidthRequestImpl()
{
    if (root_ != (HippoCanvasItem*) NULL) {
        canvasWidthReq_ = hippo_canvas_item_get_width_request(root_);
    } else {
        canvasWidthReq_ = 0;
    }

    if (hscrollable_) {
        // return a minimum width that assumes we need both scrollbars
        return hscroll_->getWidthRequest() + vscroll_->getWidthRequest();
    } else {
        return canvasWidthReq_;
    }
}

int
HippoCanvas::getHeightRequestImpl(int forWidth)
{
    if (root_ != (HippoCanvasItem*) NULL) {
        canvasHeightReq_ = hippo_canvas_item_get_height_request(root_,
            hscrollable_ ? canvasWidthReq_ : forWidth);
    } else {
        canvasHeightReq_ = 0;
    }

    if (vscrollable_) {
        // assume we need vertical scrollbar always, but 
        // only factor in horizontal scrollbar if needed
        if (canvasWidthReq_ > forWidth || !hscrollable_) {
            // don't need horizontal scrollbar
            return vscroll_->getHeightRequest(forWidth);
        } else {
            // forWidth/2 is bogus, but scrollbar ignores forWidth anyhow
            return hscroll_->getHeightRequest(forWidth/2) + vscroll_->getHeightRequest(forWidth/2);
        }
    } else {
        return canvasHeightReq_;
    }
}


void
HippoCanvas::onSizeChanged()
{
#if 0
        g_debug("control size changed %d x %d scrollable %d canvas %d x %d", getWidth(), getHeight(), scrollable_,
            canvasWidthReq_, canvasHeightReq_);
#endif

    // size and position the scrollbars, allocating the canvas item root
    // if needed.
    updateScrollbars();
}

void
HippoCanvas::updateScrollbars()
{
    int w = getWidth();
    int h = getHeight();

    int canvasWidthAlloc, canvasHeightAlloc;
    if (root_ != (HippoCanvasItem*) NULL) {
        if (hscrollable_)
            canvasWidthAlloc = MAX(canvasWidthReq_, w);
        else
            canvasWidthAlloc = w;
        if (vscrollable_)
            canvasHeightAlloc = MAX(canvasHeightReq_, h);
        else
            canvasHeightAlloc = h;   
    } else {
        canvasWidthAlloc = 0;
        canvasHeightAlloc = 0;
    }

    hscrollNeeded_ = hscrollable_ && canvasWidthAlloc > w;
    vscrollNeeded_ = vscrollable_ && canvasHeightAlloc > h;

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

    int vWidth = vscrollNeeded_ ? vscroll_->getWidthRequest() : 0;
    int hWidth = hscrollNeeded_ ? w - vWidth : 0;
    int hHeight = hscrollNeeded_ ? hscroll_->getHeightRequest(hWidth) : 0;
    int vHeight = vscrollNeeded_ ? h - hHeight : 0;
   
    if (hscrollNeeded_ && !vscrollNeeded_)
        canvasHeightAlloc -= hHeight;
    
    if (vscrollNeeded_ && !hscrollNeeded_)
        canvasWidthAlloc -= vWidth;

    if (vscrollNeeded_) {
        g_debug("setting size of vscrollbar to %d,%d %dx%d", w - vWidth, 0, vWidth, vHeight);
        vscroll_->moveResize(w - vWidth, 0, vWidth, vHeight);
        vscroll_->setBounds(0, canvasHeightAlloc, vHeight);
        if (isShowing())
            vscroll_->show(false);
    }

    if (hscrollNeeded_) {
        g_debug("setting size of hscrollbar %d,%d %dx%d", 0, h - hHeight, hWidth, hHeight);
        hscroll_->moveResize(0, h - hHeight, hWidth, hHeight);
        hscroll_->setBounds(0, canvasWidthAlloc, hWidth);
        if (isShowing())
            hscroll_->show(false);
    }
    
    if (root_ != (HippoCanvasItem*) NULL) {
        hippo_canvas_item_allocate(root_, canvasWidthAlloc, canvasHeightAlloc);
    }
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
    g_return_if_fail(hscrollable_ || vscrollable_);

    int dx = hscrollable_ ? canvasX_ - newX : 0;
    int dy = vscrollable_ ? canvasY_ - newY : 0;

    if (dx == 0 && dy == 0)
        return;

    canvasX_ = newX;
    canvasY_ = newY;

    RECT viewport;
    getViewport(&viewport);
    ScrollWindowEx(window_, 
                   dx, dy,
                   &viewport, // portion of client area to scroll
                   &viewport, // clip region (do not modify bits outside it)
                   NULL,      // return for invalid region
                   NULL,      // return for invalid rectangle
                   SW_INVALIDATE | SW_SCROLLCHILDREN | SW_ERASE);
}

bool 
HippoCanvas::processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    switch (message) {
        case WM_PAINT:
            RECT region;
            if (GetUpdateRect(window_, &region, true)) {
                PAINTSTRUCT paint;
                HDC hdc = BeginPaint(window_, &paint);

                // FIXME not the right background color (on linux it's the default gtk background)
                // should use system color, maybe GetThemeSysColorBrush is right. Note that 
                // this rectangle draws the little corner between the scrollbars in 
                // addition to the viewport background.
                HBRUSH hbrush = CreateSolidBrush(RGB(255,255,255));
                HGDIOBJ oldBrush;

                oldBrush = SelectObject(hdc, hbrush);
                FillRect(hdc, &region, hbrush);
                SelectObject(hdc, oldBrush);
                DeleteObject(hbrush);

                cairo_surface_t *surface = cairo_win32_surface_create(hdc);
                cairo_t *cr = cairo_create(surface);
                hippo_canvas_context_win_update_pango(context_, cr);

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

                cairo_destroy(cr);
                cairo_surface_destroy(surface);

                EndPaint(window_, &paint);
            }
            return true;
        case WM_HSCROLL:
            if (hscrollable_) {
                int newX = hscroll_->handleScrollMessage(message, wParam, lParam);
                scrollTo(newX, canvasY_);
            }
            return true;
        case WM_VSCROLL:
            if (vscrollable_) {
                int newY = vscroll_->handleScrollMessage(message, wParam, lParam);
                scrollTo(canvasX_, newY);
            }
            return true;
    }

    return HippoAbstractControl::processMessage(message, wParam, lParam);
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
static void hippo_canvas_context_win_iface_init (HippoCanvasContextClass *klass);


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
    canvas_win->pointer = HIPPO_CANVAS_POINTER_UNSET;
    /* canvas_win->pango = pango_win32_get_context(); */
    PangoCairoFontMap *font_map = (PangoCairoFontMap*) pango_cairo_font_map_get_default();
    canvas_win->pango = pango_cairo_font_map_create_context(font_map);
    g_object_unref((void*) font_map);
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
hippo_canvas_context_win_iface_init (HippoCanvasContextClass *klass)
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

    // FIXME

    return NULL;
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
        new_control->show(false);
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

static void
canvas_root_request_changed(HippoCanvasItem *root,
                            HippoCanvasContextWin *canvas_win)
{
    canvas_win->canvas->queueResize();
}

static void
canvas_root_paint_needed(HippoCanvasItem *root,
                         int                    x,
                         int                    y,
                         int                    width,
                         int                    height,
                         HippoCanvasContextWin *canvas_win)
{
    int cx, cy;

    canvas_win->canvas->getCanvasOrigin(&cx, &cy);
    canvas_win->canvas->invalidate(cx + x, cy + y, width, height);
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
