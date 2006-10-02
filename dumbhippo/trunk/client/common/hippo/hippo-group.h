/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_GROUP_H__
#define __HIPPO_GROUP_H__

#include <hippo/hippo-entity.h>

G_BEGIN_DECLS

typedef struct _HippoGroup      HippoGroup;
typedef struct _HippoGroupClass HippoGroupClass;

#define HIPPO_TYPE_GROUP              (hippo_group_get_type ())
#define HIPPO_GROUP(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_GROUP, HippoGroup))
#define HIPPO_GROUP_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_GROUP, HippoGroupClass))
#define HIPPO_IS_GROUP(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_GROUP))
#define HIPPO_IS_GROUP_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_GROUP))
#define HIPPO_GROUP_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_GROUP, HippoGroupClass))

GType            hippo_group_get_type                  (void) G_GNUC_CONST;
HippoGroup*      hippo_group_new                       (const char  *guid);

HippoChatRoom* hippo_group_get_chat_room           (HippoGroup    *group);
int            hippo_group_get_chatting_user_count (HippoGroup    *group);
GTime          hippo_group_get_date_last_ignored   (HippoGroup    *group);
gboolean       hippo_group_get_ignored             (HippoGroup    *group);
void           hippo_group_set_chat_room           (HippoGroup    *group,
                                                    HippoChatRoom *room);
void           hippo_group_set_date_last_ignored   (HippoGroup    *group,
                                                    GTime          date);
void           hippo_group_set_ignored             (HippoGroup    *group,
                                                    gboolean       is_ignored);

G_END_DECLS

#endif /* __HIPPO_GROUP_H__ */
