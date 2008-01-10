/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __TEST_UTILS_H__
#define __TEST_UTILS_H__

#include "static-file-backend.h"
#include "ddm-data-query.h"

G_BEGIN_DECLS

DDMDataModel *   test_init           (gboolean    load_local);
DDMDataModel *   test_get_model      (void);
void             test_flush          (void);
DDMDataResource *test_query_resource (const char *resource_id,
				      const char *fetch);

G_END_DECLS

#endif /* __TEST_UTILS_H__ */
