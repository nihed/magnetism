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
            }
            
            recipients = recipients->next;
            ++i;
        }
        
        hippo_bubble_set_recipients(bubble, infos, i);
    }
}

static void
post_watch_finalize(void *value)
{
    PostWatch *watch = POST_WATCH(value);
    if (watch->post)
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->post), G_CALLBACK(on_post_changed), watch);
    REMOVE_WEAK(&watch->post);
    REMOVE_WEAK(&watch->cache);
}

static void
post_watch_add(HippoBubble    *bubble,
               HippoPost      *post,
               HippoDataCache *cache)
{
    PostWatch *watch = g_new0(PostWatch, 1);
    
    bubble_watch_init(&watch->base, bubble,
                post_watch_finalize, WATCH_KIND_POST);
    
    watch->post = post;
    watch->cache = cache;
    ADD_WEAK(&watch->post);
    ADD_WEAK(&watch->cache);
    
    g_signal_connect(post, "changed", G_CALLBACK(on_post_changed), watch);
    
    bubble_watch_set(bubble, BUBBLE_WATCH(watch));
    
    on_post_changed(post, watch);
    
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
