#include "hippo-entity.h"

/* === HippoEntity implementation === */

static void     hippo_entity_finalize             (GObject *object);

struct _HippoEntity {
    GObject parent;
    HippoEntityType type;
    char *guid;
    char *name;
    char *small_photo_url;
};

struct _HippoEntityClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoEntity, hippo_entity, G_TYPE_OBJECT);

enum {
    NAME_CHANGED,
    SMALL_PHOTO_CHANGED,
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

    /* These should really just be notify:: on properties probably,
     * but too painful
     */
    signals[NAME_CHANGED] =
        g_signal_new ("name-changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__POINTER,
            		  G_TYPE_NONE, 0);

    signals[SMALL_PHOTO_CHANGED] =
        g_signal_new ("small-photo-changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__POINTER,
            		  G_TYPE_NONE, 0);

    object_class->finalize = hippo_entity_finalize;
}

static void
hippo_entity_finalize(GObject *object)
{
    HippoEntity *entity = HIPPO_ENTITY(object);

    g_free(entity->guid);
    g_free(entity->name);
    g_free(entity->small_photo_url);

    G_OBJECT_CLASS(hippo_entity_parent_class)->finalize(object); 
}

/* === HippoEntity exported API === */

HippoEntity*
hippo_entity_new(HippoEntityType  type,
                 const char      *guid)
{
    HippoEntity *entity = g_object_new(HIPPO_TYPE_ENTITY, NULL);
    
    entity->type = type;
    entity->guid = g_strdup(guid);
    
    return entity;
}

const char*
hippo_entity_get_guid(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->guid;
}

const char*
hippo_entity_get_name(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->name;
}

const char*
hippo_entity_get_small_photo_url(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->small_photo_url;
}

static void
set_str(char **s_p, const char *val)
{
    if (*s_p == val)
        return;
        
    g_free(*s_p);
    *s_p = g_strdup(val);
}

void
hippo_entity_set_name(HippoEntity    *entity,
                      const char     *name)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    set_str(&entity->name, name);
    g_signal_emit(entity, signals[NAME_CHANGED], 0);
}

void
hippo_entity_set_small_photo_url(HippoEntity    *entity,
                                 const char     *url)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    set_str(&entity->small_photo_url, url);
    g_signal_emit(entity, signals[SMALL_PHOTO_CHANGED], 0);
}
