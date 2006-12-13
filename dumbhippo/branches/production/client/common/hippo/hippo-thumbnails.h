/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_THUMBNAILS_H__
#define __HIPPO_THUMBNAILS_H__

/* A set of thumbnails */

#include <hippo/hippo-basics.h>
#include <hippo/hippo-xml-utils.h>

G_BEGIN_DECLS

typedef struct _HippoThumbnail      HippoThumbnail;

typedef struct _HippoThumbnails      HippoThumbnails;
typedef struct _HippoThumbnailsClass HippoThumbnailsClass;

#define HIPPO_TYPE_THUMBNAILS              (hippo_thumbnails_get_type ())
#define HIPPO_THUMBNAILS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_THUMBNAILS, HippoThumbnails))
#define HIPPO_THUMBNAILS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_THUMBNAILS, HippoThumbnailsClass))
#define HIPPO_IS_THUMBNAILS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_THUMBNAILS))
#define HIPPO_IS_THUMBNAILS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_THUMBNAILS))
#define HIPPO_THUMBNAILS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_THUMBNAILS, HippoThumbnailsClass))

GType            hippo_thumbnails_get_type               (void) G_GNUC_CONST;


int             hippo_thumbnails_get_count              (HippoThumbnails *thumbnails);
int             hippo_thumbnails_get_max_width          (HippoThumbnails *thumbnails);
int             hippo_thumbnails_get_max_height         (HippoThumbnails *thumbnails);
int             hippo_thumbnails_get_total_items        (HippoThumbnails *thumbnails);
const char*     hippo_thumbnails_get_total_items_string (HippoThumbnails *thumbnails);
const char*     hippo_thumbnails_get_more_title         (HippoThumbnails *thumbnails);
const char*     hippo_thumbnails_get_more_link          (HippoThumbnails *thumbnails);
HippoThumbnail* hippo_thumbnails_get_nth                (HippoThumbnails *thumbnails,
                                                         int              which);

/* Methods on HippoThumbnail */
const char*     hippo_thumbnail_get_src                 (HippoThumbnail  *thumbnail);
const char*     hippo_thumbnail_get_href                (HippoThumbnail  *thumbnail);
int             hippo_thumbnail_get_width               (HippoThumbnail  *thumbnail);
int             hippo_thumbnail_get_height              (HippoThumbnail  *thumbnail);
const char*     hippo_thumbnail_get_title               (HippoThumbnail  *thumbnail);

/* can return NULL */
HippoThumbnails* hippo_thumbnails_new_from_xml (HippoDataCache       *cache,
                                               LmMessageNode        *node);

G_END_DECLS

#endif /* __HIPPO_THUMBNAILS_H__ */
