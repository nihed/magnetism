/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "static-file-backend.h"
#include "ddm-data-query.h"

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

static DDMDataResource *
query_resource(DDMDataModel    *model,
               const char      *resource_id,
               const char      *fetch)
{
    DDMDataQuery *query;
    DDMDataResource *result = NULL;
    const char *error = NULL;
    
    query = ddm_data_model_query_resource(model, resource_id, fetch);
    ddm_data_query_set_single_handler(query, on_query_result, &result);
    ddm_data_query_set_error_handler(query, on_query_error, &error);
    
    ddm_static_file_model_flush(model);

    if (error != NULL)
        g_error("Error from getResource, resource_id=%s, fetch=%s: %s", resource_id, fetch, error);

    return result;
}

int
main(int argc, char **argv)
{
    GError *error = NULL;
    DDMDataModel *model;
    const char *srcdir;
    char *filename;

    DDMDataResource *result;
    DDMDataResource *user1;
    DDMDataResource *user2;
    const char *name;
    GSList *contacts;
    GSList *contacters;

    g_type_init();

    srcdir = g_getenv("DDM_SRCDIR");
    if (srcdir == NULL)
        g_error("DDM_SRCDIR is not set");

    filename = g_build_filename(srcdir, "test-data.xml", NULL);
    model = ddm_static_file_model_new(filename, &error);
    if (model == NULL)
        g_error("Failed to create test model: %s", error->message);

    g_free(filename);

    result = query_resource(model, "http://mugshot.org/o/user/USER1", "name;contacts;contacters");

    user1 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER1");
    g_assert(user1 != NULL);

    g_assert(result == user1);
    
    user2 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER2");
    g_assert(user2 != NULL);

    name = NULL;
    contacts = NULL;
    contacters = NULL;
    ddm_data_resource_get(user1,
                          "name", DDM_DATA_STRING, &name,
                          "contacts", DDM_DATA_RESOURCE | DDM_DATA_LIST, &contacts,
                          "contacters", DDM_DATA_RESOURCE | DDM_DATA_LIST, &contacters,
                          NULL);

    g_assert(strcmp(name, "John Doe") == 0);

    g_assert(g_slist_length(contacts) == 1);
    g_assert(contacts->data == user2);

    g_assert(g_slist_length(contacters) == 0);

    return 0;
}
