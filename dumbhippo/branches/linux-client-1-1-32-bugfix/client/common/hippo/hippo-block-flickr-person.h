/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_FLICKR_PERSON_H__
#define __HIPPO_BLOCK_FLICKR_PERSON_H__

#include <hippo/hippo-block.h>
#include <hippo/hippo-thumbnails.h>

G_BEGIN_DECLS

typedef struct _HippoBlockFlickrPerson      HippoBlockFlickrPerson;
typedef struct _HippoBlockFlickrPersonClass HippoBlockFlickrPersonClass;


#define HIPPO_TYPE_BLOCK_FLICKR_PERSON              (hippo_block_flickr_person_get_type ())
#define HIPPO_BLOCK_FLICKR_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_FLICKR_PERSON, HippoBlockFlickrPerson))
#define HIPPO_BLOCK_FLICKR_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_FLICKR_PERSON, HippoBlockFlickrPersonClass))
#define HIPPO_IS_BLOCK_FLICKR_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_FLICKR_PERSON))
#define HIPPO_IS_BLOCK_FLICKR_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_FLICKR_PERSON))
#define HIPPO_BLOCK_FLICKR_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_FLICKR_PERSON, HippoBlockFlickrPersonClass))

GType            hippo_block_flickr_person_get_type               (void) G_GNUC_CONST;

HippoThumbnails*  hippo_block_flickr_person_get_thumbnails  (HippoBlockFlickrPerson *block_flickr_person);

G_END_DECLS

#endif /* __HIPPO_BLOCK_FLICKR_PERSON_H__ */
