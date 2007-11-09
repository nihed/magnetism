/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "test-utils.h"
#include "ddm-data-query.h"

static DDMDataModel *model;

DDMDataModel *
test_init (gboolean load_local)
{
    GError *error = NULL;
    const char *srcdir;
    char *filename;

    g_type_init();

    srcdir = g_getenv("DDM_SRCDIR");
    if (srcdir == NULL)
        g_error("DDM_SRCDIR is not set");

    filename = g_build_filename(srcdir, "test-data.xml", NULL);
    model = ddm_static_file_model_new(filename, &error);
    if (model == NULL)
        g_error("Failed to create test model: %s", error->message);

    g_free(filename);

    if (load_local) {
        filename = g_build_filename(srcdir, "test-local-data.xml", NULL);
        if (!ddm_static_load_local_file(filename, model, &error))
            g_error("Failed to add_local data to test model: %s", error->message);
        
        g_free(filename);
    }

    return model;
}

DDMDataModel *
test_get_model(void)
{
    return model;
}

void
test_flush (void)
{
    while (ddm_data_model_needs_flush(model))
        ddm_data_model_flush(model);
}

static void
on_query_result (DDMDataResource *result,
                 gpointer         user_data)
{
    DDMDataResource **result_location = user_data;
    *result_location = result;
}

static void
on_query_error (DDMDataError     error,
                const char      *message,
                gpointer         user_data)
{
    const char **message_location = user_data;
    
    g_assert(message != NULL);
    *message_location = g_strdup(message);
}

DDMDataResource *
test_query_resource(const char   *resource_id,
                    const char   *fetch)
{
    DDMDataQuery *query;
    DDMDataResource *result = NULL;
    const char *error = NULL;
    
    query = ddm_data_model_query_resource_by_id(model, resource_id, fetch);
    ddm_data_query_set_single_handler(query, on_query_result, &result);
    ddm_data_query_set_error_handler(query, on_query_error, &error);

    test_flush();

    if (error != NULL)
        g_error("Error from getResource, resource_id=%s, fetch=%s: %s", resource_id, fetch, error);

    return result;
}
