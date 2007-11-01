/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __DDM_LOCAL_CLIENT_H__
#define __DDM_LOCAL_CLIENT_H__

#include "ddm-client.h"
#include "ddm-data-model.h"

G_BEGIN_DECLS

/* Stub client used for connections directly to the data model made within
 * process; most of the real handling of this is inside DDMDataResource -
 * for example, we track the connections directly on the DDMDataResource.
 */

typedef struct _DDMLocalClient      DDMLocalClient;
typedef struct _DDMLocalClientClass DDMLocalClientClass;

#define DDM_TYPE_LOCAL_CLIENT              (_ddm_local_client_get_type ())
#define DDM_LOCAL_CLIENT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), DDM_TYPE_LOCAL_CLIENT, DDMLocalClient))
#define DDM_LOCAL_CLIENT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), DDM_TYPE_LOCAL_CLIENT, DDMLocalClientClass))
#define DDM_IS_LOCAL_CLIENT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), DDM_TYPE_LOCAL_CLIENT))
#define DDM_IS_LOCAL_CLIENT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), DDM_TYPE_LOCAL_CLIENT))
#define DDM_LOCAL_CLIENT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), DDM_TYPE_LOCAL_CLIENT, DDMLocalClientClass))

GType _ddm_local_client_get_type (void) G_GNUC_CONST;

DDMClient *_ddm_local_client_new (DDMDataModel *model);

G_END_DECLS

#endif /* __DDM_LOCAL_CLIENT_H__ */
