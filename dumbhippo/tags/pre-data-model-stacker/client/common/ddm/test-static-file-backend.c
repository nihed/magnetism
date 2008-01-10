/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "test-utils.h"

int
main(int argc, char **argv)
{
    DDMDataModel *model;

    DDMDataResource *result;
    DDMDataResource *user1;
    DDMDataResource *user2;
    const char *name;
    GSList *contacts;
    GSList *contacters;

    model = test_init(FALSE);

    result = test_query_resource("http://mugshot.org/o/user/USER1", "name;contacts;contacters");

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
