/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-group.h"
#include "hippo-entity-protected.h"
#include "hippo-xml-utils.h"
#include <string.h>

/* === CONSTANTS === */

/* 2 hours group ignore timeout, in seconds; make this a parameter that can be set */
/* if want different ignore timeouts or want ignore to remain in effect indefinitely */
static const int GROUP_IGNORE_TIMEOUT = 2*60*60; 

/* === HippoGroup implementation === */

static void     hippo_group_finalize             (GObject *object);

struct _HippoGroup {
    HippoEntity parent;

    /* date updates about this group were last ignored */
    GTime date_last_ignored;
};

struct _HippoGroupClass {
    HippoEntityClass parent;
};

G_DEFINE_TYPE(HippoGroup, hippo_group, HIPPO_TYPE_ENTITY);

#if 0
enum {
    NONE_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

static void
hippo_group_init(HippoGroup *group)
{
    group->date_last_ignored = 0;
}

static void
hippo_group_class_init(HippoGroupClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
          
    object_class->finalize = hippo_group_finalize;
}

static void
hippo_group_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_group_parent_class)->finalize(object); 
}

/* === HippoGroup exported API === */


GTime            
hippo_group_get_date_last_ignored(HippoGroup   *group)
{
    g_return_val_if_fail(HIPPO_IS_GROUP(group), 0);
    return group->date_last_ignored;
}

gboolean         
hippo_group_get_ignored(HippoGroup  *group)
{
    GTimeVal timeval;

    g_return_val_if_fail(HIPPO_IS_GROUP(group), FALSE);

    // date_last_ignored being 0 means that the group was never ignored 
    // or that the group was explicitly unignored; we want to check for 
    // it explicitly here, rather than let this special meaning of 0 be lost
    // in the check below
    if (group->date_last_ignored == 0)
        return FALSE;
    
    g_get_current_time(&timeval);
    if (group->date_last_ignored + GROUP_IGNORE_TIMEOUT < timeval.tv_sec)
        return FALSE;
    
    return TRUE;
}

void             
hippo_group_set_date_last_ignored(HippoGroup   *group,
                                   GTime          date) 
{
    g_return_if_fail(HIPPO_IS_GROUP(group));
    
    if (group->date_last_ignored != date) {
        group->date_last_ignored = date;
        hippo_entity_notify(HIPPO_ENTITY(group));
    }
}

void             
hippo_group_set_ignored(HippoGroup    *group,
                         gboolean        is_ignored)
{
    GTimeVal timeval;

    g_return_if_fail(HIPPO_IS_GROUP(group));
    
    if (is_ignored) {
        g_get_current_time(&timeval);
        group->date_last_ignored = timeval.tv_sec; 
    } else {
        group->date_last_ignored = 0;
    }
}
