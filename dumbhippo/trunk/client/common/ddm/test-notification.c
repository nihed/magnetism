/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "static-file-backend.h"
#include "ddm-data-query.h"

static DDMDataModel *model = NULL;

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

static void
flush_model(DDMDataModel *model) {
    while (ddm_data_model_needs_flush(model))
        ddm_data_model_flush(model);

}

static DDMDataResource *
query_resource(DDMDataModel    *model,
               const char      *resource_id,
               const char      *fetch)
{
    DDMDataQuery *query;
    DDMDataResource *result = NULL;
    const char *error = NULL;
    
    query = ddm_data_model_query_resource_by_id(model, resource_id, fetch);
    ddm_data_query_set_single_handler(query, on_query_result, &result);
    ddm_data_query_set_error_handler(query, on_query_error, &error);

    flush_model(model);

    if (error != NULL)
        g_error("Error from getResource, resource_id=%s, fetch=%s: %s", resource_id, fetch, error);

    return result;
}

static void
on_buddy1_changed(DDMDataResource *resource,
                  GSList          *changed_properties,
                  gpointer         data)
{
    gboolean *was_changed = data;
    DDMDataResource *user;
    DDMDataResource *user2;
    const char *name;

    *was_changed = TRUE;

    g_assert(strcmp(ddm_data_resource_get_resource_id(resource), "online-desktop:/o/pidgin-buddy/AIM.JohnDoe1") == 0);

    user2 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER2");
    g_assert(user2 != NULL);

    user = NULL;
    ddm_data_resource_get(resource,
                          "user", DDM_DATA_RESOURCE, &user,
                          NULL);

    g_assert(user == user2);

    name = NULL;
    ddm_data_resource_get(user,
                          "name", DDM_DATA_STRING, &name,
                          NULL);

    g_assert(strcmp(name, "Sally Smith") == 0);
}

int
main(int argc, char **argv)
{
    GError *error = NULL;
    const char *srcdir;
    char *filename;

    DDMDataResource *result;
    DDMDataResource *user1;
    DDMDataResource *user2;
    DDMDataResource *buddy1;
    DDMDataResource *user;

    DDMDataValue value;

    gboolean was_changed = FALSE;

    g_type_init();

    srcdir = g_getenv("DDM_SRCDIR");
    if (srcdir == NULL)
        g_error("DDM_SRCDIR is not set");

    filename = g_build_filename(srcdir, "test-data.xml", NULL);
    model = ddm_static_file_model_new(filename, &error);
    if (model == NULL)
        g_error("Failed to create test model: %s", error->message);

    g_free(filename);

    filename = g_build_filename(srcdir, "test-local-data.xml", NULL);
    if (!ddm_static_load_local_file(filename, model, &error))
        g_error("Failed to add_local data to test model: %s", error->message);

    g_free(filename);

    result = query_resource(model, "online-desktop:/o/pidgin-buddy/AIM.JohnDoe1", "user name");

    buddy1 = ddm_data_model_lookup_resource(model, "online-desktop:/o/pidgin-buddy/AIM.JohnDoe1");
    g_assert(buddy1 != NULL);

    user1 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER1");
    g_assert(user1 != NULL);

    user = NULL;
    ddm_data_resource_get(buddy1,
                          "user", DDM_DATA_RESOURCE, &user,
                          NULL);

    g_assert(user == user1);

    ddm_data_resource_connect(buddy1, NULL, on_buddy1_changed, &was_changed);

    /* This change will trigger the immediate change to user1, but it should also
     * cause a fetch from upstream of the name property of user2, since our stored
     * fetch for buddy1 is 'user name' and we haven't yet fetched name for user2
     */
    user2 = ddm_data_model_ensure_resource(model, "http://mugshot.org/o/user/USER2", NULL);

    value.type = DDM_DATA_RESOURCE;
    value.u.resource = user2;

    ddm_data_resource_update_property(buddy1,
                                      ddm_qname_get("online-desktop://p/o/buddy", "user"),
                                      DDM_DATA_UPDATE_REPLACE, DDM_DATA_CARDINALITY_01,
                                      FALSE, NULL,
                                      &value);

    flush_model(model);

    g_assert(was_changed);

    return 0;
}
