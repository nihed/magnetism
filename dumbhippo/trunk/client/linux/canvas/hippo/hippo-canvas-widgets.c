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

/* Hack to work-around GtkViewport bug http://bugzilla.gnome.org/show_bug.cgi?id=361781
 *
 * That bug wasn't actually causing the problem the scrollbar misbehavior that I thought it
 * was, so this workaround is very low priortiy ... but it does save us from an extra stray
 * 2 pixels to the right of the list of items.
 */
static void
suppress_shadow_width(GtkWidget *widget)
{
    static gboolean parsed_rc = FALSE;
    if (!parsed_rc) {
        gtk_rc_parse_string("style \"hippo-no-shadow-style\" {\n"
                            "  xthickness = 0\n"
                            "  ythickness = 0\n"
                            "}\n"
                            "widget \"*.hippo-no-shadow-widget\" style : highest \"hippo-no-shadow-style\"\n");
        parsed_rc = TRUE;
    }
    
    gtk_widget_set_name(widget, "hippo-no-shadow-widget");
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
    suppress_shadow_width(gtk_bin_get_child(GTK_BIN(widget)));

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

    g_object_unref(sw);
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

    g_object_unref(sw);
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
