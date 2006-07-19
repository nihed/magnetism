/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-chat-window.h"
#include "main.h"
#include "hippo-person-renderer.h"
#include <gtk/gtkwindow.h>
#include <gtk/gtktreeview.h>
#include <gtk/gtktextview.h>
#include <gtk/gtkliststore.h>
#include <gtk/gtktextmark.h>
#include <gtk/gtkentry.h>
#include <gtk/gtkbutton.h>
#include <gtk/gtkvbox.h>
#include <gtk/gtkhbox.h>
#include <gtk/gtkscrolledwindow.h>
#include <string.h>

static void      hippo_chat_window_init                (HippoChatWindow       *window);
static void      hippo_chat_window_class_init          (HippoChatWindowClass  *klass);

static void      hippo_chat_window_finalize            (GObject               *object);
static void      hippo_chat_window_destroy             (GtkObject             *object);

static void      hippo_chat_window_connect_user        (HippoChatWindow *window,
                                                        HippoPerson     *person);
static void      hippo_chat_window_disconnect_user     (HippoChatWindow *window,
                                                        HippoPerson     *person);
static void      hippo_chat_window_disconnect_all_users(HippoChatWindow *window);

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

enum {
    MEMBER_COLUMN_PERSON,
    MEMBER_COLUMN_PHOTO,    
    MEMBER_NUM_COLUMNS
};

struct _HippoChatWindow {
    GtkWindow parent;
    HippoDataCache *cache;
    HippoChatRoom *room;
    GtkWindowGroup *window_group;
    GtkWidget *title_label;
    GtkWidget *member_view;
    GtkTreeModel *member_model;
    GtkWidget *chat_log_view;
    GtkWidget *send_entry;
    GtkWidget *send_button;
    GtkWidget *error_dialog;
    
    GHashTable *message_tags;
    
    GHashTable *connected_users;
};

struct _HippoChatWindowClass {
    GtkWindowClass parent_class;

};

G_DEFINE_TYPE(HippoChatWindow, hippo_chat_window, GTK_TYPE_WINDOW);

static void
hippo_chat_window_init(HippoChatWindow  *window)
{
    window->member_model = GTK_TREE_MODEL(gtk_list_store_new(MEMBER_NUM_COLUMNS, HIPPO_TYPE_PERSON, GDK_TYPE_PIXBUF));
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
#define MEMBER_VIEW_WIDTH 140

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
    GtkCellRenderer *renderer;
    GtkTreeViewColumn *column;
        
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
    
    window->member_view = gtk_tree_view_new_with_model(window->member_model);
    gtk_widget_set_size_request(window->member_view, MEMBER_VIEW_WIDTH, -1);
    gtk_tree_view_set_headers_visible(GTK_TREE_VIEW(window->member_view), FALSE);
    
    sw = gtk_scrolled_window_new(NULL, NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(sw),
                                   GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
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
    gtk_text_view_set_wrap_mode(GTK_TEXT_VIEW(window->chat_log_view), GTK_WRAP_WORD);

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

    /* Set up the tree view */
    renderer = hippo_person_renderer_new();
    g_object_set(G_OBJECT(renderer), "xalign", 0.0, "xpad", 2, "ypad", 2, NULL);
    
    gtk_tree_view_insert_column_with_attributes(GTK_TREE_VIEW(window->member_view),
                                                -1, _("People"),
                                                renderer, 
                                                "person", MEMBER_COLUMN_PERSON,
                                                "photo", MEMBER_COLUMN_PHOTO,
                                                NULL);
    column = gtk_tree_view_get_column(GTK_TREE_VIEW(window->member_view), MEMBER_COLUMN_PERSON);
    gtk_tree_view_column_set_fixed_width(column, MEMBER_VIEW_WIDTH);
    gtk_tree_view_column_set_clickable(column, FALSE);

    /* Now connect to chat room */
    window->room = room;
    g_object_ref(window->room);

    on_title_changed(window->room, window);
    
    {
        GSList *members = hippo_chat_room_get_users(window->room);
        while (members != NULL) {
            HippoChatState state;
            HippoPerson *person = HIPPO_PERSON(members->data);
            members = g_slist_remove(members, members->data);
            
            /* FIXME this is O(n^2) because we scan the list model
             * to see if the person is already in there every time -
             * part of the fix might be to do this on_loaded not 
             * on construct.
             */
            state = hippo_chat_room_get_user_state(window->room, person);
            if (state == HIPPO_CHAT_STATE_PARTICIPANT) {
                on_user_state_changed(window->room, person, window);
            }
            
            g_object_unref(person);   
        }
    }

    {
        GSList *messages;
        
        messages = hippo_chat_room_get_messages(window->room);
        while (messages != NULL) {
            on_message_added(window->room, messages->data, window);
            messages = messages->next;
        }        
    }

    g_signal_connect(window->room, "title-changed", G_CALLBACK(on_title_changed), window);
    g_signal_connect(window->room, "user-state-changed", G_CALLBACK(on_user_state_changed), window);
    g_signal_connect(window->room, "message-added", G_CALLBACK(on_message_added), window);
    g_signal_connect(window->room, "cleared", G_CALLBACK(on_cleared), window);
    g_signal_connect(window->room, "loaded", G_CALLBACK(on_loaded), window);    

    /* Set up widget signals */

    on_send_entry_changed(window->send_entry, window);
    
    g_signal_connect(window->send_entry, "changed", G_CALLBACK(on_send_entry_changed), window);
    g_signal_connect(window->send_entry, "activate", G_CALLBACK(on_send_message), window);    
    g_signal_connect(window->send_button, "clicked", G_CALLBACK(on_send_message), window);

    /* Finally, join the room */    
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
 
    hippo_chat_window_disconnect_all_users(window);
    
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

    hippo_chat_window_disconnect_all_users(window);

    g_object_unref(window->member_model);

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
hippo_chat_window_connect_user(HippoChatWindow *window,
                               HippoPerson     *person)
{
    if (window->connected_users == NULL) {
        window->connected_users = g_hash_table_new(g_direct_hash, g_direct_equal);    
    }
    
    if (g_hash_table_lookup(window->connected_users, person) != NULL) {
        return;
    }    

    g_debug("Chat window connecting to user '%s'", hippo_entity_get_guid(HIPPO_ENTITY(person)));

    /* a weak ref would be better, but harder */
    g_object_ref(person);
    on_user_changed(person, window);    
    g_signal_connect(G_OBJECT(person), "changed", G_CALLBACK(on_user_changed), window);
    g_hash_table_insert(window->connected_users, person, person);
}

static void
hippo_chat_window_disconnect_user(HippoChatWindow *window,
                                  HippoPerson     *person)
{
    if (window->connected_users == NULL)
        return;

    if (g_hash_table_lookup(window->connected_users, person) == NULL) {
        return;
    }
    
    g_debug("Chat window disconnecting from user '%s'", hippo_entity_get_guid(HIPPO_ENTITY(person)));
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(person), G_CALLBACK(on_user_changed), window);
    g_hash_table_remove(window->connected_users, person);
    g_object_unref(person);
}                                  

static void
disconnect_user_foreach(void *key, void *value, void *data)
{
    HippoPerson *person = HIPPO_PERSON(value);
    HippoChatWindow *window = HIPPO_CHAT_WINDOW(data);
   
    /* can't just call disconnect_user since it modifies the hash */
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(person), G_CALLBACK(on_user_changed), window);
    g_object_unref(person);
}

static void
hippo_chat_window_disconnect_all_users(HippoChatWindow *window)
{
    if (window->connected_users) {
        g_hash_table_foreach(window->connected_users, disconnect_user_foreach, window);
        g_hash_table_destroy(window->connected_users);
        window->connected_users = NULL;   
    }
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

typedef enum {
    PICTURE_ON_LEFT,
    PICTURE_ON_RIGHT
} PictureSpot;

typedef struct {
    HippoChatWindow *window;
    GtkTextBuffer *buffer;
    PictureSpot where;
    int serial;
} PictureData;

static void
on_picture_loaded_for_log(GdkPixbuf *pixbuf,
                          void      *data)
{
    GtkTextBuffer *buffer;
    PictureData *pd = data;
       
    if (pd->buffer == NULL) {
        buffer = NULL;
    } else {
        buffer = pd->buffer;
        REMOVE_WEAK(&pd->buffer);
    }
    
    /* pixbuf is NULL if we failed to load an image,
     * and buffer is NULL if we destroyed the chat window
     */
    if (pixbuf && buffer) {
        GtkTextIter start, end;
    
        if (!find_message_bounds(pd->window, pd->serial, &start, &end)) {
            g_warning("failed to find message bounds!");
            return;
        }

        if (pd->where == PICTURE_ON_RIGHT) {
            gtk_text_iter_forward_to_line_end(&start);
            gtk_text_buffer_insert(buffer, &start, " ", 1);
        }
        gtk_text_buffer_insert_pixbuf(buffer, &start, pixbuf);
        if (pd->where == PICTURE_ON_LEFT) {
            gtk_text_buffer_insert(buffer, &start, " ", 1);
        }
    }

    g_free(pd);
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
    HippoPerson *sender;
    HippoPerson *self;
    GtkTextTag *tag;
    PictureData *pd;
    
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

    pd = g_new0(PictureData, 1);
    pd->window = window;
    pd->buffer = buffer;
    pd->serial = serial;
    ADD_WEAK(&pd->buffer);

    tag = tag_for_serial(window, serial);
    if (self && self == sender) {
        g_object_set(G_OBJECT(tag), "justification", GTK_JUSTIFY_LEFT, NULL);
        pd->where = PICTURE_ON_LEFT;
    } else {
        g_object_set(G_OBJECT(tag), "justification", GTK_JUSTIFY_RIGHT, NULL);
        pd->where = PICTURE_ON_RIGHT;
    }

    gtk_text_buffer_insert_with_tags(buffer, &iter, text, len, tag, NULL);
    
    /* If the inserted text did not end in a newline, insert one. */
    if (!gtk_text_iter_starts_line(&iter)) {
        gtk_text_buffer_insert_with_tags(buffer, &iter, "\n", 1, tag, NULL);
    }

    /* if we inserted at the end, scroll to it */
    if (gtk_text_iter_is_end(&iter)) {
        /* insert is invisible anyway so just use it */
        gtk_text_buffer_move_mark(buffer, gtk_text_buffer_get_insert(buffer),
                                  &iter);
        gtk_text_view_scroll_mark_onscreen(GTK_TEXT_VIEW(window->chat_log_view),
                                           gtk_text_buffer_get_insert(buffer));
    }

    /* insert photo last to avoid invalidating iters */

    hippo_app_load_photo(hippo_get_app(), HIPPO_ENTITY(sender),
                         on_picture_loaded_for_log, pd);
    /* if the load photo callback was invoked synchronously, then pd is already freed here... 
     * also our buffer iterators are invalidated
     */
    
    /* watch this user (no-op if already watching) */
    hippo_chat_window_connect_user(window, sender);
}

static gboolean
find_existing_tree_row(HippoChatWindow *window,
                       HippoPerson     *person,
                       GtkTreeIter     *iter)
{
    if (!gtk_tree_model_get_iter_first(window->member_model, iter))
        return FALSE;

    do {
        HippoPerson *value = NULL;
        gtk_tree_model_get(window->member_model, iter,
                           MEMBER_COLUMN_PERSON, &value, -1);
        g_object_unref(value);
        if (value == person)
            return TRUE;
    } while (gtk_tree_model_iter_next(window->member_model, iter));
    
    return FALSE;
}

static void
on_user_state_changed(HippoChatRoom         *room,
                      HippoPerson           *person,
                      HippoChatWindow       *window)
{
    HippoChatState state;
    GtkTreeIter iter;
    gboolean exists;
    
    state = hippo_chat_room_get_user_state(room, person);

    exists = find_existing_tree_row(window, person, &iter);
    
    g_debug("User %s room %s new state %d in tree view %d",
            hippo_entity_get_guid(HIPPO_ENTITY(person)), hippo_chat_room_get_id(room),
            state, exists);
    
    if (state == HIPPO_CHAT_STATE_PARTICIPANT) {
        /* be sure they are in the members list */
        if (!exists) {
            gtk_list_store_append(GTK_LIST_STORE(window->member_model), &iter);
            gtk_list_store_set(GTK_LIST_STORE(window->member_model), &iter,
                               MEMBER_COLUMN_PERSON, person, -1);
            /* watch this user (no-op if already watching) */
            hippo_chat_window_connect_user(window, person);
        }
        /* O(n) assertions! woo! */
        g_assert(find_existing_tree_row(window, person, &iter));
    } else {
        /* be sure they are not in the members list */
        if (exists) {
            gtk_list_store_remove(GTK_LIST_STORE(window->member_model), &iter);
        }
        /* O(n) assertions! woo! */
        g_assert(!find_existing_tree_row(window, person, &iter));
    }
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
    GtkTextBuffer *new_buffer;

    hippo_chat_window_disconnect_all_users(window);
    
    gtk_list_store_clear(GTK_LIST_STORE(window->member_model));
    
    if (window->message_tags != NULL) {
        g_hash_table_destroy(window->message_tags);
        window->message_tags = NULL;
    }
    
    /* just replace the whole buffer, to get an empty one without 
     * any tags
     */
    new_buffer = gtk_text_buffer_new(NULL);
    gtk_text_view_set_buffer(GTK_TEXT_VIEW(window->chat_log_view), new_buffer);
    g_object_unref(new_buffer);
}

typedef struct
{
    HippoChatWindow *window;
    HippoPerson *person;
} TreePictureData;

static void
on_picture_loaded_for_tree(GdkPixbuf *pixbuf,
                           void      *data)
{
    TreePictureData *tpd = data;
    
    /* note pixbuf can be NULL */

    if (tpd->window && tpd->person) {
        GtkTreeIter iter;
        if (find_existing_tree_row(tpd->window, tpd->person, &iter)) {
            gtk_list_store_set(GTK_LIST_STORE(tpd->window->member_model), &iter,
                               MEMBER_COLUMN_PHOTO, pixbuf, -1);
        }
    }

    REMOVE_WEAK(&tpd->window);
    REMOVE_WEAK(&tpd->person);

    g_free(tpd);
}

static void
on_user_changed(HippoPerson           *person,
                HippoChatWindow       *window)
{
    GtkTreeIter iter;
    TreePictureData *tpd;

    g_debug("User '%s' change handler for chat window", hippo_entity_get_guid(HIPPO_ENTITY(person)));

    if (find_existing_tree_row(window, person, &iter)) {
        GtkTreePath *path;
                
        path = gtk_tree_model_get_path(window->member_model, &iter);
        gtk_tree_model_row_changed(window->member_model, path, &iter);
        gtk_tree_path_free(path);
    }
    
    tpd = g_new0(TreePictureData, 1);
    tpd->person = person;
    tpd->window = window;
    ADD_WEAK(&tpd->person);
    ADD_WEAK(&tpd->window);

    hippo_app_load_photo(hippo_get_app(), HIPPO_ENTITY(person),
                         on_picture_loaded_for_tree, tpd);
    
    /* FIXME also update photos in the chat log */
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
