/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-group.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-group-member.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-image-button.h>
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

/* Canvas block methods */
static void hippo_canvas_block_group_member_append_content_items (HippoCanvasBlock *canvas_block,
                                                                  HippoCanvasBox   *box);
static void hippo_canvas_block_group_member_set_block       (HippoCanvasBlock *canvas_block,
                                                             HippoBlock       *block);

static void hippo_canvas_block_group_member_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_group_member_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_group_member_unexpand (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_group_member_hush     (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_group_member_unhush   (HippoCanvasBlock *canvas_block);


struct _HippoCanvasBlockGroupMember {
    HippoCanvasBlock canvas_block;
    HippoCanvasItem *invite_image;
    HippoCanvasItem *invite_link;
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

    object_class->dispose = hippo_canvas_block_group_member_dispose;
    object_class->finalize = hippo_canvas_block_group_member_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_group_member_append_content_items;
    canvas_block_class->set_block = hippo_canvas_block_group_member_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_group_member_title_activated;
    canvas_block_class->expand = hippo_canvas_block_group_member_expand;
    canvas_block_class->unexpand = hippo_canvas_block_group_member_unexpand;
    canvas_block_class->hush = hippo_canvas_block_group_member_hush;
    canvas_block_class->unhush = hippo_canvas_block_group_member_unhush;
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

static void
on_invite_activated(HippoCanvasItem  *button_or_link,
                    HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoPerson *member;
    HippoGroup *group;

    actions = hippo_canvas_block_get_actions(canvas_block);
    
    g_object_get(G_OBJECT(canvas_block->block),
                 "member", &member,
                 "group", &group,
                 NULL);

    if (member && group && actions) {
        hippo_actions_invite_to_group(actions, group, member);
    }
    if (member)
        g_object_unref(member);
    if (group)
        g_object_unref(group);
}

static void
hippo_canvas_block_group_member_append_content_items (HippoCanvasBlock *block,
                                                      HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockGroupMember *canvas_group_member = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(block);
    HippoCanvasBox *invite_parent;
    
    hippo_canvas_block_set_heading(block, _("Group Update"));
    
    invite_parent = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                 "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                 "spacing", 4,
                                 NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(invite_parent), 0);
    
    canvas_group_member->invite_image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                                                     "normal-image-name", "add_icon",
                                                     "xalign", HIPPO_ALIGNMENT_CENTER,
                                                     "yalign", HIPPO_ALIGNMENT_CENTER,
                                                     NULL);
    hippo_canvas_box_append(invite_parent, canvas_group_member->invite_image, 0);

    g_signal_connect(G_OBJECT(canvas_group_member->invite_image),
                     "activated", G_CALLBACK(on_invite_activated), block);
    
    canvas_group_member->invite_link = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                                                    "text", "Invite to group",
                                                    "color-cascade", HIPPO_CASCADE_MODE_NONE,
                                                    NULL);
    hippo_canvas_box_append(invite_parent, canvas_group_member->invite_link, 0);

    g_signal_connect(G_OBJECT(canvas_group_member->invite_link),
                     "activated", G_CALLBACK(on_invite_activated), block);
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
        hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(canvas_group_member), HIPPO_ENTITY(group));
        g_object_unref(group);
    }
}

static void
update_member_and_status(HippoCanvasBlockGroupMember *canvas_group_member,
                         HippoBlock                  *block)
{
    HippoPerson *member;
    HippoMembershipStatus status;
    char *title;
    char *tooltip;
    gboolean show_invite_link;
    
    member = NULL;
    status = HIPPO_MEMBERSHIP_STATUS_NONMEMBER;
    g_object_get(G_OBJECT(block),
                 "member", &member,
                 "status", &status,
                 "viewer-can-invite", &show_invite_link,
                 NULL);

    title = NULL;
    tooltip = NULL;
    
    if (member != NULL) {
        const char *name;
        
        name = hippo_entity_get_name(HIPPO_ENTITY(member));
        
        switch (status) {
        case HIPPO_MEMBERSHIP_STATUS_NONMEMBER:
            title = g_strdup_printf("%s stopped following the group", name);
            break;
        case HIPPO_MEMBERSHIP_STATUS_INVITED_TO_FOLLOW:
            title = g_strdup_printf("%s is invited to be a follower", name);
            break;
        case HIPPO_MEMBERSHIP_STATUS_FOLLOWER:
            title = g_strdup_printf("%s is a new follower", name);
            break;
        case HIPPO_MEMBERSHIP_STATUS_REMOVED:
            title = g_strdup_printf("%s left the group", name);
            break;
        case HIPPO_MEMBERSHIP_STATUS_INVITED:
            title = g_strdup_printf("%s is invited to the group", name);
            break;
        case HIPPO_MEMBERSHIP_STATUS_ACTIVE:
            title = g_strdup_printf("%s is a new member", name);
            break;
        }
        
        tooltip = g_strdup_printf("Click to go to %s's page", name);
    }


    hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(canvas_group_member),
                                 title, tooltip, FALSE);

    hippo_canvas_item_set_visible(canvas_group_member->invite_image,
                                  show_invite_link);
    hippo_canvas_item_set_visible(canvas_group_member->invite_link,
                                  show_invite_link);

    if (member)
        g_object_unref(member);
}

static void
on_member_status_changed(HippoBlock *block,
                         GParamSpec *arg, /* null when first calling this */
                         HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGroupMember *canvas_group_member;

    canvas_group_member = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(canvas_block);
    g_assert(block == canvas_block->block);

    update_member_and_status(canvas_group_member, block);
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
                                             G_CALLBACK(on_member_status_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_group_changed),
                                             canvas_block);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_member_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::group",
                         G_CALLBACK(on_group_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::member",
                         G_CALLBACK(on_member_status_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::status",
                         G_CALLBACK(on_member_status_changed),
                         canvas_block);        
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::viewer-can-view",
                         G_CALLBACK(on_member_status_changed),
                         canvas_block);        

        on_group_changed(canvas_block->block, NULL, canvas_block);
        update_member_and_status(HIPPO_CANVAS_BLOCK_GROUP_MEMBER(canvas_block),
                                 canvas_block->block);
    }
}

static void
hippo_canvas_block_group_member_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoPerson *member;

    if (canvas_block->block == NULL)
        return;

    actions = hippo_canvas_block_get_actions(canvas_block);

    member = NULL;
    g_object_get(G_OBJECT(canvas_block->block),
                 "member", &member,
                 NULL);

    if (member == NULL)
        return;

    hippo_actions_visit_entity(actions, HIPPO_ENTITY(member));

    g_object_unref(member);
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

static void
hippo_canvas_block_group_member_hush(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGroupMember *block_group = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(canvas_block);
    
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_member_parent_class)->hush(canvas_block);

    g_object_set(G_OBJECT(block_group->invite_link),
                 "color-cascade", HIPPO_CASCADE_MODE_INHERIT,
                 NULL);
}

static void
hippo_canvas_block_group_member_unhush(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGroupMember *block_group = HIPPO_CANVAS_BLOCK_GROUP_MEMBER(canvas_block);
    
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_member_parent_class)->unhush(canvas_block);

    g_object_set(G_OBJECT(block_group->invite_link),
                 "color-cascade", HIPPO_CASCADE_MODE_NONE,
                 NULL);
}
