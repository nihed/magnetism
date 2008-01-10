/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-group.h"
#include "hippo-entity-protected.h"
#include <string.h>

/* === HippoGroup implementation === */

static void hippo_group_finalize (GObject     *object);
static void hippo_group_update   (HippoEntity *entity);

struct _HippoGroup {
    HippoEntity parent;

    HippoMembershipStatus status;
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
}

static void
hippo_group_class_init(HippoGroupClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoEntityClass *entity_class = HIPPO_ENTITY_CLASS (klass);
          
    object_class->finalize = hippo_group_finalize;

    entity_class->update = hippo_group_update;
}

static void
hippo_group_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_group_parent_class)->finalize(object); 
}

static void
hippo_group_update(HippoEntity    *entity)
{
    HippoGroup *group = HIPPO_GROUP(entity);
    const char *status_str;
    HippoMembershipStatus status;
    
    HIPPO_ENTITY_CLASS(hippo_group_parent_class)->update(entity);

    ddm_data_resource_get(entity->resource,
                          "status", DDM_DATA_STRING, &status_str,
                          NULL);
    
    if (status_str == NULL || !hippo_membership_status_from_string(status_str, &status))
        status = HIPPO_MEMBERSHIP_STATUS_NONMEMBER;

    if (status != group->status) {
        group->status = status;
        hippo_entity_notify(entity);
    }
}

HippoGroup*
hippo_group_get_for_resource(DDMDataResource *resource)
{
    HippoGroup *group = ddm_data_resource_get_data(resource, "hippo-entity");
    if (group == NULL)
        group = HIPPO_GROUP(hippo_entity_new(HIPPO_ENTITY_GROUP, resource));
    else
        g_object_ref(group);

    return group;
}

HippoMembershipStatus
hippo_group_get_status(HippoGroup *group)
{
    return group->status;
}
