/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_GROUP_CHAT_H__
#define __HIPPO_CANVAS_BLOCK_GROUP_CHAT_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockGroupChat      HippoCanvasBlockGroupChat;
typedef struct _HippoCanvasBlockGroupChatClass HippoCanvasBlockGroupChatClass;

#define HIPPO_TYPE_CANVAS_BLOCK_GROUP_CHAT              (hippo_canvas_block_group_chat_get_type ())
#define HIPPO_CANVAS_BLOCK_GROUP_CHAT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_GROUP_CHAT, HippoCanvasBlockGroupChat))
#define HIPPO_CANVAS_BLOCK_GROUP_CHAT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_GROUP_CHAT, HippoCanvasBlockGroupChatClass))
#define HIPPO_IS_CANVAS_BLOCK_GROUP_CHAT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_GROUP_CHAT))
#define HIPPO_IS_CANVAS_BLOCK_GROUP_CHAT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_GROUP_CHAT))
#define HIPPO_CANVAS_BLOCK_GROUP_CHAT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_GROUP_CHAT, HippoCanvasBlockGroupChatClass))

GType            hippo_canvas_block_group_chat_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_GROUP_CHAT_H__ */
