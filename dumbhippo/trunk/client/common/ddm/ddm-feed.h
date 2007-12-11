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

typedef struct _DDMFeed      DDMFeed;
typedef struct _DDMFeedClass DDMFeedClass;
typedef struct _DDMFeedIter  DDMFeedIter;

struct _DDMFeedIter {
    gpointer data1;
    gpointer data2;
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
 * item's timestamp and reorder it as necessary */
void ddm_feed_add_item    (DDMFeed         *feed,
                           DDMDataResource *resource,
                           gint64           timestamp);
void ddm_feed_remove_item (DDMFeed         *feed,
                           DDMDataResource *resource);
void ddm_feed_clear       (DDMFeed         *feed);

void     ddm_feed_iter_init (DDMFeedIter      *iter,
                             DDMFeed          *feed);
gboolean ddm_feed_iter_next (DDMFeedIter      *iter,
                             DDMDataResource **resource,
                             gint64           *timestamp);

G_END_DECLS

#endif /* __DDM_FEED_H__ */
