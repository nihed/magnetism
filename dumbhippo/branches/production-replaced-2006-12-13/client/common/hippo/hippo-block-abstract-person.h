/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_ABSTRACT_PERSON_H__
#define __HIPPO_BLOCK_ABSTRACT_PERSON_H__

/* Abstract base class for blocks that are per-user */

#include <hippo/hippo-block.h>
#include <hippo/hippo-person.h>

G_BEGIN_DECLS

typedef struct _HippoBlockAbstractPerson      HippoBlockAbstractPerson;
typedef struct _HippoBlockAbstractPersonClass HippoBlockAbstractPersonClass;


#define HIPPO_TYPE_BLOCK_ABSTRACT_PERSON              (hippo_block_abstract_person_get_type ())
#define HIPPO_BLOCK_ABSTRACT_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_ABSTRACT_PERSON, HippoBlockAbstractPerson))
#define HIPPO_BLOCK_ABSTRACT_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_ABSTRACT_PERSON, HippoBlockAbstractPersonClass))
#define HIPPO_IS_BLOCK_ABSTRACT_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_ABSTRACT_PERSON))
#define HIPPO_IS_BLOCK_ABSTRACT_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_ABSTRACT_PERSON))
#define HIPPO_BLOCK_ABSTRACT_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_ABSTRACT_PERSON, HippoBlockAbstractPersonClass))

struct _HippoBlockAbstractPerson {
    HippoBlock            parent;
    HippoPerson          *user;
};

struct _HippoBlockAbstractPersonClass {
    HippoBlockClass parent_class;
};

GType            hippo_block_abstract_person_get_type               (void) G_GNUC_CONST;

/* protected */

void hippo_block_abstract_person_set_user(HippoBlockAbstractPerson *block_person,
                                          HippoPerson              *user);
HippoPerson* hippo_block_abstract_person_get_user(HippoBlockAbstractPerson *block_person);


G_END_DECLS

#endif /* __HIPPO_BLOCK_ABSTRACT_PERSON_H__ */
