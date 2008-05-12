/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-internal.h"
#include "hippo-block.h"
#include "hippo-block-account-question.h"
#include "hippo-block-generic.h"
#include "hippo-block-group-chat.h"
#include "hippo-block-group-member.h"
#include "hippo-block-group-revision.h"
#include "hippo-block-post.h"
#include "hippo-block-netflix-movie.h"
#include "hippo-block-amazon-activity.h"
#include <string.h>

static void hippo_block_update (HippoBlock *block);

static void     hippo_block_finalize             (GObject *object);

static void hippo_block_set_property (GObject      *object,
                                      guint         prop_id,
                                      const GValue *value,
                                      GParamSpec   *pspec);
static void hippo_block_get_property (GObject      *object,
                                      guint         prop_id,
                                      GValue       *value,
                                      GParamSpec   *pspec);

static void hippo_block_real_update (HippoBlock *block);

static void on_block_resource_changed (DDMDataResource *resource,
                                       GSList          *changed_properties,
                                       gpointer         data);

G_DEFINE_TYPE(HippoBlock, hippo_block, G_TYPE_OBJECT);

/*
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
*/

enum {
    PROP_0,
    PROP_GUID,
    PROP_BLOCK_TYPE,
    PROP_IS_PUBLIC,
    PROP_SOURCE,    
    PROP_SORT_TIMESTAMP,
    PROP_TIMESTAMP,
    PROP_CLICKED_TIMESTAMP,
    PROP_IGNORED_TIMESTAMP,
    PROP_SIGNIFICANT_CLICKED_COUNT,
    PROP_CHAT_ID,
    PROP_CLICKED,
    PROP_ICON_URL,
    PROP_IGNORED,
    PROP_PINNED,
    PROP_TITLE,
    PROP_TITLE_LINK,
    PROP_STACK_REASON,
    PROP_IS_FEED,
    PROP_IS_MINE
};

static void
hippo_block_init(HippoBlock *block)
{
    block->stack_reason = HIPPO_STACK_NEW_BLOCK;
}

static void
hippo_block_class_init(HippoBlockClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->finalize = hippo_block_finalize;

    object_class->set_property = hippo_block_set_property;
    object_class->get_property = hippo_block_get_property;

    klass->update = hippo_block_real_update;

    g_object_class_install_property(object_class,
                                    PROP_GUID,
                                    g_param_spec_string("guid",
                                                        _("GUID"),
                                                        _("GUID of the block"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_BLOCK_TYPE,
                                    g_param_spec_int("block-type",
                                                     _("Type"),
                                                     _("What kind of block this is"),
                                                     0, G_MAXINT,
                                                     HIPPO_BLOCK_TYPE_UNKNOWN,
                                                     G_PARAM_READABLE));
                                                     
    g_object_class_install_property(object_class,
                                    PROP_SOURCE,
                                    g_param_spec_object("source",
                                                        _("Source"),
                                                        _("The entity which originated this block"),
                                                         HIPPO_TYPE_ENTITY,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));  
    
    g_object_class_install_property(object_class,
                                    PROP_IS_PUBLIC,
                                    g_param_spec_boolean("is-public",
                                                         _("Publicity"),
                                                         _("Whether or not this block is public"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_SORT_TIMESTAMP,
                                    g_param_spec_int64("sort-timestamp",
                                                       _("Sort timestamp"),
                                                       _("Timestamp to sort by"),
                                                       -1, G_MAXINT64,
                                                       0,
                                                       G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_TIMESTAMP,
                                    g_param_spec_int64("timestamp",
                                                       _("Timestamp"),
                                                       _("When the block last changed"),
                                                       -1, G_MAXINT64,
                                                       0,
                                                       G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_CLICKED_TIMESTAMP,
                                    g_param_spec_int64("clicked-timestamp",
                                                       _("Clicked timestamp"),
                                                       _("When the block was clicked by us"),
                                                       -1, G_MAXINT64,
                                                       0,
                                                       G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_IGNORED_TIMESTAMP,
                                    g_param_spec_int64("ignored-timestamp",
                                                       _("Ignored timestamp"),
                                                       _("When the block was ignored by us"),
                                                       -1, G_MAXINT64,
                                                       0,
                                                       G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_SIGNIFICANT_CLICKED_COUNT,
                                    g_param_spec_int("significant-clicked-count",
                                                     _("Significant clicked count"),
                                                     _("Last click count that caused a restack"),
                                                     0, G_MAXINT,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_CHAT_ID,
                                    g_param_spec_string("chat-id",
                                                        _("Chat ID"),
                                                        _("GUID of the block's chat room, or NULL"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_CLICKED,
                                    g_param_spec_boolean("clicked",
                                                         _("Clicked"),
                                                         _("Whether we clicked on the block"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_IGNORED,
                                    g_param_spec_boolean("ignored",
                                                         _("Ignored"),
                                                         _("Whether we ignored the block"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_PINNED,
                                    g_param_spec_boolean("pinned",
                                                         _("Pinned"),
                                                         _("Whether the block is a pinned message"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_ICON_URL,
                                    g_param_spec_string("icon-url",
                                                        _("Icon URL"),
                                                        _("URL for block 'favicon'"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_TITLE,
                                    g_param_spec_string("title",
                                                        _("Title"),
                                                        _("Block title"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_TITLE_LINK,
                                    g_param_spec_string("title-link",
                                                        _("Title link"),
                                                        _("Block title link"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_STACK_REASON,
                                    g_param_spec_int("stack-reason",
                                                     _("Stack Reason"),
                                                     _("Reason code for why the block was restacked"),
                                                     0, G_MAXINT, HIPPO_STACK_NEW_BLOCK,
                                                     G_PARAM_READABLE));
                                                     
   g_object_class_install_property(object_class,
                                   PROP_IS_FEED,
                                   g_param_spec_boolean("is-feed",
                                                        _("Feed"),
                                                        _("Whether or not this block is a feed block"),
                                                   	    FALSE,
                                                        G_PARAM_READABLE));
                                                        
	g_object_class_install_property(object_class,
                                   PROP_IS_MINE,
                                   g_param_spec_boolean("is-mine",
                                                        _("Mine"),
                                                        _("Whether or not this block originates from the user directly"),
                                                   	    FALSE,
                                                        G_PARAM_READABLE));   
}

static void
hippo_block_finalize(GObject *object)
{
    HippoBlock *block = HIPPO_BLOCK(object);

    g_free(block->guid);
    g_free(block->icon_url);
    g_free(block->title);
    g_free(block->title_link);
    g_free(block->chat_id);

    ddm_data_resource_disconnect(block->resource, on_block_resource_changed, block);

    G_OBJECT_CLASS(hippo_block_parent_class)->finalize(object); 
}

static void
hippo_block_set_property(GObject         *object,
                         guint            prop_id,
                         const GValue    *value,
                         GParamSpec      *pspec)
{
    HippoBlock *block;

    block = HIPPO_BLOCK(object);

    switch (prop_id) {
    case PROP_TIMESTAMP:
        hippo_block_set_timestamp(block,
                                  g_value_get_int64(value));
        break;
    case PROP_CLICKED_TIMESTAMP:
        hippo_block_set_clicked_timestamp(block,
                                          g_value_get_int64(value));
        break;
    case PROP_IGNORED_TIMESTAMP:
        hippo_block_set_ignored_timestamp(block,
                                          g_value_get_int64(value));        
        break;
    case PROP_SIGNIFICANT_CLICKED_COUNT:
        hippo_block_set_significant_clicked_count(block,
                                                  g_value_get_int(value));
        break;
    case PROP_CHAT_ID:
        hippo_block_set_chat_id(block,
                                g_value_get_string(value));
        break;
    case PROP_CLICKED:
        hippo_block_set_clicked(block,
                                g_value_get_boolean(value));
        break;
    case PROP_IGNORED:
        hippo_block_set_ignored(block,
                                g_value_get_boolean(value));        
        break;
    case PROP_PINNED:
        hippo_block_set_pinned(block,
                                g_value_get_boolean(value));        
        break;
    case PROP_STACK_REASON:
        hippo_block_set_stack_reason(block,
                                     g_value_get_int(value));
        break;
    case PROP_TITLE:
        hippo_block_set_title(block, 
                              g_value_get_string(value));
        break;
    case PROP_TITLE_LINK:
        hippo_block_set_title_link(block,
                                   g_value_get_string(value));
        break;
    case PROP_SOURCE:
        hippo_block_set_source(block,
                               g_value_get_object(value));
        break;
    case PROP_GUID:                  /* read-only */
    case PROP_BLOCK_TYPE:            /* read-only */
    case PROP_SORT_TIMESTAMP:        /* read-only */
    case PROP_ICON_URL:              /* read-only */
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_get_property(GObject         *object,
                         guint            prop_id,
                         GValue          *value,
                         GParamSpec      *pspec)
{
    HippoBlock *block;

    block = HIPPO_BLOCK(object);

    switch (prop_id) {
    case PROP_GUID:
        g_value_set_string(value, block->guid);
        break;
    case PROP_BLOCK_TYPE:
        g_value_set_int(value, block->type);
        break;
    case PROP_SOURCE:
        g_value_set_object(value, block->source);
        break;
    case PROP_SORT_TIMESTAMP:
        g_value_set_int64(value,
                          hippo_block_get_sort_timestamp(block));
        break;
    case PROP_TIMESTAMP:
        g_value_set_int64(value, block->timestamp);
        break;
    case PROP_CLICKED_TIMESTAMP:
        g_value_set_int64(value, block->clicked_timestamp);
        break;
    case PROP_IGNORED_TIMESTAMP:
        g_value_set_int64(value, block->ignored_timestamp);
        break;
    case PROP_SIGNIFICANT_CLICKED_COUNT:
        g_value_set_int(value, block->significant_clicked_count);
        break;
    case PROP_CHAT_ID:
        g_value_set_string(value, block->chat_id);
        break;
    case PROP_CLICKED:
        g_value_set_boolean(value, block->clicked);
        break;
    case PROP_IGNORED:
        g_value_set_boolean(value, block->ignored);
        break;
    case PROP_PINNED:
        g_value_set_boolean(value, block->pinned);
        break;
    case PROP_STACK_REASON:
        g_value_set_int(value, block->stack_reason);
        break;
    case PROP_ICON_URL:
        g_value_set_string(value, block->icon_url);
        break;
    case PROP_TITLE:
        g_value_set_string(value, block->title);
        break;
    case PROP_TITLE_LINK:
        g_value_set_string(value, block->title_link);
        break;
    case PROP_IS_FEED:
        g_value_set_boolean(value, block->is_feed);
        break;
    case PROP_IS_MINE:
        g_value_set_boolean(value, block->is_mine);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static HippoStackReason
stack_reason_from_string(const char *string) 
{
    if (strcmp(string, "NEW_BLOCK") == 0)
        return HIPPO_STACK_NEW_BLOCK;
    else if (strcmp(string, "BLOCK_UPDATE") == 0)
        return HIPPO_STACK_BLOCK_UPDATE;
    else if (strcmp(string, "VIEWER_COUNT") == 0)
        return HIPPO_STACK_VIEWER_COUNT;
    else if (strcmp(string, "CHAT_MESSAGE") == 0)
        return HIPPO_STACK_CHAT_MESSAGE;
    else {
        g_warning("Unknown stack reason %s", string);
        return HIPPO_STACK_NEW_BLOCK;
    }
}

static void
hippo_block_real_update (HippoBlock     *block)
{
    gboolean public;
    gboolean owner_content;
    gint64 timestamp;
    gint64 clicked_timestamp;
    gint64 ignored_timestamp;
    int significant_clicked_count = 0;
    const char *title;
    const char *title_link;
    const char *icon;
    const char *stack_reason_str;
    const char *chat_id;
    DDMDataModel *model;
    DDMDataResource *self;
    DDMDataResource *owner;
    DDMDataResource *source_user;
    
    HippoStackReason stack_reason = HIPPO_STACK_NEW_BLOCK;

    model = ddm_data_resource_get_model(block->resource);
    self = ddm_data_model_get_self_resource(model);

    ddm_data_resource_get(block->resource,
                          "public", DDM_DATA_BOOLEAN, &public,
                          "ownerContent", DDM_DATA_BOOLEAN, &owner_content,
                          "timestamp", DDM_DATA_LONG, &timestamp,
                          "clickedTimestamp", DDM_DATA_LONG, &clicked_timestamp,
                          "ignoredTimestamp", DDM_DATA_LONG, &ignored_timestamp,
                          "significantClickedCount", DDM_DATA_INTEGER, &significant_clicked_count,
                          "icon", DDM_DATA_URL, &icon,
                          "title", DDM_DATA_STRING, &title,
                          "titleLink", DDM_DATA_URL, &title_link,
                          "stackReason", DDM_DATA_STRING, &stack_reason_str,
                          "chatId", DDM_DATA_STRING, &chat_id,
                          "owner", DDM_DATA_RESOURCE, &owner,
                          "sourceUser", DDM_DATA_RESOURCE, &source_user,
                          NULL);
                            
    hippo_block_set_public(block, public);
    hippo_block_set_timestamp(block, timestamp);
    hippo_block_set_clicked_timestamp(block, clicked_timestamp);
    hippo_block_set_ignored_timestamp(block, ignored_timestamp);
    hippo_block_set_significant_clicked_count(block, significant_clicked_count);
    hippo_block_set_chat_id(block, chat_id);
    hippo_block_set_clicked(block, clicked_timestamp >= 0);
    hippo_block_set_ignored(block, ignored_timestamp >= 0);
    
    hippo_block_set_is_mine(block, owner == self && owner_content);

    hippo_block_set_icon_url(block, icon);
    hippo_block_set_title(block, title);
    hippo_block_set_title_link(block, title_link);

    if (source_user != NULL) {
        HippoPerson *person = hippo_person_get_for_resource(source_user);
        hippo_block_set_source(block, HIPPO_ENTITY(person));
        g_object_unref(person); 
    } else {
        hippo_block_set_source(block, NULL);
    }

    if (stack_reason_str)
        stack_reason = stack_reason_from_string(stack_reason_str);
    hippo_block_set_stack_reason(block, stack_reason);
}

/* === HippoBlock exported API === */

static void
on_block_resource_changed (DDMDataResource *resource,
                           GSList          *changed_properties,
                           gpointer         data)
{
    hippo_block_update(data);
}

static HippoBlock*
hippo_block_new(DDMDataResource *resource,
                const char      *guid,
                HippoBlockType   type)
{
    HippoBlock *block;
    GType object_type;

    object_type = HIPPO_TYPE_BLOCK;
    switch (type) {
    case HIPPO_BLOCK_TYPE_UNKNOWN:
        object_type = HIPPO_TYPE_BLOCK;
        break;
    case HIPPO_BLOCK_TYPE_ACCOUNT_QUESTION:
        object_type = HIPPO_TYPE_BLOCK_ACCOUNT_QUESTION;
        break;
    case HIPPO_BLOCK_TYPE_GROUP_CHAT:
        object_type = HIPPO_TYPE_BLOCK_GROUP_CHAT;
        break;
    case HIPPO_BLOCK_TYPE_GROUP_MEMBER:
        object_type = HIPPO_TYPE_BLOCK_GROUP_MEMBER;
        break;
    case HIPPO_BLOCK_TYPE_GROUP_REVISION:
        object_type = HIPPO_TYPE_BLOCK_GROUP_REVISION;
        break;
    case HIPPO_BLOCK_TYPE_POST:
        object_type = HIPPO_TYPE_BLOCK_POST;
        break;
    case HIPPO_BLOCK_TYPE_MUSIC_CHAT:
    case HIPPO_BLOCK_TYPE_MUSIC_PERSON:
        object_type = HIPPO_TYPE_BLOCK;
        break;
    case HIPPO_BLOCK_TYPE_FACEBOOK_EVENT:
    case HIPPO_BLOCK_TYPE_FLICKR_PERSON:
    case HIPPO_BLOCK_TYPE_FLICKR_PHOTOSET:
    case HIPPO_BLOCK_TYPE_YOUTUBE_PERSON:
        object_type = HIPPO_TYPE_BLOCK_GENERIC;
        break;
    case HIPPO_BLOCK_TYPE_NETFLIX_MOVIE:
        object_type = HIPPO_TYPE_BLOCK_NETFLIX_MOVIE;
        break;        
    case HIPPO_BLOCK_TYPE_AMAZON_REVIEW:
    case HIPPO_BLOCK_TYPE_AMAZON_WISH_LIST_ITEM:
        object_type = HIPPO_TYPE_BLOCK_AMAZON_ACTIVITY;
        break;        
    case HIPPO_BLOCK_TYPE_GENERIC:
        object_type = HIPPO_TYPE_BLOCK_GENERIC;
        break;
        /* don't add default case, it hides warnings */
    }
    
    block = g_object_new(object_type,
                         NULL);
    block->type = type;
    block->resource = ddm_data_resource_ref(resource);
    block->guid = g_strdup(guid);

    ddm_data_resource_connect(resource, NULL, on_block_resource_changed, block);
    hippo_block_update(block);
    
    return block;
}

DDMDataResource *
hippo_block_get_resource (HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), NULL);

    return block->resource;
}

static void
hippo_block_update (HippoBlock *block)
{
    g_object_freeze_notify(G_OBJECT(block));

    HIPPO_BLOCK_GET_CLASS(block)->update(block);

    g_object_thaw_notify(G_OBJECT(block));
}

const char*
hippo_block_get_guid(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), NULL);
    return block->guid;
}

HippoBlockType
hippo_block_get_block_type(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), HIPPO_BLOCK_TYPE_UNKNOWN);
    return block->type;
}

gint64
hippo_block_get_timestamp(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    return block->timestamp;
}

void
hippo_block_set_timestamp (HippoBlock *block,
                           gint64      value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->timestamp) {
        block->timestamp = value;
        g_object_notify(G_OBJECT(block), "timestamp");
        g_object_notify(G_OBJECT(block), "sort-timestamp");
    }
}

gint64
hippo_block_get_clicked_timestamp (HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    return block->clicked_timestamp;
}

void
hippo_block_set_clicked_timestamp (HippoBlock *block,
                                   gint64      value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->clicked_timestamp) {
        block->clicked_timestamp = value;
        g_object_notify(G_OBJECT(block), "clicked-timestamp");
    }
}

gint64
hippo_block_get_ignored_timestamp (HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    return block->ignored_timestamp;
}

void
hippo_block_set_ignored_timestamp (HippoBlock *block,
                                   gint64      value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->ignored_timestamp) {
        block->ignored_timestamp = value;
        g_object_notify(G_OBJECT(block), "ignored-timestamp");
        g_object_notify(G_OBJECT(block), "sort-timestamp");
    }
}

gint64
hippo_block_get_sort_timestamp(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), -1);

    if (block->ignored)
        return block->ignored_timestamp;
    else
        return block->timestamp;
}

int
hippo_block_compare_newest_first  (gconstpointer a,
                                   gconstpointer b)
{
    HippoBlock *block_a = (HippoBlock*) a;
    HippoBlock *block_b = (HippoBlock*) b;
    gint64 stamp_a = hippo_block_get_sort_timestamp(block_a);
    gint64 stamp_b = hippo_block_get_sort_timestamp(block_b);

    if (stamp_a < stamp_b)
        return 1;
    else if (stamp_a > stamp_b)
        return -1;
    else
        return 0;
}

int
hippo_block_get_clicked_count(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), 0);

    return block->clicked_count;
}

void
hippo_block_set_clicked_count(HippoBlock *block,
                              int         value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->clicked_count) {
        block->clicked_count = value;
        g_object_notify(G_OBJECT(block), "clicked-count");
    }
}

int
hippo_block_get_significant_clicked_count(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), 0);

    return block->significant_clicked_count;
}

void
hippo_block_set_significant_clicked_count(HippoBlock *block,
                                          int         value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->significant_clicked_count) {
        block->significant_clicked_count = value;
        g_object_notify(G_OBJECT(block), "significant-clicked-count");
    }
}

gboolean
hippo_block_get_clicked(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), FALSE);

    return block->clicked;
}

void
hippo_block_set_clicked(HippoBlock *block,
                        gboolean    value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    value = value != FALSE;
    if (value != block->clicked) {
        block->clicked = value;
        g_object_notify(G_OBJECT(block), "clicked");
    }
}


gboolean
hippo_block_is_public(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), FALSE);

    return block->is_public;
}

void
hippo_block_set_public(HippoBlock *block,
                        gboolean    value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->is_public) {
        block->is_public = value;
        g_object_notify(G_OBJECT(block), "is-public");
    }
}

gboolean
hippo_block_get_ignored(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), FALSE);

    return block->ignored;
}

void
hippo_block_set_ignored(HippoBlock *block,
                        gboolean    value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->ignored) {
        block->ignored = value;
        g_object_notify(G_OBJECT(block), "ignored");
    }
}

gboolean 
hippo_block_get_pinned(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), FALSE);

    return block->pinned;
}

void
hippo_block_set_pinned(HippoBlock *block,
                       gboolean    pinned)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (pinned != block->pinned) {
        block->pinned = pinned;
        g_object_notify(G_OBJECT(block), "pinned");
    }
}
 
HippoStackReason
hippo_block_get_stack_reason(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), HIPPO_STACK_NEW_BLOCK);

    return block->stack_reason;
}

void
hippo_block_set_stack_reason(HippoBlock      *block,
                             HippoStackReason value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->stack_reason) {
        block->stack_reason = value;
        g_object_notify(G_OBJECT(block), "stack-reason");
    }
}

const char *
hippo_block_get_chat_id(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), NULL);

    return block->chat_id;
}

void
hippo_block_set_chat_id(HippoBlock *block,
                        const char *chat_id)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (chat_id == block->chat_id ||
        (chat_id && block->chat_id && strcmp(block->chat_id, chat_id) == 0))
        return;

    g_free(block->chat_id);
    block->chat_id = g_strdup(chat_id);

    g_object_notify(G_OBJECT(block), "chat-id");
}

gboolean
hippo_block_get_is_feed(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), FALSE);

    return block->is_feed;
}

void
hippo_block_set_is_feed(HippoBlock *block,
                        gboolean    feed)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (feed != block->is_feed) {
        block->is_feed = feed;
        g_object_notify(G_OBJECT(block), "is-feed");
    }
}

gboolean
hippo_block_get_is_mine(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), FALSE);

    return block->is_mine;
}

void
hippo_block_set_is_mine(HippoBlock *block,
                        gboolean    mine)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (mine != block->is_mine) {
        block->is_mine = mine;
        g_object_notify(G_OBJECT(block), "is-mine");
    }
}

const char*
hippo_block_get_icon_url(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), NULL);

    return block->icon_url;
}

void
hippo_block_set_icon_url(HippoBlock *block,
                         const char *icon_url)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (icon_url == block->icon_url) /* catches both null, or self-assignment */
        return;

    if (icon_url && block->icon_url && strcmp(icon_url, block->icon_url) == 0)
        return;

    g_free(block->icon_url);
    block->icon_url = g_strdup(icon_url);
    g_object_notify(G_OBJECT(block), "icon-url");
}

const char*
hippo_block_get_title(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), NULL);

    return block->title;
}

void
hippo_block_set_title(HippoBlock *block,
                      const char *title)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (title == block->title) /* catches both null, or self-assignment */
        return;

    if (title && block->title && strcmp(title, block->title) == 0)
        return;

    g_free(block->title);
    block->title = g_strdup(title);
    g_object_notify(G_OBJECT(block), "title");
}


const char*
hippo_block_get_title_link(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), NULL);

    return block->title_link;
}

void
hippo_block_set_title_link(HippoBlock *block,
                           const char *link)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (link == block->title_link) /* catches both null, or self-assignment */
        return;

    if (link && block->title_link && strcmp(link, block->title_link) == 0)
        return;

    g_free(block->title_link);
    block->title_link = g_strdup(link);
    g_object_notify(G_OBJECT(block), "title-link");
}

HippoEntity *
hippo_block_get_source(HippoBlock *block)
{
    return block->source;
}

void
hippo_block_set_source(HippoBlock        *block,
                       HippoEntity       *source)
{
    if (source == block->source)
        return;

    if (block->source) {
        g_object_unref(block->source);
        block->source = NULL;
    }

    if (source) {
        g_object_ref(source);
        block->source = source;
    }

    g_object_notify(G_OBJECT(block), "source");
}

HippoBlock *
hippo_block_create_for_resource(DDMDataResource *resource)
{
    HippoBlockType block_type;
    HippoBlock *block;
    const char *resource_id;
    const char *type;
    const char *slash;
    char *guid;
    
    static const struct { const char *name; HippoBlockType type; } types[] = {
        { "POST", HIPPO_BLOCK_TYPE_POST },
        { "ACCOUNT_QUESTION", HIPPO_BLOCK_TYPE_ACCOUNT_QUESTION },
        { "GROUP_MEMBER", HIPPO_BLOCK_TYPE_GROUP_MEMBER },
        { "GROUP_REVISION", HIPPO_BLOCK_TYPE_GROUP_REVISION },
        { "GROUP_CHAT", HIPPO_BLOCK_TYPE_GROUP_CHAT },
        { "MUSIC_CHAT", HIPPO_BLOCK_TYPE_MUSIC_CHAT },
        { "MUSIC_PERSON", HIPPO_BLOCK_TYPE_MUSIC_PERSON },
        { "FLICKR_PERSON", HIPPO_BLOCK_TYPE_FLICKR_PERSON },
        { "FLICKR_PHOTOSET", HIPPO_BLOCK_TYPE_FLICKR_PHOTOSET },
        { "FACEBOOK_EVENT", HIPPO_BLOCK_TYPE_FACEBOOK_EVENT },
        { "YOUTUBE_PERSON", HIPPO_BLOCK_TYPE_YOUTUBE_PERSON },
        { "NETFLIX_MOVIE", HIPPO_BLOCK_TYPE_NETFLIX_MOVIE },
        { "AMAZON_REVIEW", HIPPO_BLOCK_TYPE_AMAZON_REVIEW },
        { "AMAZON_WISH_LIST_ITEM", HIPPO_BLOCK_TYPE_AMAZON_WISH_LIST_ITEM }
    };
    unsigned int i;

    g_return_val_if_fail(resource != NULL, NULL);

    resource_id = ddm_data_resource_get_resource_id(resource);
    type = strrchr(resource_id, '.');
    if (type == NULL) {
        g_warning("Cannot extract type from block resource ID");
        return NULL;
    }

    type++; /* Skip the '.' */
    
    slash = strrchr(resource_id, '/');
    if (slash == NULL || slash >= type) {
        g_warning("Cannot extract guid from block resource ID");
        return NULL;
    }

    guid = g_strndup(slash + 1, type - 1 - (slash + 1));
    
    block_type = HIPPO_BLOCK_TYPE_UNKNOWN;
    
    for (i = 0; i < G_N_ELEMENTS(types); ++i) {
        if (strcmp(type, types[i].name) == 0) {
            block_type = types[i].type;
            break;
        }
    }

    if (block_type == HIPPO_BLOCK_TYPE_UNKNOWN)
        block_type = HIPPO_BLOCK_TYPE_GENERIC;
    
    block = hippo_block_new(resource, guid, block_type);

    g_free(guid);

    return block;
}
