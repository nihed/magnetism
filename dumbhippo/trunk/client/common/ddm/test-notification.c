/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>
#include "test-utils.h"

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

    user2 = ddm_data_model_lookup_resource(test_get_model(), "http://mugshot.org/o/user/USER2");
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
    DDMDataModel *model;
    
    DDMDataResource *result;
    DDMDataResource *user1;
    DDMDataResource *user2;
    DDMDataResource *buddy1;
    DDMDataResource *user;

    DDMDataValue value;

    gboolean was_changed = FALSE;

    model = test_init(TRUE);

    result = test_query_resource("online-desktop:/o/pidgin-buddy/AIM.JohnDoe1", "fixedUser name");

    buddy1 = ddm_data_model_lookup_resource(model, "online-desktop:/o/pidgin-buddy/AIM.JohnDoe1");
    g_assert(buddy1 != NULL);

    user1 = ddm_data_model_lookup_resource(model, "http://mugshot.org/o/user/USER1");
    g_assert(user1 != NULL);

    user = NULL;
    ddm_data_resource_get(buddy1,
                          "fixedUser", DDM_DATA_RESOURCE, &user,
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
                                      ddm_qname_get("online-desktop://p/o/buddy", "fixedUser"),
                                      DDM_DATA_UPDATE_REPLACE, DDM_DATA_CARDINALITY_01,
                                      FALSE, NULL,
                                      &value);

    test_flush();

    g_assert(was_changed);

    return 0;
}
