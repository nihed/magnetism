/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-post.h"
#include "hippo-canvas-entity-photo.h"
#include "hippo-canvas-entity-name.h"
#include "hippo-canvas-box.h"
#include "hippo-canvas-image.h"
#include "hippo-canvas-text.h"
#include "hippo-canvas-link.h"
#include "hippo-actions.h"

static void      hippo_canvas_block_init                (HippoCanvasBlock       *block);
static void      hippo_canvas_block_class_init          (HippoCanvasBlockClass  *klass);
static void      hippo_canvas_block_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_block_dispose             (GObject                *object);
static void      hippo_canvas_block_finalize            (GObject                *object);

static void hippo_canvas_block_set_property (GObject      *object,
                                             guint         prop_id,
                                             const GValue *value,
                                             GParamSpec   *pspec);
static void hippo_canvas_block_get_property (GObject      *object,
                                             guint         prop_id,
                                             GValue       *value,
                                             GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_block_paint              (HippoCanvasItem *item,
                                                       cairo_t         *cr);

/* our own methods */

static void hippo_canvas_block_set_block_impl (HippoCanvasBlock *canvas_block,
                                               HippoBlock       *block);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_BLOCK,
    PROP_ACTIONS
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlock, hippo_canvas_block, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_iface_init));

static void
on_title_activated(HippoCanvasLink *title_item,
                   void            *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockClass *klass = HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block);

    if (klass->title_activated != NULL) {
        (* klass->title_activated) (canvas_block);
    }
}

static void
hippo_canvas_block_init(HippoCanvasBlock *block)
{
    HippoCanvasItem *item;
    HippoCanvasBox *box;
    HippoCanvasBox *left_column;
    HippoCanvasBox *right_column;


    HIPPO_CANVAS_BOX(block)->background_color_rgba = 0xffffffff;
    HIPPO_CANVAS_BOX(block)->padding_left = 4;
    HIPPO_CANVAS_BOX(block)->padding_right = 4;
    HIPPO_CANVAS_BOX(block)->padding_top = 4;
    HIPPO_CANVAS_BOX(block)->padding_bottom = 4;
    
    /* Create top bar */

    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block),
                            HIPPO_CANVAS_ITEM(box), 0);

    block->heading_text_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                            "text", NULL,
                                            "xalign", HIPPO_ALIGNMENT_START,
                                            NULL);
    hippo_canvas_box_append(box, block->heading_text_item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "[\\/]",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "[X]",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    
    /* Create left and right columns */

    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block),
                            HIPPO_CANVAS_ITEM(box), HIPPO_PACK_EXPAND);

    left_column = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                               "orientation", HIPPO_ORIENTATION_VERTICAL,
                               "xalign", HIPPO_ALIGNMENT_FILL,
                               "yalign", HIPPO_ALIGNMENT_START,
                               NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(left_column), HIPPO_PACK_EXPAND);
    
    right_column = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                "orientation", HIPPO_ORIENTATION_VERTICAL,
                                "xalign", HIPPO_ALIGNMENT_END,                                
                                "yalign", HIPPO_ALIGNMENT_START,
                                "padding-left", 8,
                               NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(right_column), HIPPO_PACK_END);


    /* Fill in left column */
    
    
    block->title_link_item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                                          "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                          "xalign", HIPPO_ALIGNMENT_START,
                                          "yalign", HIPPO_ALIGNMENT_START,
                                          "font", "Bold",
                                          "text", NULL,
                                          NULL);
    hippo_canvas_box_append(left_column, block->title_link_item, 0);

    g_signal_connect(G_OBJECT(block->title_link_item), "activated",
                     G_CALLBACK(on_title_activated), block);

    
    block->content_container_item = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                                 NULL);
    hippo_canvas_box_append(left_column, block->content_container_item, HIPPO_PACK_EXPAND);

    
    /* Fill in right column */


    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "xalign", HIPPO_ALIGNMENT_END,
                       "yalign", HIPPO_ALIGNMENT_START,
                       NULL);
    hippo_canvas_box_append(right_column, HIPPO_CANVAS_ITEM(box), 0);

    /* border around headshot */
    item = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                        "xalign", HIPPO_ALIGNMENT_END,
                        "yalign", HIPPO_ALIGNMENT_START,
                        "background-color", 0x000000ff,
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);                        
    
    block->headshot_item = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_PHOTO,
                                        "scale-width", 15,
                                        "scale-height", 15,
                                        "xalign", HIPPO_ALIGNMENT_CENTER,
                                        "yalign", HIPPO_ALIGNMENT_CENTER,
                                        "padding", 1,
                                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(item), block->headshot_item, 0);
    
    block->name_item = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_NAME,
                                    "font-scale", PANGO_SCALE_SMALL,
                                    "font", "Italic",
                                    "xalign", HIPPO_ALIGNMENT_END,
                                    "yalign", HIPPO_ALIGNMENT_START,
                                    "padding-right", 8,
                                    NULL);
    hippo_canvas_box_append(box, block->name_item, HIPPO_PACK_END);

    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "font-scale", PANGO_SCALE_SMALL,
                        "font", "Italic",
                        "text", "from ",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "xalign", HIPPO_ALIGNMENT_END,
                       "yalign", HIPPO_ALIGNMENT_START,
                       NULL);
    hippo_canvas_box_append(right_column, HIPPO_CANVAS_ITEM(box), 0);


    block->clicked_count_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                             "font-scale", PANGO_SCALE_SMALL,
                                             "text", NULL,
                                             NULL);
    hippo_canvas_box_append(box, block->clicked_count_item, HIPPO_PACK_END);

    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "font-scale", PANGO_SCALE_SMALL,
                        "text", " | ",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    
    block->age_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                   "font-scale", PANGO_SCALE_SMALL,
                                   NULL);
    hippo_canvas_box_append(box, block->age_item, HIPPO_PACK_END);
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_block_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_block_paint;
}

static void
hippo_canvas_block_class_init(HippoCanvasBlockClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_block_set_property;
    object_class->get_property = hippo_canvas_block_get_property;

    object_class->dispose = hippo_canvas_block_dispose;
    object_class->finalize = hippo_canvas_block_finalize;

    klass->set_block = hippo_canvas_block_set_block_impl;
    
    g_object_class_install_property(object_class,
                                    PROP_BLOCK,
                                    g_param_spec_object("block",
                                                        _("Block"),
                                                        _("Block to display"),
                                                        HIPPO_TYPE_BLOCK,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE)); 
}

static void
set_actions(HippoCanvasBlock *block,
            HippoActions     *actions)
{
    if (actions == block->actions)
        return;
    
    if (block->actions) {
        g_object_unref(block->actions);
        block->actions = NULL;
    }

    if (actions) {
        block->actions = actions;
        g_object_ref(block->actions);
    }

    g_object_set(G_OBJECT(block->headshot_item),
                 "actions", actions,
                 NULL);
    g_object_set(G_OBJECT(block->name_item),
                 "actions", actions,
                 NULL);
    
    g_object_notify(G_OBJECT(block), "actions");
}

static void
hippo_canvas_block_dispose(GObject *object)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object);

    hippo_canvas_block_set_block(block, NULL);

    set_actions(block, NULL);
    
    block->age_item = NULL;
    
    G_OBJECT_CLASS(hippo_canvas_block_parent_class)->dispose(object);
}

static void
hippo_canvas_block_finalize(GObject *object)
{
    /* HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object); */

    G_OBJECT_CLASS(hippo_canvas_block_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_block_new(HippoBlockType type,
                       HippoActions  *actions)
{
    HippoCanvasBlock *block;
    GType object_type;

    object_type = HIPPO_TYPE_CANVAS_BLOCK;
    switch (type) {
    case HIPPO_BLOCK_TYPE_UNKNOWN:
        object_type = HIPPO_TYPE_CANVAS_BLOCK;
        break;
    case HIPPO_BLOCK_TYPE_POST:
        object_type = HIPPO_TYPE_CANVAS_BLOCK_POST;
        break;
        /* FIXME add the other types */
    }

    block = g_object_new(object_type,
                         "actions", actions,
                         NULL);

    return HIPPO_CANVAS_ITEM(block);
}

static void
hippo_canvas_block_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasBlock *block;

    block = HIPPO_CANVAS_BLOCK(object);

    switch (prop_id) {
    case PROP_BLOCK:
        {
            HippoBlock *new_block = (HippoBlock*) g_value_get_object(value);
            hippo_canvas_block_set_block(block, new_block);
        }
        break;
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            set_actions(block, new_actions);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasBlock *block;

    block = HIPPO_CANVAS_BLOCK (object);

    switch (prop_id) {
    case PROP_BLOCK:
        g_value_set_object(value, (GObject*) block->block);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) block->actions);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_paint(HippoCanvasItem *item,
                         cairo_t         *cr)
{
    /* HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(item); */

    /* Draw the background and any children */
    item_parent_class->paint(item, cr);
}


static void
on_block_clicked_count_changed(HippoBlock *block,
                               GParamSpec *arg, /* null when we invoke callback manually */
                               void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    char *s;

    s = g_strdup_printf(_("%d views"), hippo_block_get_clicked_count(block));
    
    g_object_set(G_OBJECT(canvas_block->clicked_count_item),
                 "text", s,
                 NULL);
    g_free(s);
}

static void
on_block_timestamp_changed(HippoBlock *block,
                           GParamSpec *arg, /* null when we invoke callback manually */
                           void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    GTimeVal tv;
    GTime update_time;
    GTime server_time;
    GTime offset_time;
    GTime now;
    GTime then;
    char *when;

    if (block == NULL) /* should be impossible */
        return;
    
    g_get_current_time(&tv);

    update_time = hippo_block_get_update_time(block);
    server_time = (int) (hippo_block_get_server_timestamp(block) / 1000);
    offset_time = server_time - update_time;
    now = tv.tv_sec + offset_time;
    then = ((int) (hippo_block_get_timestamp(block) / 1000)) + offset_time;
    
    when = hippo_format_time_ago(now, then);

#if 0
    g_debug("Formatted offset %d now %d then %d delta %d seconds %d hours to '%s'",
            offset_time, now, then, now - then, (now - then) / (60*60),
            when);
#endif
    
    g_object_set(G_OBJECT(canvas_block->age_item),
                 "text", when,
                 NULL);

    g_free(when);

    /* FIXME update all the other info */
}

static void
hippo_canvas_block_set_block_impl(HippoCanvasBlock *canvas_block,
                                  HippoBlock       *new_block)
{
#if 0
    g_debug("setting block on canvas block to %s",
            new_block ? hippo_block_get_guid(new_block) : "null");
#endif
    
    if (new_block != canvas_block->block) {
        if (new_block) {
            if (canvas_block->required_type != HIPPO_BLOCK_TYPE_UNKNOWN &&
                hippo_block_get_block_type(new_block) != canvas_block->required_type) {
                g_warning("Trying to set block type %d on canvas block type %s",
                          hippo_block_get_block_type(new_block),
                          g_type_name_from_instance((GTypeInstance*)canvas_block));
                return;
            }
            
            g_object_ref(new_block);
            g_signal_connect(G_OBJECT(new_block), "notify::timestamp",
                             G_CALLBACK(on_block_timestamp_changed),
                             canvas_block);
            g_signal_connect(G_OBJECT(new_block), "notify::clicked-count",
                             G_CALLBACK(on_block_clicked_count_changed),
                             canvas_block);            
        }
        if (canvas_block->block) {
            g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                                 G_CALLBACK(on_block_timestamp_changed),
                                                 canvas_block);
            g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                                 G_CALLBACK(on_block_clicked_count_changed),
                                                 canvas_block);
            g_object_unref(canvas_block->block);
        }
        canvas_block->block = new_block;

        if (new_block) {
            on_block_timestamp_changed(new_block, NULL, canvas_block);
            on_block_clicked_count_changed(new_block, NULL, canvas_block);
        }
        
        g_object_notify(G_OBJECT(canvas_block), "block");
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(canvas_block));
    }
}

void
hippo_canvas_block_set_block(HippoCanvasBlock *canvas_block,
                             HippoBlock       *block)
{
    HippoCanvasBlockClass *klass;

    if (canvas_block->block == block)
        return;
    
    klass = HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block);
    
    (* klass->set_block) (canvas_block, block);
}

void
hippo_canvas_block_set_heading (HippoCanvasBlock *canvas_block,
                                const char       *heading)
{
    g_object_set(G_OBJECT(canvas_block->heading_text_item),
                 "text", heading,
                 NULL);
}

void
hippo_canvas_block_set_title(HippoCanvasBlock *canvas_block,
                             const char       *text)
{
    g_object_set(G_OBJECT(canvas_block->title_link_item),
                 "text", text,
                 NULL);
}

void
hippo_canvas_block_set_content(HippoCanvasBlock *canvas_block,
                               HippoCanvasItem  *content_item)
{
    if (content_item)
        g_object_ref(content_item); /* in case we remove it below */
    
    hippo_canvas_box_remove_all(HIPPO_CANVAS_BOX(canvas_block->content_container_item));

    if (content_item) {
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(canvas_block->content_container_item),
                                content_item, HIPPO_PACK_EXPAND);
        g_object_unref(content_item);
    }
}

void
hippo_canvas_block_set_sender(HippoCanvasBlock *canvas_block,
                              const char       *entity_guid)
{    
    if (entity_guid) {
        HippoEntity *entity;

        if (canvas_block->actions == NULL) {
            g_warning("setting block sender before setting actions");
            return;
        }
        
        entity = hippo_actions_lookup_entity(canvas_block->actions,
                                             entity_guid);
        if (entity == NULL) {
            g_warning("needed entity is unknown %s", entity_guid);
            return;
        }
        
        g_object_set(G_OBJECT(canvas_block->headshot_item),
                     "entity", entity,
                     NULL);
        
        g_object_set(G_OBJECT(canvas_block->name_item),
                     "entity", entity,
                     NULL);
    } else {        
        g_object_set(G_OBJECT(canvas_block->headshot_item),
                     "entity", NULL,
                     NULL);
        g_object_set(G_OBJECT(canvas_block->name_item),
                     "entity", NULL,
                     NULL);
    }
}

HippoActions*
hippo_canvas_block_get_actions(HippoCanvasBlock *canvas_block)
{
    return canvas_block->actions;
}
