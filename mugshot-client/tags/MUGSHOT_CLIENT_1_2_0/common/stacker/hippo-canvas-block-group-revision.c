/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-internal.h"
#include <hippo/hippo-group.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-generic.h"
#include "hippo-canvas-block-group-revision.h"
#include "hippo-canvas-entity-name.h"
#include "hippo-canvas-url-link.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-image-button.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>

static void      hippo_canvas_block_group_revision_init                (HippoCanvasBlockGroupRevision       *block);
static void      hippo_canvas_block_group_revision_class_init          (HippoCanvasBlockGroupRevisionClass  *klass);
static void      hippo_canvas_block_group_revision_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_group_revision_dispose             (GObject                *object);
static void      hippo_canvas_block_group_revision_finalize            (GObject                *object);

/* Canvas block methods */
static void hippo_canvas_block_group_revision_append_content_items (HippoCanvasBlock *canvas_block,
                                                                    HippoCanvasBox   *box);
static void hippo_canvas_block_group_revision_append_right_items   (HippoCanvasBlock *canvas_block,
                                                                    HippoCanvasBox   *parent_box);

static void hippo_canvas_block_group_revision_set_block       (HippoCanvasBlock *canvas_block,
                                                             HippoBlock       *block);

static void hippo_canvas_block_group_revision_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_group_revision_unexpand (HippoCanvasBlock *canvas_block);


struct _HippoCanvasBlockGroupRevision {
    HippoCanvasBlockGeneric parent_instance;
    HippoCanvasItem *edit_link;
    HippoCanvasItem *change_item;
};

struct _HippoCanvasBlockGroupRevisionClass {
    HippoCanvasBlockGenericClass parent_class;

};

#if 0
enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0
};
#endif

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockGroupRevision, hippo_canvas_block_group_revision, HIPPO_TYPE_CANVAS_BLOCK_GENERIC,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_group_revision_iface_init));

static void
hippo_canvas_block_group_revision_init(HippoCanvasBlockGroupRevision *block_group_revision)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_group_revision);

    block->required_type = HIPPO_BLOCK_TYPE_GROUP_REVISION;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_group_revision_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_group_revision_class_init(HippoCanvasBlockGroupRevisionClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->dispose = hippo_canvas_block_group_revision_dispose;
    object_class->finalize = hippo_canvas_block_group_revision_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_group_revision_append_content_items;
    canvas_block_class->append_right_items = hippo_canvas_block_group_revision_append_right_items;
    canvas_block_class->set_block = hippo_canvas_block_group_revision_set_block;
    canvas_block_class->expand = hippo_canvas_block_group_revision_expand;
    canvas_block_class->unexpand = hippo_canvas_block_group_revision_unexpand;
}

static void
hippo_canvas_block_group_revision_dispose(GObject *object)
{

    G_OBJECT_CLASS(hippo_canvas_block_group_revision_parent_class)->dispose(object);
}

static void
hippo_canvas_block_group_revision_finalize(GObject *object)
{
    /* HippoCanvasBlockGroupRevision *block = HIPPO_CANVAS_BLOCK_GROUP_REVISION(object); */

    G_OBJECT_CLASS(hippo_canvas_block_group_revision_parent_class)->finalize(object);
}

static void
hippo_canvas_block_group_revision_append_content_items (HippoCanvasBlock *block,
                                                        HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockGroupRevision *canvas_group_revision = HIPPO_CANVAS_BLOCK_GROUP_REVISION(block);
    HippoCanvasBox *edit_parent;

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_revision_parent_class)->append_content_items(block, parent_box);
    
    hippo_canvas_block_set_heading(block, _("Group Change"));
    
    edit_parent = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                               "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                               "spacing", 4,
                               NULL);
    hippo_canvas_box_insert_after(parent_box, HIPPO_CANVAS_ITEM(edit_parent),
                                  HIPPO_CANVAS_BLOCK_GENERIC(block)->reason_item, 0);
    
    canvas_group_revision->edit_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                                    "text", _("edit the group"),
                                                    "tooltip", _("Make more changes to the group"),
                                                    NULL);
    hippo_canvas_box_append(edit_parent, canvas_group_revision->edit_link, 0);
}

static void
hippo_canvas_block_group_revision_append_right_items(HippoCanvasBlock *canvas_block,
                                                     HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockGroupRevision *block_group_revision = HIPPO_CANVAS_BLOCK_GROUP_REVISION(canvas_block);
    HippoCanvasBox *change_box;
    HippoCanvasBox *name_box;
    HippoCanvasBox *right_box;
    HippoCanvasItem *change_item_intro;
    
    if (HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_revision_parent_class)->append_right_items)
        HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_revision_parent_class)->append_right_items(canvas_block, parent_box);

    /* Ugly... we insert the item into the box on the right after the name of the sender */
    
    name_box = HIPPO_CANVAS_BOX(hippo_canvas_item_get_parent(canvas_block->name_item));
    right_box= HIPPO_CANVAS_BOX(hippo_canvas_item_get_parent(HIPPO_CANVAS_ITEM(name_box)));

    change_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                              "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                              "xalign", HIPPO_ALIGNMENT_END,
                              NULL);
    
    hippo_canvas_box_insert_after(right_box, HIPPO_CANVAS_ITEM(change_box),
                                  HIPPO_CANVAS_ITEM(name_box), 0);

    change_item_intro = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                     "font", "12px",
                                     "text", _("change to "),
                                     NULL);
    hippo_canvas_box_append(change_box, change_item_intro, 0);
    
    block_group_revision->change_item = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_NAME,
                                                     NULL);
    hippo_canvas_box_append(change_box, block_group_revision->change_item, 0);
}

static void
on_group_changed(HippoBlock *block,
                 GParamSpec *arg, /* null when first calling this */
                 HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGroupRevision *canvas_group_revision = HIPPO_CANVAS_BLOCK_GROUP_REVISION(canvas_block);
    HippoGroup *group;

    g_assert(block == canvas_block->block);
    group = NULL;
    g_object_get(G_OBJECT(block), "group", &group, NULL);

    if (group == NULL) {
        /* can't think of much sensible to do here, presumably it will
         * get set back to non-null later
         */
    } else {
        g_object_set(canvas_group_revision->change_item,
                     "entity", group,
                     NULL);
        g_object_unref(group);
    }
}

static void
on_edit_link_changed(HippoBlock *block,
                     GParamSpec *arg, /* null when first calling this */
                     HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGroupRevision *canvas_group_revision = HIPPO_CANVAS_BLOCK_GROUP_REVISION(canvas_block);
    char *edit_link;

    g_object_get(G_OBJECT(block),
                 "edit-link", &edit_link,
                 NULL);

    hippo_canvas_item_set_visible(canvas_group_revision->edit_link,
                                  edit_link != NULL);
    g_object_set(canvas_group_revision->edit_link,
                 "url", edit_link,
                 NULL);

    g_free(edit_link);
}

static void
hippo_canvas_block_group_revision_set_block(HippoCanvasBlock *canvas_block,
                                          HippoBlock       *block)
{
    /* g_debug("canvas-block-group-member set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_edit_link_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_group_changed),
                                             canvas_block);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_revision_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::group",
                         G_CALLBACK(on_group_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::edit-link",
                         G_CALLBACK(on_edit_link_changed),
                         canvas_block);        

        on_group_changed(canvas_block->block, NULL, canvas_block);
        on_edit_link_changed(canvas_block->block, NULL, canvas_block);
    }
}

static void
hippo_canvas_block_group_revision_expand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockGroupRevision *block_group_revision = HIPPO_CANVAS_BLOCK_GROUP_REVISION(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_revision_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_group_revision_unexpand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockGroupRevision *block_group_revision = HIPPO_CANVAS_BLOCK_GROUP_REVISION(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_revision_parent_class)->unexpand(canvas_block);
}
