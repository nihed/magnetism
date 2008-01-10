/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */

#ifndef __DDM_CLIENT_H__
#define __DDM_CLIENT_H__

#include <ddm/ddm-data-resource.h>
#include <glib-object.h>

G_BEGIN_DECLS

/* DDMClient represents a consumer of the data model; there is one DDMClient
 * (a DDMLocalClient) used to represent use of the data model directly from
 * within the same process. A more typical use of DDMClient would be to represent
 * another application consuming the data model via D-BUS.
 */

typedef struct _DDMClientIface DDMClientIface;

#define DDM_TYPE_CLIENT              (ddm_client_get_type ())
#define DDM_CLIENT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), DDM_TYPE_CLIENT, DDMClient))
#define DDM_IS_CLIENT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), DDM_TYPE_CLIENT))
#define DDM_CLIENT_GET_IFACE(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), DDM_TYPE_CLIENT, DDMClientIface))

struct _DDMClientIface
{
    GTypeInterface parent_iface;

    /* Start notification; the returned gpointer is sent to all notify()
     * calls that are part of the process and to end_notification()
     */
    gpointer (* begin_notification) (DDMClient       *client);

    /* Called for each resource that has changed
     */
    void     (* notify)             (DDMClient       *client,
                                     DDMDataResource *resource,
                                     GSList          *changed_properties,
                                     gpointer         notification_data);

    /* This notification is all done, send it off
     */
    void     (* end_notification)   (DDMClient       *client,
                                     gpointer         notification_data);
};

GType ddm_client_get_type(void) G_GNUC_CONST;

gpointer ddm_client_begin_notification (DDMClient       *client);
void     ddm_client_notify             (DDMClient       *client,
                                        DDMDataResource *resource,
                                        GSList          *changed_properties,
                                        gpointer         notification_data);
void     ddm_client_end_notification   (DDMClient       *client,
                                        gpointer         notification_data);

G_END_DECLS

#endif /* __DDM_CLIENT_H__ */
