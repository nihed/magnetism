/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-actions.h"
#include <string.h>


static void      hippo_actions_init                (HippoActions       *actions);
static void      hippo_actions_class_init          (HippoActionsClass  *klass);

static void      hippo_actions_dispose             (GObject            *object);
static void      hippo_actions_finalize            (GObject            *object);

struct _HippoActions {
    GObject parent;
    HippoDataCache *cache;
};

struct _HippoActionsClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoActions, hippo_actions, G_TYPE_OBJECT);

static void
hippo_actions_init(HippoActions  *actions)
{
}

static void
hippo_actions_class_init(HippoActionsClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = hippo_actions_dispose;
    object_class->finalize = hippo_actions_finalize;
}

HippoActions*
hippo_actions_new(HippoDataCache *cache)
{
    HippoActions *actions;

    actions = g_object_new(HIPPO_TYPE_ACTIONS,
                           NULL);

    g_object_ref(cache);
    actions->cache = cache;
    
    return actions;
}

static void
hippo_actions_dispose(GObject *object)
{
    HippoActions *actions = HIPPO_ACTIONS(object);

    if (actions->cache) {
        g_object_unref(actions->cache);
        actions->cache = NULL;
    }
    
    G_OBJECT_CLASS(hippo_actions_parent_class)->dispose(object);
}

static void
hippo_actions_finalize(GObject *object)
{
    /* HippoActions *actions = HIPPO_ACTIONS(object); */

    G_OBJECT_CLASS(hippo_actions_parent_class)->finalize(object);
}

static HippoConnection*
get_connection(HippoActions *actions)
{
    return hippo_data_cache_get_connection(actions->cache);
}

static HippoPlatform*
get_platform(HippoActions *actions)
{
    return hippo_connection_get_platform(get_connection(actions));
}

void
hippo_actions_visit_post(HippoActions   *actions,
                         HippoPost      *post)
{
    hippo_connection_visit_post(get_connection(actions), post);
}

