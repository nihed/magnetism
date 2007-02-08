/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-block-group-chat.h"
#include "hippo-block-music-chat.h"
#include "hippo-block-post.h"
#include "hippo-common-internal.h"
#include "hippo-stack-manager.h"
#include "hippo-canvas-base.h"
#include "hippo-canvas-filter-area.h"
#include "hippo-canvas-stack.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-grip.h"
#include <hippo/hippo-canvas-widgets.h>
#include "hippo-window.h"
#include "hippo-actions.h"

#define UI_WIDTH 500

/* Length of time notifications are hushed after the user clicks "Hush" for the notification window */
#define HUSH_TIME (3600 * 1000)  /* One hour */

/* When the user is active (not idle), and the user isn't hovering over the notification window,
 * it closes after this much time */
#define NOTIFICATION_TIMEOUT_TIME (10 * 1000)  /* 10 seconds */

/* Border around the entire window */
#define WINDOW_BORDER 1

typedef struct {
    int              refcount;
    HippoDataCache  *cache;
    HippoActions    *actions;
    HippoConnection *connection;
    gboolean         nofeed_active;
    gboolean         noselfsource_active;
    GSList          *blocks;

    HippoWindow     *browser_window;
    HippoCanvasItem *browser_box;
    HippoCanvasItem *browser_base_item;
    HippoCanvasItem *browser_filter_area_item;    
    HippoCanvasItem *browser_scroll_item;
    HippoCanvasItem *browser_item;
    HippoCanvasItem *browser_resize_grip;

    int              saved_browser_x;
    int              saved_browser_y;

    /* Only blocks stacked after this time are visible in the notification window */
    gint64           hush_timestamp;
    
    HippoWindow     *notification_window;
    HippoCanvasItem *notification_box;
    HippoCanvasItem *notification_base_item;
    HippoCanvasItem *notification_item;

    guint            notification_timeout; 

    guint            browser_open : 1;
    guint            notification_open : 1;

    guint            notification_hovering : 1;
    
    guint            base_on_bottom : 1;
    guint            filter_area_visible : 1;    
    guint            user_resized_browser : 1;
    guint            user_moved_browser : 1;
    
    guint            idle : 1;
} StackManager;

static void manager_close_notification(StackManager *manager);

static void
position_alongside(HippoWindow     *window,
                   int              gap,
                   HippoRectangle  *alongside,
                   HippoOrientation orientation,
                   gboolean         is_west,
                   gboolean         is_north,
                   HippoRectangle  *window_position_p)
{
    int window_x, window_y;
    int window_width, window_height;
    
    hippo_window_get_size(window, &window_width, &window_height);
    if (orientation == HIPPO_ORIENTATION_VERTICAL) {
        if (is_west) {
            window_x = alongside->x + alongside->width + gap;
        } else {
            window_x = alongside->x - window_width - gap;
        }
        if (is_north) {
            window_y = alongside->y;
        } else {
            window_y = alongside->y + alongside->height - window_height;
        }
    } else {
        if (is_west) {
            window_x = alongside->x;
        } else {
            window_x = alongside->x + alongside->width - window_width;
        }
        if (is_north) {
            window_y = alongside->y + alongside->height + gap;
        } else {
            window_y = alongside->y - window_height - gap;
        }
    }
    hippo_window_set_position(window, window_x, window_y);

#if 0
    g_debug("alongside %d,%d %dx%d  window %d,%d %dx%d\n",
            alongside->x, alongside->y,
            alongside->width, alongside->height,
            window_x, window_y, window_width, window_height);
#endif
    
    if (window_position_p) {
        window_position_p->x = window_x;
        window_position_p->y = window_y;
        window_position_p->width = window_width;
        window_position_p->height = window_height;
    }
}

static void
update_for_screen_info (StackManager    *manager,
                        HippoRectangle  *monitor,
                        HippoRectangle  *icon,
                        HippoOrientation icon_orientation,
                        gboolean         position_browser,
                        gboolean         position_notification)
{
    HippoRectangle base;
    gboolean is_west;
    gboolean is_north;

#if 1
    g_debug("SIZING: work area %d,%d %dx%d icon %d,%d %dx%d orientation %d",
        monitor->x, monitor->y, monitor->width,  monitor->height,
        icon->x, icon->y, icon->width, icon->height, 
        icon_orientation);
#endif

    is_west = ((icon->x + icon->width / 2) < (monitor->x + monitor->width / 2));
    is_north = ((icon->y + icon->height / 2) < (monitor->y + monitor->height / 2));

    /* We pretend the icon is always in the corner */
    if (is_west) {
        icon->x = monitor->x;
        if (icon_orientation == HIPPO_ORIENTATION_VERTICAL)
            icon->x -= icon->width;
    } else {
        icon->x = monitor->x + monitor->width - icon->width;
        if (icon_orientation == HIPPO_ORIENTATION_VERTICAL)
            icon->x += icon->width;
    }
    
    if (is_north) {
        icon->y = monitor->y;
        if (icon_orientation == HIPPO_ORIENTATION_HORIZONTAL)
            icon->y -= icon->height;
    } else {
        icon->y = monitor->y + monitor->height - icon->height;
        if (icon_orientation == HIPPO_ORIENTATION_HORIZONTAL)
            icon->y += icon->height;
    }

    /* g_debug("base_on_bottom %d is_north %d", manager->base_on_bottom, is_north); */

    if ((manager->base_on_bottom && is_north) ||
        (!manager->base_on_bottom && !is_north)) {
        hippo_canvas_box_reverse(HIPPO_CANVAS_BOX(manager->notification_box));
        manager->base_on_bottom = !manager->base_on_bottom;
    }

    /* We only resize vertically, so we don't need to worry about EAST/WEST */
    g_object_set(manager->notification_window, 
                 "resize-gravity", is_north ? HIPPO_GRAVITY_NORTH_WEST : HIPPO_GRAVITY_SOUTH_WEST, 
                 NULL);

    if (position_browser)
        position_alongside(manager->browser_window, 3, icon,
                           icon_orientation,
                           is_west, is_north, &base);
    
    if (position_notification)
        position_alongside(manager->notification_window, 3, icon,
                           icon_orientation,
                           is_west, is_north, &base);
}

static void
update_window_positions(StackManager *manager,
                        gboolean      position_browser,
                        gboolean      position_notification)
{
    HippoPlatform *platform;
    HippoRectangle monitor;
    HippoRectangle icon;
    HippoOrientation icon_orientation;
    
    platform = hippo_connection_get_platform(manager->connection);
    hippo_platform_get_screen_info(platform, &monitor, &icon, &icon_orientation);
    
    update_for_screen_info(manager, &monitor, &icon, icon_orientation, position_browser, position_notification);
}

static void
resize_browser_to_natural_size(StackManager *manager)
{
    HippoPlatform *platform;
    HippoRectangle monitor;
    int natural_width;
    int natural_height;
    
    platform = hippo_connection_get_platform(manager->connection);
    hippo_platform_get_screen_info(platform, &monitor, NULL, NULL);
    
    natural_width = hippo_canvas_item_get_width_request(manager->browser_box);

    natural_height = 2 * WINDOW_BORDER;
    
    natural_height += hippo_canvas_item_get_height_request(manager->browser_base_item, natural_width);

    /* The width we'll give to browser_item is actually less than this by the width of the scrollbar,
     * but our stack items have a height independent of width when collapsed in any case
     */
    natural_height += hippo_canvas_item_get_height_request(manager->browser_item, natural_width);

    natural_height += hippo_canvas_item_get_height_request(manager->browser_resize_grip, natural_width);

    if (natural_height > monitor.height * 0.75)
        natural_height = (int)monitor.height * 0.75;
    
    hippo_window_set_size(manager->browser_window, natural_width, natural_height);
}

static gboolean
browser_is_active(StackManager *manager)
{
    gboolean active;
    
    if (!manager->browser_open)
        return FALSE;

    g_object_get(manager->browser_window,
                 "active", &active,
                 NULL);

    return active;
}

static gboolean
browser_is_onscreen(StackManager *manager)
{
    gboolean onscreen;
    
    if (!manager->browser_open)
        return FALSE;

    g_object_get(manager->browser_window,
                 "onscreen", &onscreen,
                 NULL);

    return onscreen;
}

static gboolean
on_notification_timeout(gpointer data)
{
    StackManager *manager = data;

    g_debug("Running notification window timeout\n");

    manager->notification_timeout = 0;
    manager_close_notification(manager);

    return FALSE;
}

static void
start_notification_timeout(StackManager *manager)
{
    if (!manager->notification_timeout) {
        g_debug("Starting notification window timeout\n");
        manager->notification_timeout = g_timeout_add(NOTIFICATION_TIMEOUT_TIME,
                                                      on_notification_timeout, manager);
    }
}

static void
stop_notification_timeout(StackManager *manager)
{
    if (manager->notification_timeout) {
        g_debug("Stopping notification window timeout\n");
        g_source_remove(manager->notification_timeout);
        manager->notification_timeout = 0;
    }
}

static gint64
manager_get_newest_timestamp(StackManager *manager)
{
    if (manager->blocks) {
        HippoBlock *block = (manager->blocks)->data;
        return hippo_block_get_sort_timestamp(block);
    } else {
        return 0;
    }
}

static void
manager_set_hush_timestamp(StackManager *manager,
                           gint64        hush_timestamp)
{
    manager->hush_timestamp = hush_timestamp;
    hippo_canvas_stack_set_min_timestamp(HIPPO_CANVAS_STACK(manager->notification_item),
                                         hush_timestamp);
}

static void
manager_set_notification_visible(StackManager *manager,
                                 gboolean      visible)
{
    if (!visible == !manager->notification_open)
        return;

    manager->notification_open = visible;
    
    if (visible) {
        update_window_positions(manager, FALSE, TRUE);
        if (!manager->idle)
            start_notification_timeout(manager);
    } else {
        manager->notification_hovering = FALSE;
        stop_notification_timeout(manager);
    }

    hippo_window_set_visible(manager->notification_window, visible);
}

static void
manager_hush(StackManager *manager)
{
    HippoConnection *connection = hippo_data_cache_get_connection(manager->cache);
    gint64 hush_timestamp = hippo_current_time_ms() + hippo_connection_get_server_time_offset(connection) + HUSH_TIME;

    manager_set_hush_timestamp(manager, hush_timestamp);
    manager_set_notification_visible(manager, FALSE);
}

static void
manager_close_notification(StackManager *manager)
{
    /* + 1 so that we include only blocks *newer* than the current newest block */
    manager_set_hush_timestamp(manager, manager_get_newest_timestamp(manager) + 1);
    manager_set_notification_visible(manager, FALSE);
}

static void
manager_set_browser_visible(StackManager *manager,
                            gboolean      visible)
{
    if (!visible == !manager->browser_open)
        return;
    
    manager->browser_open = visible;
    
    if (visible) {
        if (!manager->user_resized_browser)
            resize_browser_to_natural_size(manager);
        if (manager->user_moved_browser)
            hippo_window_set_position(manager->browser_window,
                                      manager->saved_browser_x, manager->saved_browser_y);
        else
            update_window_positions(manager, TRUE, FALSE);

        hippo_window_set_visible(manager->browser_window, TRUE);
    } else {
        HippoPlatform *platform;
        HippoRectangle icon;

        /* For GTK+, if you position a window once, then every time you show it again,
         * GTK+ will want to pop it back to the original position. To workaround this
         * misfeature of GTK+, whenever we hide our browser window, we remember where
         * it was, and set that position back before showing it again. In theory, this
         * could be useful in the future if we were saving the window position as a
         * user preference.
         */
        hippo_window_get_position(manager->browser_window,
                                  &manager->saved_browser_x, &manager->saved_browser_y);

        /* Closing the browser is always triggered by user action at this point, and we
         * don't distinguish close and minimize, so we show the minimize animation if
         * possible.
         */
        platform = hippo_connection_get_platform(manager->connection);
        hippo_platform_get_screen_info(platform, NULL, &icon, NULL);

        hippo_window_hide_to_icon(manager->browser_window, &icon);
    }
}

static void
manager_toggle_browser(StackManager *manager)
{
    if (manager->browser_open) {
        if (browser_is_active(manager))
            manager_set_browser_visible(manager, FALSE);
        else {
            hippo_window_present(manager->browser_window);
            manager_set_notification_visible(manager, FALSE);
        }
    } else {
        manager_set_browser_visible(manager, TRUE);
        manager_set_notification_visible(manager, FALSE);
    }
}

static void
manager_toggle_filter(StackManager *manager)
{
    manager->filter_area_visible = !manager->filter_area_visible;
    g_debug("Setting filter area visible: %d", manager->filter_area_visible);
    hippo_canvas_box_set_child_visible(HIPPO_CANVAS_BOX(manager->browser_box),
                                       manager->browser_filter_area_item,
                                       manager->filter_area_visible);                
}

static gboolean
apply_current_filter(StackManager *manager,
                     HippoBlock   *block)
{
    if (manager->nofeed_active &&
        hippo_block_get_filter_flags(block) & HIPPO_BLOCK_FILTER_FLAG_FEED)
        return FALSE;
    else if (manager->noselfsource_active &&
             hippo_block_get_source(block) == ((HippoEntity*) hippo_data_cache_get_self(manager->cache)))
        return FALSE;
           
    return TRUE;
}

static void 
filter_canvas_item(HippoCanvasItem *item,
                   gpointer         data)
{
    StackManager *manager = (StackManager*) data;
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(item);
    
    hippo_canvas_box_set_child_visible(HIPPO_CANVAS_BOX(manager->browser_item),
                                       HIPPO_CANVAS_ITEM(canvas_block),
                                       apply_current_filter(manager, canvas_block->block));
}

static void
apply_filter_all(StackManager *manager)
{
    GList *conditions = NULL, *elt;
    GString *filter_string;
    g_debug("Refiltering all blocks");    
    hippo_canvas_box_foreach(HIPPO_CANVAS_BOX(manager->browser_item),
                             filter_canvas_item,
                             manager);
    filter_string = g_string_new("");
    if (manager->nofeed_active)
        conditions = g_list_prepend(conditions, "nofeed");       
    if (manager->noselfsource_active)
        conditions = g_list_prepend(conditions, "noselfsource");
    for (elt = conditions; elt; elt = elt->next) {
        g_string_append(filter_string, (const char*) elt->data);
        if (elt->next != NULL)
            g_string_append(filter_string, ",");
    }
    hippo_connection_request_blocks(manager->connection, 0, 
                                    filter_string->str);
    g_string_free(filter_string, TRUE); 
}

static void
manager_toggle_nofeed(StackManager *manager)
{
    manager->nofeed_active = !manager->nofeed_active;
    
    apply_filter_all(manager);                             
}

static void
manager_toggle_noselfsource(StackManager *manager)
{
    manager->noselfsource_active = !manager->noselfsource_active;
    
    apply_filter_all(manager);                             
}

static void
handle_filter_change(HippoConnection *connection, const char *filter, StackManager *manager) 
{
    char **elts = g_strsplit(filter, ",", 0);
    char **elt;
    gboolean nofeed = FALSE;
    gboolean noselfsource = FALSE;
    
    for (elt = elts; *elt; elt++) {
        if (strcmp(*elt, "nofeed") == 0) {
            nofeed = TRUE;    
        } else if (strcmp(*elt, "noselfsource") == 0) {
            noselfsource = TRUE;    
        } else {
            g_warning("Unknown block filter qualifier: '%s'", *elt);
        }
    }
    g_strfreev(elts);
    
    manager->nofeed_active = nofeed;
    hippo_canvas_filter_area_set_nofeed_active(HIPPO_CANVAS_FILTER_AREA(manager->browser_filter_area_item), manager->nofeed_active);
    manager->noselfsource_active = noselfsource;  
    hippo_canvas_filter_area_set_noselfsource_active(HIPPO_CANVAS_FILTER_AREA(manager->browser_filter_area_item), manager->noselfsource_active);
        
    apply_filter_all(manager);
    
    if ((manager->nofeed_active || manager->noselfsource_active)
        && !manager->filter_area_visible) {
        manager_toggle_filter(manager);
    }
}

static gboolean
chat_is_visible(StackManager *manager,
                const char   *chat_id)
{
    HippoPlatform *platform = hippo_connection_get_platform(manager->connection);
    
    switch (hippo_platform_get_chat_window_state(platform, chat_id)) {
    case HIPPO_WINDOW_STATE_CLOSED:
        return FALSE;
    case HIPPO_WINDOW_STATE_HIDDEN:
        return FALSE;
    case HIPPO_WINDOW_STATE_ONSCREEN:
        return TRUE;
    case HIPPO_WINDOW_STATE_ACTIVE:
        return TRUE;
    }

    return TRUE;
}

typedef struct {
    StackManager *manager;
    gboolean is_needed;
} NotificationNeededInfo;

static void
notification_is_needed_callback(HippoCanvasItem *item,
                                void            *data)
{
    NotificationNeededInfo *info = data;
    gboolean notify_for_block = TRUE;

    HippoBlock *block;

    g_object_get(G_OBJECT(item), "block", &block, NULL);

    g_debug("Checking if notification is needed for block %s\n", hippo_block_get_guid(block));
    
    if (HIPPO_IS_BLOCK_POST(block)) {
        HippoPost *post;
        
        g_object_get(G_OBJECT(block), "post", &post, NULL);
        if (post) {
            if (chat_is_visible(info->manager, hippo_post_get_guid(post)))
                notify_for_block = FALSE;
            g_object_unref(post);
        }
    } else if (HIPPO_IS_BLOCK_GROUP_CHAT(block)) {
        HippoGroup *group;
        
        g_object_get(G_OBJECT(block), "group", &group, NULL);
        if (group) {
            if (chat_is_visible(info->manager, hippo_entity_get_guid(HIPPO_ENTITY(group))))
                notify_for_block = FALSE;
            g_object_unref(group);
        }
    } else if (HIPPO_IS_BLOCK_MUSIC_CHAT(block)) {
        HippoTrack *track = NULL;
    
        g_object_get(G_OBJECT(block), "track", &track, NULL);
        if (track) {
            if (chat_is_visible(info->manager, hippo_track_get_play_id(track)))
                notify_for_block = FALSE;

            g_object_unref(track);
        }
    }

    g_object_unref(block);

    if (notify_for_block)
        info->is_needed = TRUE;
}

static gboolean
notification_is_needed(StackManager *manager)
{
    NotificationNeededInfo info;
    
    info.manager = manager;
    info.is_needed = FALSE;
    
    g_debug("Checking if notification is needed\n");
    
    hippo_canvas_box_foreach(HIPPO_CANVAS_BOX(manager->notification_item), 
                             notification_is_needed_callback,
                             &info);

    return info.is_needed;
}

static void
resort_block(StackManager *manager,
             HippoBlock   *block)
{
    GSList *link;
    gboolean visible;
    
    link = g_slist_find(manager->blocks, block);
    
    if (link != NULL) {
        manager->blocks = g_slist_remove(manager->blocks, block);
    }   
    
    manager->blocks = g_slist_insert_sorted(manager->blocks,
                                            block,
                                            hippo_block_compare_newest_first);

   
    visible = apply_current_filter(manager, block);
    
        hippo_canvas_stack_add_block(HIPPO_CANVAS_STACK(manager->browser_item),
                                 block, 
                                 visible);                

    if (visible)
            hippo_canvas_stack_add_block(HIPPO_CANVAS_STACK(manager->notification_item),
                                     block,
                                     TRUE);

    if (visible && !browser_is_onscreen(manager) && notification_is_needed(manager))
        manager_set_notification_visible(manager, TRUE);
}

static void
on_block_sort_changed(HippoBlock *block,
                      GParamSpec *arg,
                      void       *data)
{
    StackManager *manager = data;

    resort_block(manager, block);
}

static void
on_block_added(HippoDataCache *cache,
               HippoBlock     *block,
               void           *data)
{
    StackManager *manager = data;
    
    /* We won't know how to display this anyway */
    if (hippo_block_get_block_type(block) == HIPPO_BLOCK_TYPE_UNKNOWN)
        return;
    
    if (g_slist_find(manager->blocks, block) != NULL) {
        return;
    }

    g_object_ref(block);

    g_signal_connect(G_OBJECT(block), "notify::sort-timestamp",
                     G_CALLBACK(on_block_sort_changed),
                     data);
                     
    resort_block(manager, block);
}

static void
remove_block(HippoBlock   *block,
             StackManager *manager)
{
    GSList *link;
    
    link = g_slist_find(manager->blocks, block);
    
    if (link != NULL) {
        manager->blocks = g_slist_remove(manager->blocks, block);
    }
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(block),
                                         G_CALLBACK(on_block_sort_changed),
                                             manager);                                       
    g_object_unref(block);    

    hippo_canvas_stack_remove_block(HIPPO_CANVAS_STACK(manager->browser_item), block); 
    hippo_canvas_stack_remove_block(HIPPO_CANVAS_STACK(manager->notification_item), block);
}

static void
on_block_removed(HippoDataCache *cache,
                 HippoBlock     *block,
                 void           *data)
{
    StackManager *manager = data;

    remove_block(block, manager);
}

static void
manager_disconnect(StackManager *manager)
{
    if (manager->cache) {
        stop_notification_timeout(manager);
        
        g_signal_handlers_disconnect_by_func(manager->cache,
                                             G_CALLBACK(on_block_added),
                                             manager);
        g_signal_handlers_disconnect_by_func(manager->cache,
                                             G_CALLBACK(on_block_removed),
                                             manager);

        while (manager->blocks != NULL) {
                remove_block(manager->blocks->data, manager);
        }

        hippo_window_set_visible(manager->notification_window, FALSE);
        g_object_unref(manager->notification_window);
        manager->notification_window = NULL;
        manager->notification_base_item = NULL;
        manager->notification_item = NULL;
        
        hippo_window_set_visible(manager->browser_window, FALSE);
        g_object_unref(manager->browser_window);
        manager->browser_window = NULL;
        manager->browser_base_item = NULL;
        manager->browser_scroll_item = NULL;
        manager->browser_item = NULL;
        manager->browser_resize_grip = NULL;

        g_object_unref(manager->actions);
        manager->actions = NULL;
        
        g_object_unref(manager->cache);
        manager->cache = NULL;
        manager->connection = NULL;
    }
}

static void
manager_ref(StackManager *manager)
{
    manager->refcount += 1;
}

static void
manager_unref(StackManager *manager)
{
    g_return_if_fail(manager->refcount > 0);
    manager->refcount -= 1;
    if (manager->refcount == 0) {
        g_debug("Finalizing stack manager");
        manager_disconnect(manager);
        g_free(manager);
    }
}

static StackManager*
manager_new(void)
{
    StackManager *manager;

    manager = g_new0(StackManager, 1);
    manager->refcount = 1;
    
    return manager;
}

static void
on_browser_minimize(HippoWindow *window,
                    void        *data)
{
    StackManager *manager = data;

    manager_set_browser_visible(manager, FALSE);
}

static gboolean
on_browser_title_bar_button_press(HippoCanvasItem *item,
                                  HippoEvent      *event,
                                  void            *data)
{
    StackManager *manager = data;

    hippo_window_begin_move_drag(manager->browser_window, event);
    manager->user_moved_browser = TRUE;

    return TRUE;
}

static gboolean
on_browser_resize_grip_button_press(HippoCanvasItem *item,
                                    HippoEvent      *event,
                                    void            *data)
{
    StackManager *manager = data;

    hippo_window_begin_resize_drag(manager->browser_window,
                                   HIPPO_SIDE_BOTTOM,
                                   event);
    manager->user_resized_browser = TRUE;

    return TRUE;
}

static gboolean
on_notification_motion_notify(HippoCanvasItem *item,
                              HippoEvent      *event,
                              void            *data)
{
    StackManager *manager = data;

    if (!manager->notification_open)
        return FALSE;
    
    if (event->u.motion.detail == HIPPO_MOTION_DETAIL_ENTER) {
        manager->notification_hovering = TRUE;
        stop_notification_timeout(manager);
    } else if (event->u.motion.detail == HIPPO_MOTION_DETAIL_LEAVE) {
        manager->notification_hovering = FALSE;
        if (manager->notification_open && !manager->idle)
            start_notification_timeout(manager);
    }

    /* Don't eat the event, just observe it */
    return FALSE;
}

static void
manager_attach(StackManager    *manager,
               HippoDataCache  *cache)
{
    HippoPlatform *platform;
    
    g_debug("Stack manager attaching to data cache");
    
    manager->cache = cache;
    g_object_ref(manager->cache);
    manager->connection = hippo_data_cache_get_connection(manager->cache);

    g_signal_connect(manager->connection, "block-filter-changed",
                     G_CALLBACK(handle_filter_change), manager);

    /* FIXME really the "actions" should probably be more global, e.g.
     * shared with the tray icon, but the way I wanted to do that
     * is to stuff it on the data cache, but that requires moving
     * hippo-actions to the common library which can't be done with
     * right this second since a bunch of eventually xp stuff is in
     * the linux dir.
     */
    manager->actions = hippo_actions_new(manager->cache);
    
    platform = hippo_connection_get_platform(manager->connection);
    
    /* this creates a refcount cycle, but
     * hippo_stack_manager_unmanage breaks it.
     * Also, too lazy right now to key to the cache/icon
     * pair, right now it just keys to the cache
     */
    manager_ref(manager);
    g_object_set_data_full(G_OBJECT(cache), "stack-manager",
                           manager, (GFreeFunc) manager_unref);

    manager->browser_window = hippo_platform_create_window(platform);

    g_signal_connect(manager->browser_window, "minimize",
                     G_CALLBACK(on_browser_minimize), manager);

    hippo_window_set_resizable(manager->browser_window,
                               HIPPO_ORIENTATION_VERTICAL,
                               TRUE);
    
    manager->browser_base_item = g_object_new(HIPPO_TYPE_CANVAS_BASE,
                                              "actions", manager->actions,
                                              "notification-mode", FALSE,
                                              NULL);
    g_signal_connect(manager->browser_base_item,
                     "title-bar-button-press-event",
                     G_CALLBACK(on_browser_title_bar_button_press),
                     manager);
                     
    manager->browser_filter_area_item = g_object_new(HIPPO_TYPE_CANVAS_FILTER_AREA,
                                                     "actions", manager->actions,
                                                     NULL);
                     
    manager->browser_item = g_object_new(HIPPO_TYPE_CANVAS_STACK,
                                         "box-width", UI_WIDTH,
                                         "actions", manager->actions,
                                         "pin-messages", TRUE,
                                         NULL);
    manager->browser_scroll_item = hippo_canvas_scrollbars_new();                                         
    hippo_canvas_scrollbars_set_policy(HIPPO_CANVAS_SCROLLBARS(manager->browser_scroll_item),
                                       HIPPO_ORIENTATION_HORIZONTAL,
                                       HIPPO_SCROLLBAR_NEVER);
    hippo_canvas_scrollbars_set_policy(HIPPO_CANVAS_SCROLLBARS(manager->browser_scroll_item),
                                       HIPPO_ORIENTATION_VERTICAL,
                                       HIPPO_SCROLLBAR_ALWAYS);                                     
    
    manager->browser_resize_grip = g_object_new(HIPPO_TYPE_CANVAS_GRIP,
                                              NULL);

    g_signal_connect(G_OBJECT(manager->browser_resize_grip),
                     "button-press-event",
                     G_CALLBACK(on_browser_resize_grip_button_press),
                     manager);
    
    manager->browser_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                        "orientation", HIPPO_ORIENTATION_VERTICAL,
                                        "border", WINDOW_BORDER,
                                        "border-color", 0x9c9c9cff,
                                        NULL);
    
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->browser_box),
                            manager->browser_base_item,
                            0);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->browser_box),
                            manager->browser_filter_area_item,
                            0);                            
    hippo_canvas_box_set_child_visible(HIPPO_CANVAS_BOX(manager->browser_box),
                                       manager->browser_filter_area_item,
                                       FALSE);
    hippo_canvas_scrollbars_set_root(HIPPO_CANVAS_SCROLLBARS(manager->browser_scroll_item),
                                     manager->browser_item);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->browser_box),
                            manager->browser_scroll_item,
                            HIPPO_PACK_EXPAND);
                            
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->browser_box),
                            manager->browser_resize_grip,
                            0);
    
    hippo_window_set_contents(manager->browser_window, manager->browser_box);

    manager->notification_window = hippo_platform_create_window(platform);

    /* Omit the window from the task-list and (for platforms where there is one) the pager */
    g_object_set(manager->notification_window, "role", HIPPO_WINDOW_ROLE_NOTIFICATION, NULL);

    manager->notification_base_item = g_object_new(HIPPO_TYPE_CANVAS_BASE,
                                                   "actions", manager->actions,
                                                   "notification-mode", TRUE,
                                                   NULL);
    manager->notification_item = g_object_new(HIPPO_TYPE_CANVAS_STACK,
                                              "box-width", UI_WIDTH,
                                              "actions", manager->actions,
                                              "max-blocks", 3,
                                              NULL);

    manager->notification_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                             "orientation", HIPPO_ORIENTATION_VERTICAL,
                                             "border", 1,
                                             "border-color", 0x9c9c9cff,
                                             NULL);

    g_signal_connect(manager->notification_box, "motion-notify-event",
                     G_CALLBACK(on_notification_motion_notify), manager);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->notification_box),
                            manager->notification_base_item, 0);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->notification_box),
                            manager->notification_item, 0);

    hippo_window_set_contents(manager->notification_window, manager->notification_box);
    
    g_signal_connect(manager->cache, "block-added",
                     G_CALLBACK(on_block_added), manager);
    g_signal_connect(manager->cache, "block-removed",
                     G_CALLBACK(on_block_removed), manager);
}

static void
manager_detach(HippoDataCache  *cache)
{
    StackManager *manager;

    manager = g_object_get_data(G_OBJECT(cache), "stack-manager");
    g_return_if_fail(manager != NULL);

    manager_disconnect(manager);

    /* may destroy the manager */
    g_object_set_data(G_OBJECT(cache), "stack-manager", NULL);
}

void
hippo_stack_manager_manage(HippoDataCache  *cache)
{
    StackManager *manager;

    manager = manager_new();

    manager_attach(manager, cache);
    manager_unref(manager);
}

void
hippo_stack_manager_unmanage(HippoDataCache  *cache)
{
    manager_detach(cache);
}

void
hippo_stack_manager_set_idle (HippoDataCache  *cache,
                              gboolean         idle)
{
    StackManager *manager;

    manager = g_object_get_data(G_OBJECT(cache), "stack-manager");

    manager->idle = idle != FALSE;
    
    if (idle)
        stop_notification_timeout(manager);
    else if (manager->notification_open && !manager->notification_hovering)
        start_notification_timeout(manager);
}


void
hippo_stack_manager_set_screen_info(HippoDataCache  *cache,
                                    HippoRectangle  *monitor,
                                    HippoRectangle  *icon,
                                    HippoOrientation icon_orientation)
{
    StackManager *manager;

    manager = g_object_get_data(G_OBJECT(cache), "stack-manager");

    if (manager->notification_open)
        update_for_screen_info(manager, monitor, icon, icon_orientation, FALSE, TRUE);
}

void
hippo_stack_manager_hush(HippoDataCache  *cache)
{
    StackManager *manager = g_object_get_data(G_OBJECT(cache), "stack-manager");

    manager_hush(manager);
}

void
hippo_stack_manager_close_notification(HippoDataCache  *cache)
{
    StackManager *manager = g_object_get_data(G_OBJECT(cache), "stack-manager");

    manager_close_notification(manager);
}

void
hippo_stack_manager_close_browser(HippoDataCache  *cache)
{
    StackManager *manager = g_object_get_data(G_OBJECT(cache), "stack-manager");

    manager_set_browser_visible(manager, FALSE);
}

void
hippo_stack_manager_toggle_browser(HippoDataCache  *cache)
{
    StackManager *manager = g_object_get_data(G_OBJECT(cache), "stack-manager");
    HippoConnection *connection = hippo_data_cache_get_connection(manager->cache);

    if (!hippo_connection_get_connected(connection)) {
        HippoPlatform *platform;
        
        platform = hippo_connection_get_platform(manager->connection);
        
        hippo_platform_show_disconnected_window(platform, manager->connection);
        
        return;
    }

    manager_toggle_browser(manager);
}

void
hippo_stack_manager_toggle_filter(HippoDataCache  *cache)
{
    StackManager *manager = g_object_get_data(G_OBJECT(cache), "stack-manager");
    HippoConnection *connection = hippo_data_cache_get_connection(manager->cache);

    if (!hippo_connection_get_connected(connection)) {
        g_debug("ignoring filter toggle due to current disconnection state");
        return;
    }

    manager_toggle_filter(manager);
}

void
hippo_stack_manager_toggle_nofeed(HippoDataCache  *cache)
{
    StackManager *manager = g_object_get_data(G_OBJECT(cache), "stack-manager");
    HippoConnection *connection = hippo_data_cache_get_connection(manager->cache);

    if (!hippo_connection_get_connected(connection)) {
        g_debug("ignoring nofeed toggle due to current disconnection state");        
        return;
    }

    manager_toggle_nofeed(manager);
}

void
hippo_stack_manager_toggle_noselfsource(HippoDataCache  *cache)
{
    StackManager *manager = g_object_get_data(G_OBJECT(cache), "stack-manager");
    HippoConnection *connection = hippo_data_cache_get_connection(manager->cache);

    if (!hippo_connection_get_connected(connection)) {
        g_debug("ignoring noselfsource toggle due to current disconnection state");        
        return;
    }

    manager_toggle_noselfsource(manager);
}
