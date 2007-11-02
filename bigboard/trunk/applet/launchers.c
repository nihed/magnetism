/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>
#include "launchers.h"
#include "self.h"
#include "apps.h"
#include "desktop.h"
#include <string.h>

typedef struct {
    GtkWidget *box;
    GSList *buttons;

} LaunchersData;

static void
update_button_icon(GtkWidget *button)
{
    App *app;
    GdkPixbuf *icon;
    GtkWidget *image;
    
    app = g_object_get_data(G_OBJECT(button), "launchers-app");

    icon = app_get_icon(app);
    
    image = gtk_bin_get_child(GTK_BIN(button));    
    
    gtk_image_set_from_pixbuf(GTK_IMAGE(image), icon);
    
    if (icon == NULL)
        gtk_widget_hide(button);
    else
        gtk_widget_show(button);
}

static GtkWidget*
find_button_for_app(LaunchersData *ld,
                    App *app)
{
    GSList *l;

    for (l = ld->buttons; l != NULL; l = l->next) {
        GtkWidget *button = l->data;

        if (g_object_get_data(G_OBJECT(button), "launchers-app") == app)
            return button;
    }

    return NULL;
}

static void
set_tooltip (GtkWidget  *widget,
             const char *tip)
{
        GtkTooltips *tooltips;
        
        tooltips = g_object_get_data (G_OBJECT (widget), "tooltips");
        if (!tooltips) {
                tooltips = gtk_tooltips_new ();
                g_object_ref (tooltips);
                gtk_object_sink (GTK_OBJECT (tooltips));
                g_object_set_data_full (G_OBJECT (widget), "tooltips", tooltips,
                                        (GDestroyNotify) g_object_unref);
        }

        gtk_tooltips_set_tip (tooltips, widget, tip, NULL);
}

static void
app_changed(App  *app,
            void *data)
{
    GtkWidget *button;
    LaunchersData *ld;
    const char *tooltip;
    
    ld = data;

    button = find_button_for_app(ld, app);

    if (button == NULL)
        return;

    tooltip = app_get_tooltip(app);
    
    if (tooltip)
        set_tooltip(button, tooltip);
    
    update_button_icon(button);
}

static gboolean
app_launch(App       *app,
           GdkScreen *screen,
           GError   **error)
{
    const char *desktop_names;
    
    desktop_names = app_get_desktop_names(app);
    if (desktop_names == NULL) {
        g_set_error(error, G_FILE_ERROR,
                    G_FILE_ERROR_FAILED,
                    "Unable to launch this application");
        return FALSE;
    }

    return desktop_launch_list(screen, desktop_names, error);
}

static void
on_button_clicked(GtkWidget *button,
                  void      *data)
{
    App *app;
    GError *error;
    
    app = g_object_get_data(G_OBJECT(button), "launchers-app");

    error = NULL;
    if (!app_launch(app, gtk_widget_get_screen(button),
                    &error)) {
        GtkWidget *dialog;
        dialog = gtk_message_dialog_new_with_markup (NULL, /* parent */
                                                     GTK_DIALOG_NO_SEPARATOR,
                                                     GTK_MESSAGE_ERROR,
                                                     GTK_BUTTONS_CLOSE,
                                                     "<b>%s</b>\n%s",
                                                     "Unable to start application",
                                                     error->message);
        g_signal_connect(dialog, "response", G_CALLBACK(gtk_object_destroy), NULL);
        gtk_window_present(GTK_WINDOW(dialog));
        
        /* g_printerr("Failed to launch app: %s\n", error->message); */
        g_error_free(error);
    }
}

static GtkWidget*
make_button_for_app(LaunchersData *ld,
                    App           *app)
{
    GtkWidget *button;
    GtkWidget *image;
    
    button = gtk_button_new();
    image = gtk_image_new();
    gtk_widget_show(image);
    
    gtk_button_set_relief(GTK_BUTTON(button), GTK_RELIEF_NONE);
    gtk_widget_set_name (button, "bigboard-button-launcher-button");
    gtk_rc_parse_string ("\n"
                         "   style \"bigboard-button-launcher-button-style\"\n"
                         "   {\n"
                         "      xthickness=0\n"
                         "      ythickness=0\n"                         
                         "      GtkWidget::focus-line-width=0\n"
                         "      GtkWidget::focus-padding=0\n"
                         "      GtkButton::default-border={0,0,0,0}\n"
                         "      GtkButton::default-outside-border={0,0,0,0}\n"
                         "      GtkButton::inner-border={0,0,0,0}\n" 
                         "      GtkButton::interior-focus=0\n"                         
                             "   }\n"
                         "\n"
                         "    widget \"*.bigboard-button-launcher-button\" style \"bigboard-button-launcher-button-style\"\n"
                         "\n");
    
    gtk_container_add(GTK_CONTAINER(button), image);
    
    app_ref(app);
    g_object_set_data_full(G_OBJECT(button), "launchers-app", app,
                           (GFreeFunc) app_unref);

    g_signal_connect(G_OBJECT(button), "clicked",
                     G_CALLBACK(on_button_clicked),
                     NULL);
    
    update_button_icon(button);

    g_signal_connect(G_OBJECT(app), "changed",
                     G_CALLBACK(app_changed), ld);
    
    return button;
}

static void
apps_changed_callback (GSList    *apps,
                       void      *data)
{
    GSList *l;
    GSList *buttons;
    LaunchersData *ld;
    int app_count;
    
    ld = data;
    buttons = NULL;
    app_count = 0;

    /* limit to 7 apps shown in the applet */
    for (l = apps; l != NULL && app_count < 7; l = l->next) {
        App *app = l->data;
        GtkWidget *button;

        button = find_button_for_app(ld, app);
        if (button == NULL) {
            button = make_button_for_app(ld, app);
        }
        buttons = g_slist_prepend(buttons, button);

        app_count += 1;
    }

    buttons = g_slist_reverse(buttons); /* put back in order */

    /* ref and remove all the old buttons */
    for (l = ld->buttons; l != NULL; l = l->next) {
        GtkWidget *parent;
        GtkWidget *button;

        button = l->data;

        g_object_ref(button);

        parent = gtk_widget_get_parent(button);
        gtk_container_remove(GTK_CONTAINER(parent), button);
    }

    /* pack all the new buttons (which may be some of the same ones) */
    for (l = buttons; l != NULL; l = l->next) {
        GtkWidget *button;

        button = l->data;        

        gtk_box_pack_start(GTK_BOX(ld->box), button, TRUE, TRUE, 0);
    }

    /* unref the old buttons */
    for (l = ld->buttons; l != NULL; l = l->next) {
        GtkWidget *button;

        button = l->data;

        g_object_unref(button);
    }

    /* replace the list */
    g_slist_free(ld->buttons);
    ld->buttons = buttons;

    /* don't show all; we show/hide the buttons according to whether we have
     * the app's icon downloaded.
     */
    gtk_widget_show(ld->box);
}

static void
launchers_destroyed_callback(GtkWidget *launchers,
                             void      *data)
{
    LaunchersData *ld;

    ld = data;

    self_remove_apps_changed_callback(apps_changed_callback, ld);
    
    g_free(ld);
}

GtkWidget*
launchers_new(void)
{
    GtkWidget *hbox;
    LaunchersData *ld;
    
    hbox = gtk_hbox_new(FALSE, 1);

    ld = g_new0(LaunchersData, 1);
    ld->box = hbox;

    g_signal_connect(G_OBJECT(hbox), "destroy",
                     G_CALLBACK(launchers_destroyed_callback),
                     ld);
    
    self_add_apps_changed_callback(apps_changed_callback, ld);

    return hbox;
}
