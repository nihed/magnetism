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
    static void hippo_canvas_##lower##_class_init(HippoCanvas##Camel##Class *lower) {} \
    G_DEFINE_TYPE(HippoCanvas##Camel, hippo_canvas_##lower, HIPPO_TYPE_CANVAS_WIDGET)

#define HIPPO_DEFINE_WIDGET_ITEM_CUSTOM_INIT(lower, Camel)                             \
    struct _HippoCanvas##Camel { HippoCanvasWidget parent; };                          \
    struct _HippoCanvas##Camel##Class { HippoCanvasWidgetClass parent; };              \
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

static void
hippo_canvas_button_init(HippoCanvasButton * button)
{
    GtkWidget *gtk_button;

    gtk_button = gtk_button_new();
    g_object_set(button, "widget", gtk_button, NULL);

    g_signal_connect(gtk_button, "clicked",
                     G_CALLBACK(on_canvas_button_clicked), button);
}

HippoCanvasItem*
hippo_canvas_button_new(void)
{
    return g_object_new(HIPPO_TYPE_CANVAS_BUTTON, NULL);
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

/* We want height-for-width behavior when we have a vertical scrollbar
 * but no horizontal scrollbar. We hack this in by queuing a new
 * request each time we are allocated with a different width. We can
 * be pretty sure this won't cause infinite loops because of two
 * things: 1) hippo_canvas_helper_set_width() short-circuits when
 * setting the same width and doesn't queue a new size. 2) the
 * presence of a vertical scrollbar means that the height request of
 * the scrolled window won't depend on the height request of the
 * viewport, reducing the chance of getting into a loop with some other
 * part of the user interface that might also be queuing a resize
 * on allocate in similar circumstance.
 */
static void
on_viewport_size_allocate(GtkWidget    *viewport,
                          GdkRectangle *allocation)
{
    GtkWidget *scrolled_window = viewport->parent;
    GtkPolicyType hscrollbar_policy;
    GtkPolicyType vscrollbar_policy;
    
    gtk_scrolled_window_get_policy(GTK_SCROLLED_WINDOW(scrolled_window),
                                   &hscrollbar_policy, &vscrollbar_policy);

    if (hscrollbar_policy == GTK_POLICY_NEVER && vscrollbar_policy != GTK_POLICY_NEVER) {
        GtkWidget *viewport = GTK_BIN(scrolled_window)->child;
        GtkWidget *canvas = GTK_BIN(viewport)->child;
        hippo_canvas_set_width(HIPPO_CANVAS(canvas), allocation->width);
    }
}

static void
hippo_canvas_scrollbars_init(HippoCanvasScrollbars *scrollbars)
{
    GtkWidget *widget;
    GtkWidget *canvas;
    GtkWidget *viewport;

    widget = gtk_scrolled_window_new(NULL,NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(widget),
                                   GTK_POLICY_AUTOMATIC,
                                   GTK_POLICY_AUTOMATIC);
    gtk_scrolled_window_set_shadow_type(GTK_SCROLLED_WINDOW(widget),
                                        GTK_SHADOW_NONE);
    canvas = hippo_canvas_new();
    gtk_widget_show(canvas);
    gtk_scrolled_window_add_with_viewport(GTK_SCROLLED_WINDOW(widget), canvas);
    viewport = GTK_BIN(widget)->child;

    g_signal_connect(viewport, "size-allocate",
                     G_CALLBACK(on_viewport_size_allocate), NULL);
               
    gtk_viewport_set_shadow_type(GTK_VIEWPORT(gtk_bin_get_child(GTK_BIN(widget))),
                                 GTK_SHADOW_NONE);
    suppress_shadow_width(gtk_bin_get_child(GTK_BIN(widget)));
    g_object_set(scrollbars, "widget", widget, NULL);
}


HippoCanvasItem*
hippo_canvas_scrollbars_new(void)
{
    return g_object_new(HIPPO_TYPE_CANVAS_SCROLLBARS, NULL);
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
    ENTRY_PROP_TEXT,
    ENTRY_PROP_PASSWORD_MODE
};

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
    case GDK_Tab:
        key = HIPPO_KEY_TAB;
        break;
    case GDK_ISO_Left_Tab:
        key = HIPPO_KEY_LEFTTAB;
        break;
    default:
        key = HIPPO_KEY_UNKNOWN;
        break;
    }

    character = gdk_keyval_to_unicode(event->keyval);
        
    return hippo_canvas_item_emit_key_press_event(HIPPO_CANVAS_ITEM(canvas_entry), key, character,
                                                  (event->state & GDK_SHIFT_MASK ? HIPPO_MODIFIER_SHIFT : 0) +
                                                  (event->state & GDK_CONTROL_MASK ? HIPPO_MODIFIER_CTRL : 0) +
                                                  (event->state & GDK_MOD1_MASK ? HIPPO_MODIFIER_ALT : 0));
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
    case ENTRY_PROP_PASSWORD_MODE:
        gtk_entry_set_visibility(GTK_ENTRY(entry), !g_value_get_boolean(value));
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
    case ENTRY_PROP_PASSWORD_MODE:
        g_value_set_boolean(value, !gtk_entry_get_visibility(GTK_ENTRY(entry)));
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

    g_object_class_install_property(object_class,
                                    ENTRY_PROP_PASSWORD_MODE,
                                    g_param_spec_boolean("password-mode",
                                                         _("Password mode"),
                                                         _("Show text as bullets/stars"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_entry_init(HippoCanvasEntry *entry)
{
    GtkWidget *gtk_entry;

    gtk_entry = gtk_entry_new();

    /* GTK forces a min width of 150, which is often too big for our canvas layouts */
    gtk_widget_set_size_request(gtk_entry, 60, -1);
    
    g_object_set(entry, "widget", gtk_entry, NULL);

    g_signal_connect(gtk_entry, "changed",
                     G_CALLBACK(on_canvas_entry_changed), entry);
    g_signal_connect(gtk_entry, "key-press-event",
                     G_CALLBACK(on_canvas_entry_key_press_event), entry);

}

HippoCanvasItem*
hippo_canvas_entry_new(void)
{
    return g_object_new(HIPPO_TYPE_CANVAS_ENTRY, NULL);
}

guint 
hippo_canvas_entry_get_position(HippoCanvasEntry      *entry)
{
    GtkEntry *gtk_entry;
    guint pos;

    g_object_get(entry, "widget", &gtk_entry, NULL);
    pos = gtk_editable_get_position(GTK_EDITABLE(gtk_entry));
    g_object_unref(gtk_entry);

    return pos;
}

void
hippo_canvas_entry_set_position(HippoCanvasEntry *entry,
                                guint pos)
{
    GtkEntry *gtk_entry;

    g_object_get(entry, "widget", &gtk_entry, NULL);
    gtk_editable_set_position(GTK_EDITABLE(gtk_entry), pos);
    g_object_unref(gtk_entry);
}
