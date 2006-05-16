#include <config.h>
#include "hippo-chat-window.h"
#include "main.h"
#include <gtk/gtk.h>
#include <string.h>

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
static void      on_loaded                             (HippoChatRoom         *room,
                                                        HippoChatWindow       *window);
static void      on_cleared                            (HippoChatRoom         *room,
                                                        HippoChatWindow       *window);
static void      on_user_changed                       (HippoPerson           *user,
                                                        HippoChatWindow       *window);
static void      on_send_message                       (GtkWidget             *entry_or_button,
                                                        HippoChatWindow       *window);
static void      on_send_entry_changed                 (GtkWidget             *entry,
                                                        HippoChatWindow       *window);

struct _HippoChatWindow {
    GtkWindow parent;
    HippoDataCache *cache;
    HippoChatRoom *room;
    GtkWindowGroup *window_group;    
    GtkWidget *title_label;
    GtkWidget *member_view;
    GtkWidget *chat_log_view;
    GtkWidget *send_entry;
    GtkWidget *send_button;
    GtkWidget *error_dialog;
    
    GHashTable *message_tags;
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

    window->window_group = gtk_window_group_new();
    gtk_window_group_add_window(window->window_group, GTK_WINDOW(window));

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

    window->chat_log_view = gtk_text_view_new();
    gtk_text_view_set_editable(GTK_TEXT_VIEW(window->chat_log_view), FALSE);
    gtk_text_view_set_cursor_visible(GTK_TEXT_VIEW(window->chat_log_view), FALSE);
    gtk_text_view_set_left_margin(GTK_TEXT_VIEW(window->chat_log_view), SPACING);
    gtk_text_view_set_right_margin(GTK_TEXT_VIEW(window->chat_log_view), SPACING);

    sw = gtk_scrolled_window_new(NULL, NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(sw),
                                   GTK_POLICY_AUTOMATIC, GTK_POLICY_AUTOMATIC);
    gtk_scrolled_window_set_shadow_type(GTK_SCROLLED_WINDOW(sw), GTK_SHADOW_IN);
    gtk_container_add(GTK_CONTAINER(sw), window->chat_log_view);    
    gtk_box_pack_start(GTK_BOX(vbox2), sw, TRUE, TRUE, 0);

    hbox2 = gtk_hbox_new(FALSE, SPACING);
    gtk_box_pack_end(GTK_BOX(vbox2), hbox2, FALSE, FALSE, 0);
    
    window->send_entry = gtk_entry_new();
    gtk_box_pack_start(GTK_BOX(hbox2), window->send_entry, TRUE, TRUE, 0);
    
    window->send_button = gtk_button_new_with_mnemonic(_("_Send"));
    gtk_box_pack_end(GTK_BOX(hbox2), window->send_button, FALSE, FALSE, 0);

    gtk_widget_show_all(vbox);

    gtk_window_set_default_size(GTK_WINDOW(window), 600, 400);

    /* Set up widget signals */

    on_send_entry_changed(window->send_entry, window);
    
    g_signal_connect(window->send_entry, "changed", G_CALLBACK(on_send_entry_changed), window);
    g_signal_connect(window->send_entry, "activate", G_CALLBACK(on_send_message), window);    
    g_signal_connect(window->send_button, "clicked", G_CALLBACK(on_send_message), window);

    /* Now connect to chat room */
    window->room = room;
    g_object_ref(window->room);

    on_title_changed(window->room, window);

    g_signal_connect(window->room, "title-changed", G_CALLBACK(on_title_changed), window);
    g_signal_connect(window->room, "user-state-changed", G_CALLBACK(on_user_state_changed), window);
    g_signal_connect(window->room, "message-added", G_CALLBACK(on_message_added), window);
    g_signal_connect(window->room, "cleared", G_CALLBACK(on_cleared), window);
    g_signal_connect(window->room, "loaded", G_CALLBACK(on_loaded), window);    
    
    hippo_connection_join_chat_room(get_connection(window), window->room, HIPPO_CHAT_STATE_PARTICIPANT);
    
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
        g_signal_handlers_disconnect_by_func(window->room, G_CALLBACK(on_loaded), window);      
        
        hippo_connection_leave_chat_room(get_connection(window), window->room, HIPPO_CHAT_STATE_PARTICIPANT);
        
        g_object_unref(window->room);
        window->room = NULL;
    }
    
    if (window->message_tags != NULL) {
        g_hash_table_destroy(window->message_tags);
        window->message_tags = NULL;
    }    
    
    /* will get destroyed by default container destroy */
    window->title_label = NULL;
    window->member_view = NULL;
    window->chat_log_view = NULL;
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

static GtkTextTag*
tag_for_serial(HippoChatWindow *window,
               int              serial)
{
    GtkTextTag *tag;
    GtkTextBuffer *buffer;
    
    if (window->message_tags == NULL) {
        /* hash message serials to GtkTextTag delimiting representation of that message */
        window->message_tags = g_hash_table_new_full(g_direct_hash, g_direct_equal, NULL, (GFreeFunc) g_object_unref);
    }
    
    buffer = gtk_text_view_get_buffer(GTK_TEXT_VIEW(window->chat_log_view));
    
    tag = g_hash_table_lookup(window->message_tags, GINT_TO_POINTER(serial));
    if (tag == NULL) {
        tag = gtk_text_buffer_create_tag(buffer, NULL, NULL);
        g_object_ref(tag);
        g_hash_table_replace(window->message_tags, GINT_TO_POINTER(serial), tag);
    }
    
    return tag;
}

static gboolean
find_message_bounds(HippoChatWindow *window,
                    int              serial,
                    GtkTextIter     *start,
                    GtkTextIter     *end)
{
    GtkTextTag *tag;
    GtkTextIter iter;
    GtkTextBuffer *buffer;
    
    buffer = gtk_text_view_get_buffer(GTK_TEXT_VIEW(window->chat_log_view));
    tag = tag_for_serial(window, serial);
    
    gtk_text_buffer_get_start_iter(buffer, &iter);
    if (!gtk_text_iter_begins_tag(&iter, tag)) {
        if (!gtk_text_iter_forward_to_tag_toggle(&iter, tag)) {
            return FALSE; /* tag doesn't exist in buffer */
        }
    }
    if (start)
        *start = iter;
    if (!gtk_text_iter_forward_to_tag_toggle(&iter, tag)) {
        g_warning("Tag starts but never ends!");
        g_assert_not_reached();   
    }
    if (end)
        *end = iter;
    return TRUE;
}

typedef struct {
    int new_serial;
    int lower_serial;
    int higher_serial;
} FindInsertionData;

static void
find_insertion_foreach(void *key, void *value, void *data)
{
    FindInsertionData *fid = data;
    int this_serial = GPOINTER_TO_INT(key);
    
    if (this_serial == fid->new_serial) {
        g_warning("Finding insertion point for a message %d already in the buffer!", fid->new_serial);
        return;
    }
    
    if (this_serial > fid->lower_serial && this_serial < fid->new_serial)
        fid->lower_serial = this_serial;
    if (this_serial < fid->higher_serial && this_serial > fid->new_serial)
        fid->higher_serial = this_serial;
}

static void
find_insertion_point(HippoChatWindow *window,
                     int              serial,
                     GtkTextIter     *iter)
{
    FindInsertionData fid;
    GtkTextBuffer *buffer;

    /* This function finds an insertion point for a message that 
     * isn't already in the buffer
     */

#define BELOW_REAL_SERIAL -2
#define ABOVE_REAL_SERIAL G_MAXINT

    fid.new_serial = serial;
    fid.lower_serial = BELOW_REAL_SERIAL;
    fid.higher_serial = ABOVE_REAL_SERIAL;

    if (window->message_tags != NULL) {
        g_hash_table_foreach(window->message_tags, find_insertion_foreach, &fid);
    } else {
        /* can't possibly be any rows in there already */   
        ;
    }
    
    buffer = gtk_text_view_get_buffer(GTK_TEXT_VIEW(window->chat_log_view));
    
    if (fid.lower_serial == BELOW_REAL_SERIAL) {
        gtk_text_buffer_get_start_iter(buffer, iter);
    } else if (fid.higher_serial == ABOVE_REAL_SERIAL) {
        gtk_text_buffer_get_end_iter(buffer, iter);
    } else {
        /* we are between two existing messages, find the point after 
         * the first one
         */
        if (!find_message_bounds(window, fid.lower_serial, NULL, iter)) {
            g_warning("Did not find bounds of message %d we knew to exist", fid.lower_serial);
            /* don't crash */
            gtk_text_buffer_get_end_iter(buffer, iter);
        }
    }
}

enum {
    PICTURE_ON_LEFT,
    PICTURE_ON_RIGHT
};

static void
on_picture_loaded_for_log(GdkPixbuf *pixbuf,
                          void      *data)
{
    GtkTextMark *mark;
    GtkTextBuffer *buffer;
    int where;
    
    mark = GTK_TEXT_MARK(data);
    buffer = gtk_text_mark_get_buffer(mark);
    
    /* pixbuf is NULL if we failed to load an image,
     * and buffer is NULL if we destroyed the chat window
     */
    if (pixbuf && buffer) {
        GtkTextIter iter;
        
        where = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(mark), "where"));
        
        gtk_text_buffer_get_iter_at_mark(buffer, &iter, mark);
        
        if (where == PICTURE_ON_RIGHT)
            gtk_text_iter_forward_to_line_end(&iter);
        
        gtk_text_buffer_insert_pixbuf(buffer, &iter, pixbuf);
    }
    if (!gtk_text_mark_get_deleted(mark))
        gtk_text_buffer_delete_mark(buffer, mark);
    g_object_unref(mark);
}

static void
on_message_added(HippoChatRoom         *room,
                 HippoChatMessage      *message,
                 HippoChatWindow       *window)
{
    GtkTextBuffer *buffer;
    GtkTextIter iter;
    const char *text;
    int len;
    int serial;
    GtkTextMark *picture_mark;
    HippoPerson *sender;
    HippoPerson *self;
    GtkTextTag *tag;
    
    serial = hippo_chat_message_get_serial(message);
    sender = hippo_chat_message_get_person(message);
    self = hippo_data_cache_get_self(window->cache);
    
    if (find_message_bounds(window, serial, NULL, NULL)) {
        /* we already have this message */
        return;
    }
    
    text = hippo_chat_message_get_text(message);
    len = strlen(text);
    
    buffer = gtk_text_view_get_buffer(GTK_TEXT_VIEW(window->chat_log_view));
  
    find_insertion_point(window, serial, &iter);

    picture_mark = gtk_text_buffer_create_mark(buffer, NULL, &iter, TRUE);
  
    tag = tag_for_serial(window, serial);
    if (self && self == sender) {
        g_object_set(G_OBJECT(tag), "justification", GTK_JUSTIFY_LEFT, NULL);
        g_object_set_data(G_OBJECT(picture_mark), "where", GINT_TO_POINTER(PICTURE_ON_LEFT));
    } else {
        g_object_set(G_OBJECT(tag), "justification", GTK_JUSTIFY_RIGHT, NULL);
        g_object_set_data(G_OBJECT(picture_mark), "where", GINT_TO_POINTER(PICTURE_ON_RIGHT));        
    }
    g_object_ref(picture_mark);
    hippo_app_load_photo(hippo_get_app(), HIPPO_ENTITY(sender),
                         on_picture_loaded_for_log, picture_mark);
    /* if the load photo callback was invoked synchronously, the mark is already deleted here... */
    picture_mark = NULL;

    gtk_text_buffer_insert_with_tags(buffer, &iter, text, len, tag, NULL);
    
    /* If the inserted text did not end in a newline, insert one. */
    if (!gtk_text_iter_starts_line(&iter)) {
        gtk_text_buffer_insert_with_tags(buffer, &iter, "\n", 1, tag, NULL);
    }
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
    if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_POST) {
        window_title = g_strdup_printf(_("%s - Mugshot Link Chat"), title);
    } else if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_GROUP) {
        window_title = g_strdup_printf(_("%s - Mugshot Group Chat"), title);
    } else {
        window_title = g_strdup_printf(_("%s - Mugshot Chat"), title);
    }
    gtk_window_set_title(GTK_WINDOW(window), window_title);
    g_free(window_title);
}

static void
on_loaded(HippoChatRoom         *room,
          HippoChatWindow       *window)
{
    if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_BROKEN) {
        if (window->error_dialog)
            gtk_object_destroy(GTK_OBJECT(window->error_dialog));
        window->error_dialog = gtk_message_dialog_new(GTK_WINDOW(window),
                                        GTK_DIALOG_DESTROY_WITH_PARENT | GTK_DIALOG_MODAL,
                                        GTK_MESSAGE_ERROR,
                                        GTK_BUTTONS_CLOSE,
                                        _("This chat doesn't seem to exist!"));
        gtk_window_group_add_window(window->window_group, GTK_WINDOW(window->error_dialog));
        
        g_signal_connect(window->error_dialog, "response", G_CALLBACK(gtk_widget_destroy), NULL);
        g_signal_connect(window->error_dialog, "destroy", G_CALLBACK(gtk_widget_destroyed),
                        &window->error_dialog);
        /* destroy chat window with this error dialog */
        g_signal_connect_swapped(window->error_dialog, "destroy", G_CALLBACK(gtk_widget_destroy), window);

        gtk_widget_show(window->error_dialog);
    }
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

static gboolean
is_all_whitespace(const char *text)
{
    const char *p;
    
    p = text;
    while (*p) {
        gunichar c = g_utf8_get_char(p);
        
        if (!g_unichar_isspace(c))
            return FALSE;
    
        p = g_utf8_next_char(p); 
    }
    return TRUE;
}

static void
on_send_message(GtkWidget             *entry_or_button,
                HippoChatWindow       *window)
{
    HippoConnection *connection;
    const char *text;
    
    connection = get_connection(window);
    
    text = gtk_entry_get_text(GTK_ENTRY(window->send_entry));
    
    /* the send button isn't enabled if all whitespace, but you can still
     * press enter on the entry and get here
     */
    if (is_all_whitespace(text))
        return;    
    
    hippo_connection_send_chat_room_message(connection, window->room, text);
    
    gtk_entry_set_text(GTK_ENTRY(window->send_entry), "");
}

static void
on_send_entry_changed(GtkWidget             *entry,
                      HippoChatWindow       *window)
{
    const char *text;
    
    text = gtk_entry_get_text(GTK_ENTRY(window->send_entry));
    gtk_widget_set_sensitive(window->send_button, !is_all_whitespace(text));
}
