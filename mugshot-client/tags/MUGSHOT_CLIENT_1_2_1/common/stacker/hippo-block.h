/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_H__
#define __HIPPO_BLOCK_H__

#include <hippo/hippo-basics.h>
#include <hippo/hippo-entity.h>
#include <ddm/ddm.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_BLOCK_TYPE_UNKNOWN,
    HIPPO_BLOCK_TYPE_ACCOUNT_QUESTION,
    HIPPO_BLOCK_TYPE_POST,
    HIPPO_BLOCK_TYPE_GROUP_MEMBER,
    HIPPO_BLOCK_TYPE_GROUP_REVISION,
    HIPPO_BLOCK_TYPE_GROUP_CHAT,
    HIPPO_BLOCK_TYPE_MUSIC_CHAT,
    HIPPO_BLOCK_TYPE_MUSIC_PERSON,
    HIPPO_BLOCK_TYPE_FLICKR_PERSON,
    HIPPO_BLOCK_TYPE_FLICKR_PHOTOSET,
    HIPPO_BLOCK_TYPE_FACEBOOK_EVENT,
    HIPPO_BLOCK_TYPE_YOUTUBE_PERSON,
    HIPPO_BLOCK_TYPE_NETFLIX_MOVIE,    
    HIPPO_BLOCK_TYPE_AMAZON_REVIEW,
    HIPPO_BLOCK_TYPE_AMAZON_WISH_LIST_ITEM,
    HIPPO_BLOCK_TYPE_GENERIC
} HippoBlockType;

typedef enum {
    HIPPO_STACK_NEW_BLOCK,
    HIPPO_STACK_BLOCK_UPDATE,
    HIPPO_STACK_VIEWER_COUNT,
    HIPPO_STACK_CHAT_MESSAGE
} HippoStackReason;

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
    DDMDataResource *resource;
    char *guid;
    HippoBlockType type;
    HippoEntity *source;
    gboolean is_public;
    GTime  update_time;
    gint64 timestamp;
    gint64 clicked_timestamp;
    gint64 ignored_timestamp;
    int significant_clicked_count;
    int clicked_count;
    char *chat_id;
    char *icon_url;
    char *title;
    char *title_link;
    HippoStackReason stack_reason;
    guint is_feed : 1;
    guint is_mine : 1;
    guint clicked : 1;
    guint ignored : 1;
    guint pinned : 1;
};

struct _HippoBlockClass {
    GObjectClass parent;

    void (*update) (HippoBlock *block);
};

GType            hippo_block_get_type                  (void) G_GNUC_CONST;

const char*      hippo_block_get_guid                  (HippoBlock *block);
HippoBlockType   hippo_block_get_block_type            (HippoBlock *block);

HippoEntity *    hippo_block_get_source                (HippoBlock *block);
void             hippo_block_set_source                (HippoBlock *block, HippoEntity *entity);

gboolean hippo_block_is_public             (HippoBlock *block);
void     hippo_block_set_public            (HippoBlock *block,
                                            gboolean    value);
gint64   hippo_block_get_timestamp         (HippoBlock *block);
void     hippo_block_set_timestamp         (HippoBlock *block,
                                            gint64      value);
gint64   hippo_block_get_clicked_timestamp (HippoBlock *block); /* Unused */
void     hippo_block_set_clicked_timestamp (HippoBlock *block,
                                            gint64      value);
gint64   hippo_block_get_ignored_timestamp (HippoBlock *block);
void     hippo_block_set_ignored_timestamp (HippoBlock *block,
                                            gint64      value);
gint64   hippo_block_get_sort_timestamp    (HippoBlock *block);
int      hippo_block_get_clicked_count     (HippoBlock *block); /* Unused */
void     hippo_block_set_clicked_count     (HippoBlock *block,
                                            int         value);
int      hippo_block_get_significant_clicked_count     (HippoBlock *block);
void     hippo_block_set_significant_clicked_count     (HippoBlock *block,
                                                        int         value);
gboolean hippo_block_get_clicked           (HippoBlock *block);
void     hippo_block_set_clicked           (HippoBlock *block,
                                            gboolean    value);
gboolean hippo_block_get_ignored           (HippoBlock *block); /* Unused */
void     hippo_block_set_ignored           (HippoBlock *block,
                                            gboolean    value);
gboolean hippo_block_get_pinned            (HippoBlock *block); /* Client side */
void     hippo_block_set_pinned            (HippoBlock *block,
                                            gboolean    pinned);

gboolean hippo_block_get_is_feed           (HippoBlock *block);
void     hippo_block_set_is_feed           (HippoBlock *block,
                                            gboolean    feed);     
                                                                                        
gboolean hippo_block_get_is_mine           (HippoBlock *block);
void     hippo_block_set_is_mine           (HippoBlock *block,
                                            gboolean    mine);                                            
    
HippoStackReason hippo_block_get_stack_reason (HippoBlock      *block);
void             hippo_block_set_stack_reason (HippoBlock      *block,
                                               HippoStackReason value);

const char * hippo_block_get_chat_id       (HippoBlock *block);
void         hippo_block_set_chat_id       (HippoBlock *block,
                                            const char *chat_id);
const char* hippo_block_get_title          (HippoBlock *block); 
void        hippo_block_set_title          (HippoBlock *block,
                                            const char *title);

const char* hippo_block_get_title_link     (HippoBlock *block);
void        hippo_block_set_title_link     (HippoBlock *block,
                                            const char *title_link);

const char* hippo_block_get_icon_url       (HippoBlock *block);
void        hippo_block_set_icon_url       (HippoBlock *block,
                                            const char *icon_url);

int      hippo_block_compare_newest_first  (gconstpointer block_a,
                                            gconstpointer block_b);

DDMDataResource *hippo_block_get_resource (HippoBlock *block);

HippoBlock *hippo_block_create_for_resource (DDMDataResource *resource);

G_END_DECLS

#endif /* __HIPPO_BLOCK_H__ */
