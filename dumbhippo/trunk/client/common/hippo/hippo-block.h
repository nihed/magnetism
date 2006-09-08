/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_H__
#define __HIPPO_BLOCK_H__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_BLOCK_TYPE_UNKNOWN,
    HIPPO_BLOCK_TYPE_POST,
    HIPPO_BLOCK_TYPE_GROUP_MEMBER,
    HIPPO_BLOCK_TYPE_GROUP_CHAT,
    HIPPO_BLOCK_TYPE_MUSIC_PERSON,
    HIPPO_BLOCK_TYPE_EXTERNAL_ACCOUNT_UPDATE
} HippoBlockType;

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

GTime    hippo_block_get_update_time       (HippoBlock *block);
void     hippo_block_set_update_time       (HippoBlock *block,
                                            GTime       t);
gint64   hippo_block_get_server_timestamp  (HippoBlock *block);
void     hippo_block_set_server_timestamp  (HippoBlock *block,
                                            gint64      value);
gint64   hippo_block_get_timestamp         (HippoBlock *block);
void     hippo_block_set_timestamp         (HippoBlock *block,
                                            gint64      value);
gint64   hippo_block_get_clicked_timestamp (HippoBlock *block);
void     hippo_block_set_clicked_timestamp (HippoBlock *block,
                                            gint64      value);
gint64   hippo_block_get_ignored_timestamp (HippoBlock *block);
void     hippo_block_set_ignored_timestamp (HippoBlock *block,
                                            gint64      value);
gint64   hippo_block_get_sort_timestamp    (HippoBlock *block);
int      hippo_block_get_clicked_count     (HippoBlock *block);
void     hippo_block_set_clicked_count     (HippoBlock *block,
                                            int         value);
gboolean hippo_block_get_clicked           (HippoBlock *block);
void     hippo_block_set_clicked           (HippoBlock *block,
                                            gboolean    value);
gboolean hippo_block_get_ignored           (HippoBlock *block);
void     hippo_block_set_ignored           (HippoBlock *block,
                                            gboolean    value);

G_END_DECLS

#endif /* __HIPPO_BLOCK_H__ */
