#ifndef __STATIC_FILE_BACKEND_H__
#define __STATIC_FILE_BACKEND_H__

#include "ddm-data-model.h"

G_BEGIN_DECLS

DDMDataModel *ddm_static_file_model_new   (const char    *filename,
					   GError       **error);

void ddm_static_file_model_flush (DDMDataModel *model);

gboolean ddm_static_file_parse (const char    *filename,
				DDMDataModel  *model,
				GError       **error);

G_END_DECLS

#endif /* __STATIC_FILE_BACKEND_H__ */
