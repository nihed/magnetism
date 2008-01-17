/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-status-icon.h"
#include <gtk/gtkstatusicon.h>
#include "main.h"
#include <stacker/hippo-stack-manager.h>

static void      hippo_status_icon_init                (HippoStatusIcon       *icon);
static void      hippo_status_icon_class_init          (HippoStatusIconClass  *klass);

static void      hippo_status_icon_finalize            (GObject                 *object);

static void      hippo_status_icon_activate            (GtkStatusIcon           *gtk_icon);
static void      hippo_status_icon_popup_menu          (GtkStatusIcon           *gtk_icon,
                                                        guint                    button,
                                                        guint32                  activate_time);

struct _HippoStatusIcon {
    GtkStatusIcon parent;
    DDMDataModel *model;
    DDMDataResource *global_resource;
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
}

static const char *
get_icon_name(DDMDataModel *model)
{
    if (ddm_data_model_is_online(model))
        return "mugshot_notification";
    else
        return "mugshot_notification_disabled";
}

static const char *
get_icon_tooltip(DDMDataModel *model)
{
    return "Mugshot";
}

static void
on_online_changed(DDMDataResource *global_resource,
                  GSList          *changed_properties,
                  gpointer         data)
{
    HippoStatusIcon *icon = data;
    
    g_object_set(G_OBJECT(icon), 
                 "icon-name", get_icon_name(icon->model),
                 NULL);
}

static void
set_global_resource(HippoStatusIcon *icon,
                    DDMDataResource *global_resource)
{
    if (icon->global_resource) {
        ddm_data_resource_disconnect(icon->global_resource,
                                     on_online_changed,
                                     icon);
        ddm_data_resource_unref(icon->global_resource);
        icon->global_resource = NULL;
    }

    icon->global_resource = global_resource;

    if (icon->global_resource) {
        ddm_data_resource_ref(icon->global_resource);
        ddm_data_resource_connect(icon->global_resource, "online",
                                  on_online_changed,
                                  icon);
    }

    on_online_changed(icon->global_resource, NULL, icon);
}

static void
on_ready(DDMDataModel    *model,
         HippoStatusIcon *icon)
{
    set_global_resource(icon,
                        ddm_data_model_get_global_resource(model));
}

HippoStatusIcon*
hippo_status_icon_new(DDMDataModel *model)
{
    HippoStatusIcon *icon;
    
    icon = g_object_new(HIPPO_TYPE_STATUS_ICON,
                        "icon-name", get_icon_name(model),
                        NULL);
    
    icon->model = g_object_ref(model);

    g_signal_connect(icon->model, "ready",
                     G_CALLBACK(on_ready), icon);

    gtk_status_icon_set_tooltip(GTK_STATUS_ICON(icon),
                                get_icon_tooltip(model));

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

    set_global_resource(icon, NULL);
    destroy_menu(icon);

    g_signal_handlers_disconnect_by_func(icon->model,
                                         (gpointer)on_ready,
                                         icon);
    
    g_object_unref(icon->model);
    
    G_OBJECT_CLASS(hippo_status_icon_parent_class)->finalize(object);
}

static void
hippo_status_icon_activate(GtkStatusIcon *gtk_icon)
{
    /* HippoStatusIcon *icon = HIPPO_STATUS_ICON(gtk_icon); */
    GdkEvent *event;
    guint button;
    guint32 time;
    
    event = gtk_get_current_event();
    if (event != NULL && event->type == GDK_BUTTON_PRESS)
        button = event->button.button;
    else
        button = 1;

    if (button == 1) {
#if 0
        HippoConnection *connection;

        connection = hippo_data_cache_get_connection(icon->cache);
        if (!hippo_connection_get_connected(connection)) {
            /* FIXME: Implement this getting the necessary information via the data model */
            HippoStackerPlatform *platform = hippo_stacker_app_get_stacker_platform(hippo_get_stacker_app());
            hippo_stacker_platform_show_disconnected_window(platform, connection);
        } else
#endif            
        {
            /* the UI has to exist since we (the tray icon) are part of it */
            HippoStackManager *stack_manager = hippo_stacker_app_get_stack(hippo_get_stacker_app());
            hippo_stack_manager_show_browser(stack_manager, TRUE);
        }
    } else if (button == 3) {
        time = gtk_get_current_event_time();
    
        hippo_status_icon_popup_menu(gtk_icon, button, time);
    }
}

static void
on_quit_activated(GtkMenuItem *menu_item,
                  void        *data)
{
    hippo_stacker_app_quit(hippo_get_stacker_app());
}

static void
hippo_status_icon_popup_menu(GtkStatusIcon *gtk_icon,
                             guint          button,
                             guint32        activate_time)
{
    HippoStatusIcon *icon = HIPPO_STATUS_ICON(gtk_icon);
    GtkWidget *menu_item;
    GtkWidget *label;

    destroy_menu(icon);
    
    icon->popup_menu = gtk_menu_new();

    menu_item = gtk_image_menu_item_new_from_stock(GTK_STOCK_HOME, NULL);
    label = gtk_bin_get_child(GTK_BIN(menu_item));
    gtk_label_set_text(GTK_LABEL(label), _("My Mugshot home page"));
    g_signal_connect_swapped(menu_item, "activate", G_CALLBACK(hippo_stacker_app_show_home),
        hippo_get_stacker_app());
    gtk_widget_show(menu_item);
    gtk_menu_shell_append(GTK_MENU_SHELL(icon->popup_menu), menu_item);
                
    menu_item = gtk_image_menu_item_new_from_stock(GTK_STOCK_ABOUT, NULL);
    g_signal_connect_swapped(menu_item, "activate", G_CALLBACK(hippo_stacker_app_show_about),
        hippo_get_stacker_app());
    gtk_widget_show(menu_item);
    gtk_menu_shell_append(GTK_MENU_SHELL(icon->popup_menu), menu_item);

    menu_item = gtk_separator_menu_item_new();
    gtk_widget_show(menu_item);
    gtk_menu_shell_append(GTK_MENU_SHELL(icon->popup_menu), menu_item);
    
    menu_item = gtk_image_menu_item_new_from_stock (GTK_STOCK_QUIT, NULL);
    g_signal_connect_swapped(menu_item, "activate", G_CALLBACK(on_quit_activated),
                             NULL);
    gtk_widget_show(menu_item);
    gtk_menu_shell_append(GTK_MENU_SHELL(icon->popup_menu), menu_item);

    gtk_menu_popup (GTK_MENU(icon->popup_menu), NULL, NULL,
                    gtk_status_icon_position_menu, icon,
                    button, activate_time);
    gtk_menu_shell_select_first(GTK_MENU_SHELL(icon->popup_menu), FALSE);                    
}                             
