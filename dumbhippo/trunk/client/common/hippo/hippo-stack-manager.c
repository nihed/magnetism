/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-stack-manager.h"
#include "hippo-canvas-base.h"
#include "hippo-canvas-stack.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-grip.h"
#include <hippo/hippo-canvas-widgets.h>
#include "hippo-window.h"
#include "hippo-actions.h"

#define UI_WIDTH 500

/* Length of time notifications are hushed after the user clicks "Hush" for the notification window */
#define HUSH_TIME (3600 * 1000)  /* One hour */

/* Border around the entire window */
#define WINDOW_BORDER 1

typedef struct {
    int              refcount;
    HippoDataCache  *cache;
    HippoActions    *actions;
    HippoConnection *connection;
    GSList          *blocks;

    gboolean         browser_open;
    HippoWindow     *browser_window;
    HippoCanvasItem *browser_box;
    HippoCanvasItem *browser_base_item;
    HippoCanvasItem *browser_scroll_item;
    HippoCanvasItem *browser_item;
    HippoCanvasItem *browser_resize_grip;

    int              saved_browser_x;
    int              saved_browser_y;

    /* Only blocks stacked after this time are visible in the notification window */
    gint64           hush_timestamp;
    
    gboolean         notification_open;
    HippoWindow     *notification_window;
    HippoCanvasItem *notification_box;
    HippoCanvasItem *notification_base_item;
    HippoCanvasItem *notification_item;

    guint            base_on_bottom : 1;
    guint            user_resized_browser : 1;
    guint            user_moved_browser : 1;
    
    guint            idle : 1;
} StackManager;

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
    } else {
        icon->x = monitor->x + monitor->width - icon->width;
    }
    
    if (is_north) {
        icon->y = monitor->y;
    } else {
        icon->y = monitor->y + monitor->height - icon->height;
    }

    /* g_debug("base_on_bottom %d is_north %d", manager->base_on_bottom, is_north); */

    if ((manager->base_on_bottom && is_north) ||
        (!manager->base_on_bottom && !is_north)) {
        hippo_canvas_box_reverse(HIPPO_CANVAS_BOX(manager->notification_box));
        manager->base_on_bottom = !manager->base_on_bottom;
    }

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
        natural_height = monitor.height * 0.75;
    
    hippo_window_set_size(manager->browser_window, natural_width, natural_height);
}

static gint64
manager_get_newest_timestamp(StackManager *manager)
{
    if (manager->blocks) {
        HippoBlock *block = manager->blocks->data;
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
    
    if (visible)
        update_window_positions(manager, FALSE, TRUE);

    hippo_window_set_visible(manager->notification_window, visible);
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
    } else {
        /* For GTK+, if you position a window once, then every time you show it again,
         * GTK+ will want to pop it back to the original position. To workaround this
         * misfeature of GTK+, whenever we hide our browser window, we remember where
         * it was, and set that position back before showing it again. In theory, this
         * could be useful in the future if we were saving the window position as a
         * user preference.
         */
        hippo_window_get_position(manager->browser_window,
                                  &manager->saved_browser_x, &manager->saved_browser_y);
    }

    hippo_window_set_visible(manager->browser_window, visible);
}

static void
resort_block(StackManager *manager,
             HippoBlock   *block)
{
    GSList *link;

    link = g_slist_find(manager->blocks, block);
    
    if (link != NULL) {
        manager->blocks = g_slist_remove(manager->blocks, block);
    }
    
    manager->blocks = g_slist_insert_sorted(manager->blocks,
                                            block,
                                            hippo_block_compare_newest_first);

    hippo_canvas_stack_add_block(HIPPO_CANVAS_STACK(manager->browser_item),
                                 block);
    hippo_canvas_stack_add_block(HIPPO_CANVAS_STACK(manager->notification_item),
                                 block);

    if (!manager->browser_open && !hippo_canvas_box_is_empty(HIPPO_CANVAS_BOX(manager->notification_item)))
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

    if (g_slist_find(manager->blocks, block) != NULL)
        return;

    g_object_ref(block);

    g_signal_connect(G_OBJECT(block), "notify::sort-timestamp",
                     G_CALLBACK(on_block_sort_changed),
                     manager);

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
        g_signal_handlers_disconnect_by_func(G_OBJECT(block),
                                             G_CALLBACK(on_block_sort_changed),
                                             manager);
        g_object_unref(block);
    }

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
        g_signal_handlers_disconnect_by_func(manager->cache,
                                             G_CALLBACK(on_block_added),
                                             manager);
        g_signal_handlers_disconnect_by_func(manager->cache,
                                             G_CALLBACK(on_block_removed),
                                             manager);

        while (manager->blocks != NULL) {
            remove_block(manager->blocks->data, manager);
        }

        g_object_unref(manager->notification_window);
        manager->notification_window = NULL;
        manager->notification_base_item = NULL;
        manager->notification_item = NULL;
        
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

static void
manager_attach(StackManager    *manager,
               HippoDataCache  *cache)
{
    HippoPlatform *platform;
    
    g_debug("Stack manager attaching to data cache");
    
    manager->cache = cache;
    g_object_ref(manager->cache);
    manager->connection = hippo_data_cache_get_connection(manager->cache);

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
                     
    manager->browser_item = g_object_new(HIPPO_TYPE_CANVAS_STACK,
                                         "box-width", UI_WIDTH,
                                         "actions", manager->actions,
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
    g_object_set(manager->notification_window, "app-window", FALSE, NULL);

    manager->notification_base_item = g_object_new(HIPPO_TYPE_CANVAS_BASE,
                                                   "actions", manager->actions,
                                                   "notification-mode", TRUE,
                                                   NULL);
    manager->notification_item = g_object_new(HIPPO_TYPE_CANVAS_STACK,
                                              "box-width", UI_WIDTH,
                                              "actions", manager->actions,
                                              NULL);

    manager->notification_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                             "orientation", HIPPO_ORIENTATION_VERTICAL,
                                             "border", 1,
                                             "border-color", 0x9c9c9cff,
                                             NULL);

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

    HippoConnection *connection = hippo_data_cache_get_connection(cache);
    gint64 hush_timestamp = hippo_current_time_ms() + hippo_connection_get_server_time_offset(connection) + HUSH_TIME;

    manager_set_hush_timestamp(manager, hush_timestamp);
    manager_set_notification_visible(manager, FALSE);
}

void
hippo_stack_manager_close_notification(HippoDataCache  *cache)
{
    StackManager *manager = g_object_get_data(G_OBJECT(cache), "stack-manager");

    manager_set_hush_timestamp(manager, manager_get_newest_timestamp(manager));
    manager_set_notification_visible(manager, FALSE);
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

    /* FIXME: Handle the case where the browser is visible but obscured or on
     * a different desktop
     */
    manager_set_browser_visible(manager, !manager->browser_open);
}
