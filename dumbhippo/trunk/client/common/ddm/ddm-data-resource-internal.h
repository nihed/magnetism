/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_RESOURCE_INTERNAL_H__
#define __DDM_DATA_RESOURCE_INTERNAL_H__

#include <ddm/ddm-data-fetch.h>
#include <ddm/ddm-data-resource.h>
#include <ddm/ddm-data-model.h>
#include "ddm-client-notification.h"

G_BEGIN_DECLS

DDMDataResource *_ddm_data_resource_new (DDMDataModel    *model,
                                         const char      *resource_id,
                                         const char      *class_id,
                                         gboolean         local);

/* Called on reconnection to the backend server. returns TRUE if the resource
 * should be removed from the resource table.
 */
gboolean _ddm_data_resource_reset (DDMDataResource *resource);

GSList *_ddm_data_resource_get_default_properties (DDMDataResource *resource);

DDMDataFetch *_ddm_data_resource_get_received_fetch   (DDMDataResource *resource);
DDMDataFetch *_ddm_data_resource_get_requested_fetch  (DDMDataResource *resource);
gint64        _ddm_data_resource_get_requested_serial (DDMDataResource *resource);

void _ddm_data_resource_fetch_requested (DDMDataResource *resource,
                                         DDMDataFetch    *fetch,
                                         guint64          serial);
void _ddm_data_resource_fetch_received  (DDMDataResource *resource,
                                         DDMDataFetch    *received_fetch);

void _ddm_data_resource_send_local_notifications (DDMDataResource    *resource,
                                                  GSList             *changed_properties);

void _ddm_data_resource_update_rule_properties(DDMDataResource *resource);

void _ddm_data_resource_resolve_notifications (DDMDataResource          *resource,
                                               DDMClientNotificationSet *notification_set);

void _ddm_data_resource_dump(DDMDataResource *resource);

G_END_DECLS

#endif /* __DDM_DATA_RESOURCE_INTERNAL_H__ */
