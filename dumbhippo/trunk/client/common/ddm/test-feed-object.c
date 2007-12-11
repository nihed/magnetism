/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "ddm-feed.h"
#include "static-file-backend.h"

/* Three timestamps used when testing */
#define TS1 G_GINT64_CONSTANT(1197332388000)
#define TS2 G_GINT64_CONSTANT(1197335965000)
#define TS3 G_GINT64_CONSTANT(1197337121000)

static enum {
    ITEM_ADDED,
    ITEM_CHANGED,
    ITEM_REMOVED
} last_op;

static DDMDataResource *last_resource;
static gint64 last_timestamp;

static void
on_item_added(DDMFeed       *feed,
            DDMDataResource *resource,
            gint64           timestamp)
{
    last_op = ITEM_ADDED;
    last_resource = resource;
    last_timestamp = timestamp;
}

static void
on_item_changed(DDMFeed       *feed,
                  DDMDataResource *resource,
                  gint64           timestamp)
{
    last_op = ITEM_CHANGED;
    last_resource = resource;
    last_timestamp = timestamp;
}

static void
on_item_removed(DDMFeed       *feed,
                DDMDataResource *resource,
                gint64           timestamp)
{
    last_op = ITEM_REMOVED;
    last_resource = resource;
    last_timestamp = -1;
}

int
main(int argc, char **argv)
{
    GError *error = NULL;
    DDMDataModel *model;
    const char *srcdir;
    char *filename;

    DDMDataResource *user1;
    DDMDataResource *user2;

    DDMFeed *feed;
    DDMFeedIter iter;

    DDMDataResource *resource;
    gint64 timestamp;

    g_type_init();

    /* Load up some data just to have resources to stick into our feed */

    model = ddm_data_model_new_no_backend();

    srcdir = g_getenv("DDM_SRCDIR");
    if (srcdir == NULL)
        g_error("DDM_SRCDIR is not set");

    filename = g_build_filename(srcdir, "test-data.xml", NULL);
    if (!ddm_static_file_parse(filename, model, &error))
        g_error("Failed to parse test data: %s", error->message);

    g_free(filename);

    user1 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER1");
    g_assert(user1 != NULL);
    
    user2 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER2");
    g_assert(user2 != NULL);

    /* Create a feed */
    
    feed = ddm_feed_new();
    g_signal_connect(feed, "item-added",
                     G_CALLBACK(on_item_added), NULL);
    g_signal_connect(feed, "item-changed",
                     G_CALLBACK(on_item_changed), NULL);
    g_signal_connect(feed, "item-removed",
                     G_CALLBACK(on_item_removed), NULL);

    /* Add two items to it */

    ddm_feed_add_item(feed, user1, TS1);
    g_assert(last_op == ITEM_ADDED);
    g_assert(last_resource == user1);
    g_assert(last_timestamp == TS1);
    
    ddm_feed_add_item(feed, user2, TS2);
    g_assert(last_op == ITEM_ADDED);
    g_assert(last_resource == user2);
    g_assert(last_timestamp == TS2);

    /* Restack the older one to the top */

    ddm_feed_add_item(feed, user1, TS3);
    g_assert(last_op == ITEM_CHANGED);
    g_assert(last_resource == user1);
    g_assert(last_timestamp == TS3);

    /* Iterate through the feed */
    
    ddm_feed_iter_init(&iter, feed);
    
    g_assert(ddm_feed_iter_next(&iter, &resource, &timestamp));
    g_assert(resource == user1);
    g_assert(timestamp == TS3);
    
    g_assert(ddm_feed_iter_next(&iter, &resource, &timestamp));
    g_assert(resource == user2);
    g_assert(timestamp == TS2);

    g_assert(!ddm_feed_iter_next(&iter, &resource, &timestamp));

    /* Remove an item */

    ddm_feed_remove_item(feed, user2);
    g_assert(last_op == ITEM_REMOVED);
    g_assert(last_resource == user2);

    ddm_feed_iter_init(&iter, feed);
    
    g_assert(ddm_feed_iter_next(&iter, &resource, &timestamp));
    g_assert(resource == user1);
    g_assert(timestamp == TS3);
    
    g_assert(!ddm_feed_iter_next(&iter, &resource, &timestamp));

    /* Remove all items */

    ddm_feed_clear(feed);
    g_assert(last_op == ITEM_REMOVED);
    g_assert(last_resource == user1);

    ddm_feed_iter_init(&iter, feed);
    g_assert(!ddm_feed_iter_next(&iter, &resource, &timestamp));
    
    return 0;
}
