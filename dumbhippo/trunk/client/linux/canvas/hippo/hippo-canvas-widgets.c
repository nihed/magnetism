/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <string.h>
#include <gtk/gtkbutton.h>
#include <gtk/gtkentry.h>
#include <gtk/gtkscrolledwindow.h>
#include <hippo/hippo-canvas-widgets.h>
#include "hippo-canvas-widget.h"
#include "hippo-canvas.h"

#define HIPPO_DEFINE_WIDGET_ITEM(lower, Camel)                                         \
    struct _HippoCanvas##Camel { HippoCanvasWidget parent; };                          \
    struct _HippoCanvas##Camel##Class { HippoCanvasWidgetClass parent; };              \
    static void hippo_canvas_##lower##_init(HippoCanvas##Camel *lower) {}              \
    static void hippo_canvas_##lower##_class_init(HippoCanvas##Camel##Class *lower) {} \
    G_DEFINE_TYPE(HippoCanvas##Camel, hippo_canvas_##lower, HIPPO_TYPE_CANVAS_WIDGET)


HIPPO_DEFINE_WIDGET_ITEM(button, Button);
HIPPO_DEFINE_WIDGET_ITEM(scrollbars, Scrollbars);
HIPPO_DEFINE_WIDGET_ITEM(entry, Entry);

HippoCanvasItem*
hippo_canvas_button_new(void)
{
    GtkWidget *widget;
    widget = gtk_button_new();
    return g_object_new(HIPPO_TYPE_CANVAS_BUTTON,
                        "widget", widget,
                        NULL);
}

HippoCanvasItem*
hippo_canvas_scrollbars_new(void)
{
    GtkWidget *widget;
    GtkWidget *canvas;

    widget = gtk_scrolled_window_new(NULL,NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(widget),
                                   GTK_POLICY_AUTOMATIC,
                                   GTK_POLICY_AUTOMATIC);
    gtk_scrolled_window_set_shadow_type(GTK_SCROLLED_WINDOW(widget),
                                        GTK_SHADOW_NONE);
    canvas = hippo_canvas_new();
    gtk_widget_show(canvas);
    gtk_scrolled_window_add_with_viewport(GTK_SCROLLED_WINDOW(widget), canvas);

    gtk_viewport_set_shadow_type(GTK_VIEWPORT(gtk_bin_get_child(GTK_BIN(widget))),
                                 GTK_SHADOW_NONE);
    
    return g_object_new(HIPPO_TYPE_CANVAS_SCROLLBARS,
                        "widget", widget,
                        NULL);
}

void
hippo_canvas_scrollbars_set_root(HippoCanvasScrollbars *scrollbars,
                                 HippoCanvasItem       *item)
{
    GtkWidget *sw;
    HippoCanvas *canvas;
    GtkWidget *viewport;

    g_return_if_fail(HIPPO_IS_CANVAS_SCROLLBARS(scrollbars));
    
    sw = NULL;
    g_object_get(G_OBJECT(scrollbars), "widget", &sw, NULL);
    g_return_if_fail(GTK_IS_SCROLLED_WINDOW(sw));
    
    viewport = gtk_bin_get_child(GTK_BIN(sw));
    canvas = HIPPO_CANVAS(gtk_bin_get_child(GTK_BIN(viewport)));
    
    hippo_canvas_set_root(canvas, item);
}

void
hippo_canvas_scrollbars_set_policy (HippoCanvasScrollbars *scrollbars,
                                    HippoOrientation       orientation,
                                    HippoScrollbarPolicy   policy)
{
    GtkWidget *sw;
    GtkPolicyType gtk_policy;
    const char *property;

    g_return_if_fail(HIPPO_IS_CANVAS_SCROLLBARS(scrollbars));
    
    sw = NULL;
    g_object_get(G_OBJECT(scrollbars), "widget", &sw, NULL);
    g_return_if_fail(GTK_IS_SCROLLED_WINDOW(sw));

    switch (policy) {
    default:
        g_critical("Bad value for HippoScrollbarPolicy");
    case HIPPO_SCROLLBAR_NEVER:
        gtk_policy = GTK_POLICY_NEVER;
        break;
    case HIPPO_SCROLLBAR_AUTOMATIC:
        gtk_policy = GTK_POLICY_AUTOMATIC;
        break;
    case HIPPO_SCROLLBAR_ALWAYS:
        gtk_policy = GTK_POLICY_ALWAYS;
        break;
    }

    property = orientation == HIPPO_ORIENTATION_VERTICAL ? "vscrollbar-policy" : "hscrollbar-policy";

    g_object_set(G_OBJECT(sw),
                 property, gtk_policy,
                 NULL);
}

HippoCanvasItem*
hippo_canvas_entry_new(void)
{
    GtkWidget *widget;
    widget = gtk_entry_new();
    return g_object_new(HIPPO_TYPE_CANVAS_ENTRY,
                        "widget", widget,
                        NULL);
}
