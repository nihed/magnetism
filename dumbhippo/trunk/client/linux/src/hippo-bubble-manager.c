#include "hippo-bubble-manager.h"

typedef struct {
    GtkWidget *window;
    HippoBubble *bubble;
} BubbleWindow;

typedef struct {
    int refcount;
    HippoDataCache *cache;
    GHashTable *chats;
    GSList *bubbles;
} BubbleManager;

static BubbleWindow*
bubble_window_new(void) 
{
    BubbleWindow *window;
    GdkColor border_color;
    
    window = g_new0(BubbleWindow, 1);
    window->window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_decorated(GTK_WINDOW(window->window), FALSE);
    gtk_window_set_resizable(GTK_WINDOW(window->window), FALSE);
    border_color.red = 0x9999;
    border_color.green = 0x9999;
    border_color.blue = 0x9999;
    gtk_widget_modify_bg(window->window, GTK_STATE_NORMAL, &border_color);
    gtk_container_set_border_width(GTK_CONTAINER(window->window), 1);
    
    window->bubble = HIPPO_BUBBLE(hippo_bubble_new());
    gtk_container_add(GTK_CONTAINER(window->window), GTK_WIDGET(window->bubble));
    gtk_widget_show(GTK_WIDGET(window->bubble));
    
    ADD_WEAK(&window->window);
    ADD_WEAK(&window->bubble);
    
    return window;
}

static void
bubble_window_free(BubbleWindow *window)
{
    if (window->window) {
        gtk_object_destroy(GTK_OBJECT(window->window));
    }

    REMOVE_WEAK(&window->window);
    REMOVE_WEAK(&window->bubble);
    g_free(window);
}

static void
manager_bubble_post(BubbleManager *manager,
                    HippoPost     *post)
{
    GSList *link;
    BubbleWindow *window;
    
    window = NULL;
    for (link = manager->bubbles; link != NULL; link = link->next) {
        BubbleWindow *w = link->data;
     
        if (hippo_bubble_get_post(w->bubble) == post) {
            window = w;
            break;
        }
    }
    
    if (window == NULL) {
        window = bubble_window_new();
        hippo_bubble_set_post(window->bubble, post, manager->cache);
        manager->bubbles = g_slist_append(manager->bubbles, window);
    }

    g_debug("Showing bubble window");
    gtk_widget_show(window->window);
}

static HippoPost*
manager_post_for_room(BubbleManager *manager,
                      HippoChatRoom *room)
{
    HippoPost *post;
    
    post = hippo_data_cache_lookup_post(manager->cache, 
                hippo_chat_room_get_id(room));

    return post;
}                      

/* happens for both participants and chatters */
static void
on_user_joined(HippoChatRoom *room,
               HippoPerson   *user,
               BubbleManager *manager)
{
    HippoPost *post;
    post = manager_post_for_room(manager, room);
    if (post != NULL) {                
        manager_bubble_post(manager, post);
        /* FIXME add the "viewed by" annotation to the bubble */
    }
}

static void
on_message_added(HippoChatRoom    *room,
                 HippoChatMessage *message,
                 BubbleManager    *manager)
{
    HippoPost *post;
    
    if (hippo_chat_room_get_loading(room))
        return;
    
    post = manager_post_for_room(manager, room);    
    if (post != NULL) {                
        manager_bubble_post(manager, post);
        
        /* FIXME add the "someone said" annotation to the bubble */
    }
}                                

static void
chat_room_disconnect(BubbleManager *manager,
                     HippoChatRoom *room)
{
    g_signal_handlers_disconnect_by_func(G_OBJECT(room), G_CALLBACK(on_user_joined), manager);
    g_signal_handlers_disconnect_by_func(G_OBJECT(room), G_CALLBACK(on_message_added), manager);
}

/* we can get this multiple times on a single chat room, remember */
static void 
on_chat_room_loaded(HippoPost     *post,
                    HippoChatRoom *room,
                    BubbleManager *manager)
{
    g_debug("bubble manager, room loaded %s", hippo_chat_room_get_id(room));
    
    if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_POST &&
        g_hash_table_lookup(manager->chats, room) == NULL) {
        g_signal_connect(G_OBJECT(room), "user-joined", G_CALLBACK(on_user_joined), manager);
        g_signal_connect(G_OBJECT(room), "message-added", G_CALLBACK(on_message_added), manager);
        
        g_hash_table_replace(manager->chats, room, room);
    }
}

static void 
on_post_added(HippoDataCache *cache,
              HippoPost      *post,
              BubbleManager  *manager)
{
    g_debug("bubble manager, post added %s new = %d",
            hippo_post_get_guid(post), hippo_post_get_new(post));
    if (hippo_post_get_new(post)) {
        hippo_post_set_new(post, FALSE);

        manager_bubble_post(manager, post);
    }
}

static void
foreach_disconnect(void *key, void *value, void *data)
{
    BubbleManager *manager = data;
    HippoChatRoom *room = value;
    
    chat_room_disconnect(manager, room);
}

static void
manager_disconnect(BubbleManager *manager)
{
    if (manager->cache) {
        GSList *link;
        
        for (link = manager->bubbles; link != NULL; link = link->next) {
            BubbleWindow *w = link->data;
            bubble_window_free(w);         
        }
        g_slist_free(manager->bubbles);
        manager->bubbles = NULL;
    
        g_hash_table_foreach(manager->chats, foreach_disconnect, manager);
        g_hash_table_destroy(manager->chats);
        manager->chats  = NULL;
    
        g_signal_handlers_disconnect_by_func(manager->cache, G_CALLBACK(on_chat_room_loaded), manager);
        g_signal_handlers_disconnect_by_func(manager->cache, G_CALLBACK(on_post_added), manager);    
    
        g_object_unref(manager->cache);
        manager->cache = NULL;        
    }
}

static void
manager_ref(BubbleManager *manager)
{
    manager->refcount += 1;
}

static void
manager_unref(BubbleManager *manager)
{
    g_return_if_fail(manager->refcount > 0);
    manager->refcount -= 1;
    if (manager->refcount == 0) {
        g_debug("Finalizing bubble manager");
        manager_disconnect(manager);
        g_free(manager);
    }
}

static BubbleManager*
manager_new(void)
{
    BubbleManager *manager;
    
    manager = g_new0(BubbleManager, 1);
    manager->refcount = 1;

    return manager;
}


static void
manager_attach(BubbleManager   *manager,
               HippoDataCache  *cache)
{
    g_debug("Bubble manager attaching to data cache");

    manager->cache = cache;
    g_object_ref(manager->cache);

    /* this creates a refcount cycle, but
     * hippo_bubble_manager_unmanage breaks it.
     * Also, too lazy right now to key to the cache/icon 
     * pair, right now it just keys to the cache
     */
    manager_ref(manager);
    g_object_set_data_full(G_OBJECT(cache), "bubble-manager",
                           manager, (GFreeFunc) manager_unref);

    manager->chats = g_hash_table_new(g_direct_hash, g_direct_equal);
                           
    g_signal_connect(cache, "chat-room-loaded", G_CALLBACK(on_chat_room_loaded), manager);
    g_signal_connect(cache, "post-added", G_CALLBACK(on_post_added), manager);
}

static void
manager_detach(HippoDataCache  *cache)
{
    BubbleManager *manager;
    
    manager = g_object_get_data(G_OBJECT(cache), "bubble-manager");
    g_return_if_fail(manager != NULL);
    
    manager_disconnect(manager);

    /* may destroy the manager */
    g_object_set_data(G_OBJECT(cache), "bubble-manager", NULL);
}

void
hippo_bubble_manager_manage(HippoDataCache  *cache)
{
    BubbleManager *manager;
    
    manager = manager_new();

    manager_attach(manager, cache);
    manager_unref(manager);
}                            

void
hippo_bubble_manager_unmanage(HippoDataCache  *cache)
{
    manager_detach(cache);
}
