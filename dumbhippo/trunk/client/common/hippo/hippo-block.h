/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_H__
#define __HIPPO_BLOCK_H__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct _HippoBlock      HippoBlock;
typedef struct _HippoBlockClass HippoBlockClass;

#define HIPPO_TYPE_BLOCK              (hippo_block_get_type ())
#define HIPPO_BLOCK(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK, HippoBlock))
#define HIPPO_BLOCK_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK, HippoBlockClass))
#define HIPPO_IS_BLOCK(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK))
#define HIPPO_IS_BLOCK_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK))
#define HIPPO_BLOCK_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK, HippoBlockClass))

GType        	 hippo_block_get_type                  (void) G_GNUC_CONST;
HippoBlock*      hippo_block_new                       (const char *guid);

const char*      hippo_block_get_guid                  (HippoBlock *block);

G_END_DECLS

#endif /* __HIPPO_BLOCK_H__ */
