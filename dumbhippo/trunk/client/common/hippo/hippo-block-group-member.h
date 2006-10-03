/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_GROUP_MEMBER_H__
#define __HIPPO_BLOCK_GROUP_MEMBER_H__

#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoBlockGroupMember      HippoBlockGroupMember;
typedef struct _HippoBlockGroupMemberClass HippoBlockGroupMemberClass;


#define HIPPO_TYPE_BLOCK_GROUP_MEMBER              (hippo_block_group_member_get_type ())
#define HIPPO_BLOCK_GROUP_MEMBER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_GROUP_MEMBER, HippoBlockGroupMember))
#define HIPPO_BLOCK_GROUP_MEMBER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_GROUP_MEMBER, HippoBlockGroupMemberClass))
#define HIPPO_IS_BLOCK_GROUP_MEMBER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_GROUP_MEMBER))
#define HIPPO_IS_BLOCK_GROUP_MEMBER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_GROUP_MEMBER))
#define HIPPO_BLOCK_GROUP_MEMBER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_GROUP_MEMBER, HippoBlockGroupMemberClass))

GType            hippo_block_group_member_get_type               (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_BLOCK_GROUP_MEMBER_H__ */
