/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-resource.h"
#include "hippo-entity-protected.h"
#include <string.h>

/* === HippoResource implementation === */

static void     hippo_resource_finalize             (GObject *object);

struct _HippoResource {
    HippoEntity parent;
};

struct _HippoResourceClass {
    HippoEntityClass parent;
};

G_DEFINE_TYPE(HippoResource, hippo_resource, HIPPO_TYPE_ENTITY);

enum {
    NONE_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_resource_init(HippoResource *resource)
{
}

static void
hippo_resource_class_init(HippoResourceClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    object_class->finalize = hippo_resource_finalize;
}

static void
hippo_resource_finalize(GObject *object)
{
    HippoResource *resource = HIPPO_RESOURCE(object);

    G_OBJECT_CLASS(hippo_resource_parent_class)->finalize(object); 
}

/* === HippoResource exported API === */

                               
