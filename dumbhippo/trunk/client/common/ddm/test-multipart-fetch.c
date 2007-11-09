/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "test-utils.h"

int
main(int argc, char **argv)
{
    DDMDataModel *model;

    DDMDataResource *result;
    DDMDataResource *user1;
    DDMDataResource *buddy1;
    DDMDataResource *user;

    model = test_init(TRUE);
    
    result = test_query_resource("online-desktop:/o/pidgin-buddy/AIM.JohnDoe1", "fixedUser name");

    buddy1 = ddm_data_model_lookup_resource(model, "online-desktop:/o/pidgin-buddy/AIM.JohnDoe1");
    g_assert(buddy1 != NULL);

    user1 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER1");
    g_assert(user1 != NULL);

    user = NULL;
    ddm_data_resource_get(buddy1,
                          "fixedUxuser", DDM_DATA_RESOURCE, &user,
                          NULL);

    g_assert(user == user1);

    return 0;
}
