/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_ENTITY_PROTECTED_H__
#define __HIPPO_ENTITY_PROTECTED_H__

#include <hippo/hippo-entity.h>

G_BEGIN_DECLS

struct _HippoEntity {
    GObject parent;
    HippoEntityType type;
    char *guid;
    char *name;
    char *home_url;
    char *photo_url;
    int version;
    int notify_freeze_count;
    gboolean need_notify;
};

struct _HippoEntityClass {
    GObjectClass parent;

    gboolean (*update_from_xml) (HippoEntity    *entity,
                                 HippoDataCache *cache,
                                 LmMessageNode  *node);
};

void hippo_entity_freeze_notify (HippoEntity *entity);
void hippo_entity_thaw_notify   (HippoEntity *entity);
void hippo_entity_notify        (HippoEntity *entity);

void hippo_entity_set_string     (HippoEntity *entity,
                                  char       **s_p,
                                  const char  *val);
    
G_END_DECLS

#endif /* __HIPPO_ENTITY_PROTECTED_H__ */
