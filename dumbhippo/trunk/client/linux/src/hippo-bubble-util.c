#include "hippo-bubble-util.h"

/* We also bubble e.g. myspace comments, so this is supposed to 
 * handle that cleanly or something ... 
 */ 
enum {
    WATCH_KIND_POST,
    WATCH_KIND_MYSPACE
};
 
typedef struct {
    int          refcount;
    int          kind;
    HippoBubble *bubble;
    GFreeFunc    finalize;
} BubbleWatch;

typedef struct {
    BubbleWatch     base;
    HippoPost      *post;
    HippoChatRoom  *room;
    HippoDataCache *cache;
} PostWatch;

#define BUBBLE_WATCH(w) ((BubbleWatch*) w)
#define POST_WATCH(w)   ((PostWatch*) w)

static void
bubble_watch_init(BubbleWatch *watch,
                  HippoBubble *bubble,
                  GFreeFunc    finalize,
                  int          kind)
{
    watch->refcount = 1;
    watch->bubble = bubble;
    watch->finalize = finalize;
    watch->kind = kind;

    ADD_WEAK(&watch->bubble);    
}

#define bubble_watch_ref(watch) do { (watch)->refcount += 1; } while(0)

static void
bubble_watch_unref(BubbleWatch *watch)
{
    g_return_if_fail(watch->refcount > 0);
    
    watch->refcount -= 1;
    if (watch->refcount == 0) {
        REMOVE_WEAK(&watch->bubble);

        (* watch->finalize) (watch);
        g_free(watch);
    }
}

static void
bubble_watch_set(HippoBubble *bubble,
                 BubbleWatch *watch)
{
    g_assert(watch->bubble == bubble);
    bubble_watch_ref(watch);
    g_object_set_data_full(G_OBJECT(bubble), "hippo-bubble-watch", watch,
                           (GFreeFunc) bubble_watch_unref);
}

static BubbleWatch*
bubble_watch_get(HippoBubble* bubble)
{
    return g_object_get_data(G_OBJECT(bubble), "hippo-bubble-watch");
}

static gboolean
bubble_watch_is_attached(BubbleWatch *watch)
{
    BubbleWatch *current;
    
    if (watch->bubble == NULL)
        return FALSE;
    current = bubble_watch_get(watch->bubble);
    return watch == current;
}

static void
on_chatter_photo_loaded(GdkPixbuf *pixbuf,
                        void      *data)
{
    PostWatch *watch;
    
    /* pixbuf NULL if we failed to load */
    
    watch = data;
    
    if (bubble_watch_is_attached(BUBBLE_WATCH(watch))) {
        hippo_bubble_set_last_chat_photo(watch->base.bubble, pixbuf);
    }
    
    bubble_watch_unref(BUBBLE_WATCH(watch));
}

static void
update_last_message(PostWatch        *watch)
{
    HippoChatMessage *last;
    
    /* be sure we're still the current post */
    if (!bubble_watch_is_attached(BUBBLE_WATCH(watch)))
        return;
    
    last = hippo_chat_room_get_last_message(watch->room);
    
    if (last == NULL) {
        hippo_bubble_set_last_chat_photo(watch->base.bubble, NULL);
        hippo_bubble_set_last_chat_message(watch->base.bubble, NULL);
    } else {
        hippo_bubble_set_last_chat_message(watch->base.bubble,
                                        hippo_chat_message_get_text(last));

        /* FIXME ordering of this isn't guaranteed, i.e. we could get a reply for 
         * a previous chatter after the reply for this chatter
         */
    
        bubble_watch_ref(BUBBLE_WATCH(watch));
        hippo_app_load_photo(hippo_get_app(), HIPPO_ENTITY(hippo_chat_message_get_person(last)),
                             on_chatter_photo_loaded, watch);
    }
}

static void
on_chat_message_added(HippoChatRoom    *room,
                      HippoChatMessage *message,
                      PostWatch        *watch)
{
    /* we don't care about this specific message, just 
     * whether the last message changed... 
     */
    update_last_message(watch);
}

static void
update_viewers(PostWatch        *watch)
{
    GSList *chatters;
    GSList *link;
    GSList *ever_viewed;
    HippoChatRoom *room;
#define MAX_VIEWERS_SHOWN 10
    HippoViewerInfo infos[MAX_VIEWERS_SHOWN];
    int n_viewers;    
    HippoPerson *self;
        
    /* be sure we're still the current post */
    if (!bubble_watch_is_attached(BUBBLE_WATCH(watch)))
        return;

    room = watch->room;
    self = hippo_data_cache_get_self(watch->cache);
    /* remember self is null if not logged in */

    /* reload viewer list, preferring people there live */
    n_viewers = 0;    
    chatters = hippo_chat_room_get_users(room);
    for (link = chatters; link != NULL; link = link->next) {
        HippoPerson *user = HIPPO_PERSON(link->data);
        
        if (n_viewers == MAX_VIEWERS_SHOWN)
            break;

        if (user == self)
            continue;
    
        infos[n_viewers].name = hippo_entity_get_name(HIPPO_ENTITY(user));
        infos[n_viewers].entity_guid = hippo_entity_get_guid(HIPPO_ENTITY(user));
        infos[n_viewers].present = TRUE;
        infos[n_viewers].chatting = hippo_chat_room_get_user_state(room, user) == HIPPO_CHAT_STATE_PARTICIPANT;
    
        ++n_viewers;
    }
    
    /* show some people who have visited in the past */
    if (n_viewers < MAX_VIEWERS_SHOWN) {
        ever_viewed = hippo_post_get_viewers(watch->post);
        for (link = ever_viewed; link != NULL; link = link->next) {
            HippoPerson *user = HIPPO_PERSON(link->data);
            
            if (n_viewers == MAX_VIEWERS_SHOWN)
                break;

            if (user == self)
                continue;
    
            infos[n_viewers].name = hippo_entity_get_name(HIPPO_ENTITY(user));
            infos[n_viewers].entity_guid = hippo_entity_get_guid(HIPPO_ENTITY(user));
            infos[n_viewers].present = FALSE;
            infos[n_viewers].chatting = FALSE;
        
            ++n_viewers;
        }
    }
    
    /* "ever_viewed" isn't a copy, but "chatters" is */
    g_slist_foreach(chatters, (GFunc) g_object_unref, NULL);
    g_slist_free(chatters);
}

static void
on_chat_user_state_changed(HippoChatRoom    *room,
                           HippoPerson      *person,
                           PostWatch        *watch)
{
    update_viewers(watch);
}

static void
on_sender_photo_loaded(GdkPixbuf *pixbuf,
                       void      *data)
{
    PostWatch *watch;
    
    /* pixbuf NULL if we failed to load */
    
    watch = data;
    
    if (bubble_watch_is_attached(BUBBLE_WATCH(watch))) {
        hippo_bubble_set_sender_photo(watch->base.bubble, pixbuf);
    }
    
    bubble_watch_unref(BUBBLE_WATCH(watch));
}

static void
on_post_changed(HippoPost *post,
                PostWatch *watch)
{
    const char  *sender_id;
    HippoEntity *sender;
    HippoPerson *self;
    HippoBubble *bubble;
    
    /* FIXME this is all pretty expensive to do every time someone calls 
     * a setter on a post ... but we'll wait and see if it's an issue
     * and optimize later
     */
    
    /* be sure we're still the current post */
    if (!bubble_watch_is_attached(BUBBLE_WATCH(watch)))
        return;

    g_assert(watch->post != NULL); /* shouldn't get a signal from a dead post */

    bubble = watch->base.bubble;
    g_assert(bubble != NULL); /* is_attached checks this */
    
    sender_id = hippo_post_get_sender(watch->post);
    sender = hippo_data_cache_lookup_entity(watch->cache, sender_id);
    self = hippo_data_cache_get_self(watch->cache);
    
    if (sender == HIPPO_ENTITY(self)) {
        hippo_bubble_set_sender_name(bubble, _("You"));
    } else {
        hippo_bubble_set_sender_name(bubble, hippo_entity_get_name(sender));
    }
    hippo_bubble_set_sender_guid(bubble, sender_id);

    hippo_bubble_set_link_title(bubble, hippo_post_get_title(watch->post));
    hippo_bubble_set_post_guid(bubble, hippo_post_get_guid(watch->post));

    /* FIXME ordering of this isn't guaranteed, i.e. we could get a reply for 
     * an "old" post in theory
     */
    bubble_watch_ref(BUBBLE_WATCH(watch)); /* on_sender_photo_loaded will unref */    
    hippo_app_load_photo(hippo_get_app(), sender, on_sender_photo_loaded, watch);
    
    hippo_bubble_set_link_description(bubble, hippo_post_get_description(watch->post));

    {
#define MAX_DISPLAYED_RECIPIENTS 5    
        HippoRecipientInfo infos[5];
        GSList *recipients;
        GSList *link;
        int i;
        
        /* not a copy of the list */
        recipients = hippo_post_get_recipients(post);
        
        i = 0;
    
        /* "you" always goes first */        
        link = g_slist_find(recipients, self);
        if (link != NULL) {
            infos[i].name = _("you");
            infos[i].entity_guid = hippo_entity_get_guid(HIPPO_ENTITY(self));
            ++i;
        }
        
        while (recipients != NULL && i < MAX_DISPLAYED_RECIPIENTS) {
            HippoEntity *entity = recipients->data;
            
            if (entity != HIPPO_ENTITY(self)) {
                infos[i].name = hippo_entity_get_name(entity);
                infos[i].entity_guid = hippo_entity_get_guid(entity);
                ++i;
            }
            
            recipients = recipients->next;
        }
        
        hippo_bubble_set_recipients(bubble, infos, i);
    }
    
    update_viewers(watch);
}

static void
post_watch_finalize(void *value)
{
    PostWatch *watch = POST_WATCH(value);
    if (watch->post)
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->post),
                                             G_CALLBACK(on_post_changed), watch);
    if (watch->room) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->room),
                                             G_CALLBACK(on_chat_message_added), watch);
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->room),
                                             G_CALLBACK(on_chat_user_state_changed), watch);
    }                                             
    REMOVE_WEAK(&watch->post);
    REMOVE_WEAK(&watch->room);
    REMOVE_WEAK(&watch->cache);
}

static void
post_watch_add(HippoBubble    *bubble,
               HippoPost      *post,
               HippoDataCache *cache)
{
    PostWatch *watch;
        
    watch = g_new0(PostWatch, 1);
    
    bubble_watch_init(&watch->base, bubble,
                post_watch_finalize, WATCH_KIND_POST);
    
    watch->post = post;
    watch->cache = cache;
    watch->room = hippo_post_get_chat_room(watch->post);
    ADD_WEAK(&watch->post);
    ADD_WEAK(&watch->room);
    ADD_WEAK(&watch->cache);
    
    g_signal_connect(post, "changed", G_CALLBACK(on_post_changed), watch);
    
    g_signal_connect(watch->room, "message-added", G_CALLBACK(on_chat_message_added), watch);
    g_signal_connect(watch->room, "user-state-changed", G_CALLBACK(on_chat_user_state_changed), watch);
    
    bubble_watch_set(bubble, BUBBLE_WATCH(watch));
    
    on_post_changed(post, watch); /* includes an update_viewers() */
    update_last_message(watch);
    
    bubble_watch_unref(BUBBLE_WATCH(watch));
}

void
hippo_bubble_set_post(HippoBubble    *bubble,
                      HippoPost      *post,
                      HippoDataCache *cache)
{
    post_watch_add(bubble, post, cache);
}

HippoPost*
hippo_bubble_get_post(HippoBubble *bubble)
{
    BubbleWatch *watch;
    
    watch = bubble_watch_get(bubble);
    if (watch && watch->kind == WATCH_KIND_POST)
        return POST_WATCH(watch)->post;
    else
        return NULL;
}
