/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <string.h>
#include <gdk/gdkkeysyms.h>
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

#define HIPPO_DEFINE_WIDGET_ITEM_CUSTOM_INIT(lower, Camel)                             \
    struct _HippoCanvas##Camel { HippoCanvasWidget parent; };                          \
    struct _HippoCanvas##Camel##Class { HippoCanvasWidgetClass parent; };              \
    static void hippo_canvas_##lower##_init(HippoCanvas##Camel *lower) {}              \
    G_DEFINE_TYPE(HippoCanvas##Camel, hippo_canvas_##lower, HIPPO_TYPE_CANVAS_WIDGET)


HIPPO_DEFINE_WIDGET_ITEM_CUSTOM_INIT(button, Button);
HIPPO_DEFINE_WIDGET_ITEM(scrollbars, Scrollbars);
HIPPO_DEFINE_WIDGET_ITEM_CUSTOM_INIT(entry, Entry);

enum {
    BUTTON_PROP_0,
    BUTTON_PROP_TEXT
} ;

static void
on_canvas_button_clicked(GtkButton         *button,
                         HippoCanvasButton *canvas_button)
{
    hippo_canvas_item_emit_activated(HIPPO_CANVAS_ITEM(canvas_button));
}

static void
hippo_canvas_button_dispose(GObject *object)
{
    HippoCanvasButton *canvas_button = HIPPO_CANVAS_BUTTON (object);
    GtkWidget *button = HIPPO_CANVAS_WIDGET(object)->widget;

    if (button) {
        g_signal_handlers_disconnect_by_func(button, (void *)on_canvas_button_clicked, canvas_button);
    }

    G_OBJECT_CLASS(hippo_canvas_button_parent_class)->dispose(object);
}

static void
hippo_canvas_button_set_property(GObject        *object,
                                   guint            prop_id,
                                   const GValue    *value,
                                   GParamSpec      *pspec)
{
    /* HippoCanvasButton *canvas_button = HIPPO_CANVAS_BUTTON(object); */
    GtkWidget *button = HIPPO_CANVAS_WIDGET(object)->widget;

    switch (prop_id) {
    case BUTTON_PROP_TEXT:
        gtk_button_set_label(GTK_BUTTON(button), g_value_get_string(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_button_get_property(GObject        *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    /* HippoCanvasButton *canvas_button = HIPPO_CANVAS_BUTTON (object); */
    GtkWidget *button = HIPPO_CANVAS_WIDGET(object)->widget;

    switch (prop_id) {
    case BUTTON_PROP_TEXT:
        g_value_set_string(value, gtk_button_get_label(GTK_BUTTON(button)));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_button_class_init(HippoCanvasButtonClass *class)
{
    GObjectClass *object_class = G_OBJECT_CLASS(class);

    object_class->dispose = hippo_canvas_button_dispose;
    object_class->set_property = hippo_canvas_button_set_property;
    object_class->get_property = hippo_canvas_button_get_property;
    
    g_object_class_install_property(object_class,
                                    BUTTON_PROP_TEXT,
                                    g_param_spec_string("text",
                                                        _("Text"),
                                                        _("Text in the button"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

HippoCanvasItem*
hippo_canvas_button_new(void)
{
    GtkWidget *button;
    HippoCanvasItem *item;
    
    button = gtk_button_new();
    item = g_object_new(HIPPO_TYPE_CANVAS_BUTTON,
                        "widget", button,
                        NULL);

    g_signal_connect(button, "clicked",
                     G_CALLBACK(on_canvas_button_clicked), item);

    return item;
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

enum {
    ENTRY_PROP_0,
    ENTRY_PROP_TEXT
} ;

static void
on_canvas_entry_changed(GtkEditable      *editable,
                        HippoCanvasEntry *canvas_entry)
{
    g_object_notify(G_OBJECT(canvas_entry), "text");
}

static gboolean
on_canvas_entry_key_press_event(GtkWidget        *widget,
                                GdkEventKey      *event,
                                HippoCanvasEntry *canvas_entry)
{
    HippoKey key;
    gunichar character;

    switch (event->keyval) {
    case GDK_Return:
    case GDK_KP_Enter:
        key = HIPPO_KEY_RETURN;
        break;
    case GDK_Escape:
        key = HIPPO_KEY_ESCAPE;
        break;
    default:
        key = HIPPO_KEY_UNKNOWN;
        break;
    }

    character = gdk_keyval_to_unicode(event->keyval);
        
    return hippo_canvas_item_emit_key_press_event(HIPPO_CANVAS_ITEM(canvas_entry), key, character);
}

static void
hippo_canvas_entry_dispose(GObject *object)
{
    HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY (object);
    GtkWidget *entry = HIPPO_CANVAS_WIDGET(object)->widget;

    if (entry) {
        g_signal_handlers_disconnect_by_func(entry, (void *)on_canvas_entry_changed, canvas_entry);
        g_signal_handlers_disconnect_by_func(entry, (void *)on_canvas_entry_key_press_event, canvas_entry);
    }

    G_OBJECT_CLASS(hippo_canvas_entry_parent_class)->dispose(object);
}

static void
hippo_canvas_entry_set_property(GObject        *object,
                                   guint            prop_id,
                                   const GValue    *value,
                                   GParamSpec      *pspec)
{
    /* HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY(object); */
    GtkWidget *entry = HIPPO_CANVAS_WIDGET(object)->widget;

    switch (prop_id) {
    case ENTRY_PROP_TEXT:
        gtk_entry_set_text(GTK_ENTRY(entry), g_value_get_string(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_entry_get_property(GObject        *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    /* HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY (object); */
    GtkWidget *entry = HIPPO_CANVAS_WIDGET(object)->widget;

    switch (prop_id) {
    case ENTRY_PROP_TEXT:
        g_value_set_string(value, gtk_entry_get_text(GTK_ENTRY(entry)));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_entry_class_init(HippoCanvasEntryClass *class)
{
    GObjectClass *object_class = G_OBJECT_CLASS(class);

    object_class->dispose = hippo_canvas_entry_dispose;
    object_class->set_property = hippo_canvas_entry_set_property;
    object_class->get_property = hippo_canvas_entry_get_property;
    
    g_object_class_install_property(object_class,
                                    ENTRY_PROP_TEXT,
                                    g_param_spec_string("text",
                                                        _("Text"),
                                                        _("Text in the entry"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

HippoCanvasItem*
hippo_canvas_entry_new(void)
{
    GtkWidget *entry;
    HippoCanvasItem *item;
    
    entry = gtk_entry_new();
    item = g_object_new(HIPPO_TYPE_CANVAS_ENTRY,
                        "widget", entry,
                        NULL);

    g_signal_connect(entry, "changed",
                     G_CALLBACK(on_canvas_entry_changed), item);
    g_signal_connect(entry, "key-press-event",
                     G_CALLBACK(on_canvas_entry_key_press_event), item);

    return item;
}
