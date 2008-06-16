/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_GROUP_REVISION_H__
#define __HIPPO_BLOCK_GROUP_REVISION_H__

#include "hippo-block.h"

G_BEGIN_DECLS

typedef struct _HippoBlockGroupRevision      HippoBlockGroupRevision;
typedef struct _HippoBlockGroupRevisionClass HippoBlockGroupRevisionClass;


#define HIPPO_TYPE_BLOCK_GROUP_REVISION              (hippo_block_group_revision_get_type ())
#define HIPPO_BLOCK_GROUP_REVISION(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_GROUP_REVISION, HippoBlockGroupRevision))
#define HIPPO_BLOCK_GROUP_REVISION_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_GROUP_REVISION, HippoBlockGroupRevisionClass))
#define HIPPO_IS_BLOCK_GROUP_REVISION(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_GROUP_REVISION))
#define HIPPO_IS_BLOCK_GROUP_REVISION_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_GROUP_REVISION))
#define HIPPO_BLOCK_GROUP_REVISION_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_GROUP_REVISION, HippoBlockGroupRevisionClass))

GType            hippo_block_group_revision_get_type               (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_BLOCK_GROUP_REVISION_H__ */
