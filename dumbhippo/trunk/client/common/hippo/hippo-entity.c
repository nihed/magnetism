#include "hippo-entity.h"

/* === HippoEntity implementation === */

static void     hippo_entity_finalize             (GObject *object);

struct _HippoEntity {
    GObject parent;
};

struct _HippoEntityClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoEntity, hippo_entity, G_TYPE_OBJECT);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_entity_init(HippoEntity *entity)
{
}

static void
hippo_entity_class_init(HippoEntityClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    object_class->finalize = hippo_entity_finalize;
}

static void
hippo_entity_finalize(GObject *object)
{
    HippoEntity *entity = HIPPO_ENTITY(object);

    G_OBJECT_CLASS(hippo_entity_parent_class)->finalize(object); 
}

/* === HippoEntity exported API === */

HippoEntity*
hippo_entity_new(void)
{
    HippoEntity *entity = g_object_new(HIPPO_TYPE_ENTITY, NULL);
    
    return entity;
}
