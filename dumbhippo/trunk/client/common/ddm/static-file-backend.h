#ifndef __STATIC_FILE_BACKEND_H__
#define __STATIC_FILE_BACKEND_H__

#include "ddm-data-model.h"

G_BEGIN_DECLS

DDMDataModel *ddm_static_file_model_new (const char    *filename,
					 GError       **error);

/* Processes all pending requests, avoids having to run the main loop
 */
void ddm_static_file_model_flush (DDMDataModel *model);

/* Used internally to ddm_static_file_model_new
 */
gboolean ddm_static_file_parse (const char    *filename,
				DDMDataModel  *model,
				GError       **error);

/* This one one uses the same XML parsing mechanism that is used to
 * load the backend model to fill in local resources in the frontend
 * model (or any other model.)
 */
gboolean ddm_static_load_local_string(const char   *str,
				      DDMDataModel *mode,
				      GError      **error);

G_END_DECLS

#endif /* __STATIC_FILE_BACKEND_H__ */
