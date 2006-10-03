/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-group-member.h"
#include "hippo-group.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_group_member_init                (HippoBlockGroupMember       *block_group_member);
static void      hippo_block_group_member_class_init          (HippoBlockGroupMemberClass  *klass);

static void      hippo_block_group_member_dispose             (GObject              *object);
static void      hippo_block_group_member_finalize            (GObject              *object);

static gboolean  hippo_block_group_member_update_from_xml     (HippoBlock           *block,
                                                               HippoDataCache       *cache,
                                                               LmMessageNode        *node);

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
    PROP_STATUS
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

    block_class->update_from_xml = hippo_block_group_member_update_from_xml;
    
    g_object_class_install_property(object_class,
                                    PROP_GROUP,
                                    g_param_spec_object("group",
                                                        _("Group"),
                                                        _("Group this block is about"),
                                                        HIPPO_TYPE_GROUP,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_GROUP,
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
}

static void
set_group(HippoBlockGroupMember *block_group_member,
          HippoGroup            *group)
{
    if (group == block_group_member->group)
        return;
    
    if (block_group_member->group) {
        g_object_unref(block_group_member->group);
        block_group_member->group = NULL;
    }

    if (group) {
        g_object_ref(group);
        block_group_member->group = group;
    }

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
        g_value_set_object(value, G_OBJECT(block_group_member->group));
        break;
    case PROP_MEMBER:
        g_value_set_object(value, G_OBJECT(block_group_member->member));
        break;
    case PROP_STATUS:
        g_value_set_int(value, block_group_member->status);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}


static gboolean
membership_status_from_string(const char            *s,
                              HippoMembershipStatus *result)
{
    static const struct { const char *name; HippoMembershipStatus status; } statuses[] = {
        { "NONMEMBER", HIPPO_MEMBERSHIP_STATUS_NONMEMBER },
        { "INVITED_TO_FOLLOW", HIPPO_MEMBERSHIP_STATUS_INVITED_TO_FOLLOW },
        { "FOLLOWER", HIPPO_MEMBERSHIP_STATUS_FOLLOWER },
        { "REMOVED", HIPPO_MEMBERSHIP_STATUS_REMOVED },
        { "INVITED", HIPPO_MEMBERSHIP_STATUS_INVITED },
        { "ACTIVE", HIPPO_MEMBERSHIP_STATUS_ACTIVE }
    };
    unsigned int i;
    for (i = 0; i < G_N_ELEMENTS(statuses); ++i) {
        if (strcmp(s, statuses[i].name) == 0) {
            *result = statuses[i].status;
            return TRUE;
        }
    }
    g_warning("Unknown membership status '%s'", s);
    return FALSE;
}

static gboolean
hippo_block_group_member_update_from_xml (HippoBlock           *block,
                                          HippoDataCache       *cache,
                                          LmMessageNode        *node)
{
    HippoBlockGroupMember *block_group_member = HIPPO_BLOCK_GROUP_MEMBER(block);
    LmMessageNode *group_node;
    HippoGroup *group;
    HippoPerson *member;
    const char *status_str;
    HippoMembershipStatus status;

    if (!HIPPO_BLOCK_CLASS(hippo_block_group_member_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "groupMember", HIPPO_SPLIT_NODE, &group_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, group_node, NULL,
                         "groupId", HIPPO_SPLIT_GROUP, &group,
                         "memberId", HIPPO_SPLIT_PERSON, &member,
                         "status", HIPPO_SPLIT_STRING, &status_str,
                         NULL))
        return FALSE;

    if (!membership_status_from_string(status_str, &status))
        return FALSE;

    set_group(block_group_member, group);
    set_member(block_group_member, member);
    set_status(block_group_member, status);

    return TRUE;
}
