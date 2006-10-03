/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_POST_H__
#define __HIPPO_POST_H__

#include <hippo/hippo-basics.h>
#include <hippo/hippo-chat-room.h>
#include <loudmouth/loudmouth.h>

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
HippoPost*       hippo_post_new                       (const char *guid);

gboolean         hippo_post_update_from_xml           (HippoPost      *post,
                                                       HippoDataCache *cache,
                                                       LmMessageNode  *node);

const char*      hippo_post_get_guid                  (HippoPost *post);
const char*      hippo_post_get_sender                (HippoPost *post);
const char*      hippo_post_get_url                   (HippoPost *post);
const char*      hippo_post_get_title                 (HippoPost *post);
const char*      hippo_post_get_description           (HippoPost *post);
/* list of HippoEntity, list is not copied and entities not ref'd */
GSList*          hippo_post_get_recipients            (HippoPost *post);
GSList*          hippo_post_get_viewers               (HippoPost *post);
const char*      hippo_post_get_info                  (HippoPost *post);
GTime            hippo_post_get_date                  (HippoPost *post);
int              hippo_post_get_timeout               (HippoPost *post);
gboolean         hippo_post_is_to_world               (HippoPost *post);
int              hippo_post_get_viewing_user_count    (HippoPost *post);
int              hippo_post_get_chatting_user_count   (HippoPost *post);
int              hippo_post_get_total_viewers         (HippoPost *post);
gboolean         hippo_post_get_have_viewed           (HippoPost *post);
gboolean         hippo_post_get_ignored               (HippoPost *post);
gboolean         hippo_post_get_new                   (HippoPost *post);

void             hippo_post_set_sender                (HippoPost  *post,
                                                       const char *value);
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
void             hippo_post_set_info                  (HippoPost  *post,
                                                       const char *value);
void             hippo_post_set_date                  (HippoPost  *post,
                                                       GTime       value);
void             hippo_post_set_to_world              (HippoPost  *post,
                                                       gboolean    world);
void             hippo_post_set_timeout               (HippoPost  *post,
                                                       int         value);
void             hippo_post_set_viewing_user_count    (HippoPost  *post,
                                                       int         value);
void             hippo_post_set_chatting_user_count   (HippoPost  *post,
                                                       int         value);
void             hippo_post_set_total_viewers         (HippoPost  *post,
                                                       int         value);
void             hippo_post_set_have_viewed           (HippoPost  *post,
                                                       gboolean    value);
void             hippo_post_set_ignored               (HippoPost  *post,
                                                       gboolean    value);
void             hippo_post_set_new                   (HippoPost  *post,
                                                       gboolean    value);

HippoChatRoom*   hippo_post_get_chat_room             (HippoPost     *post);
void             hippo_post_set_chat_room             (HippoPost     *post,
                                                       HippoChatRoom *room);

G_END_DECLS

#endif /* __HIPPO_POST_H__ */
