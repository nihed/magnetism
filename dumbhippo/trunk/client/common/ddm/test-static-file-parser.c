/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "static-file-backend.h"

int
main(int argc, char **argv)
{
    GError *error = NULL;
    DDMDataModel *model;
    const char *srcdir;
    char *filename;

    DDMDataResource *user1;
    DDMDataResource *user2;
    const char *name;
    GSList *contacts;
    GSList *contacters;

    g_type_init();

    model = ddm_data_model_get_default();

    srcdir = g_getenv("DDM_SRCDIR");
    if (srcdir == NULL)
        g_error("DDM_SRCDIR is not set");

    filename = g_build_filename(srcdir, "test-data.xml", NULL);
    if (!ddm_static_file_parse(filename, model, &error))
        g_error("Failed to parse test data: %s", error->message);

    g_free(filename);

    user1 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER1");
    g_assert(user1 != NULL);
    
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
