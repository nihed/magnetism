/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_MYSPACE_PERSON_H__
#define __HIPPO_BLOCK_MYSPACE_PERSON_H__

#include <hippo/hippo-block.h>
#include <hippo/hippo-feed-entry.h>

G_BEGIN_DECLS

typedef struct _HippoBlockMySpacePerson      HippoBlockMySpacePerson;
typedef struct _HippoBlockMySpacePersonClass HippoBlockMySpacePersonClass;


#define HIPPO_TYPE_BLOCK_MYSPACE_PERSON              (hippo_block_myspace_person_get_type ())
#define HIPPO_BLOCK_MYSPACE_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_MYSPACE_PERSON, HippoBlockMySpacePerson))
#define HIPPO_BLOCK_MYSPACE_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_MYSPACE_PERSON, HippoBlockMySpacePersonClass))
#define HIPPO_IS_BLOCK_MYSPACE_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_MYSPACE_PERSON))
#define HIPPO_IS_BLOCK_MYSPACE_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_MYSPACE_PERSON))
#define HIPPO_BLOCK_MYSPACE_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_MYSPACE_PERSON, HippoBlockMySpacePersonClass))

GType            hippo_block_myspace_person_get_type               (void) G_GNUC_CONST;

HippoFeedEntry*  hippo_block_myspace_person_get_entry    (HippoBlockMySpacePerson *block_myspace_person);

G_END_DECLS

#endif /* __HIPPO_BLOCK_MYSPACE_PERSON_H__ */
