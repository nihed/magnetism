/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-group.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-group-member.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>

static void      hippo_canvas_block_group_member_init                (HippoCanvasBlockGroupMember       *block);
static void      hippo_canvas_block_group_member_class_init          (HippoCanvasBlockGroupMemberClass  *klass);
static void      hippo_canvas_block_group_member_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_group_member_dispose             (GObject                *object);
static void      hippo_canvas_block_group_member_finalize            (GObject                *object);

static void hippo_canvas_block_group_member_set_property (GObject      *object,
                                                          guint         prop_id,
                                                          const GValue *value,
                                                          GParamSpec   *pspec);
static void hippo_canvas_block_group_member_get_property (GObject      *object,
                                                          guint         prop_id,
                                                          GValue       *value,
                                                          GParamSpec   *pspec);
static GObject* hippo_canvas_block_group_member_constructor (GType                  type,
                                                             guint                  n_construct_properties,
                                                             GObjectConstructParam *construct_properties);

/* Canvas block methods */
static void hippo_canvas_block_group_member_set_block       (HippoCanvasBlock *canvas_block,
                                                             HippoBlock       *block);

static void hippo_canvas_block_group_member_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_group_member_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_group_member_unexpand (HippoCanvasBlock *canvas_block);


struct _HippoCanvasBlockGroupMember {
    HippoCanvasBlock canvas_block;
};

struct _HippoCanvasBlockGroupMemberClass {
    HippoCanvasBlockClass parent_class;

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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockGroupMember, hippo_canvas_block_group_member, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_group_member_iface_init));

static void
hippo_canvas_block_group_member_init(HippoCanvasBlockGroupMember *block_group_member)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_group_member);

    block->required_type = HIPPO_BLOCK_TYPE_GROUP_MEMBER;
    block->expandable = FALSE; /* currently we have nothing to show on expand */
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_group_member_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_group_member_class_init(HippoCanvasBlockGroupMemberClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_group_member_set_property;
    object_class->get_property = hippo_canvas_block_group_member_get_property;
    object_class->constructor = hippo_canvas_block_group_member_constructor;

    object_class->dispose = hippo_canvas_block_group_member_dispose;
    object_class->finalize = hippo_canvas_block_group_member_finalize;

    canvas_block_class->set_block = hippo_canvas_block_group_member_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_group_member_title_activated;
    canvas_block_class->expand = hippo_canvas_block_group_member_expand;
    canvas_block_class->unexpand = hippo_canvas_block_group_member_unexpand;
}

static void
hippo_canvas_block_group_member_dispose(GObject *object)
{

    G_OBJECT_CLASS(hippo_canvas_block_group_member_parent_class)->dispose(object);
}

static void
hippo_canvas_block_group_member_finalize(GObject *object)
{
    /* HippoCanvasBlockGroupMember *block = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(object); */

    G_OBJECT_CLASS(hippo_canvas_block_group_member_parent_class)->finalize(object);
}

static void
hippo_canvas_block_group_member_set_property(GObject         *object,
                                             guint            prop_id,
                                             const GValue    *value,
                                             GParamSpec      *pspec)
{
    HippoCanvasBlockGroupMember *block_group_member;

    block_group_member = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_group_member_get_property(GObject         *object,
                                             guint            prop_id,
                                             GValue          *value,
                                             GParamSpec      *pspec)
{
    HippoCanvasBlockGroupMember *block_group_member;

    block_group_member = HIPPO_CANVAS_BLOCK_GROUP_MEMBER (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_block_group_member_constructor (GType                  type,
                                             guint                  n_construct_properties,
                                             GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_block_group_member_parent_class)->constructor(type,
                                                                                                n_construct_properties,
                                                                                                construct_properties);
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object);
    HippoCanvasBox *box;

    hippo_canvas_block_set_heading(block, _("Group: "));

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       NULL);

    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(box));

    return object;
}

static void
on_group_changed(HippoBlock *block,
                 GParamSpec *arg, /* null when first calling this */
                 HippoCanvasBlock *canvas_block)
{
    HippoGroup *group;
    HippoCanvasBlockGroupMember *canvas_group_member;

    canvas_group_member = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(canvas_block);
    g_assert(block == canvas_block->block);
    group = NULL;
    g_object_get(G_OBJECT(block), "group", &group, NULL);

    if (group == NULL) {
        /* can't think of much sensible to do here, presumably it will
         * get set back to non-null later
         */
    } else {
        hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(canvas_group_member),
                                      hippo_entity_get_guid(HIPPO_ENTITY(group)));
    }
}

static void
on_member_changed(HippoBlock *block,
                  GParamSpec *arg, /* null when first calling this */
                  HippoCanvasBlock *canvas_block)
{
    /* FIXME */
}

static void
on_status_changed(HippoBlock *block,
                  GParamSpec *arg, /* null when first calling this */
                  HippoCanvasBlock *canvas_block)
{
    /* FIXME */
}

static void
hippo_canvas_block_group_member_set_block(HippoCanvasBlock *canvas_block,
                                          HippoBlock       *block)
{
    /* g_debug("canvas-block-group-member set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_member_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_status_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_group_changed),
                                             canvas_block);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_member_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::member",
                         G_CALLBACK(on_member_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::group",
                         G_CALLBACK(on_group_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::status",
                         G_CALLBACK(on_status_changed),
                         canvas_block);        

        on_group_changed(canvas_block->block, NULL, canvas_block);
        on_member_changed(canvas_block->block, NULL, canvas_block);
        on_status_changed(canvas_block->block, NULL, canvas_block);
    }
}

static void
hippo_canvas_block_group_member_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoGroup *group;

    if (canvas_block->block == NULL)
        return;

    actions = hippo_canvas_block_get_actions(canvas_block);

    group = NULL;
    g_object_get(G_OBJECT(canvas_block->block),
                 "group", &group,
                 NULL);

    if (group == NULL)
        return;

    hippo_actions_visit_entity(actions, HIPPO_ENTITY(group));
}

static void
hippo_canvas_block_group_member_expand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockGroupMember *block_group_member = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_member_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_group_member_unexpand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockGroupMember *block_group_member = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_member_parent_class)->unexpand(canvas_block);
}
