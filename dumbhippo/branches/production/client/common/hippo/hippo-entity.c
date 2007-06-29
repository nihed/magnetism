/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-entity-protected.h"
#include "hippo-feed.h"
#include "hippo-group.h"
#include "hippo-person.h"
#include "hippo-resource.h"
#include "hippo-chat-room.h"
#include "hippo-xml-utils.h"
#include <string.h>

/* === HippoEntity implementation === */

static void     hippo_entity_finalize             (GObject *object);

static gboolean hippo_entity_real_update_from_xml(HippoEntity    *entity,
                                                  HippoDataCache *cache,
                                                  LmMessageNode  *node);

G_DEFINE_ABSTRACT_TYPE(HippoEntity, hippo_entity, G_TYPE_OBJECT);

enum {
    CHANGED,
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

    /* This should really just be notify:: on properties probably,
     * but too painful
     */
    signals[CHANGED] =
        g_signal_new ("changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    object_class->finalize = hippo_entity_finalize;
    klass->update_from_xml = hippo_entity_real_update_from_xml;
}

static void
hippo_entity_finalize(GObject *object)
{
    HippoEntity *entity = HIPPO_ENTITY(object);

    g_free(entity->guid);
    g_free(entity->name);
    g_free(entity->photo_url);
    g_free(entity->home_url);

    G_OBJECT_CLASS(hippo_entity_parent_class)->finalize(object); 
}

static gboolean
hippo_entity_real_update_from_xml(HippoEntity    *entity,
                                  HippoDataCache *cache,
                                  LmMessageNode  *node)
{
    const char *id;
    const char *name; 
    const char *photoUrl = NULL;
    const char *homeUrl = NULL;

    if (!hippo_xml_split(cache, node, NULL,
                         "id", HIPPO_SPLIT_GUID, &id,
                         "name", HIPPO_SPLIT_STRING, &name,
                         "photoUrl", HIPPO_SPLIT_URI_RELATIVE | HIPPO_SPLIT_OPTIONAL, &photoUrl,
                         /* Home url isn't relative if this is a feed, for example */
                         "homeUrl", HIPPO_SPLIT_URI_EITHER | HIPPO_SPLIT_OPTIONAL, &homeUrl,
                         NULL))
        return FALSE;
    
    if (!strcmp(id, entity->guid) == 0) {
        g_warning("ID on node for update doesn't match entity's ID");
        return FALSE;
    }

    hippo_entity_set_name(entity, name);

    if (photoUrl)
        hippo_entity_set_photo_url(entity, photoUrl);

    if (homeUrl)
        hippo_entity_set_home_url(entity, homeUrl);

    return TRUE;
}

/* === HippoEntity "protected" API === */

void
hippo_entity_freeze_notify(HippoEntity *entity)
{
    entity->notify_freeze_count++;
}

void
hippo_entity_thaw_notify(HippoEntity *entity)
{
    entity->notify_freeze_count--;
    if (entity->notify_freeze_count == 0 && entity->need_notify) {
        entity->need_notify = FALSE;
        g_signal_emit(entity, signals[CHANGED], 0);
    }
}

void 
hippo_entity_notify(HippoEntity *entity)
{
    if (entity->notify_freeze_count == 0)
        g_signal_emit(entity, signals[CHANGED], 0);
    else
        entity->need_notify = TRUE;
}

void
hippo_entity_set_string(HippoEntity *entity,
                        char       **s_p,
                        const char  *val)
{
    if (*s_p == val) /* catches both null, and self assignment */
        return;
    if (*s_p && val && strcmp(*s_p, val) == 0)
        return;        
        
    g_free(*s_p);
    *s_p = g_strdup(val);
    hippo_entity_notify(entity);
}

/* === HippoEntity exported API === */

HippoEntity*
hippo_entity_new(HippoEntityType  type,
                 const char      *guid)
{
    HippoEntity *entity = NULL;

    switch (type) {
    case HIPPO_ENTITY_RESOURCE:
        entity = g_object_new(HIPPO_TYPE_RESOURCE, NULL);
        break;
    case HIPPO_ENTITY_GROUP:
        entity = g_object_new(HIPPO_TYPE_GROUP, NULL);
        break;
    case HIPPO_ENTITY_PERSON:
        entity = g_object_new(HIPPO_TYPE_PERSON, NULL);
        break;
    case HIPPO_ENTITY_FEED:
        entity = g_object_new(HIPPO_TYPE_FEED, NULL);
        break;
    }

    g_assert(entity != NULL);
    
    entity->type = type;
    entity->guid = g_strdup(guid);
    
    return entity;
}

gboolean
hippo_entity_update_from_xml(HippoEntity    *entity,
                             HippoDataCache *cache,
                             LmMessageNode  *node)
{
    gboolean success;
    
    hippo_entity_freeze_notify(entity);

    success = HIPPO_ENTITY_GET_CLASS(entity)->update_from_xml(entity, cache, node);

    hippo_entity_thaw_notify(entity);

    return success;
    
}

const char*
hippo_entity_get_guid(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->guid;
}

HippoEntityType
hippo_entity_get_entity_type(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), -1);
    return entity->type;
}

const char*
hippo_entity_get_name(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->name;
}

const char*
hippo_entity_get_home_url(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->home_url;
}

const char*
hippo_entity_get_photo_url(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->photo_url;
}

gboolean
hippo_entity_get_in_network(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), FALSE);
    return entity->in_network;
}

void
hippo_entity_set_name(HippoEntity    *entity,
                      const char     *name)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    hippo_entity_set_string(entity, &entity->name, name);
}

void
hippo_entity_set_home_url(HippoEntity    *entity,
                          const char     *url)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    
    hippo_entity_set_string(entity, &entity->home_url, url);
}

void
hippo_entity_set_photo_url(HippoEntity    *entity,
                           const char     *url)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    /* g_debug("Setting photo for '%s' to '%s'", entity->guid, url ? url : "null"); */
    hippo_entity_set_string(entity, &entity->photo_url, url);
}

void
hippo_entity_set_in_network(HippoEntity    *entity,
                            gboolean        in_network)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));

    entity->in_network = in_network != FALSE;
    hippo_entity_notify(entity);
}
