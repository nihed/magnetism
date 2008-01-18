/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */

#ifndef __DDM_FEED_H__
#define __DDM_FEED_H__

#include <glib-object.h>
#include <ddm/ddm-data-resource.h>

G_BEGIN_DECLS

/* DDMFeed is an object used to store the contents of a single resource property
 * of type DDM_DATA_FEED. It has signals:
 *
 * ::item-added   - an item was added
 * ::item-removed - an item was removed
 * ::item-changed - an item's timestamp changed, possibly reordering it in the feed
 *
 * DDMFeedIter can be used to iterate through a feed in order starting with the
 * most recent item.
 */

typedef struct _DDMFeedClass DDMFeedClass;
typedef struct _DDMFeedIter  DDMFeedIter;

struct _DDMFeedIter {
    gpointer data1;
    gpointer data2;
    gpointer data3;
    gint data4;
};

#define DDM_TYPE_FEED              (ddm_feed_get_type ())
#define DDM_FEED(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), DDM_TYPE_FEED, DDMFeed))
#define DDM_FEED_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), DDM_TYPE_FEED, DDMFeedClass))
#define DDM_IS_FEED(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), DDM_TYPE_FEED))
#define DDM_IS_FEED_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), DDM_TYPE_FEED))
#define DDM_FEED_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), DDM_TYPE_FEED, DDMFeedClass))

GType            ddm_feed_get_type               (void) G_GNUC_CONST;

DDMFeed *ddm_feed_new      (void);

/* If an item for this resource is already in the feed, will update the
 * item's timestamp and reorder it as necessary; return value is whether
 * anything changed at all */
gboolean ddm_feed_add_item    (DDMFeed         *feed,
                               DDMDataResource *resource,
                               gint64           timestamp);
/* return value is whether the item was found and removed */
gboolean ddm_feed_remove_item (DDMFeed         *feed,
                               DDMDataResource *resource);
void     ddm_feed_clear       (DDMFeed         *feed);
gboolean ddm_feed_is_empty    (DDMFeed          *feed);

/* We handle keeping track of what feed items need to be notified to
 * downstream clients very simply ... we just keep a single item
 * timestamp to track what items might not have been sent to
 * clients. This means that we'll occasionally oversend updates (in
 * particular, we have to resend the entire feed on any removal) but
 * we expects to have mostly adds at the end of the feed, and keeping
 * a log is a) more complicated b) and poses a problem if nobody is
 * consuming and clearing the log. (Which will be the case for the
 * data model when used in an application instead of in the
 * engine... there is no "downstream' to the applications.)
 */

/* Gets the minimum timestamp for items that need to be resent for a
 * notification. A timestamp of 0 means "resend everything", so the
 * first property update should be sent as a REPLACE not an ADD.
 */
gint64 ddm_feed_get_notify_timestamp   (DDMFeed *feed);

/* Call when all notifications have been sent out */
void   ddm_feed_reset_notify_timestamp (DDMFeed *feed);

void     ddm_feed_iter_init   (DDMFeedIter      *iter,
                               DDMFeed          *feed);
gboolean ddm_feed_iter_next   (DDMFeedIter      *iter,
                               DDMDataResource **resource,
                               gint64           *timestamp);
/* Remove the last item retrieved by ddm_feed_iter_next() */
void     ddm_feed_iter_remove (DDMFeedIter      *iter);

G_END_DECLS

#endif /* __DDM_FEED_H__ */
