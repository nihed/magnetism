/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_GROUP_MEMBER_H__
#define __HIPPO_CANVAS_BLOCK_GROUP_MEMBER_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockGroupMember      HippoCanvasBlockGroupMember;
typedef struct _HippoCanvasBlockGroupMemberClass HippoCanvasBlockGroupMemberClass;

#define HIPPO_TYPE_CANVAS_BLOCK_GROUP_MEMBER              (hippo_canvas_block_group_member_get_type ())
#define HIPPO_CANVAS_BLOCK_GROUP_MEMBER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_GROUP_MEMBER, HippoCanvasBlockGroupMember))
#define HIPPO_CANVAS_BLOCK_GROUP_MEMBER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_GROUP_MEMBER, HippoCanvasBlockGroupMemberClass))
#define HIPPO_IS_CANVAS_BLOCK_GROUP_MEMBER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_GROUP_MEMBER))
#define HIPPO_IS_CANVAS_BLOCK_GROUP_MEMBER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_GROUP_MEMBER))
#define HIPPO_CANVAS_BLOCK_GROUP_MEMBER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_GROUP_MEMBER, HippoCanvasBlockGroupMemberClass))

GType            hippo_canvas_block_group_member_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_block_group_member_new         (void);

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_GROUP_MEMBER_H__ */
