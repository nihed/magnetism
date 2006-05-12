#include <config.h>
#include "hippo-status-icon.h"
#include <gtk/gtkstatusicon.h>
#include "main.h"

static void      hippo_status_icon_init                (HippoStatusIcon       *icon);
static void      hippo_status_icon_class_init          (HippoStatusIconClass  *klass);

static void      hippo_status_icon_finalize            (GObject                 *object);

static void      hippo_status_icon_activate            (GtkStatusIcon           *gtk_icon);
static void      hippo_status_icon_popup_menu          (GtkStatusIcon           *gtk_icon,
                                                        guint                    button,
                                                        guint32                  activate_time);

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
}

HippoStatusIcon*
hippo_status_icon_new(HippoDataCache *cache)
{
    HippoStatusIcon *icon =
        g_object_new(HIPPO_TYPE_STATUS_ICON,
                     "icon-name", "gnome-fish",
                     NULL);

    icon->cache = cache;
    g_object_ref(icon->cache);

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
        button = 0;
    
    time = gtk_get_current_event_time();
    
    hippo_status_icon_popup_menu(gtk_icon, button, time);
}

static void
hippo_status_icon_popup_menu(GtkStatusIcon *gtk_icon,
                             guint          button,
                             guint32        activate_time)
{
    HippoStatusIcon *icon = HIPPO_STATUS_ICON(gtk_icon);
    GtkWidget *menu_item;
    GdkModifierType state;
    gboolean leet_mode;
    
    leet_mode = FALSE;
    if (gtk_get_current_event_state(&state)) {
        if (state & GDK_CONTROL_MASK)
            leet_mode = TRUE;
    }
    
    destroy_menu(icon);
    
    icon->popup_menu = gtk_menu_new();

    menu_item = gtk_separator_menu_item_new();
    gtk_widget_show(menu_item);
    gtk_menu_shell_append(GTK_MENU_SHELL (icon->popup_menu), menu_item);
    
    menu_item = gtk_image_menu_item_new_from_stock (GTK_STOCK_ABOUT, NULL);
    g_signal_connect_swapped(menu_item, "activate", G_CALLBACK(hippo_app_show_about),
        hippo_get_app());
    gtk_widget_show(menu_item);
    gtk_menu_shell_append(GTK_MENU_SHELL (icon->popup_menu), menu_item);

    if (leet_mode) {
        menu_item = gtk_separator_menu_item_new();
        gtk_widget_show(menu_item);
        gtk_menu_shell_append(GTK_MENU_SHELL (icon->popup_menu), menu_item);
            
        menu_item = gtk_image_menu_item_new_from_stock (GTK_STOCK_QUIT, NULL);
        g_signal_connect_swapped(menu_item, "activate", G_CALLBACK(hippo_app_quit),
            hippo_get_app());
        gtk_widget_show(menu_item);
        gtk_menu_shell_append(GTK_MENU_SHELL (icon->popup_menu), menu_item);
    }        
    
    gtk_menu_popup (GTK_MENU (icon->popup_menu), NULL, NULL,
                    gtk_status_icon_position_menu, icon,
                    button, activate_time);
    gtk_menu_shell_select_first(GTK_MENU_SHELL(icon->popup_menu), FALSE);                    
}                             
