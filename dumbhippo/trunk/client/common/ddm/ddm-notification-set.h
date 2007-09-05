/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */

#ifndef __DDM_NOTIFICATION_SET_H__
#define __DDM_NOTIFICATION_SET_H__

#include <ddm/ddm.h>

G_BEGIN_DECLS

typedef struct _DDMNotificationSet      DDMNotificationSet;

typedef void (*DDMNotificationSetForeachFunc) (DDMNotificationSet *notifications,
                                               DDMDataResource    *resource,
                                               GSList             *changed_properties,
                                               void               *data);

DDMNotificationSet *ddm_notification_set_new          (DDMDataModel                  *model);
void                ddm_notification_set_free         (DDMNotificationSet            *notifications);
void                ddm_notification_set_add          (DDMNotificationSet            *notifications,
                                                       DDMDataResource               *resource,
                                                       DDMQName                      *property_id);
void                ddm_notification_set_send         (DDMNotificationSet            *notifications);
gboolean            ddm_notification_set_has_property (DDMNotificationSet            *notifications,
                                                       const char                    *resource_id,
                                                       DDMQName                      *property_id);
void                ddm_notification_set_foreach      (DDMNotificationSet            *notifications,
                                                       DDMNotificationSetForeachFunc  func,
                                                       void                          *data);



G_END_DECLS

#endif /* __DDM_NOTIFICATION_SET_H__ */
