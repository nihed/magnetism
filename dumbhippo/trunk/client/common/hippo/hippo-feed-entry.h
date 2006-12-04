/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_FEED_ENTRY_H__
#define __HIPPO_FEED_ENTRY_H__

/* Entry from an RSS or Atom feed */

#include <hippo/hippo-basics.h>
#include <hippo/hippo-xml-utils.h>

G_BEGIN_DECLS

typedef struct _HippoFeedEntry      HippoFeedEntry;
typedef struct _HippoFeedEntryClass HippoFeedEntryClass;

#define HIPPO_TYPE_FEED_ENTRY              (hippo_feed_entry_get_type ())
#define HIPPO_FEED_ENTRY(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_FEED_ENTRY, HippoFeedEntry))
#define HIPPO_FEED_ENTRY_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_FEED_ENTRY, HippoFeedEntryClass))
#define HIPPO_IS_FEED_ENTRY(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_FEED_ENTRY))
#define HIPPO_IS_FEED_ENTRY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_FEED_ENTRY))
#define HIPPO_FEED_ENTRY_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_FEED_ENTRY, HippoFeedEntryClass))

GType            hippo_feed_entry_get_type               (void) G_GNUC_CONST;

const char* hippo_feed_entry_get_description (HippoFeedEntry *feed_entry);
const char* hippo_feed_entry_get_title       (HippoFeedEntry *feed_entry);
const char* hippo_feed_entry_get_url         (HippoFeedEntry *feed_entry);

/* can return NULL */
HippoFeedEntry* hippo_feed_entry_new_from_xml (HippoDataCache       *cache,
                                               LmMessageNode        *node);

G_END_DECLS

#endif /* __HIPPO_FEED_ENTRY_H__ */
