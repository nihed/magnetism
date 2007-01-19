/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block.h"
#include "hippo-block-generic.h"
#include "hippo-block-group-chat.h"
#include "hippo-block-group-member.h"
#include "hippo-block-post.h"
#include "hippo-block-music-chat.h"
#include "hippo-block-music-person.h"
#include "hippo-block-facebook-person.h"
#include "hippo-block-flickr-person.h"
#include "hippo-block-youtube-person.h"
#include "hippo-block-flickr-photoset.h"
#include "hippo-block-facebook-event.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void     hippo_block_finalize             (GObject *object);

static void hippo_block_set_property (GObject      *object,
                                      guint         prop_id,
                                      const GValue *value,
                                      GParamSpec   *pspec);
static void hippo_block_get_property (GObject      *object,
                                      guint         prop_id,
                                      GValue       *value,
                                      GParamSpec   *pspec);

static gboolean hippo_block_real_update_from_xml (HippoBlock     *block,
                                                  HippoDataCache *cache,
                                                  LmMessageNode  *node);

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
    PROP_SORT_TIMESTAMP,
    PROP_TIMESTAMP,
    PROP_CLICKED_TIMESTAMP,
    PROP_IGNORED_TIMESTAMP,
    PROP_CLICKED_COUNT,
    PROP_SIGNIFICANT_CLICKED_COUNT,
    PROP_MESSAGE_COUNT,
    PROP_CLICKED,
    PROP_ICON_URL,
    PROP_IGNORED,
    PROP_TITLE,
    PROP_TITLE_LINK,
    PROP_STACK_REASON
};

static void
hippo_block_init(HippoBlock *block)
{
    block->stack_reason = HIPPO_STACK_NEW_BLOCK;
    block->message_count = -1;
}

static void
hippo_block_class_init(HippoBlockClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
          
    object_class->finalize = hippo_block_finalize;

    object_class->set_property = hippo_block_set_property;
    object_class->get_property = hippo_block_get_property;

    klass->update_from_xml = hippo_block_real_update_from_xml;

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
                                    PROP_CLICKED_COUNT,
                                    g_param_spec_int("clicked-count",
                                                     _("Clicked count"),
                                                     _("Number of people who clicked on the block"),
                                                     0, G_MAXINT,
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
                                    PROP_MESSAGE_COUNT,
                                    g_param_spec_int("message-count",
                                                     _("Message count"),
                                                     _("Number of comments and quips for this block"),
                                                     -1, G_MAXINT,
                                                     -1,
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
}

static void
hippo_block_finalize(GObject *object)
{
    HippoBlock *block = HIPPO_BLOCK(object);

    g_free(block->guid);

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
    case PROP_CLICKED_COUNT:
        hippo_block_set_clicked_count(block,
                                      g_value_get_int(value));
        break;
    case PROP_SIGNIFICANT_CLICKED_COUNT:
        hippo_block_set_significant_clicked_count(block,
                                                  g_value_get_int(value));
        break;
    case PROP_MESSAGE_COUNT:
        hippo_block_set_message_count(block,
                                      g_value_get_int(value));
        break;
    case PROP_CLICKED:
        hippo_block_set_clicked(block,
                                g_value_get_boolean(value));
        break;
    case PROP_IGNORED:
        hippo_block_set_ignored(block,
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
    case PROP_CLICKED_COUNT:
        g_value_set_int(value, block->clicked_count);
        break;
    case PROP_SIGNIFICANT_CLICKED_COUNT:
        g_value_set_int(value, block->significant_clicked_count);
        break;
    case PROP_MESSAGE_COUNT:
        g_value_set_int(value, block->message_count);
        break;
    case PROP_CLICKED:
        g_value_set_boolean(value, block->clicked);
        break;
    case PROP_IGNORED:
        g_value_set_boolean(value, block->ignored);
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

static gboolean
hippo_block_real_update_from_xml (HippoBlock     *block,
                                  HippoDataCache *cache,
                                  LmMessageNode  *node)
{
    const char *guid;
    const char *type_str;
    HippoBlockType type;
    gboolean is_public;
    gint64 timestamp;
    gint64 clicked_timestamp;
    gint64 ignored_timestamp;
    int clicked_count;
    int significant_clicked_count = 0;
    int message_count = -1;
    gboolean clicked;
    gboolean ignored;
    LmMessageNode *title_node;
    const char *title = NULL;
    const char *title_link = NULL;
    const char *icon_url = NULL;
    const char *stack_reason_str = NULL;
    const char *generic_types = NULL;
    HippoStackReason stack_reason = HIPPO_STACK_NEW_BLOCK;
    
    g_assert(cache != NULL);

    if (!hippo_xml_split(cache, node, NULL,
                         "id", HIPPO_SPLIT_GUID, &guid,
                         "type", HIPPO_SPLIT_STRING, &type_str,
                         "genericTypes", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &generic_types,
                         "isPublic", HIPPO_SPLIT_BOOLEAN, &is_public,
                         "timestamp", HIPPO_SPLIT_TIME_MS, &timestamp,
                         "clickedTimestamp", HIPPO_SPLIT_TIME_MS, &clicked_timestamp,
                         "ignoredTimestamp", HIPPO_SPLIT_TIME_MS, &ignored_timestamp,
                         "clickedCount", HIPPO_SPLIT_INT32, &clicked_count,
                         "significantClickedCount", HIPPO_SPLIT_INT32 | HIPPO_SPLIT_OPTIONAL, &significant_clicked_count ,
                         "messageCount", HIPPO_SPLIT_INT32 | HIPPO_SPLIT_OPTIONAL, &message_count,
                         "clicked", HIPPO_SPLIT_BOOLEAN, &clicked, 
                         "ignored", HIPPO_SPLIT_BOOLEAN, &ignored,
                         "icon", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &icon_url,
                         "stackReason", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &stack_reason_str,
                         NULL)) {
        g_debug("missing attributes on <block> %s update", block->guid);
        return FALSE;
    }

    if (strcmp(block->guid, guid) != 0) {
        g_warning("Update to <block/> node doesn't match original ID");
        return FALSE;
    }
                         
    type = hippo_block_type_from_attributes(type_str, generic_types);
    if (type != block->type) {
        g_warning("Update to <block/> node doesn't match original type");
        return FALSE;
    }

    hippo_block_set_public(block, is_public);
    hippo_block_set_timestamp(block, timestamp);
    hippo_block_set_clicked_timestamp(block, clicked_timestamp);
    hippo_block_set_ignored_timestamp(block, ignored_timestamp);
    hippo_block_set_clicked_count(block, clicked_count);
    hippo_block_set_significant_clicked_count(block, significant_clicked_count);
    hippo_block_set_message_count(block, message_count);
    hippo_block_set_clicked(block, clicked);
    hippo_block_set_ignored(block, ignored);
    hippo_block_set_icon_url(block, icon_url);

    title_node = lm_message_node_get_child(node, "title");
    if (title_node) {
        title_link = lm_message_node_get_attribute(title_node, "link");
        title = lm_message_node_get_value(title_node);
        hippo_block_set_title(block, title);
        hippo_block_set_title_link(block, title_link);
    }

    if (stack_reason_str)
        stack_reason = stack_reason_from_string(stack_reason_str);
    hippo_block_set_stack_reason(block, stack_reason);
    
    g_debug("Parsed block %s type %s - %s timestamp = %" G_GINT64_FORMAT,
            guid, g_type_name_from_instance((GTypeInstance*) block), type_str,
            timestamp);
    
    return TRUE;
}

/* === HippoBlock exported API === */



HippoBlock*
hippo_block_new(const char    *guid,
                HippoBlockType type)
{
    HippoBlock *block;
    GType object_type;

    object_type = HIPPO_TYPE_BLOCK;
    switch (type) {
    case HIPPO_BLOCK_TYPE_UNKNOWN:
        object_type = HIPPO_TYPE_BLOCK;
        break;
    case HIPPO_BLOCK_TYPE_GROUP_CHAT:
        object_type = HIPPO_TYPE_BLOCK_GROUP_CHAT;
        break;
    case HIPPO_BLOCK_TYPE_GROUP_MEMBER:
        object_type = HIPPO_TYPE_BLOCK_GROUP_MEMBER;
        break;
    case HIPPO_BLOCK_TYPE_POST:
        object_type = HIPPO_TYPE_BLOCK_POST;
        break;
    case HIPPO_BLOCK_TYPE_MUSIC_CHAT:
        object_type = HIPPO_TYPE_BLOCK_MUSIC_CHAT;
        break;
    case HIPPO_BLOCK_TYPE_MUSIC_PERSON:
        object_type = HIPPO_TYPE_BLOCK_MUSIC_PERSON;
        break;
    case HIPPO_BLOCK_TYPE_FLICKR_PERSON:
        object_type = HIPPO_TYPE_BLOCK_FLICKR_PERSON;
        break;
    case HIPPO_BLOCK_TYPE_FLICKR_PHOTOSET:
        object_type = HIPPO_TYPE_BLOCK_FLICKR_PHOTOSET;
        break;
    case HIPPO_BLOCK_TYPE_FACEBOOK_EVENT:
        object_type = HIPPO_TYPE_BLOCK_FACEBOOK_EVENT;
        break;
    case HIPPO_BLOCK_TYPE_YOUTUBE_PERSON:
        object_type = HIPPO_TYPE_BLOCK_YOUTUBE_PERSON;
        break;
    case HIPPO_BLOCK_TYPE_GENERIC:
        object_type = HIPPO_TYPE_BLOCK_GENERIC;
        break;
        /* don't add default case, it hides warnings */
    }
    
    block = g_object_new(object_type,
                         NULL);
    block->type = type;
    block->guid = g_strdup(guid);
    
    return block;
}

gboolean
hippo_block_update_from_xml(HippoBlock     *block,
                            HippoDataCache *cache,
                            LmMessageNode  *node)
{
    gboolean success;
    
    g_object_freeze_notify(G_OBJECT(block));

    success = HIPPO_BLOCK_GET_CLASS(block)->update_from_xml(block, cache, node);

    g_object_thaw_notify(G_OBJECT(block));

    return success;
    
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

int
hippo_block_get_message_count(HippoBlock *block)
{
    g_return_val_if_fail(HIPPO_IS_BLOCK(block), 0);

    return block->message_count;
}

void
hippo_block_set_message_count(HippoBlock *block,
                              int         value)
{
    g_return_if_fail(HIPPO_IS_BLOCK(block));

    if (value != block->message_count) {
        block->message_count = value;
        g_object_notify(G_OBJECT(block), "message-count");
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

HippoBlockType
hippo_block_type_from_attributes(const char *type,
                                 const char *generic_types)
{
    HippoBlockType block_type;
    
    static const struct { const char *name; HippoBlockType type; } types[] = {
        { "POST", HIPPO_BLOCK_TYPE_POST },
        { "GROUP_MEMBER", HIPPO_BLOCK_TYPE_GROUP_MEMBER },
        { "GROUP_CHAT", HIPPO_BLOCK_TYPE_GROUP_CHAT },
        { "MUSIC_CHAT", HIPPO_BLOCK_TYPE_MUSIC_CHAT },
        { "MUSIC_PERSON", HIPPO_BLOCK_TYPE_MUSIC_PERSON },
        { "FLICKR_PERSON", HIPPO_BLOCK_TYPE_FLICKR_PERSON },
        { "FLICKR_PHOTOSET", HIPPO_BLOCK_TYPE_FLICKR_PHOTOSET },
        { "FACEBOOK_EVENT", HIPPO_BLOCK_TYPE_FACEBOOK_EVENT },
        { "YOUTUBE_PERSON", HIPPO_BLOCK_TYPE_YOUTUBE_PERSON }
    };
    unsigned int i;

    g_return_val_if_fail(type != NULL, HIPPO_BLOCK_TYPE_UNKNOWN);
    /* generic_types can be NULL */
    
    block_type = HIPPO_BLOCK_TYPE_UNKNOWN;
    
    for (i = 0; i < G_N_ELEMENTS(types); ++i) {
        if (strcmp(type, types[i].name) == 0) {
            block_type = types[i].type;
            break;
        }
    }

    if (block_type == HIPPO_BLOCK_TYPE_UNKNOWN &&
        generic_types != NULL) {
        char **generics;

        generics = g_strsplit(generic_types, ",", -1);

        if (generics != NULL) {
            for (i = 0; generics[i] != NULL; ++i) {
                /* TITLE_DESCRIPTION and ENTITY_SOURCE are also possible,
                 * the generic block type simply uses the description and source
                 * information if it's available, but requires the title information.
                 */
                if (strcmp(generics[i], "TITLE") == 0)
                    block_type = HIPPO_BLOCK_TYPE_GENERIC;
            }

            g_strfreev(generics);
        }
    }
    
    return block_type;
}
