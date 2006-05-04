#ifndef __HIPPO_ENTITY_H__
#define __HIPPO_ENTITY_H__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct _HippoEntity      HippoEntity;
typedef struct _HippoEntityClass HippoEntityClass;

#define HIPPO_TYPE_ENTITY              (hippo_entity_get_type ())
#define HIPPO_ENTITY(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_ENTITY, HippoEntity))
#define HIPPO_ENTITY_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_ENTITY, HippoEntityClass))
#define HIPPO_IS_ENTITY(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_ENTITY))
#define HIPPO_IS_ENTITY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_ENTITY))
#define HIPPO_ENTITY_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_ENTITY, HippoEntityClass))

GType        	 hippo_entity_get_type                  (void) G_GNUC_CONST;
HippoEntity       *hippo_entity_new                       (void);

G_END_DECLS

#endif /* __HIPPO_ENTITY_H__ */
