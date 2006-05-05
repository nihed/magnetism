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
    int timeout;
    guint have_viewed : 1;  
    HippoChatRoom *chat_room;

    /* FIXME all this info is also stored in the chat room,
     * here it's from post-related XMPP messages and in the
     * chat room from the chat-related messages. We should 
     * always use the chat room info when we have it, probably.
     * But this could be here for posts where we aren't chatting,
     * allowing us to avoid loading the whole chat room.
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
    return post->viewing_user_count;
}

int
hippo_post_get_chatting_user_count(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), 0);
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
}

void
hippo_post_set_description(HippoPost  *post,
                           const char *value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_str(post, &post->description, value);                           
}

static void
set_entity_list(HippoPost *post, GSList **list_p, GSList *value)
{
    GSList *copy;
    
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
                    
void
hippo_post_set_timeout(HippoPost  *post,
                       int         value)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    set_int(post, &post->timeout, value);
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

HippoChatRoom*
hippo_post_get_chat_room(HippoPost *post)
{
    g_return_val_if_fail(HIPPO_IS_POST(post), NULL);
    
    return post->chat_room;
}

/* Don't emit the changed signal for this for now, it 
 * wouldn't be interesting
 */
void
hippo_post_set_chat_room(HippoPost     *post,
                         HippoChatRoom *room)
{
    g_return_if_fail(HIPPO_IS_POST(post));
    
    if (room)
        g_object_ref(room);
    if (post->chat_room)
        g_object_unref(post->chat_room);
    post->chat_room = room;
}                         
