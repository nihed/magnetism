/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_MUSIC_CHAT_H__
#define __HIPPO_BLOCK_MUSIC_CHAT_H__

#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoBlockMusicChat      HippoBlockMusicChat;
typedef struct _HippoBlockMusicChatClass HippoBlockMusicChatClass;


#define HIPPO_TYPE_BLOCK_MUSIC_CHAT              (hippo_block_music_chat_get_type ())
#define HIPPO_BLOCK_MUSIC_CHAT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_MUSIC_CHAT, HippoBlockMusicChat))
#define HIPPO_BLOCK_MUSIC_CHAT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_MUSIC_CHAT, HippoBlockMusicChatClass))
#define HIPPO_IS_BLOCK_MUSIC_CHAT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_MUSIC_CHAT))
#define HIPPO_IS_BLOCK_MUSIC_CHAT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_MUSIC_CHAT))
#define HIPPO_BLOCK_MUSIC_CHAT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_MUSIC_CHAT, HippoBlockMusicChatClass))

GType            hippo_block_music_chat_get_type               (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_BLOCK_MUSIC_CHAT_H__ */
