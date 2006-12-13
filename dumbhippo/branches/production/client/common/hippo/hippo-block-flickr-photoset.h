/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_FLICKR_PHOTOSET_H__
#define __HIPPO_BLOCK_FLICKR_PHOTOSET_H__

#include <hippo/hippo-block.h>
#include <hippo/hippo-thumbnails.h>

G_BEGIN_DECLS

typedef struct _HippoBlockFlickrPhotoset      HippoBlockFlickrPhotoset;
typedef struct _HippoBlockFlickrPhotosetClass HippoBlockFlickrPhotosetClass;


#define HIPPO_TYPE_BLOCK_FLICKR_PHOTOSET              (hippo_block_flickr_photoset_get_type ())
#define HIPPO_BLOCK_FLICKR_PHOTOSET(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_FLICKR_PHOTOSET, HippoBlockFlickrPhotoset))
#define HIPPO_BLOCK_FLICKR_PHOTOSET_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_FLICKR_PHOTOSET, HippoBlockFlickrPhotosetClass))
#define HIPPO_IS_BLOCK_FLICKR_PHOTOSET(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_FLICKR_PHOTOSET))
#define HIPPO_IS_BLOCK_FLICKR_PHOTOSET_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_FLICKR_PHOTOSET))
#define HIPPO_BLOCK_FLICKR_PHOTOSET_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_FLICKR_PHOTOSET, HippoBlockFlickrPhotosetClass))

GType            hippo_block_flickr_photoset_get_type               (void) G_GNUC_CONST;

HippoThumbnails*  hippo_block_flickr_photoset_get_thumbnails  (HippoBlockFlickrPhotoset *block_flickr_photoset);
const char*       hippo_block_flickr_photoset_get_title       (HippoBlockFlickrPhotoset *block_flickr_photoset);

G_END_DECLS

#endif /* __HIPPO_BLOCK_FLICKR_PHOTOSET_H__ */
