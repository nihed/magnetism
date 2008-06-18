/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-internal.h"
#include "hippo-block-group-revision.h"
#include <hippo/hippo-group.h>
#include <hippo/hippo-person.h>
#include <string.h>

static void      hippo_block_group_revision_init                (HippoBlockGroupRevision       *block_group_revision);
static void      hippo_block_group_revision_class_init          (HippoBlockGroupRevisionClass  *klass);

static void      hippo_block_group_revision_dispose             (GObject              *object);
static void      hippo_block_group_revision_finalize            (GObject              *object);

static void      hippo_block_group_revision_update              (HippoBlock           *block);

static void hippo_block_group_revision_set_property (GObject      *object,
                                                   guint         prop_id,
                                                   const GValue *value,
                                                   GParamSpec   *pspec);
static void hippo_block_group_revision_get_property (GObject      *object,
                                                   guint         prop_id,
                                                   GValue       *value,
                                                   GParamSpec   *pspec);

struct _HippoBlockGroupRevision {
    HippoBlock parent;
    HippoGroup *group;
    char *edit_link;
};

struct _HippoBlockGroupRevisionClass {
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
    PROP_EDIT_LINK,
};

G_DEFINE_TYPE(HippoBlockGroupRevision, hippo_block_group_revision, HIPPO_TYPE_BLOCK);
                       
static void
hippo_block_group_revision_init(HippoBlockGroupRevision *block_group_revision)
{
}

static void
hippo_block_group_revision_class_init(HippoBlockGroupRevisionClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_group_revision_set_property;
    object_class->get_property = hippo_block_group_revision_get_property;

    object_class->dispose = hippo_block_group_revision_dispose;
    object_class->finalize = hippo_block_group_revision_finalize;

    block_class->update = hippo_block_group_revision_update;
    
    g_object_class_install_property(object_class,
                                    PROP_GROUP,
                                    g_param_spec_object("group",
                                                        _("Group"),
                                                        _("Group this block is about"),
                                                        HIPPO_TYPE_GROUP,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_EDIT_LINK,
                                    g_param_spec_string("edit-link",
                                                        _("Edit Link"),
                                                        _("Link to edit the group"),
                                                        NULL,
                                                        G_PARAM_READABLE));
}
static void
set_edit_link(HippoBlockGroupRevision *block_group_revision,
              const char              *edit_link)
{
    if (edit_link == block_group_revision->edit_link ||
        (edit_link != NULL && block_group_revision->edit_link != NULL && strcmp(edit_link, block_group_revision->edit_link) == 0))
        return;

    g_free(block_group_revision->edit_link);
    block_group_revision->edit_link = g_strdup(edit_link);

    g_object_notify(G_OBJECT(block_group_revision), "edit-link");
}

static void
update_edit_link(HippoBlockGroupRevision *block_group_revision)
{
    HippoBlock *block = HIPPO_BLOCK(block_group_revision);
    const char *edit_link = NULL;

    /* Only active members can edit */
    if (block_group_revision->group != NULL &&
        hippo_group_get_status(block_group_revision->group) >= HIPPO_MEMBERSHIP_STATUS_ACTIVE)
    {
        ddm_data_resource_get(block->resource,
                              "editLink", DDM_DATA_URL, &edit_link,
                              NULL);
    }

    set_edit_link(block_group_revision, edit_link);
}

static void
update_chat_id(HippoBlockGroupRevision *block_group_revision)
{
    HippoBlock *block = HIPPO_BLOCK(block_group_revision);
    const char *chat_id = NULL;

    /* Only invited and active members can chat */
    if (block_group_revision->group != NULL &&
        hippo_group_get_status(block_group_revision->group) >= HIPPO_MEMBERSHIP_STATUS_INVITED)
    {
        ddm_data_resource_get(block->resource,
                              "chatId", DDM_DATA_STRING, &chat_id,
                              NULL);
    }

    hippo_block_set_chat_id(block, chat_id);
}

static void
on_group_changed(HippoGroup              *group,
                 HippoBlockGroupRevision *block_group_revision)
{
    update_edit_link(block_group_revision);
    update_chat_id(block_group_revision);
}

static void
set_group(HippoBlockGroupRevision *block_group_revision,
          HippoGroup              *group)
{
    if (group == block_group_revision->group)
        return;
    
    if (block_group_revision->group) {
        g_signal_handlers_disconnect_by_func(block_group_revision->group,
                                             (gpointer)on_group_changed,
                                             block_group_revision);
        
        g_object_unref(block_group_revision->group);
        block_group_revision->group = NULL;
    }

    if (group) {
        g_object_ref(group);
        block_group_revision->group = group;
        
        g_signal_connect(group, "changed",
                         G_CALLBACK(on_group_changed), block_group_revision);
    }

    on_group_changed(group, block_group_revision);

    g_object_notify(G_OBJECT(block_group_revision), "group");
}

static void
hippo_block_group_revision_dispose(GObject *object)
{
    HippoBlockGroupRevision *block_group_revision = HIPPO_BLOCK_GROUP_REVISION(object);

    set_group(block_group_revision, NULL);
    set_edit_link(block_group_revision, NULL);
    
    G_OBJECT_CLASS(hippo_block_group_revision_parent_class)->dispose(object); 
}

static void
hippo_block_group_revision_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_group_revision_parent_class)->finalize(object); 
}

static void
hippo_block_group_revision_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_block_group_revision_get_property(GObject         *object,
                                        guint            prop_id,
                                        GValue          *value,
                                        GParamSpec      *pspec)
{
    HippoBlockGroupRevision *block_group_revision = HIPPO_BLOCK_GROUP_REVISION(object);

    switch (prop_id) {
    case PROP_GROUP:
        g_value_set_object(value, (GObject*) block_group_revision->group);
        break;
    case PROP_EDIT_LINK:
        g_value_set_string(value, block_group_revision->edit_link);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_group_revision_update (HippoBlock *block)
{
    HippoBlockGroupRevision *block_group_revision = HIPPO_BLOCK_GROUP_REVISION(block);
    DDMDataResource *group_resource;
    DDMDataResource *revision_resource;
    HippoGroup *group = NULL;

    HIPPO_BLOCK_CLASS(hippo_block_group_revision_parent_class)->update(block);

    ddm_data_resource_get(block->resource,
                          "group", DDM_DATA_RESOURCE, &group_resource,
                          "revision", DDM_DATA_RESOURCE, &revision_resource,
                          NULL);

    if (group_resource != NULL)
        group = hippo_group_get_for_resource(group_resource);
    
    set_group(block_group_revision, group);
    update_edit_link(block_group_revision);
    update_chat_id(block_group_revision);

    if (group != NULL)
        g_object_unref(group);
}
