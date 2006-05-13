#include <config.h>
#include "hippo-chat-window.h"
#include "main.h"
#include <gtk/gtk.h>

static void      hippo_chat_window_init                (HippoChatWindow       *window);
static void      hippo_chat_window_class_init          (HippoChatWindowClass  *klass);

static void      hippo_chat_window_finalize            (GObject               *object);
static void      hippo_chat_window_destroy             (GtkObject             *object);

static void      on_message_added                      (HippoChatRoom         *room,
                                                        HippoChatMessage      *message,
                                                        HippoChatWindow       *window);
static void      on_user_state_changed                 (HippoChatRoom         *room,
                                                        HippoPerson           *user,
                                                        HippoChatWindow       *window);
static void      on_title_changed                      (HippoChatRoom         *room,
                                                        HippoChatWindow       *window);
static void      on_cleared                            (HippoChatRoom         *room,
                                                        HippoChatWindow       *window);
static void      on_user_changed                       (HippoPerson           *user,
                                                        HippoChatWindow       *window);


struct _HippoChatWindow {
    GtkWindow parent;
    HippoDataCache *cache;
    HippoChatRoom *room;
    GtkWidget *title_label;
    GtkWidget *member_view;
    GtkWidget *chat_log;
    GtkWidget *send_entry;
    GtkWidget *send_button;
};

struct _HippoChatWindowClass {
    GtkWindowClass parent_class;

};

G_DEFINE_TYPE(HippoChatWindow, hippo_chat_window, GTK_TYPE_WINDOW);

static void
hippo_chat_window_init(HippoChatWindow  *window)
{
    
}

static void
hippo_chat_window_class_init(HippoChatWindowClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    GtkObjectClass *gtk_object_class = GTK_OBJECT_CLASS(klass);

    object_class->finalize = hippo_chat_window_finalize;
    gtk_object_class->destroy = hippo_chat_window_destroy;
}

static HippoConnection*
get_connection(HippoChatWindow *window)
{
    return hippo_data_cache_get_connection(window->cache);
}

#define SPACING 10

HippoChatWindow*
hippo_chat_window_new(HippoDataCache *cache,
                      HippoChatRoom  *room)
{
    HippoChatWindow *window;
    GtkWidget *vbox;
    GtkWidget *vbox2;    
    GtkWidget *hbox;
    GtkWidget *hbox2;
    GtkWidget *sw;
    
    window = g_object_new(HIPPO_TYPE_CHAT_WINDOW,
                          NULL);

    window->cache = cache;
    g_object_ref(window->cache);

    gtk_container_set_border_width(GTK_CONTAINER(window), SPACING);

    vbox = gtk_vbox_new(FALSE, SPACING);
    gtk_container_add(GTK_CONTAINER(window), vbox);
    
    window->title_label = gtk_label_new(NULL);
    gtk_misc_set_alignment(GTK_MISC(window->title_label), 0.0, 0.5);
    
    gtk_box_pack_start(GTK_BOX(vbox), window->title_label, FALSE, FALSE, 0);
    
    hbox = gtk_hbox_new(FALSE, SPACING);
    gtk_box_pack_end(GTK_BOX(vbox), hbox, TRUE, TRUE, 0);
    
    window->member_view = gtk_tree_view_new();
    gtk_widget_set_size_request(window->member_view, 140, -1);

    sw = gtk_scrolled_window_new(NULL, NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(sw),
                                   GTK_POLICY_AUTOMATIC, GTK_POLICY_AUTOMATIC);
    gtk_scrolled_window_set_shadow_type(GTK_SCROLLED_WINDOW(sw), GTK_SHADOW_IN);
    gtk_container_add(GTK_CONTAINER(sw), window->member_view);
    gtk_box_pack_start(GTK_BOX(hbox), sw, FALSE, FALSE, 0);
    
    vbox2 = gtk_vbox_new(FALSE, SPACING);
    gtk_box_pack_end(GTK_BOX(hbox), vbox2, TRUE, TRUE, 0);

    window->chat_log = gtk_text_view_new();

    sw = gtk_scrolled_window_new(NULL, NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(sw),
                                   GTK_POLICY_AUTOMATIC, GTK_POLICY_AUTOMATIC);
    gtk_scrolled_window_set_shadow_type(GTK_SCROLLED_WINDOW(sw), GTK_SHADOW_IN);
    gtk_container_add(GTK_CONTAINER(sw), window->chat_log);    
    gtk_box_pack_start(GTK_BOX(vbox2), sw, TRUE, TRUE, 0);

    hbox2 = gtk_hbox_new(FALSE, SPACING);
    gtk_box_pack_end(GTK_BOX(vbox2), hbox2, FALSE, FALSE, 0);
    
    window->send_entry = gtk_entry_new();
    gtk_box_pack_start(GTK_BOX(hbox2), window->send_entry, TRUE, TRUE, 0);
    
    window->send_button = gtk_button_new_with_mnemonic(_("_Send"));
    gtk_box_pack_end(GTK_BOX(hbox2), window->send_button, FALSE, FALSE, 0);

    gtk_widget_show_all(vbox);

    gtk_window_set_default_size(GTK_WINDOW(window), 600, 400);

    window->room = room;
    g_object_ref(window->room);

    on_title_changed(window->room, window);

    g_signal_connect(window->room, "title-changed", G_CALLBACK(on_title_changed), window);
    g_signal_connect(window->room, "user-state-changed", G_CALLBACK(on_user_state_changed), window);
    g_signal_connect(window->room, "message-added", G_CALLBACK(on_message_added), window);
    g_signal_connect(window->room, "cleared", G_CALLBACK(on_cleared), window);
    
    hippo_connection_join_chat_room(get_connection(window), window->room, HIPPO_CHAT_PARTICIPANT);
    
    return window;
}

static void
hippo_chat_window_destroy(GtkObject *gtk_object)
{
    HippoChatWindow *window = HIPPO_CHAT_WINDOW(gtk_object);

    if (window->room) {    
        g_signal_handlers_disconnect_by_func(window->room, G_CALLBACK(on_title_changed), window);
        g_signal_handlers_disconnect_by_func(window->room, G_CALLBACK(on_user_state_changed), window);
        g_signal_handlers_disconnect_by_func(window->room, G_CALLBACK(on_message_added), window);
        g_signal_handlers_disconnect_by_func(window->room, G_CALLBACK(on_cleared), window);        
        
        hippo_connection_leave_chat_room(get_connection(window), window->room, HIPPO_CHAT_PARTICIPANT);        
        
        g_object_unref(window->room);
        window->room = NULL;
    }
    
    /* will get destroyed by default container destroy */
    window->title_label = NULL;
    window->member_view = NULL;
    window->chat_log = NULL;
    window->send_entry = NULL;
    window->send_button = NULL;
    
    GTK_OBJECT_CLASS(hippo_chat_window_parent_class)->destroy(gtk_object);
}

static void
hippo_chat_window_finalize(GObject *object)
{
    HippoChatWindow *window = HIPPO_CHAT_WINDOW(object);

    g_object_unref(window->cache);
    
    G_OBJECT_CLASS(hippo_chat_window_parent_class)->finalize(object);
}

HippoChatRoom*
hippo_chat_window_get_room(HippoChatWindow *window)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_WINDOW(window), NULL);
    
    return window->room;
}

static void
on_message_added(HippoChatRoom         *room,
                 HippoChatMessage      *message,
                 HippoChatWindow       *window)
{

}
                 
static void
on_user_state_changed(HippoChatRoom         *room,
                      HippoPerson           *user,
                      HippoChatWindow       *window)
{

}
                      
static void
on_title_changed(HippoChatRoom         *room,
                 HippoChatWindow       *window)
{
    char *window_title;
    const char *title;
    
    title = hippo_chat_room_get_title(room);
    
    gtk_label_set_text(GTK_LABEL(window->title_label),
                       title);
    window_title = g_strdup_printf(_("%s - Mugshot Chat"), title);
    gtk_window_set_title(GTK_WINDOW(window), window_title);
    g_free(window_title);
}

static void
on_cleared(HippoChatRoom         *room,
           HippoChatWindow       *window)
{

}           

static void
on_user_changed(HippoPerson           *user,
                HippoChatWindow       *window)
{


}                
