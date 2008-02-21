/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_H__
#define __HIPPO_CANVAS_BLOCK_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>
#include "hippo-actions.h"
#include "hippo-block.h"

G_BEGIN_DECLS

#define HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR 0x666666ff

typedef struct _HippoCanvasBlock      HippoCanvasBlock;
typedef struct _HippoCanvasBlockClass HippoCanvasBlockClass;

#define HIPPO_TYPE_CANVAS_BLOCK              (hippo_canvas_block_get_type ())
#define HIPPO_CANVAS_BLOCK(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK, HippoCanvasBlock))
#define HIPPO_CANVAS_BLOCK_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK, HippoCanvasBlockClass))
#define HIPPO_IS_CANVAS_BLOCK(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK))
#define HIPPO_IS_CANVAS_BLOCK_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK))
#define HIPPO_CANVAS_BLOCK_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK, HippoCanvasBlockClass))

struct _HippoCanvasBlock {
    HippoCanvasBox box;
    HippoBlockType required_type;
    HippoBlock *block;
    HippoActions *actions;
    HippoCanvasBox *main_box;
    HippoCanvasItem *expand_pointer;
    HippoCanvasItem *unexpand_pointer;
    HippoCanvasBox  *age_parent;
    HippoCanvasItem *age_separator_item;
    HippoCanvasItem *age_prefix_item;
    HippoCanvasItem *age_item;
    HippoCanvasBox  *original_age_box;
    HippoCanvasItem *original_age_prefix_item;
    HippoCanvasItem *original_age_item;
    HippoCanvasBox  *sent_to_box;
    HippoCanvasItem *sent_to_text_item;
    HippoCanvasItem *sent_to_item;
    HippoCanvasBox  *heading_box;
    HippoCanvasItem *heading_icon_item;
    HippoCanvasItem *heading_lock_item;
    HippoCanvasItem *title_link_item;
    HippoCanvasItem *headshot_item;
    HippoCanvasItem *name_item;
    HippoCanvasItem *toggle_hush_link;
    GTime original_age;
    /* probably a class props not instance props, but free bits anyway */
    unsigned int expandable : 1;
    unsigned int linkify_title : 1;
    unsigned int skip_heading : 1;
    unsigned int skip_lock : 1;
    unsigned int skip_standard_right : 1;
    unsigned int message_block : 1;
    
    unsigned int expanded : 1;
    unsigned int hushed : 1;
    unsigned int sent_to_set : 1;
    unsigned int age_set : 1;
    unsigned int original_age_set : 1;
    unsigned int child_changed_pointer : 1;
};

struct _HippoCanvasBlockClass {
    HippoCanvasBoxClass parent_class;

    void (* append_content_items)  (HippoCanvasBlock *canvas_block,
                                    HippoCanvasBox   *parent_box);
    void (* append_right_items)    (HippoCanvasBlock *canvas_block,
                                    HippoCanvasBox   *parent_box);
    void (* set_block)             (HippoCanvasBlock *canvas_block,
                                    HippoBlock       *block);
    void (* title_activated)       (HippoCanvasBlock *canvas_block);
    void (* clicked_count_changed) (HippoCanvasBlock *canvas_block);
    void (* significant_clicked_count_changed) (HippoCanvasBlock *canvas_block);
    void (* stack_reason_changed)  (HippoCanvasBlock *canvas_block);
    void (* expand)                (HippoCanvasBlock *canvas_block);
    void (* unexpand)              (HippoCanvasBlock *canvas_block);
    void (* hush)                  (HippoCanvasBlock *canvas_block);
    void (* unhush)                (HippoCanvasBlock *canvas_block);
};

GType            hippo_canvas_block_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_block_new         (HippoBlockType type,
                                                 HippoActions  *actions);

void             hippo_canvas_block_set_block   (HippoCanvasBlock *canvas_block,
                                                 HippoBlock       *block);


/* Protected methods */
void hippo_canvas_block_set_heading (HippoCanvasBlock *canvas_block,
                                     const char       *heading);
void hippo_canvas_block_set_title   (HippoCanvasBlock *canvas_block,
                                     const char       *text,
                                     const char       *tooltip,
                                     gboolean          visited);

void hippo_canvas_block_set_expanded(HippoCanvasBlock *canvas_block,
                                     gboolean          value);

/* probably has to get factored out into subclass */
void hippo_canvas_block_set_sender  (HippoCanvasBlock *canvas_block,
                                     HippoEntity      *entity);
void hippo_canvas_block_set_sent_to (HippoCanvasBlock *canvas_block,
                                     GSList           *entities);
void hippo_canvas_block_set_original_age (HippoCanvasBlock *canvas_block,
                                          GTime            age);

HippoActions* hippo_canvas_block_get_actions  (HippoCanvasBlock *canvas_block);

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_H__ */
