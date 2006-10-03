/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_H__
#define __HIPPO_BLOCK_H__

#include <loudmouth/loudmouth.h>
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


struct _HippoBlock {
    GObject parent;
    char   *guid;
    HippoBlockType type;
    GTime  update_time;
    gint64 server_timestamp;
    gint64 timestamp;
    gint64 clicked_timestamp;
    gint64 ignored_timestamp;
    int clicked_count;
    guint clicked : 1;
    guint ignored : 1;
};

struct _HippoBlockClass {
    GObjectClass parent;

    gboolean (*update_from_xml) (HippoBlock     *block,
                                 HippoDataCache *cache,
                                 LmMessageNode  *node,
                                 guint64         server_time);
};

GType            hippo_block_get_type                  (void) G_GNUC_CONST;
HippoBlock*      hippo_block_new                       (const char    *guid,
                                                        HippoBlockType type);

gboolean         hippo_block_update_from_xml           (HippoBlock     *block,
                                                        HippoDataCache *cache,
                                                        LmMessageNode  *node,
                                                        guint64         server_time);

const char*      hippo_block_get_guid                  (HippoBlock *block);
HippoBlockType   hippo_block_get_block_type            (HippoBlock *block);

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

HippoBlockType hippo_block_type_from_string(const char  *s);

G_END_DECLS

#endif /* __HIPPO_BLOCK_H__ */
