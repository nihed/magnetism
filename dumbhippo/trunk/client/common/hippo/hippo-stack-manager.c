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

#define UI_WIDTH 400

typedef struct {
    int              refcount;
    HippoDataCache  *cache;
    HippoActions    *actions;
    HippoConnection *connection;
    GSList          *blocks;
    HippoStackMode   mode;

    HippoWindow     *stack_window;
    HippoCanvasItem *stack_box;
    HippoCanvasItem *stack_base_item;
    HippoCanvasItem *stack_scroll_item;
    HippoCanvasItem *stack_item;
    HippoCanvasItem *stack_resize_grip;
    
    HippoWindow     *single_block_window;
    HippoCanvasItem *single_block_box;
    HippoCanvasItem *single_block_base_item;
    HippoCanvasItem *single_block_item;

    guint            base_on_bottom : 1;
    
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
                        HippoOrientation icon_orientation)
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
        hippo_canvas_box_reverse(HIPPO_CANVAS_BOX(manager->stack_box));
        hippo_canvas_box_reverse(HIPPO_CANVAS_BOX(manager->single_block_box));
        manager->base_on_bottom = !manager->base_on_bottom;
    }
    
    position_alongside(manager->single_block_window, 3, icon,
                       icon_orientation,
                       is_west, is_north, &base);
    position_alongside(manager->stack_window, 3, icon,
                       icon_orientation,
                       is_west, is_north, &base);
}

static void
update_window_positions(StackManager    *manager)
{
   HippoRectangle monitor;
   HippoRectangle icon;
   HippoOrientation icon_orientation;
   HippoPlatform *platform;

   platform = hippo_connection_get_platform(manager->connection);
   
   hippo_platform_get_screen_info(platform,
                                  &monitor, &icon, &icon_orientation);

   update_for_screen_info(manager, &monitor, &icon, icon_orientation);
}

static void
manager_set_mode(StackManager     *manager,
                 HippoStackMode    mode)
{
    if (mode == manager->mode)
        return;

    manager->mode = mode;
    
    switch (mode) {
    case HIPPO_STACK_MODE_HIDDEN:
        hippo_window_set_visible(manager->single_block_window, FALSE);
        hippo_window_set_visible(manager->stack_window, FALSE);
        break;
    case HIPPO_STACK_MODE_SINGLE_BLOCK:
        update_window_positions(manager);
        hippo_window_set_visible(manager->single_block_window, TRUE);
        hippo_window_set_visible(manager->stack_window, FALSE);
        break;
    case HIPPO_STACK_MODE_STACK:
        update_window_positions(manager);
        hippo_window_set_visible(manager->single_block_window, FALSE);
        hippo_window_set_visible(manager->stack_window, TRUE);
        break;
    }
}

static int
block_sort_newest_first_func(gconstpointer a,
                             gconstpointer b)
{
    HippoBlock *block_a = (HippoBlock*) a;
    HippoBlock *block_b = (HippoBlock*) b;
    gint64 stamp_a = hippo_block_get_sort_timestamp(block_a);
    gint64 stamp_b = hippo_block_get_sort_timestamp(block_b);

    if (stamp_a < stamp_b)
        return 1;
    else if (stamp_a > stamp_b)
        return -1;
    else
        return 0;
}

static void
update_current_block(StackManager *manager)
{
    HippoBlock *new_block = manager->blocks ? manager->blocks->data : NULL;
    
    if (manager->single_block_item) {
        HippoBlock *old_block;
        old_block = NULL;
        g_object_get(G_OBJECT(manager->single_block_item),
                     "block", &old_block,
                     NULL);
        if (old_block == new_block)
            return;

        hippo_canvas_box_remove(HIPPO_CANVAS_BOX(manager->single_block_box),
                                manager->single_block_item);
        manager->single_block_item = NULL;
    }
    
    if (new_block)
        manager->single_block_item = hippo_canvas_block_new(hippo_block_get_block_type(new_block),
                                                            manager->actions);
    else
        manager->single_block_item = hippo_canvas_block_new(HIPPO_BLOCK_TYPE_UNKNOWN,
                                                            manager->actions);

    hippo_canvas_block_set_block(HIPPO_CANVAS_BLOCK(manager->single_block_item),
                                 new_block);
    
    g_object_set(manager->single_block_item, "box-width", UI_WIDTH, NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->single_block_box),
                            manager->single_block_item,
                            manager->base_on_bottom ?
                            0 : HIPPO_PACK_END);
}

static void
resort_block(StackManager *manager,
             HippoBlock   *block)
{
    GSList *link;

    link = g_slist_find(manager->blocks, block);
    
    if (link != NULL) {
        manager->blocks = g_slist_remove(manager->blocks, block);

        hippo_canvas_stack_remove_block(HIPPO_CANVAS_STACK(manager->stack_item),
                                        block);
    }
    
    manager->blocks = g_slist_insert_sorted(manager->blocks,
                                            block,
                                            block_sort_newest_first_func);

    hippo_canvas_stack_add_block(HIPPO_CANVAS_STACK(manager->stack_item),
                                 block);
    
    update_current_block(manager);
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

    update_current_block(manager);
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

        g_object_unref(manager->single_block_window);
        manager->single_block_window = NULL;
        manager->single_block_base_item = NULL;
        manager->single_block_item = NULL;
        
        g_object_unref(manager->stack_window);
        manager->stack_window = NULL;
        manager->stack_base_item = NULL;
        manager->stack_scroll_item = NULL;
        manager->stack_item = NULL;
        manager->stack_resize_grip = NULL;

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
    manager->mode = HIPPO_STACK_MODE_HIDDEN;
    
    return manager;
}

static gboolean
on_stack_resize_grip_button_press(HippoCanvasItem *item,
                                  HippoEvent      *event,
                                  void            *data)
{
    StackManager *manager = data;

    hippo_window_begin_resize_drag(manager->stack_window,
                                   manager->base_on_bottom ?
                                   HIPPO_SIDE_TOP : HIPPO_SIDE_BOTTOM,
                                   event);

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

    manager->stack_window = hippo_platform_create_window(platform);

    hippo_window_set_resizable(manager->stack_window,
                               HIPPO_ORIENTATION_VERTICAL,
                               TRUE);
    
    manager->stack_base_item = hippo_canvas_base_new();
    
    manager->stack_item = g_object_new(HIPPO_TYPE_CANVAS_STACK,
                                       "box-width", UI_WIDTH,
                                       "actions", manager->actions,
                                       NULL);
    manager->stack_scroll_item = hippo_canvas_scrollbars_new();
    hippo_canvas_scrollbars_set_enabled(HIPPO_CANVAS_SCROLLBARS(manager->stack_scroll_item),
                                        HIPPO_ORIENTATION_HORIZONTAL,
                                        FALSE);
    
    manager->stack_resize_grip = g_object_new(HIPPO_TYPE_CANVAS_GRIP,
                                              NULL);

    g_signal_connect(G_OBJECT(manager->stack_resize_grip),
                     "button-press-event",
                     G_CALLBACK(on_stack_resize_grip_button_press),
                     manager);
    
    manager->stack_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                      "orientation", HIPPO_ORIENTATION_VERTICAL,
                                      NULL);
    
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->stack_box),
                            manager->stack_base_item,
                            0);
    hippo_canvas_scrollbars_set_root(HIPPO_CANVAS_SCROLLBARS(manager->stack_scroll_item),
                                     manager->stack_item);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->stack_box),
                            manager->stack_scroll_item,
                            HIPPO_PACK_EXPAND);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->stack_box),
                            manager->stack_resize_grip,
                            0);
    
    hippo_window_set_contents(manager->stack_window, manager->stack_box);

    manager->single_block_window = hippo_platform_create_window(platform);

    manager->single_block_base_item = hippo_canvas_base_new();
    manager->single_block_item = NULL; /* filled in later */

    manager->single_block_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                             "orientation", HIPPO_ORIENTATION_VERTICAL,
                                             NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(manager->single_block_box),
                            manager->single_block_base_item, 0);

    hippo_window_set_contents(manager->single_block_window, manager->single_block_box);
    
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
hippo_stack_manager_set_mode (HippoDataCache  *cache,
                              HippoStackMode   mode)
{
    StackManager *manager;

    manager = g_object_get_data(G_OBJECT(cache), "stack-manager");

    manager_set_mode(manager, mode);
}

void
hippo_stack_manager_toggle_stack(HippoDataCache  *cache)
{
    StackManager *manager;

    manager = g_object_get_data(G_OBJECT(cache), "stack-manager");

    if (manager->mode == HIPPO_STACK_MODE_STACK) {
        manager_set_mode(manager, HIPPO_STACK_MODE_HIDDEN);
    } else {
        manager_set_mode(manager, HIPPO_STACK_MODE_STACK);
    }
}

void
hippo_stack_manager_set_screen_info(HippoDataCache  *cache,
                                    HippoRectangle  *monitor,
                                    HippoRectangle  *icon,
                                    HippoOrientation icon_orientation)
{
    StackManager *manager;

    manager = g_object_get_data(G_OBJECT(cache), "stack-manager");
    
    update_for_screen_info(manager, monitor, icon, icon_orientation);
}
