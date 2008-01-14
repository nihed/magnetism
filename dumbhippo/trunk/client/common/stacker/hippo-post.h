/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_POST_H__
#define __HIPPO_POST_H__

#include <ddm/ddm.h>
#include <hippo/hippo-basics.h>
#include <hippo/hippo-entity.h>

G_BEGIN_DECLS

typedef struct _HippoPost      HippoPost;
typedef struct _HippoPostClass HippoPostClass;

#define HIPPO_TYPE_POST              (hippo_post_get_type ())
#define HIPPO_POST(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_POST, HippoPost))
#define HIPPO_POST_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_POST, HippoPostClass))
#define HIPPO_IS_POST(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_POST))
#define HIPPO_IS_POST_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_POST))
#define HIPPO_POST_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_POST, HippoPostClass))

GType            hippo_post_get_type                  (void) G_GNUC_CONST;
HippoPost*       hippo_post_new                       (DDMDataResource *resource);

DDMDataResource *hippo_post_get_resource              (HippoPost *post);

const char*      hippo_post_get_guid                  (HippoPost *post);
HippoEntity*     hippo_post_get_sender                (HippoPost *post);
const char*      hippo_post_get_url                   (HippoPost *post);
const char*      hippo_post_get_title                 (HippoPost *post);
const char*      hippo_post_get_description           (HippoPost *post);
/* list of HippoEntity, list is not copied and entities not ref'd */
GSList*          hippo_post_get_recipients            (HippoPost *post);
GTime            hippo_post_get_date                  (HippoPost *post);

void             hippo_post_set_sender                (HippoPost   *post,
                                                       HippoEntity *sender);
void             hippo_post_set_url                   (HippoPost  *post,
                                                       const char *value);
void             hippo_post_set_title                 (HippoPost  *post,
                                                       const char *value);
void             hippo_post_set_description           (HippoPost  *post,
                                                       const char *value);
void             hippo_post_set_recipients            (HippoPost  *post,
                                                       GSList     *value);
void             hippo_post_set_viewers               (HippoPost  *post,
                                                       GSList     *value);
void             hippo_post_set_date                  (HippoPost  *post,
                                                       GTime       value);

G_END_DECLS

#endif /* __HIPPO_POST_H__ */
