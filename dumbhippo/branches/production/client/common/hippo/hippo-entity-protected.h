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
    char *small_photo_url;
    int version;
	HippoChatRoom *room;
	/* date updates about this entity were last ignored */
	GTime date_last_ignored;
};

struct _HippoEntityClass {
    GObjectClass parent;
};

void hippo_entity_emit_changed   (HippoEntity *entity);

void hippo_entity_set_string     (HippoEntity *entity,
                                  char       **s_p,
                                  const char  *val);
    
G_END_DECLS

#endif /* __HIPPO_ENTITY_PROTECTED_H__ */
