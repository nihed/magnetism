/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-post.h"
#include <string.h>

/* === HippoPost implementation === */

static void     hippo_post_finalize             (GObject *object);

struct _HippoPost {
    GObject parent;
    char *guid;
    char *sender;
    char *url;
    char *title;
    char *description;
    GSList *recipients;
    char *info;
    GTime date;
    gboolean to_world;
    int timeout;
    /* have we clicked on this post ever */
    guint have_viewed : 1;
    /* did we just now get this as a newPost message 
     * (gets unset when we bubble it up)
     */
    guint is_new : 1;
    guint is_ignored : 1;
    HippoChatRoom *chat_room;

    /* Once we load the chat room, it overrides viewing_user_count and chatting_user_count
     * which are the CURRENT counts of people actively present. The total_viewers and 
     * viewers fields are people who have EVER viewed the post, so the chat room 
     * does not override those.
     */
    int viewing_user_count;
    int chatting_user_count;
    int total_viewers;
    GSList *viewers;
};

struct _HippoPostClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoPost, hippo_post, G_TYPE_OBJECT);

enum {
    CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_post_init(HippoPost *post)
{
}

static void
hippo_post_class_init(HippoPostClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    signals[CHANGED] =
        g_signal_new ("changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0);
          
    object_class->finalize = hippo_post_finalize;
}

static void
hippo_post_finalize(GObject *object)
{
    HippoPost *post = HIPPO_POST(object);

    if (post->chat_room)
        g_object_unref(post->chat_room);

    g_free(post->guid);
    g_free(post->sender);
    g_free(post->url);
    g_free(post->title);
    g_free(post->description);
    g_slist_foreach(post->recipients, (GFunc) g_object_unref, NULL);
    g_slist_free(post->recipients);    
    g_slist_foreach(post->viewers, (GFunc) g_object_unref, NULL);
    g_slist_free(post->viewers);
    g_free(post->info);

    G_OBJECT_CLASS(hippo_post_parent_class)->finalize(object); 
}

static void 
hippo_post_emit_changed(HippoPost *post)
{
    g_signal_emit(post, signals[CHANGED], 0);
}

/* === HippoPost exported API === */

HippoPost*
hippo_post_new(const char *guid)
{
    HippoPost *post = g_object_new(HIPPO_TYPE_POST, NULL);
    
    post->guid = g_strdup(guid);
    
    return post;
}

const char*
hippo_post_get_guid(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->guid;
}

const char*
hippo_post_get_sender(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->sender;
}

const char*
hippo_post_get_url(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->url;
}

const char*
hippo_post_get_title(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->title;
}

const char*
hippo_post_get_description(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->description;
}

GSList*
hippo_post_get_recipients(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->recipients;
}

GSList*
hippo_post_get_viewers(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->viewers;
}

const char*
hippo_post_get_info(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    return post->info;
}

GTime
hippo_post_get_date(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), 0);
    return post->date;
}

gboolean
hippo_post_is_to_world(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), 0);
    return post->to_world;
}

int
hippo_post_get_timeout(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), 0);
    return post->timeout;
}

int
hippo_post_get_viewing_user_count(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), 0);

    /* When we first get a Post, the data in the post is more accurate -
     * waiting until we are filled to use the chatroom count avoids
     * the user seeing a count-up as the chatroom fills
     */
    if (post->chat_room && !hippo_chat_room_get_loading(post->chat_room))
        return hippo_chat_room_get_viewing_user_count(post->chat_room);
    else
        return post->viewing_user_count;
}

int
hippo_post_get_chatting_user_count(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), 0);

    if (post->chat_room && !hippo_chat_room_get_loading(post->chat_room))
        return hippo_chat_room_get_chatting_user_count(post->chat_room);
    else
        return post->chatting_user_count;
}

int
hippo_post_get_total_viewers(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), 0);
    return post->total_viewers;
}

gboolean
hippo_post_get_have_viewed(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), FALSE);
    return post->have_viewed;
}

gboolean
hippo_post_get_ignored(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), FALSE);
	return post->is_ignored;
}

gboolean
hippo_post_get_new(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), FALSE);
    return post->is_new;
}

static void
set_str(HippoPost *post, char **s_p, const char *val)
{
    if (*s_p == val) /* catches both null, or self-assignment */
        return;
    if (*s_p && val && strcmp(*s_p, val) == 0)
        return;
        
    g_free(*s_p);
    *s_p = g_strdup(val);
    
    hippo_post_emit_changed(post);
}
                   
void
hippo_post_set_sender(HippoPost  *post,
                      const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->sender, value);
}

void
hippo_post_set_url(HippoPost  *post,
                   const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->url, value);
}

void
hippo_post_set_title(HippoPost  *post,
                   const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->title, value);
    
    if (post->chat_room)
        hippo_chat_room_set_title(post->chat_room, post->title);
}

void
hippo_post_set_description(HippoPost  *post,
                           const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->description, value);                           
}

#if 0
static void
warn_if_entity_list_has_dups(GSList *value)
{
    GHashTable *table;
    
    table = g_hash_table_new(g_str_hash, g_str_equal);
    
    while (value != NULL) {
        HippoEntity *new = HIPPO_ENTITY(value->data);
        HippoEntity *old = g_hash_table_lookup(table, hippo_entity_get_guid(new));
        if (old != NULL) {
            g_warning("Entity list has dup new '%s' %p old '%s' %p", 
                       hippo_entity_get_guid(new), new,
                       hippo_entity_get_guid(old), old);
        }
        g_hash_table_replace(table, (char*) hippo_entity_get_guid(new), new);
    
        value = value->next;
    }
    g_hash_table_destroy(table);
}
#endif

static void
set_entity_list(HippoPost *post, GSList **list_p, GSList *value)
{
    GSList *copy;
    
    /* warn_if_entity_list_has_dups(value); */
    
    copy = g_slist_copy(value);
    g_slist_foreach(copy, (GFunc) g_object_ref, NULL);
    g_slist_foreach(*list_p, (GFunc) g_object_unref, NULL);
    g_slist_free(*list_p);
    *list_p = copy;
    hippo_post_emit_changed(post);
}

void
hippo_post_set_recipients(HippoPost  *post,
                          GSList     *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_entity_list(post, &post->recipients, value);
}

void
hippo_post_set_viewers(HippoPost  *post,
                       GSList     *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_entity_list(post, &post->viewers, value);
}

void
hippo_post_set_info(HippoPost  *post,
                    const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->info, value);
}

void
hippo_post_set_date(HippoPost  *post,
                    GTime       value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    if (post->date != value) {
        post->date = value;
        hippo_post_emit_changed(post);
    }
}

static void
set_int(HippoPost *post, int *ip, int value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    if (*ip != value) {
        *ip = value;
        hippo_post_emit_changed(post);
    }
}

static void
set_bool(HippoPost *post, gboolean *ip, gboolean value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
	value = !!value;
    if (*ip != value) {
        *ip = value;
        hippo_post_emit_changed(post);
    }
}
                    
void
hippo_post_set_timeout(HippoPost  *post,
                       int         value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_int(post, &post->timeout, value);
}

void
hippo_post_set_to_world(HippoPost  *post,
                        gboolean    value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_bool(post, &post->to_world, value);
}
                       
void
hippo_post_set_viewing_user_count(HippoPost  *post,
                                  int         value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_int(post, &post->viewing_user_count, value);
}                                  

void
hippo_post_set_chatting_user_count(HippoPost  *post,
                                   int         value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_int(post, &post->chatting_user_count, value);
}                                   

void
hippo_post_set_total_viewers(HippoPost  *post,
                             int         value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_int(post, &post->total_viewers, value);
}                             

void
hippo_post_set_have_viewed(HippoPost  *post,
                           gboolean    value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    value = value != FALSE;
    if (post->have_viewed != value) {
        post->have_viewed = value;
        hippo_post_emit_changed(post);
    }
}

void
hippo_post_set_ignored (HippoPost  *post,
                        gboolean    value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    value = value != FALSE;
    if (post->is_ignored != value) {
        post->is_ignored = value;
        hippo_post_emit_changed(post);
    }
}

void
hippo_post_set_new(HippoPost  *post,
                   gboolean    value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    value = value != FALSE;
    if (post->is_new != value) {
        post->is_new = value;
        hippo_post_emit_changed(post);
    }
}

HippoChatRoom*
hippo_post_get_chat_room(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    
    return post->chat_room;
}

void
hippo_post_set_chat_room(HippoPost     *post,
                         HippoChatRoom *room)
{
    g_return_if_fail(HIPPO_IS_POST(post));

    if (room == post->chat_room)
        return;
            
    if (room)
        g_object_ref(room);
    if (post->chat_room)
        g_object_unref(post->chat_room);
    post->chat_room = room;

    if (post->chat_room)
        hippo_chat_room_set_title(post->chat_room, post->title);
    
    hippo_post_emit_changed(post);
}
