/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stack-manager.h"

typedef struct {
    int              refcount;
    HippoDataCache  *cache;
    HippoConnection *connection;
    guint            idle : 1;
} StackManager;

static void
manager_disconnect(StackManager *manager)
{
    if (manager->cache) {
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
manager_attach(StackManager    *manager,
               HippoDataCache  *cache)
{
    g_debug("Stack manager attaching to data cache");

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
