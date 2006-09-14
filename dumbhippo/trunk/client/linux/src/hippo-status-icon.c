/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-status-icon.h"
#include <gtk/gtkstatusicon.h>
#include "main.h"
#include "hippo-stack-manager.h"

typedef struct {
    const HippoHotness hotness;
    const char *icon_name;
} HotnessIcon;

static const HotnessIcon icon_names[] = {
   { HIPPO_HOTNESS_COLD, "mugshot_swarm_1" },
   { HIPPO_HOTNESS_COOL, "mugshot_swarm_2" },
   { HIPPO_HOTNESS_WARM, "mugshot_swarm_3" },
   { HIPPO_HOTNESS_GETTING_HOT, "mugshot_swarm_4" },
   { HIPPO_HOTNESS_HOT, "mugshot_swarm_5" },
   { HIPPO_HOTNESS_UNKNOWN, "mugshot_swarm_1" }
};

static void      hippo_status_icon_init                (HippoStatusIcon       *icon);
static void      hippo_status_icon_class_init          (HippoStatusIconClass  *klass);

static void      hippo_status_icon_finalize            (GObject                 *object);

static void      hippo_status_icon_activate            (GtkStatusIcon           *gtk_icon);
static void      hippo_status_icon_popup_menu          (GtkStatusIcon           *gtk_icon,
                                                        guint                    button,
                                                        guint32                  activate_time);
static void      on_hotness_changed                    (HippoDataCache          *cache,
                                                        HippoHotness             old,
                                                        HippoStatusIcon         *icon);
static void      on_state_changed                      (HippoConnection         *connection,
                                                        HippoStatusIcon         *icon);

struct _HippoStatusIcon {
    GtkStatusIcon parent;
    HippoDataCache *cache;
    GtkWidget *popup_menu;
};

struct _HippoStatusIconClass {
    GtkStatusIconClass parent_class;

};

G_DEFINE_TYPE(HippoStatusIcon, hippo_status_icon, GTK_TYPE_STATUS_ICON);
                       

static void
hippo_status_icon_init(HippoStatusIcon       *icon)
{
    
}

static void
hippo_status_icon_class_init(HippoStatusIconClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    GtkStatusIconClass *gtk_icon_class = GTK_STATUS_ICON_CLASS(klass);

    object_class->finalize = hippo_status_icon_finalize;
    
    gtk_icon_class->activate = hippo_status_icon_activate;
    gtk_icon_class->popup_menu = hippo_status_icon_popup_menu;
    
    {
        unsigned int i;
        for (i = 0; i < G_N_ELEMENTS(icon_names); ++i) {
            if (icon_names[i].hotness != i) {
                g_warning("Icons for HippoHotness out of sync with hotness enum");
                g_assert_not_reached();
            }
        }
    }
}

HippoStatusIcon*
hippo_status_icon_new(HippoDataCache *cache)
{
    HippoStatusIcon *icon;
    
    icon = g_object_new(HIPPO_TYPE_STATUS_ICON,
                     "icon-name", icon_names[HIPPO_HOTNESS_UNKNOWN].icon_name,
                     NULL);
    
    icon->cache = cache;
    g_object_ref(icon->cache);

    g_signal_connect(icon->cache, "hotness-changed", G_CALLBACK(on_hotness_changed), icon);
    g_signal_connect(hippo_data_cache_get_connection(icon->cache), "state-changed",
                     G_CALLBACK(on_state_changed), icon);

    /* initialize tooltip */
    on_state_changed(hippo_data_cache_get_connection(icon->cache), icon);

    return HIPPO_STATUS_ICON(icon);
}

static void
destroy_menu(HippoStatusIcon *icon)
{
    if (icon->popup_menu) {
        gtk_object_destroy(GTK_OBJECT(icon->popup_menu));
        icon->popup_menu = NULL;
    }
}

static void
hippo_status_icon_finalize(GObject *object)
{
    HippoStatusIcon *icon = HIPPO_STATUS_ICON(object);

    g_signal_handlers_disconnect_by_func(icon->cache, G_CALLBACK(on_hotness_changed), icon);
    g_signal_handlers_disconnect_by_func(hippo_data_cache_get_connection(icon->cache),
                                         G_CALLBACK(on_state_changed), icon);

    destroy_menu(icon);
    
    g_object_unref(icon->cache);
    
    G_OBJECT_CLASS(hippo_status_icon_parent_class)->finalize(object);
}

static void
hippo_status_icon_activate(GtkStatusIcon *gtk_icon)
{
    HippoStatusIcon *icon = HIPPO_STATUS_ICON(gtk_icon);
    GdkEvent *event;
    guint button;
    guint32 time;
    
    event = gtk_get_current_event();
    if (event != NULL && event->type == GDK_BUTTON_PRESS)
        button = event->button.button;
    else
        button = 1;

    if (button == 1) {
        hippo_stack_manager_toggle_stack(icon->cache);
    } else if (button == 3) {
        time = gtk_get_current_event_time();
    
        hippo_status_icon_popup_menu(gtk_icon, button, time);
    }
}

static void
append_escaped(GString    *str,
               const char *text)
{
    char *escaped;
    escaped = g_markup_escape_text(text, -1);
    g_string_append(str, escaped);
    g_free(escaped);
}

static void
hippo_status_icon_popup_menu(GtkStatusIcon *gtk_icon,
                             guint          button,
                             guint32        activate_time)
{
    HippoStatusIcon *icon = HIPPO_STATUS_ICON(gtk_icon);
    GtkWidget *menu_item;
    GtkWidget *label;
    GdkModifierType state;
    gboolean leet_mode;
    
    leet_mode = FALSE;
    if (gtk_get_current_event_state(&state)) {
        if (state & GDK_CONTROL_MASK)
            leet_mode = TRUE;
    }
    
    destroy_menu(icon);
    
    icon->popup_menu = gtk_menu_new();

    menu_item = gtk_image_menu_item_new_from_stock(GTK_STOCK_HOME, NULL);
    label = gtk_bin_get_child(GTK_BIN(menu_item));
    gtk_label_set_text(GTK_LABEL(label), _("My Mugshot home page"));
    g_signal_connect_swapped(menu_item, "activate", G_CALLBACK(hippo_app_show_home),
        hippo_get_app());
    gtk_widget_show(menu_item);
    gtk_menu_shell_append(GTK_MENU_SHELL(icon->popup_menu), menu_item);
                
    menu_item = gtk_image_menu_item_new_from_stock(GTK_STOCK_ABOUT, NULL);
    g_signal_connect_swapped(menu_item, "activate", G_CALLBACK(hippo_app_show_about),
        hippo_get_app());
    gtk_widget_show(menu_item);
    gtk_menu_shell_append(GTK_MENU_SHELL(icon->popup_menu), menu_item);

    if (leet_mode) {
        menu_item = gtk_separator_menu_item_new();
        gtk_widget_show(menu_item);
        gtk_menu_shell_append(GTK_MENU_SHELL(icon->popup_menu), menu_item);
            
        menu_item = gtk_image_menu_item_new_from_stock (GTK_STOCK_QUIT, NULL);
        g_signal_connect_swapped(menu_item, "activate", G_CALLBACK(hippo_app_quit),
            hippo_get_app());
        gtk_widget_show(menu_item);
        gtk_menu_shell_append(GTK_MENU_SHELL(icon->popup_menu), menu_item);
    }        
    
    gtk_menu_popup (GTK_MENU(icon->popup_menu), NULL, NULL,
                    gtk_status_icon_position_menu, icon,
                    button, activate_time);
    gtk_menu_shell_select_first(GTK_MENU_SHELL(icon->popup_menu), FALSE);                    
}                             

static void
on_hotness_changed(HippoDataCache  *cache,
                   HippoHotness     old,
                   HippoStatusIcon *icon)
{
    int new_hotness = hippo_data_cache_get_hotness(cache);
    
    g_return_if_fail(new_hotness >= 0);
    g_return_if_fail(new_hotness < (int) G_N_ELEMENTS(icon_names));
    
    g_object_set(G_OBJECT(icon), "icon-name", icon_names[new_hotness].icon_name, NULL);
}

static void
on_state_changed(HippoConnection         *connection,
                 HippoStatusIcon         *icon)
{
    gtk_status_icon_set_tooltip(GTK_STATUS_ICON(icon),
                                hippo_connection_get_tooltip(connection));
}
