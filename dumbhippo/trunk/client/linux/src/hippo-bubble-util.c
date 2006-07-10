#include "hippo-bubble-util.h"
#include <string.h>

enum {
    WATCH_KIND_POST,
    WATCH_KIND_GROUP,
    WATCH_KIND_GROUP_MEMBERSHIP
};
 
typedef struct {
    int          refcount;
    int          kind;
    HippoBubble *bubble;
    HippoChatRoom  *room; 
    HippoDataCache *cache;   
    GFreeFunc    finalize;
} BubbleWatch;

typedef struct {
    BubbleWatch     base;
    HippoPost      *post;
} PostWatch;

typedef struct {
    BubbleWatch     base;
    HippoEntity    *group;
} GroupWatch;

typedef struct {
    BubbleWatch     base;
    HippoEntity    *group;
    HippoEntity    *user;
    char           *status;
} GroupMembershipWatch;

#define BUBBLE_WATCH(w) ((BubbleWatch*) w)
#define POST_WATCH(w)   ((PostWatch*) w)
#define GROUP_WATCH(w)   ((GroupWatch*) w)
#define GROUP_MEMBERSHIP_WATCH(w)   ((GroupMembershipWatch*) w)

static void  on_chat_message_added(HippoChatRoom    *room,
                                   HippoChatMessage *message,
                                   BubbleWatch      *watch);

static void
bubble_watch_init(BubbleWatch *watch,
                  HippoBubble *bubble,
                  HippoChatRoom *room,
                  HippoDataCache *cache,
                  GFreeFunc    finalize,
                  int          kind)
{
    watch->refcount = 1;
    watch->bubble = bubble;
    watch->finalize = finalize;
    watch->kind = kind;
    watch->room = room;
    watch->cache = cache;
    
    ADD_WEAK(&watch->bubble);    
    if (watch->room) {
	    ADD_WEAK(&watch->room);
	    g_signal_connect(watch->room, "message-added", G_CALLBACK(on_chat_message_added), watch);   	      
	}
    ADD_WEAK(&watch->cache);    
}

#define bubble_watch_ref(watch) do { (watch)->refcount += 1; } while(0)

static void
bubble_watch_unref(BubbleWatch *watch)
{
    g_return_if_fail(watch->refcount > 0);
    
    watch->refcount -= 1;
    if (watch->refcount == 0) {
        REMOVE_WEAK(&watch->bubble);
	
	    if (watch->room) {
    	    g_signal_handlers_disconnect_by_func(G_OBJECT(watch->room),
                                             G_CALLBACK(on_chat_message_added), watch);
	    	REMOVE_WEAK(&watch->room);                                             
	    }
	    REMOVE_WEAK(&watch->cache);         

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
        hippo_bubble_set_swarm_photo(watch->base.bubble, pixbuf);
    }
    
    bubble_watch_unref(BUBBLE_WATCH(watch));
}

static void
update_last_message(BubbleWatch        *watch)
{
    HippoChatMessage *last;
    
    /* be sure we're still the current post */
    if (!bubble_watch_is_attached(BUBBLE_WATCH(watch)))
        return;
    
    last = hippo_chat_room_get_last_message(watch->room);
    
    if (last == NULL) {
        hippo_bubble_set_swarm_photo(watch->bubble, NULL);
        hippo_bubble_set_last_chat_message(watch->bubble, NULL, NULL);
    } else {
        const char *sender_id;
        
        sender_id = hippo_entity_get_guid(HIPPO_ENTITY(hippo_chat_message_get_person(last)));

        hippo_bubble_set_last_chat_message(watch->bubble,
                                        hippo_chat_message_get_text(last),
                                        sender_id);
                                        

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
                      BubbleWatch        *watch)
{
    /* we don't care about this specific message, just 
     * whether the last message changed... 
     */
    update_last_message(watch);
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
update_viewers(BubbleWatch *watch, GSList *ever_viewed)
{
    GSList *chatters;
    GSList *link;
    HippoChatRoom *room;
#define MAX_VIEWERS_SHOWN 10
    HippoViewerInfo infos[MAX_VIEWERS_SHOWN];
    int n_viewers;    
    HippoPerson *self;
        
    /* be sure we're still the current post */
    if (!bubble_watch_is_attached(watch))
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

        if (user != self) {
            infos[n_viewers].name = hippo_entity_get_name(HIPPO_ENTITY(user));
            infos[n_viewers].entity_guid = hippo_entity_get_guid(HIPPO_ENTITY(user));
            infos[n_viewers].present = TRUE;
            infos[n_viewers].chatting =
                hippo_chat_room_get_user_state(room, user) == HIPPO_CHAT_STATE_PARTICIPANT;
            ++n_viewers;
        }
    }
   
    /* show some people who have visited in the past */
    if (n_viewers < MAX_VIEWERS_SHOWN && ever_viewed) { 
        for (link = ever_viewed; link != NULL; link = link->next) {
            HippoPerson *user = HIPPO_PERSON(link->data);
            
            if (n_viewers == MAX_VIEWERS_SHOWN)
                break;

            if (user != self && g_slist_find(chatters, user) == NULL) {
                infos[n_viewers].name = hippo_entity_get_name(HIPPO_ENTITY(user));
                infos[n_viewers].entity_guid = hippo_entity_get_guid(HIPPO_ENTITY(user));
                infos[n_viewers].present = FALSE;
                infos[n_viewers].chatting = FALSE;
            
                ++n_viewers;
            }
        }
    }
    
    /* "ever_viewed" isn't a copy, but "chatters" is */
    g_slist_foreach(chatters, (GFunc) g_object_unref, NULL);
    g_slist_free(chatters);
    
    hippo_bubble_set_viewers(watch->bubble, infos, n_viewers);
}

static void
on_post_chat_user_state_changed(HippoChatRoom    *room,
                                HippoPerson      *person,
                                PostWatch        *watch)
{
    GSList *ever_viewed = hippo_post_get_viewers(watch->post);    
    update_viewers(BUBBLE_WATCH(watch), ever_viewed);
}

static void
on_group_chat_user_state_changed(HippoChatRoom    *room,
                                 HippoPerson      *person,
                                 GroupWatch        *watch)
{    
    update_viewers(BUBBLE_WATCH(watch), NULL);
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
    sender = hippo_data_cache_lookup_entity(watch->base.cache, sender_id);
    self = hippo_data_cache_get_self(watch->base.cache);
    
	hippo_bubble_set_foreground_color(bubble, HIPPO_BUBBLE_COLOR_ORANGE);
	hippo_bubble_set_header_image(bubble, "bublinkswarm");
	
	hippo_bubble_set_ignore_text(bubble, "Ignore", "");
	hippo_bubble_set_actions(bubble, HIPPO_BUBBLE_ACTION_JOIN_CHAT | HIPPO_BUBBLE_ACTION_IGNORE);	
    
    if (sender == HIPPO_ENTITY(self)) {
        hippo_bubble_set_sender_name(bubble, _("You"));
    } else {
        hippo_bubble_set_sender_name(bubble, hippo_entity_get_name(sender));
    }
    hippo_bubble_set_sender_guid(bubble, sender_id);

    hippo_bubble_set_link_title(bubble, hippo_post_get_title(watch->post));
    hippo_bubble_set_post_guid(bubble, hippo_post_get_guid(watch->post));

    hippo_bubble_set_chat_count(bubble, hippo_post_get_chatting_user_count(post));

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

        /* "the world" goes second if there */
        if (hippo_post_is_to_world(post)) {
            infos[i].name = _("The World");
            infos[i].entity_guid = NULL;
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
    
    update_viewers(BUBBLE_WATCH(watch), hippo_post_get_viewers(watch->post));
}

static void
on_group_changed(HippoEntity *entity,
                 GroupWatch *watch)
{
    HippoBubble *bubble;
    
    /* FIXME this is all pretty expensive to do every time someone calls 
     * a setter on a group ... but we'll wait and see if it's an issue
     * and optimize later
     */
    
    /* be sure we're still the current group */
    if (!bubble_watch_is_attached(BUBBLE_WATCH(watch)))
        return;

    g_assert(watch->group != NULL); /* shouldn't get a signal from a dead body */

    bubble = watch->base.bubble;
    g_assert(bubble != NULL); /* is_attached checks this */
    
	hippo_bubble_set_foreground_color(bubble, HIPPO_BUBBLE_COLOR_PURPLE);
	hippo_bubble_set_header_image(bubble, "bubgroupupdate");

	hippo_bubble_set_ignore_text(bubble, "Hush Chat", " (2hrs)");	
	hippo_bubble_set_actions(bubble, HIPPO_BUBBLE_ACTION_JOIN_CHAT | HIPPO_BUBBLE_ACTION_IGNORE);
    
    hippo_bubble_set_sender_name(bubble, "");
    hippo_bubble_set_sender_guid(bubble, hippo_entity_get_guid(watch->group));

    hippo_bubble_set_link_title(bubble, hippo_entity_get_name(watch->group));
    hippo_bubble_set_group_guid(bubble, hippo_entity_get_guid(watch->group));

    hippo_bubble_set_chat_count(bubble, hippo_entity_get_chatting_user_count(watch->group));

    /* FIXME ordering of this isn't guaranteed, i.e. we could get a reply for 
     * an "old" post in theory
     */
    bubble_watch_ref(BUBBLE_WATCH(watch)); /* on_sender_photo_loaded will unref */    
    hippo_app_load_photo(hippo_get_app(), watch->group, on_sender_photo_loaded, watch);
    
    hippo_bubble_set_link_description(bubble, _("New chat activity."));
    
    update_viewers(BUBBLE_WATCH(watch), NULL);    
}

static void
on_group_membership_changed(HippoEntity *entity,
                            GroupMembershipWatch *watch)
{
    HippoBubble *bubble;
    char *description;
    char *header;
    const char *member_id;
    int actions = HIPPO_BUBBLE_ACTION_IGNORE;
    
    /* be sure we're still the current group */
    if (!bubble_watch_is_attached(BUBBLE_WATCH(watch)))
        return;

    g_assert(watch->group != NULL);
    
    bubble = watch->base.bubble;
    g_assert(bubble != NULL); /* is_attached checks this */
    
	hippo_bubble_set_foreground_color(bubble, HIPPO_BUBBLE_COLOR_PURPLE);
	hippo_bubble_set_header_image(bubble, "bubgroupupdate");
    
    hippo_bubble_set_sender_name(bubble, "");
    hippo_bubble_set_sender_guid(bubble, hippo_entity_get_guid(watch->group));

    hippo_bubble_set_link_title(bubble, hippo_entity_get_name(watch->group));
    hippo_bubble_set_group_guid(bubble, hippo_entity_get_guid(watch->group));
    hippo_bubble_set_assoc_guid(bubble, hippo_entity_get_guid(watch->user));        

    hippo_bubble_set_chat_count(bubble, hippo_entity_get_chatting_user_count(watch->group));

	if (strcmp(watch->status, "ACTIVE") == 0) {
		description = _("Someone has joined this group as a member.");
		header = _("New member");
 	} else if (strcmp(watch->status, "FOLLOWER") == 0) {
		description = _("Someone has joined this group as a follower.");
		header = _("New follower");
		actions |= HIPPO_BUBBLE_ACTION_INVITE;
	} else {
		g_warning("Unknown membership status %s", watch->status);
		description = "";
		header = "";
	}
	
	hippo_bubble_set_ignore_text(bubble, "Hush Updates", " (2hrs)");  
	hippo_bubble_set_actions(bubble, actions);
	   
    member_id = hippo_entity_get_guid(HIPPO_ENTITY(watch->user));
    hippo_bubble_set_assoc_guid(bubble, member_id);

    hippo_bubble_set_swarm_user_link(bubble,
    								 header,
                                     hippo_entity_get_name(watch->user),
                                     member_id);
    bubble_watch_ref(BUBBLE_WATCH(watch));
    hippo_app_load_photo(hippo_get_app(), HIPPO_ENTITY(watch->user),
                         on_chatter_photo_loaded, watch);    

    /* FIXME ordering of this isn't guaranteed, i.e. we could get a reply for 
     * an "old" post in theory
     */
    bubble_watch_ref(BUBBLE_WATCH(watch)); /* on_sender_photo_loaded will unref */    
    hippo_app_load_photo(hippo_get_app(), watch->group, on_sender_photo_loaded, watch);

    hippo_bubble_set_link_description(bubble, description);
}

static void
post_watch_finalize(void *value)
{
    PostWatch *watch = POST_WATCH(value);
    
    if (watch->post)
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->post),
                                             G_CALLBACK(on_post_changed), watch);
    if (watch->base.room) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->base.room),
                                             G_CALLBACK(on_post_chat_user_state_changed), watch);
    }                                             
    REMOVE_WEAK(&watch->post);
}

static void
group_watch_finalize(void *value)
{
	GroupWatch *watch = GROUP_WATCH(value);

    if (watch->group)
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->group),
                                             G_CALLBACK(on_group_changed), watch);                  	
    if (watch->base.room) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->base.room),
                                             G_CALLBACK(on_group_chat_user_state_changed), watch);
    }                                                  
    REMOVE_WEAK(&watch->group);
}

static void
group_membership_watch_finalize(void *value)
{
	GroupMembershipWatch *watch = GROUP_MEMBERSHIP_WATCH(value);

    if (watch->group)
        g_signal_handlers_disconnect_by_func(G_OBJECT(watch->group),
                                             G_CALLBACK(on_group_membership_changed), watch);                  	
    REMOVE_WEAK(&watch->group);
	REMOVE_WEAK(&watch->user);    
	g_free(watch->status);
	watch->status = NULL;
}

static void
post_watch_add(HippoBubble    *bubble,
               HippoPost      *post,
               HippoDataCache *cache)
{
    PostWatch *watch;
        
    watch = g_new0(PostWatch, 1);
    
    bubble_watch_init(&watch->base, bubble, 
				      hippo_post_get_chat_room(post),
				      cache,
                      post_watch_finalize, WATCH_KIND_POST);
    
    watch->post = post;
    ADD_WEAK(&watch->post);
    g_signal_connect(watch->base.room, "user-state-changed", G_CALLBACK(on_post_chat_user_state_changed), watch);	      
    
    g_signal_connect(post, "changed", G_CALLBACK(on_post_changed), watch); 
    
    bubble_watch_set(bubble, BUBBLE_WATCH(watch));
    
    on_post_changed(post, watch); /* includes an update_viewers() */
    update_last_message(BUBBLE_WATCH(watch));
    
    bubble_watch_unref(BUBBLE_WATCH(watch));
}

static void
group_watch_add(HippoBubble    *bubble,
                HippoEntity    *group,
                HippoDataCache *cache)
{
    GroupWatch *watch;
        
    watch = g_new0(GroupWatch, 1);
    
    bubble_watch_init(&watch->base, bubble,
    				  hippo_entity_get_chat_room(group),
				      cache,
                      group_watch_finalize, WATCH_KIND_GROUP);
     
    watch->group = group;
    ADD_WEAK(&watch->group);
    
    g_signal_connect(group, "changed", G_CALLBACK(on_group_changed), watch);
    g_signal_connect(watch->base.room, "user-state-changed", G_CALLBACK(on_group_chat_user_state_changed), watch);    
    
    bubble_watch_set(bubble, BUBBLE_WATCH(watch));
    
    on_group_changed(group, watch); /* includes an update_viewers() */
    update_last_message(BUBBLE_WATCH(watch));
    
    bubble_watch_unref(BUBBLE_WATCH(watch));
}

static void
group_membership_watch_add(HippoBubble    *bubble,
                           HippoEntity    *group,
                           HippoEntity    *user,
                           const char     *status,                           
                           HippoDataCache *cache)
{
    GroupMembershipWatch *watch;
        
    watch = g_new0(GroupMembershipWatch, 1);
    
    bubble_watch_init(&watch->base, bubble,
    				  NULL,
				      cache,
                      group_membership_watch_finalize, WATCH_KIND_GROUP_MEMBERSHIP);
     
    watch->group = group;
    ADD_WEAK(&watch->group);
    watch->user = user;
    ADD_WEAK(&watch->user);
    watch->status = g_strdup(status);
    
    g_signal_connect(group, "changed", G_CALLBACK(on_group_membership_changed), watch);
    // TODO: implement me
    // g_signal_connect(watch->room, "user-state-changed", G_CALLBACK(on_group_chat_user_state_changed), watch);
    
    bubble_watch_set(bubble, BUBBLE_WATCH(watch));
    
    on_group_membership_changed(group, watch);
    
    bubble_watch_unref(BUBBLE_WATCH(watch));
}

void
hippo_bubble_set_post(HippoBubble    *bubble,
                      HippoPost      *post,
                      HippoDataCache *cache)
{
    post_watch_add(bubble, post, cache);
}

void
hippo_bubble_set_group(HippoBubble    *bubble,
                       HippoEntity    *group,
                       HippoDataCache *cache)
{
    group_watch_add(bubble, group, cache);
}

void
hippo_bubble_set_group_membership_change(HippoBubble    *bubble,
                                         HippoEntity    *group,
                                         HippoEntity    *user,
                                         const char     *status,                                         
                                         HippoDataCache *cache)
{
    group_membership_watch_add(bubble, group, user, status, cache);
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

HippoEntity *
hippo_bubble_get_group(HippoBubble *bubble)
{
    BubbleWatch *watch;
    
    watch = bubble_watch_get(bubble);
    if (watch && watch->kind == WATCH_KIND_GROUP)
        return GROUP_WATCH(watch)->group;
    else
        return NULL;
}

void
hippo_bubble_get_group_membership_change(HippoBubble    *bubble,
                                         HippoEntity    **group,
                                         HippoEntity    **user)
{
	BubbleWatch *watch;
    
    watch = bubble_watch_get(bubble);
    if (watch && watch->kind == WATCH_KIND_GROUP_MEMBERSHIP) {
        *group = GROUP_MEMBERSHIP_WATCH(watch)->group;
        *user = GROUP_MEMBERSHIP_WATCH(watch)->user;
    } else {
        *group = NULL;
        *user = NULL;
    }
}
