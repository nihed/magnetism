/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-feed.h"
#include "ddm-marshal.h"

typedef struct _DDMFeedItem     DDMFeedItem;
typedef struct _DDMFeedIterReal DDMFeedIterReal;

struct _DDMFeedItem {
    DDMDataResource *resource;
    gint64 timestamp;
};

struct _DDMFeed {
    GObject parent;

    GList *items;
    GHashTable *nodes_by_resource;
};

struct _DDMFeedClass {
    GObjectClass parent_class;
};

struct _DDMFeedIterReal {
    DDMFeed *feed;
    GList  *node;
};

static void ddm_feed_clear_internal (DDMFeed *feed,
                                     gboolean emit_signals);
static void ddm_feed_finalize       (GObject *object);

enum {
    ITEM_ADDED,
    ITEM_CHANGED,
    ITEM_REMOVED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

G_DEFINE_TYPE(DDMFeed, ddm_feed, G_TYPE_OBJECT);

static void
ddm_feed_init(DDMFeed *feed)
{
    feed->items = NULL;
    feed->nodes_by_resource = g_hash_table_new(g_direct_hash, NULL);
}

static void
ddm_feed_class_init(DDMFeedClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->finalize = ddm_feed_finalize;
    
    signals[ITEM_ADDED] =
        g_signal_new ("item-added",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      ddm_marshal_VOID__POINTER_INT64,
                      G_TYPE_NONE, 2, G_TYPE_POINTER, G_TYPE_INT64);

    signals[ITEM_CHANGED] =
        g_signal_new ("item-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      ddm_marshal_VOID__POINTER_INT64,
                      G_TYPE_NONE, 2, G_TYPE_POINTER, G_TYPE_INT64);
    
    signals[ITEM_REMOVED] =
        g_signal_new ("item-removed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      ddm_marshal_VOID__POINTER,
                      G_TYPE_NONE, 1, G_TYPE_POINTER);
}

static void
ddm_feed_finalize (GObject *object)
{
    DDMFeed *feed = DDM_FEED(object);
    
    ddm_feed_clear_internal(feed, FALSE);
    
    g_hash_table_destroy(feed->nodes_by_resource);
    g_assert(feed->items == NULL);
}

DDMFeed *
ddm_feed_new (void)
{
    return g_object_new(DDM_TYPE_FEED, NULL);
}

static void
feed_insert_sorted (DDMFeed *feed,
                    GList   *node,
                    gint64  timestamp)
{
    GList *l, *prev;
    
    prev = NULL;
    for (l = feed->items; l; l = l->next) {
        DDMFeedItem *other = l->data;
        if (other->timestamp < timestamp)
            break;
        prev = l;
    }

    if (prev == NULL) {
        node->prev = NULL;
        node->next = feed->items;
        if (node->next)
            node->next->prev = node;
        feed->items = node;
    } else {
        node->prev = prev;
        node->next = prev->next;
        if (node->next)
            node->next->prev = node;
        prev->next = node;
    }
}

void
ddm_feed_add_item (DDMFeed         *feed,
                   DDMDataResource *resource,
                   gint64           timestamp)
{
    GList *node;
    DDMFeedItem *item;

    g_return_if_fail(DDM_IS_FEED(feed));
        
    node = g_hash_table_lookup(feed->nodes_by_resource, resource);
    if (node != NULL) {
        item = node->data;

        if (item->timestamp == timestamp)
            return;
        
        item->timestamp = timestamp;

        feed->items = g_list_remove_link(feed->items, node);
        feed_insert_sorted(feed, node, timestamp);

        g_signal_emit(feed, signals[ITEM_CHANGED], 0, resource, timestamp);
    } else {
        item = g_slice_new(DDMFeedItem);
        item->resource = ddm_data_resource_ref(resource);
        item->timestamp = timestamp;
        
        node = g_list_alloc();
        node->data = item;
        
        g_hash_table_insert(feed->nodes_by_resource, resource, node);
        feed_insert_sorted(feed, node, timestamp);
        
        g_signal_emit(feed, signals[ITEM_ADDED], 0, resource, timestamp);
    }
}

void
ddm_feed_remove_item (DDMFeed         *feed,
                      DDMDataResource *resource)
{
    GList *node;
    DDMFeedItem *item;
    
    g_return_if_fail(DDM_IS_FEED(feed));

    node = g_hash_table_lookup(feed->nodes_by_resource, resource);
    if (node == NULL) {
        g_warning("ddm_feed_remove_item(): resource %s not in feed",
                  ddm_data_resource_get_resource_id(resource));
        return;
    }

    feed->items = g_list_remove_link(feed->items, node);
    g_hash_table_remove(feed->nodes_by_resource, resource);
    
    item = node->data;

    g_signal_emit(feed, signals[ITEM_REMOVED], 0, item->resource);
    ddm_data_resource_unref(item->resource);
    g_slice_free(DDMFeedItem, item);
}

static void
ddm_feed_clear_internal (DDMFeed *feed,
                         gboolean emit_signals)
{
    GList *items, *l;

    g_return_if_fail(DDM_IS_FEED(feed));
    
    g_hash_table_remove_all(feed->nodes_by_resource);

    items = feed->items;
    feed->items = NULL;

    for (l = items; l; l = l->next) {
        DDMFeedItem *item = items->data;

        if (emit_signals)
            g_signal_emit(feed, signals[ITEM_REMOVED], 0, item->resource);
        ddm_data_resource_unref(item->resource);
        g_slice_free(DDMFeedItem, item);
    }

    g_list_free(items);
}

void
ddm_feed_clear (DDMFeed *feed)
{
    g_return_if_fail(DDM_IS_FEED(feed));

    ddm_feed_clear_internal(feed, TRUE);
}

void
ddm_feed_iter_init (DDMFeedIter      *iter,
                    DDMFeed          *feed)
{
    DDMFeedIterReal *real = (DDMFeedIterReal *)iter;

    g_return_if_fail(DDM_IS_FEED(feed));

    real->feed = feed;
    real->node = feed->items;
}

gboolean
ddm_feed_iter_next (DDMFeedIter      *iter,
                    DDMDataResource **resource,
                    gint64           *timestamp)
{
    DDMFeedIterReal *real = (DDMFeedIterReal *)iter;
    DDMFeedItem *item;

    if (real->node == NULL)
        return FALSE;

    item = real->node->data;
    *resource = item->resource;
    *timestamp = item->timestamp;

    real->node = real->node->next;

    return TRUE;
}
