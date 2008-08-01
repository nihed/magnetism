/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_GROUP_REVISION_H__
#define __HIPPO_CANVAS_BLOCK_GROUP_REVISION_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockGroupRevision      HippoCanvasBlockGroupRevision;
typedef struct _HippoCanvasBlockGroupRevisionClass HippoCanvasBlockGroupRevisionClass;

#define HIPPO_TYPE_CANVAS_BLOCK_GROUP_REVISION              (hippo_canvas_block_group_revision_get_type ())
#define HIPPO_CANVAS_BLOCK_GROUP_REVISION(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_GROUP_REVISION, HippoCanvasBlockGroupRevision))
#define HIPPO_CANVAS_BLOCK_GROUP_REVISION_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_GROUP_REVISION, HippoCanvasBlockGroupRevisionClass))
#define HIPPO_IS_CANVAS_BLOCK_GROUP_REVISION(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_GROUP_REVISION))
#define HIPPO_IS_CANVAS_BLOCK_GROUP_REVISION_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_GROUP_REVISION))
#define HIPPO_CANVAS_BLOCK_GROUP_REVISION_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_GROUP_REVISION, HippoCanvasBlockGroupRevisionClass))

GType            hippo_canvas_block_group_revision_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_GROUP_REVISION_H__ */
