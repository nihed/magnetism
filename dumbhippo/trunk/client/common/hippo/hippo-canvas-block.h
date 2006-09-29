/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_H__
#define __HIPPO_CANVAS_BLOCK_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>
#include "hippo-actions.h"
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

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
    HippoCanvasBox  *close_controls_parent;
    HippoCanvasItem *close_controls;    
    HippoCanvasItem *age_item;
    HippoCanvasItem *heading_text_item;
    HippoCanvasItem *title_link_item;
    HippoCanvasItem *content_container_item;
    HippoCanvasItem *headshot_item;
    HippoCanvasItem *name_item;
    unsigned int expanded : 1;
};

struct _HippoCanvasBlockClass {
    HippoCanvasBoxClass parent_class;

    void (* set_block)             (HippoCanvasBlock *canvas_block,
                                    HippoBlock       *block);
    void (* title_activated)       (HippoCanvasBlock *canvas_block);
    void (* clicked_count_changed) (HippoCanvasBlock *canvas_block);
    void (* expand)                (HippoCanvasBlock *canvas_block);
    void (* unexpand)              (HippoCanvasBlock *canvas_block);
};

GType        	 hippo_canvas_block_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_block_new         (HippoBlockType type,
                                                 HippoActions  *actions);

void             hippo_canvas_block_set_block   (HippoCanvasBlock *canvas_block,
                                                 HippoBlock       *block);


/* Protected methods */
void hippo_canvas_block_set_heading (HippoCanvasBlock *canvas_block,
                                     const char       *heading);
void hippo_canvas_block_set_title   (HippoCanvasBlock *canvas_block,
                                     const char       *text);
void hippo_canvas_block_set_content (HippoCanvasBlock *canvas_block,
                                     HippoCanvasItem  *content_item);
/* probably has to get factored out into subclass */
void hippo_canvas_block_set_sender  (HippoCanvasBlock *canvas_block,
                                     const char       *entity_guid);

HippoActions* hippo_canvas_block_get_actions  (HippoCanvasBlock *canvas_block);

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_H__ */
