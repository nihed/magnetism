/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_GROUP_CHAT_H__
#define __HIPPO_BLOCK_GROUP_CHAT_H__

#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoBlockGroupChat      HippoBlockGroupChat;
typedef struct _HippoBlockGroupChatClass HippoBlockGroupChatClass;


#define HIPPO_TYPE_BLOCK_GROUP_CHAT              (hippo_block_group_chat_get_type ())
#define HIPPO_BLOCK_GROUP_CHAT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_GROUP_CHAT, HippoBlockGroupChat))
#define HIPPO_BLOCK_GROUP_CHAT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_GROUP_CHAT, HippoBlockGroupChatClass))
#define HIPPO_IS_BLOCK_GROUP_CHAT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_GROUP_CHAT))
#define HIPPO_IS_BLOCK_GROUP_CHAT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_GROUP_CHAT))
#define HIPPO_BLOCK_GROUP_CHAT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_GROUP_CHAT, HippoBlockGroupChatClass))

GType            hippo_block_group_chat_get_type               (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_BLOCK_GROUP_CHAT_H__ */
