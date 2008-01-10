/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_GROUP_H__
#define __HIPPO_GROUP_H__

#include <hippo/hippo-entity.h>

G_BEGIN_DECLS

typedef struct _HippoGroup      HippoGroup;
typedef struct _HippoGroupClass HippoGroupClass;

#define HIPPO_TYPE_GROUP              (hippo_group_get_type ())
#define HIPPO_GROUP(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_GROUP, HippoGroup))
#define HIPPO_GROUP_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_GROUP, HippoGroupClass))
#define HIPPO_IS_GROUP(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_GROUP))
#define HIPPO_IS_GROUP_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_GROUP))
#define HIPPO_GROUP_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_GROUP, HippoGroupClass))

GType            hippo_group_get_type                  (void) G_GNUC_CONST;
HippoGroup*      hippo_group_new                       (const char  *guid);

HippoGroup*      hippo_group_get_for_resource (DDMDataResource *resource);

HippoMembershipStatus hippo_group_get_status (HippoGroup *group);

G_END_DECLS

#endif /* __HIPPO_GROUP_H__ */
