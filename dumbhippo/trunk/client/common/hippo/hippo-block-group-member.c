/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-group-member.h"
#include "hippo-group.h"
#include "hippo-person.h"
#include <string.h>

static void      hippo_block_group_member_init                (HippoBlockGroupMember       *block_group_member);
static void      hippo_block_group_member_class_init          (HippoBlockGroupMemberClass  *klass);

static void      hippo_block_group_member_dispose             (GObject              *object);
static void      hippo_block_group_member_finalize            (GObject              *object);

static void      hippo_block_group_member_update              (HippoBlock           *block);

static void hippo_block_group_member_set_property (GObject      *object,
                                                   guint         prop_id,
                                                   const GValue *value,
                                                   GParamSpec   *pspec);
static void hippo_block_group_member_get_property (GObject      *object,
                                                   guint         prop_id,
                                                   GValue       *value,
                                                   GParamSpec   *pspec);

struct _HippoBlockGroupMember {
    HippoBlock            parent;
    HippoGroup           *group;
    HippoPerson          *member;
    HippoMembershipStatus status;
    gboolean              viewer_can_invite;
};

struct _HippoBlockGroupMemberClass {
    HippoBlockClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_GROUP,
    PROP_MEMBER,
    PROP_STATUS,
    PROP_VIEWER_CAN_INVITE
};

G_DEFINE_TYPE(HippoBlockGroupMember, hippo_block_group_member, HIPPO_TYPE_BLOCK);
                       
static void
hippo_block_group_member_init(HippoBlockGroupMember *block_group_member)
{
}

static void
hippo_block_group_member_class_init(HippoBlockGroupMemberClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_group_member_set_property;
    object_class->get_property = hippo_block_group_member_get_property;

    object_class->dispose = hippo_block_group_member_dispose;
    object_class->finalize = hippo_block_group_member_finalize;

    block_class->update = hippo_block_group_member_update;
    
    g_object_class_install_property(object_class,
                                    PROP_GROUP,
                                    g_param_spec_object("group",
                                                        _("Group"),
                                                        _("Group this block is about"),
                                                        HIPPO_TYPE_GROUP,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_MEMBER,
                                    g_param_spec_object("member",
                                                        _("Member"),
                                                        _("Group member this group is about"),
                                                        HIPPO_TYPE_PERSON,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_STATUS,
                                    g_param_spec_int("status",
                                                     _("Status"),
                                                     _("Status of the user in the group"),
                                                     HIPPO_MEMBERSHIP_STATUS_NONMEMBER,
                                                     HIPPO_MEMBERSHIP_STATUS_ACTIVE,
                                                     HIPPO_MEMBERSHIP_STATUS_NONMEMBER,
                                                     G_PARAM_READABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_VIEWER_CAN_INVITE,
                                    g_param_spec_boolean("viewer-can-invite",
                                                         _("Viewer Can Invite"),
                                                         _("Is it useful for the viewer to invite the user to the group"),
                                                         FALSE,
                                                         G_PARAM_READABLE));
}

static void
set_viewer_can_invite(HippoBlockGroupMember *block_group_member,
                      gboolean               viewer_can_invite)
{
    viewer_can_invite = viewer_can_invite != FALSE;
    
    if (viewer_can_invite == block_group_member->viewer_can_invite)
        return;
    
    block_group_member->viewer_can_invite = viewer_can_invite;

    g_object_notify(G_OBJECT(block_group_member), "viewer-can-invite");
}

static void
update_viewer_can_invite(HippoBlockGroupMember *block_group_member)
{
    gboolean viewer_can_invite = FALSE;
    
    if (block_group_member->group != NULL) {
        HippoMembershipStatus viewer_status = hippo_group_get_status(block_group_member->group);
        if ((block_group_member->status == HIPPO_MEMBERSHIP_STATUS_FOLLOWER ||
             block_group_member->status == HIPPO_MEMBERSHIP_STATUS_INVITED_TO_FOLLOW) &&
            viewer_status == HIPPO_MEMBERSHIP_STATUS_ACTIVE)
        {
            viewer_can_invite = TRUE;
        }
    }

    set_viewer_can_invite(block_group_member, viewer_can_invite);
}

static void
on_group_changed(HippoGroup            *group,
                 HippoBlockGroupMember *block_group_member)
{
    update_viewer_can_invite(block_group_member);
}

static void
set_group(HippoBlockGroupMember *block_group_member,
          HippoGroup            *group)
{
    if (group == block_group_member->group)
        return;
    
    if (block_group_member->group) {
        g_signal_handlers_disconnect_by_func(block_group_member->group,
                                             (gpointer)on_group_changed,
                                             block_group_member);
        
        g_object_unref(block_group_member->group);
        block_group_member->group = NULL;
    }

    if (group) {
        g_object_ref(group);
        block_group_member->group = group;
        
        g_signal_connect(group, "changed",
                         G_CALLBACK(on_group_changed), block_group_member);
    }

    on_group_changed(group, block_group_member);

    g_object_notify(G_OBJECT(block_group_member), "group");
}

static void
set_member(HippoBlockGroupMember *block_group_member,
           HippoPerson           *member)
{
    if (member == block_group_member->member)
        return;
    
    if (block_group_member->member) {
        g_object_unref(block_group_member->member);
        block_group_member->member = NULL;
    }

    if (member) {
        g_object_ref(member);
        block_group_member->member = member;
    }

    g_object_notify(G_OBJECT(block_group_member), "member");
}

static void
set_status(HippoBlockGroupMember *block_group_member,
           HippoMembershipStatus  status)
{
    if (status == block_group_member->status)
        return;

    block_group_member->status = status;

    update_viewer_can_invite(block_group_member);
    
    g_object_notify(G_OBJECT(block_group_member), "status");
}

static void
hippo_block_group_member_dispose(GObject *object)
{
    HippoBlockGroupMember *block_group_member = HIPPO_BLOCK_GROUP_MEMBER(object);

    set_group(block_group_member, NULL);
    set_member(block_group_member, NULL);
    
    G_OBJECT_CLASS(hippo_block_group_member_parent_class)->dispose(object); 
}

static void
hippo_block_group_member_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_group_member_parent_class)->finalize(object); 
}

static void
hippo_block_group_member_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_block_group_member_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoBlockGroupMember *block_group_member = HIPPO_BLOCK_GROUP_MEMBER(object);

    switch (prop_id) {
    case PROP_GROUP:
        g_value_set_object(value, (GObject*) block_group_member->group);
        break;
    case PROP_MEMBER:
        g_value_set_object(value, (GObject*) block_group_member->member);
        break;
    case PROP_STATUS:
        g_value_set_int(value, block_group_member->status);
        break;
    case PROP_VIEWER_CAN_INVITE:
        g_value_set_boolean(value, block_group_member->viewer_can_invite);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_group_member_update (HippoBlock *block)
{
    HippoBlockGroupMember *block_group_member = HIPPO_BLOCK_GROUP_MEMBER(block);
    DDMDataResource *group_resource;
    DDMDataResource *member_resource;
    HippoGroup *group = NULL;
    HippoPerson *member = NULL;
    const char *status_str;
    HippoMembershipStatus status;

    HIPPO_BLOCK_CLASS(hippo_block_group_member_parent_class)->update(block);

    ddm_data_resource_get(block->resource,
                          "group", DDM_DATA_RESOURCE, &group_resource,
                          "member", DDM_DATA_RESOURCE, &member_resource,
                          "status", DDM_DATA_STRING, &status_str,
                          NULL);

    if (status_str == NULL || !hippo_membership_status_from_string(status_str, &status))
        status = HIPPO_MEMBERSHIP_STATUS_ACTIVE;

    if (group_resource != NULL)
        group = hippo_group_get_for_resource(group_resource);
    if (member_resource != NULL)
        member = hippo_person_get_for_resource(member_resource);
    
    set_group(block_group_member, group);
    set_member(block_group_member, member);
    set_status(block_group_member, status);

    if (group != NULL)
        g_object_unref(group);
    if (member != NULL)
        g_object_unref(member);
}
