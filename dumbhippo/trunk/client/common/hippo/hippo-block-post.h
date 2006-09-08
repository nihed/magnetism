/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_POST_H__
#define __HIPPO_BLOCK_POST_H__

#include <hippo/hippo-connection.h>
#include <hippo/hippo-person.h>
#include <hippo/hippo-post.h>
#include <hippo/hippo-block.h>
#include <hippo/hippo-chat-room.h>

G_BEGIN_DECLS

typedef struct _HippoBlockPost      HippoBlockPost;
typedef struct _HippoBlockPostClass HippoBlockPostClass;


#define HIPPO_TYPE_BLOCK_POST              (hippo_block_post_get_type ())
#define HIPPO_BLOCK_POST(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_POST, HippoBlockPost))
#define HIPPO_BLOCK_POST_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_POST, HippoBlockPostClass))
#define HIPPO_IS_BLOCK_POST(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_POST))
#define HIPPO_IS_BLOCK_POST_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_POST))
#define HIPPO_BLOCK_POST_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_POST, HippoBlockPostClass))

GType        	 hippo_block_post_get_type               (void) G_GNUC_CONST;

HippoBlock*      hippo_block_post_new                    (void);

G_END_DECLS

#endif /* __HIPPO_BLOCK_POST_H__ */
