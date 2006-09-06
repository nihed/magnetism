/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stack-manager.h"
#include "hippo-canvas-base.h"
#include "hippo-canvas-stack.h"
#include "hippo-canvas-block.h"
#include "hippo-window.h"

typedef struct {
    int              refcount;
    HippoPlatform   *platform;
    HippoDataCache  *cache;
    HippoConnection *connection;
    HippoStackMode mode;
    HippoWindow     *base_window;
    HippoCanvasItem *base_item;
    HippoWindow     *single_block_window;
    HippoWindow     *stack_window;
    HippoCanvasItem *stack_item;
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

    position_alongside(manager->base_window, 3, icon,
                       icon_orientation,
                       is_west, is_north, &base);
    position_alongside(manager->single_block_window, 0, &base,
                       HIPPO_ORIENTATION_HORIZONTAL, is_west, is_north, NULL);
    position_alongside(manager->stack_window, 0, &base,
                       HIPPO_ORIENTATION_HORIZONTAL, is_west, is_north, NULL);
}

static void
update_window_positions(StackManager    *manager)
{
   HippoRectangle monitor;
   HippoRectangle icon;
   HippoOrientation icon_orientation;
   
   hippo_platform_get_screen_info(manager->platform, &monitor, &icon, &icon_orientation);

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
        hippo_window_set_visible(manager->base_window, FALSE);
        hippo_window_set_visible(manager->single_block_window, FALSE);
        hippo_window_set_visible(manager->stack_window, FALSE);
        break;
    case HIPPO_STACK_MODE_SINGLE_BLOCK:
        update_window_positions(manager);
        hippo_window_set_visible(manager->base_window, TRUE);
        hippo_window_set_visible(manager->single_block_window, TRUE);
        hippo_window_set_visible(manager->stack_window, FALSE);
        break;
    case HIPPO_STACK_MODE_STACK:
        update_window_positions(manager);
        hippo_window_set_visible(manager->base_window, TRUE);
        hippo_window_set_visible(manager->single_block_window, FALSE);
        hippo_window_set_visible(manager->stack_window, TRUE);
        break;
    }
}

static void
manager_disconnect(StackManager *manager)
{
    if (manager->cache) {
        g_object_unref(manager->base_window);
        g_object_unref(manager->single_block_window);
        g_object_unref(manager->stack_window);
        manager->base_window = NULL;
        manager->single_block_window = NULL;
        manager->stack_window = NULL;

        g_object_unref(manager->platform);
        manager->platform = NULL;
        
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

static void
manager_attach(StackManager    *manager,
               HippoDataCache  *cache,
               HippoPlatform   *platform)
{
    g_debug("Stack manager attaching to data cache");

    manager->platform = platform;
    g_object_ref(manager->platform);
    
    manager->cache = cache;
    g_object_ref(manager->cache);
    manager->connection = hippo_data_cache_get_connection(manager->cache);

    /* this creates a refcount cycle, but
     * hippo_stack_manager_unmanage breaks it.
     * Also, too lazy right now to key to the cache/icon
     * pair, right now it just keys to the cache
     */
    manager_ref(manager);
    g_object_set_data_full(G_OBJECT(cache), "stack-manager",
                           manager, (GFreeFunc) manager_unref);


    manager->base_window = hippo_platform_create_window(platform);
    manager->single_block_window = hippo_platform_create_window(platform);
    manager->stack_window = hippo_platform_create_window(platform);

    manager->base_item = hippo_canvas_base_new();
    g_object_set(manager->base_item, "fixed-width", 400, NULL);
    hippo_window_set_contents(manager->base_window, manager->base_item);
    
    manager->stack_item = hippo_canvas_stack_new();
    g_object_set(manager->stack_item, "fixed-width", 400, NULL);
    hippo_window_set_contents(manager->stack_window, manager->stack_item);
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
hippo_stack_manager_manage(HippoDataCache  *cache,
                           HippoPlatform   *platform)
{
    StackManager *manager;

    manager = manager_new();

    manager_attach(manager, cache, platform);
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
hippo_stack_manager_set_screen_info(HippoDataCache  *cache,
                                    HippoRectangle  *monitor,
                                    HippoRectangle  *icon,
                                    HippoOrientation icon_orientation)
{
    StackManager *manager;

    manager = g_object_get_data(G_OBJECT(cache), "stack-manager");
    
    update_for_screen_info(manager, monitor, icon, icon_orientation);
}
