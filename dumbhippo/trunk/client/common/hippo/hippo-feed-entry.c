/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-feed-entry.h"
#include <string.h>

static void      hippo_feed_entry_init                (HippoFeedEntry       *feed_entry);
static void      hippo_feed_entry_class_init          (HippoFeedEntryClass  *klass);

static void      hippo_feed_entry_dispose             (GObject                *object);
static void      hippo_feed_entry_finalize            (GObject                *object);

static void hippo_feed_entry_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_feed_entry_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);


struct _HippoFeedEntry {
    GObject parent;
    char *title;
    char *url;
};

struct _HippoFeedEntryClass {
    GObjectClass parent_class;
};

enum {
    PROP_0,
    PROP_TITLE,
    PROP_URL
};

G_DEFINE_TYPE(HippoFeedEntry, hippo_feed_entry, G_TYPE_OBJECT);


static void
hippo_feed_entry_init(HippoFeedEntry  *feed_entry)
{

}

static void
hippo_feed_entry_class_init(HippoFeedEntryClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->set_property = hippo_feed_entry_set_property;
    object_class->get_property = hippo_feed_entry_get_property;

    object_class->dispose = hippo_feed_entry_dispose;
    object_class->finalize = hippo_feed_entry_finalize;

    g_object_class_install_property(object_class,
                                    PROP_TITLE,
                                    g_param_spec_string("title",
                                                        _("Title"),
                                                        _("Title of entry"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_URL,
                                    g_param_spec_string("url",
                                                        _("URL"),
                                                        _("Link to entry"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_feed_entry_dispose(GObject *object)
{

    G_OBJECT_CLASS(hippo_feed_entry_parent_class)->dispose(object);
}

static void
hippo_feed_entry_finalize(GObject *object)
{
    HippoFeedEntry *feed_entry = HIPPO_FEED_ENTRY(object);

    g_free(feed_entry->title);
    g_free(feed_entry->url);
    
    G_OBJECT_CLASS(hippo_feed_entry_parent_class)->finalize(object);
}

static void
hippo_feed_entry_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoFeedEntry *feed_entry;

    feed_entry = HIPPO_FEED_ENTRY (object);

    switch (prop_id) {
    case PROP_TITLE:
        {
            const char *new_title = g_value_get_string(value);
            if (new_title != feed_entry->title) {
                g_free(feed_entry->title);
                feed_entry->title = g_strdup(new_title);
            }
        }
        break;
    case PROP_URL:
        {
            const char *new_url = g_value_get_string(value);
            if (new_url != feed_entry->url) {
                g_free(feed_entry->url);
                feed_entry->url = g_strdup(new_url);
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_feed_entry_get_property(GObject         *object,
                              guint            prop_id,
                              GValue          *value,
                              GParamSpec      *pspec)
{
    HippoFeedEntry *feed_entry;

    feed_entry = HIPPO_FEED_ENTRY (object);

    switch (prop_id) {
    case PROP_TITLE:
        g_value_set_string(value, feed_entry->title);
        break;
    case PROP_URL:
        g_value_set_string(value, feed_entry->url);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

const char*
hippo_feed_entry_get_title (HippoFeedEntry *feed_entry)
{
    return feed_entry->title;
}

const char*
hippo_feed_entry_get_url (HippoFeedEntry *feed_entry)
{
    return feed_entry->url;
}

HippoFeedEntry*
hippo_feed_entry_new_from_xml (HippoDataCache       *cache,
                               LmMessageNode        *node)
{
    const char *title;
    const char *href;
    HippoFeedEntry *entry;
    
    if (!hippo_xml_split(cache, node, NULL,
                         "title", HIPPO_SPLIT_STRING, &title,
                         "href", HIPPO_SPLIT_URI_ABSOLUTE, &href,
                         NULL))
        return NULL;

    /* we never display this right now anyway */
    /* description = lm_message_node_get_value(node); */

    entry = g_object_new(HIPPO_TYPE_FEED_ENTRY,
                         "title", title,
                         "url", href,
                         NULL);
    return entry;
}
