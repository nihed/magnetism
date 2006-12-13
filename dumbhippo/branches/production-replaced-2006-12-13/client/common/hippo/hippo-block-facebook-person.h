/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_FACEBOOK_PERSON_H__
#define __HIPPO_BLOCK_FACEBOOK_PERSON_H__

#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoBlockFacebookPerson      HippoBlockFacebookPerson;
typedef struct _HippoBlockFacebookPersonClass HippoBlockFacebookPersonClass;


#define HIPPO_TYPE_BLOCK_FACEBOOK_PERSON              (hippo_block_facebook_person_get_type ())
#define HIPPO_BLOCK_FACEBOOK_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_FACEBOOK_PERSON, HippoBlockFacebookPerson))
#define HIPPO_BLOCK_FACEBOOK_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_FACEBOOK_PERSON, HippoBlockFacebookPersonClass))
#define HIPPO_IS_BLOCK_FACEBOOK_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_FACEBOOK_PERSON))
#define HIPPO_IS_BLOCK_FACEBOOK_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_FACEBOOK_PERSON))
#define HIPPO_BLOCK_FACEBOOK_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_FACEBOOK_PERSON, HippoBlockFacebookPersonClass))

GType            hippo_block_facebook_person_get_type               (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_BLOCK_FACEBOOK_PERSON_H__ */
