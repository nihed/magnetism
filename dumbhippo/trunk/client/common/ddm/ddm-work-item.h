/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __DDM_WORK_ITEM_H__
#define __DDM_WORK_ITEM_H__

#include "ddm-client.h"
#include "ddm-data-fetch.h"
#include "ddm-data-model.h"

G_BEGIN_DECLS

/* A "work item", is a task that is queued pending on completion of all
 * fetches needed to execute it.
 *
 * We have two types of work item currently:
 *
 *  - An pending notification, either externally to a "client", or locally.
 *    (represented as being to to the "local client")
 *  - The response to a query IQ
 */

typedef struct _DDMWorkItem DDMWorkItem;

DDMWorkItem *_ddm_work_item_notify_client_new (DDMDataModel    *model,
					       DDMClient       *client);

void _ddm_work_item_notify_client_add (DDMWorkItem     *item,
				       DDMDataResource *resource,
				       DDMDataFetch    *fetch,
				       GSList          *changed_properties);

DDMWorkItem *_ddm_work_item_query_response_new (DDMDataModel    *model,
						DDMDataQuery    *query);

DDMWorkItem *_ddm_work_item_ref              (DDMWorkItem *item);
void         _ddm_work_item_unref            (DDMWorkItem *item);


/* Try to execute the item; a TRUE return means that it was executed,
 * and can be freed; a FALSE return means that additional fetches
 * have been sent upstream, item->min_serial has been updated, and
 * the item needs to be requeued.
 */
gboolean _ddm_work_item_process (DDMWorkItem *item);

/* The item can't continue until a response has been received for
 * this query serial */
gint64       _ddm_work_item_get_min_serial   (const DDMWorkItem *item);

G_END_DECLS

#endif /* __DDM_WORK_ITEM_H__ */
