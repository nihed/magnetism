/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block.h"
#include "hippo-block-group-chat.h"
#include "hippo-block-group-member.h"
#include "hippo-block-post.h"
#include "hippo-block-music-person.h"
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
    PROP_SORT_TIMESTAMP,
    PROP_TIMESTAMP,
    PROP_CLICKED_TIMESTAMP,
    PROP_IGNORED_TIMESTAMP,
    PROP_CLICKED_COUNT,
    PROP_CLICKED,
    PROP_IGNORED
};

static void
hippo_block_init(HippoBlock *block)
{
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
    case PROP_CLICKED:
        hippo_block_set_clicked(block,
                                g_value_get_boolean(value));
        break;
    case PROP_IGNORED:
        hippo_block_set_ignored(block,
                                g_value_get_boolean(value));        
        break;
    case PROP_GUID:                  /* read-only */
    case PROP_BLOCK_TYPE:            /* read-only */
    case PROP_SORT_TIMESTAMP:        /* read-only */
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
    case PROP_CLICKED:
        g_value_set_boolean(value, block->clicked);
        break;
    case PROP_IGNORED:
        g_value_set_boolean(value, block->ignored);
        break;        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
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
    gint64 timestamp;
    gint64 clicked_timestamp;
    gint64 ignored_timestamp;
    int clicked_count;
    gboolean clicked;
    gboolean ignored;
    
    g_assert(cache != NULL);

    if (!hippo_xml_split(cache, node, NULL,
                         "id", HIPPO_SPLIT_GUID, &guid,
                         "type", HIPPO_SPLIT_STRING, &type_str,
                         "timestamp", HIPPO_SPLIT_TIME_MS, &timestamp,
                         "clickedTimestamp", HIPPO_SPLIT_TIME_MS, &clicked_timestamp,
                         "ignoredTimestamp", HIPPO_SPLIT_TIME_MS, &ignored_timestamp,
                         "clickedCount", HIPPO_SPLIT_INT32, &clicked_count,
                         "clicked", HIPPO_SPLIT_BOOLEAN, &clicked, 
                         "ignored", HIPPO_SPLIT_BOOLEAN, &ignored,
                         NULL))
        return FALSE;

    if (strcmp(block->guid, guid) != 0) {
        g_warning("Update to <block/> node doesn't match original ID");
        return FALSE;
    }
                         
    type = hippo_block_type_from_string(type_str);
    if (type != block->type) {
        g_warning("Update to <block/> node doesn't match original type");
        return FALSE;
    }

    hippo_block_set_timestamp(block, timestamp);
    hippo_block_set_clicked_timestamp(block, clicked_timestamp);
    hippo_block_set_ignored_timestamp(block, ignored_timestamp);
    hippo_block_set_clicked_count(block, clicked_count);
    hippo_block_set_clicked(block, clicked);
    hippo_block_set_ignored(block, ignored);

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
    case HIPPO_BLOCK_TYPE_MUSIC_PERSON:
        object_type = HIPPO_TYPE_BLOCK_MUSIC_PERSON;
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

HippoBlockType
hippo_block_type_from_string(const char *s)
{
    static const struct { const char *name; HippoBlockType type; } types[] = {
        { "POST", HIPPO_BLOCK_TYPE_POST },
        { "GROUP_MEMBER", HIPPO_BLOCK_TYPE_GROUP_MEMBER },
        { "GROUP_CHAT", HIPPO_BLOCK_TYPE_GROUP_CHAT },
        { "MUSIC_PERSON", HIPPO_BLOCK_TYPE_MUSIC_PERSON },
        { "EXTERNAL_ACCOUNT_UPDATE", HIPPO_BLOCK_TYPE_EXTERNAL_ACCOUNT_UPDATE }
    };
    unsigned int i;
    for (i = 0; i < G_N_ELEMENTS(types); ++i) {
        if (strcmp(s, types[i].name) == 0)
            return types[i].type;
    }
    return HIPPO_BLOCK_TYPE_UNKNOWN;
}
