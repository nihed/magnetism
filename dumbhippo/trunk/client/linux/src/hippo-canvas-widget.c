/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <string.h>
#include <gtk/gtkwidget.h>
#include "hippo-canvas-widget.h"

static void      hippo_canvas_widget_init                (HippoCanvasWidget       *widget);
static void      hippo_canvas_widget_class_init          (HippoCanvasWidgetClass  *klass);
static void      hippo_canvas_widget_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_widget_dispose             (GObject                *object);
static void      hippo_canvas_widget_finalize            (GObject                *object);

static void hippo_canvas_widget_set_property (GObject      *object,
                                              guint         prop_id,
                                              const GValue *value,
                                              GParamSpec   *pspec);
static void hippo_canvas_widget_get_property (GObject      *object,
                                              guint         prop_id,
                                              GValue       *value,
                                              GParamSpec   *pspec);


/* Canvas item methods */
static void hippo_canvas_widget_set_context (HippoCanvasItem    *item,
                                             HippoCanvasContext *context);
static void hippo_canvas_widget_allocate    (HippoCanvasItem    *item,
                                             int                 width,
                                             int                 height);

/* Canvas box methods */
static void hippo_canvas_widget_paint_below_children       (HippoCanvasBox  *box,
                                                            cairo_t         *cr);
static int  hippo_canvas_widget_get_content_width_request  (HippoCanvasBox  *box);
static int  hippo_canvas_widget_get_content_height_request (HippoCanvasBox  *box,
                                                            int              for_width);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_WIDGET
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasWidget, hippo_canvas_widget, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_widget_iface_init));

static void
hippo_canvas_widget_init(HippoCanvasWidget *widget)
{

}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_widget_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->set_context = hippo_canvas_widget_set_context;
    item_class->allocate = hippo_canvas_widget_allocate;
}

static void
hippo_canvas_widget_class_init(HippoCanvasWidgetClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);

    object_class->set_property = hippo_canvas_widget_set_property;
    object_class->get_property = hippo_canvas_widget_get_property;

    object_class->dispose = hippo_canvas_widget_dispose;
    object_class->finalize = hippo_canvas_widget_finalize;

    box_class->paint_below_children = hippo_canvas_widget_paint_below_children;
    box_class->get_content_width_request = hippo_canvas_widget_get_content_width_request;
    box_class->get_content_height_request = hippo_canvas_widget_get_content_height_request;

    g_object_class_install_property(object_class,
                                    PROP_WIDGET,
                                    g_param_spec_object("widget",
                                                        _("Widget"),
                                                        _("Widget to put in the canvas item"),
                                                        GTK_TYPE_WIDGET,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_widget_dispose(GObject *object)
{
    HippoCanvasWidget *widget = HIPPO_CANVAS_WIDGET(object);

    if (widget->widget) {
        g_object_unref(widget->widget);
        widget->widget = NULL;
        g_object_notify(object, "widget");
    }

    G_OBJECT_CLASS(hippo_canvas_widget_parent_class)->dispose(object);
}

static void
hippo_canvas_widget_finalize(GObject *object)
{
    /* HippoCanvasWidget *widget = HIPPO_CANVAS_WIDGET(object); */


    G_OBJECT_CLASS(hippo_canvas_widget_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_widget_new(void)
{
    HippoCanvasWidget *widget = g_object_new(HIPPO_TYPE_CANVAS_WIDGET, NULL);


    return HIPPO_CANVAS_ITEM(widget);
}

static void
hippo_canvas_widget_set_property(GObject         *object,
                                 guint            prop_id,
                                 const GValue    *value,
                                 GParamSpec      *pspec)
{
    HippoCanvasWidget *widget;

    widget = HIPPO_CANVAS_WIDGET(object);

    switch (prop_id) {
    case PROP_WIDGET:
        {
            GtkWidget *w = (GtkWidget*) g_value_get_object(value);
            if (widget->widget != w) {
                if (w) {
                    gtk_object_ref(GTK_OBJECT(w));
                    gtk_object_sink(GTK_OBJECT(w));
                }
                if (widget->widget)
                    g_object_unref(widget->widget);
                widget->widget = w;

                hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(widget));
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_widget_get_property(GObject         *object,
                                 guint            prop_id,
                                 GValue          *value,
                                 GParamSpec      *pspec)
{
    HippoCanvasWidget *widget;

    widget = HIPPO_CANVAS_WIDGET (object);

    switch (prop_id) {
    case PROP_WIDGET:
        g_value_set_object(value, widget->widget);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_widget_set_context(HippoCanvasItem    *item,
                                HippoCanvasContext *context)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    if (context == box->context)
        return;

    if (box->context)
        hippo_canvas_context_unregister_widget_item(box->context, item);

    /* chain up, which invalidates our old context */
    item_parent_class->set_context(item, context);

    if (box->context)
        hippo_canvas_context_register_widget_item(box->context, item);
}

static void
hippo_canvas_widget_allocate(HippoCanvasItem *item,
                             int              width,
                             int              height)
{
    int x, y, w, h;
    int widget_x, widget_y;
    GtkAllocation child_allocation;
    HippoCanvasWidget *widget;
    HippoCanvasBox *box;

    widget = HIPPO_CANVAS_WIDGET(item);
    box = HIPPO_CANVAS_BOX(item);
    
    /* get the box set up */
    item_parent_class->allocate(item, width, height);

    /* Now do the GTK allocation for the child widget */
    if (widget->widget == NULL || !GTK_WIDGET_VISIBLE(widget->widget))
        return;
    
    w = widget->widget->requisition.width;
    h = widget->widget->requisition.height;

    hippo_canvas_box_align(box, w, h, &x, &y, &w, &h);

    widget_x = 0;
    widget_y = 0;
    if (box->context)
        hippo_canvas_context_translate_to_widget(box->context, item,
                                                 &widget_x, &widget_y);

    child_allocation.x = widget_x + x;
    child_allocation.y = widget_y + y;
    child_allocation.width = w;
    child_allocation.height = h;

    gtk_widget_size_allocate(widget->widget, &child_allocation);
}

static void
hippo_canvas_widget_paint_below_children(HippoCanvasBox  *box,
                                         cairo_t         *cr)
{
    HippoCanvasWidget *widget = HIPPO_CANVAS_WIDGET(box);

    if (widget->widget == NULL)
        return;

    /* For now the HippoCanvas is responsible for drawing all widgets; it
     * plops them all on top, after rending all canvas items.
     * 
     * For no-window widgets, adding a canvas_context_paint_widget_item() to
     * call right here
     * is a simple way to get them in the right z-order, if we ever
     * need it.
     */
}

static int
hippo_canvas_widget_get_content_width_request(HippoCanvasBox *box)
{
    HippoCanvasWidget *widget = HIPPO_CANVAS_WIDGET(box);
    int children_width;
    int widget_width;
    GtkRequisition req;
    
    children_width = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_widget_parent_class)->get_content_width_request(box);

    if (widget->widget && GTK_WIDGET_VISIBLE(widget->widget)) {
        gtk_widget_size_request(widget->widget, &req);
        widget_width = req.width;
    } else {
        widget_width = 0;
    }

    return MAX(widget_width, children_width);
}

static int
hippo_canvas_widget_get_content_height_request(HippoCanvasBox  *box,
                                               int              for_width)
{
    HippoCanvasWidget *widget = HIPPO_CANVAS_WIDGET(box);
    int children_height;
    int widget_height;
    GtkRequisition req;
    
    /* get height of children and the box padding */
    children_height = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_widget_parent_class)->get_content_height_request(box,
                                                                                                           for_width);
    
    if (widget->widget && GTK_WIDGET_VISIBLE(widget->widget)) {
        /* We know a get_height_request was done first, so we can
         * just get widget->requisition instead of doing the size request
         * computation again.
         */
        gtk_widget_get_child_requisition(widget->widget, &req);
        widget_height = req.height;
    } else {
        widget_height = 0;
    }
    
    return MAX(widget_height, children_height);
}
