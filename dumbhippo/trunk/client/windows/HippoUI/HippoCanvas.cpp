/* HippoCanvas.cpp: a control that contains a canvas item
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"

#include "HippoCanvas.h"

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
{
    HippoCanvasContextWin *context;

    setClassName(L"HippoCanvasClass");
    setClassStyle(CS_HREDRAW | CS_VREDRAW);
    setTitle(L"Canvas");

    context = hippo_canvas_context_win_new(this);
    context_ = context;
    g_object_unref((void*) context); // lose the extra reference
    g_assert(HIPPO_IS_CANVAS_CONTEXT(context_));
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

bool
HippoCanvas::create()
{
    bool result;
    
    result = HippoAbstractControl::create();
    if (!result)
        return false;
    
    if (root_ != (HippoCanvasItem*) NULL) {
        g_assert(HIPPO_IS_CANVAS_CONTEXT(context_));
        hippo_canvas_item_set_context(root_, HIPPO_CANVAS_CONTEXT(context_));
        queueResize();
    }

    return result;
}

void
HippoCanvas::onSizeChanged()
{
    if (root_ != (HippoCanvasItem*) NULL)
        hippo_canvas_item_allocate(root_, getWidth(), getHeight());
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
                // should use system color, maybe GetThemeSysColorBrush is right
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
                    hippo_canvas_item_process_paint(root_, cr, 0, 0);
                }

                cairo_destroy(cr);
                cairo_surface_destroy(surface);
                EndPaint(window_, &paint);
            }
            return true;
    }

    return HippoAbstractControl::processMessage(message, wParam, lParam);
}

int
HippoCanvas::getWidthRequestImpl()
{
    if (root_ != (HippoCanvasItem*) NULL)
        return hippo_canvas_item_get_width_request(root_);
    else
        return 0;
}

int
HippoCanvas::getHeightRequestImpl(int forWidth)
{
    if (root_ != (HippoCanvasItem*) NULL)
        return hippo_canvas_item_get_height_request(root_, forWidth);
    else
        return 0;
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
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));

    /* convert coords of root canvas item to coords of
     * the HWND
     */

    // right now, they are identical
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
    // canvas root coords are currently the same as control coords
    canvas_win->canvas->invalidate(x, y, width, height);
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
