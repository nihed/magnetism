/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_ENTITY_H__
#define __HIPPO_ENTITY_H__

#include <hippo/hippo-basics.h>
#include <loudmouth/loudmouth.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_ENTITY_RESOURCE,
    HIPPO_ENTITY_GROUP,
    HIPPO_ENTITY_PERSON,
    HIPPO_ENTITY_FEED
} HippoEntityType;

typedef struct _HippoEntity      HippoEntity;
typedef struct _HippoEntityClass HippoEntityClass;

#define HIPPO_TYPE_ENTITY              (hippo_entity_get_type ())
#define HIPPO_ENTITY(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_ENTITY, HippoEntity))
#define HIPPO_ENTITY_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_ENTITY, HippoEntityClass))
#define HIPPO_IS_ENTITY(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_ENTITY))
#define HIPPO_IS_ENTITY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_ENTITY))
#define HIPPO_ENTITY_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_ENTITY, HippoEntityClass))

GType            hippo_entity_get_type            (void) G_GNUC_CONST;
HippoEntity*     hippo_entity_new                 (HippoEntityType type,
                                                   const char     *guid);

gboolean         hippo_entity_update_from_xml     (HippoEntity    *entity,
                                                   HippoDataCache *cache,
                                                   LmMessageNode  *node);

const char*      hippo_entity_get_guid            (HippoEntity    *entity);
HippoEntityType  hippo_entity_get_entity_type     (HippoEntity    *entity);
const char*      hippo_entity_get_name            (HippoEntity    *entity);
const char*      hippo_entity_get_home_url        (HippoEntity    *entity);
const char*      hippo_entity_get_small_photo_url (HippoEntity    *entity);

void             hippo_entity_set_name            (HippoEntity    *entity,
                                                   const char     *name);
void             hippo_entity_set_home_url        (HippoEntity    *entity,
                                                   const char     *link);
void             hippo_entity_set_small_photo_url (HippoEntity    *entity,
                                                   const char     *url);
G_END_DECLS

#endif /* __HIPPO_ENTITY_H__ */
