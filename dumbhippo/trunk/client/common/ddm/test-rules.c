/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "test-utils.h"

int
main(int argc, char **argv)
{
    DDMDataModel *model;

    DDMDataResource *user1;
    DDMDataResource *user2;
    DDMDataResource *buddy1;
    DDMDataResource *user;
    GSList *aimBuddies;

    DDMDataValue value;

    model = test_init(TRUE);

    ddm_data_model_add_rule(model,
                            "online-desktop:/p/o/buddy",
                            "online-desktop:/p/o/buddy#user",
                            "http://mugshot.org/p/o/user",
                            DDM_DATA_CARDINALITY_01, FALSE, NULL,
                            "source.aim = target.name and target.protocol = 'aim'");
    
    ddm_data_model_add_rule(model,
                            "online-desktop:/p/o/global",
                            "online-desktop:/p/o/global#aimBuddies",
                            "online-desktop:/p/o/buddy",
                            DDM_DATA_CARDINALITY_N, FALSE, NULL,
                            "source.protocol = 'aim'");

    user1 = test_query_resource("http://mugshot.org/o/user/USER1", "aim");
    g_assert(user1 != NULL);
    
    user2 = test_query_resource("http://mugshot.org/o/user/USER2", "aim");
    g_assert(user2 != NULL);
    
    buddy1 = ddm_data_model_lookup_resource(model, "online-desktop:/o/pidgin-buddy/AIM.JohnDoe1");
    g_assert(buddy1 != NULL);

    /* Test that the rule was computed on initial data load */

    ddm_data_resource_get(buddy1,
                          "user", DDM_DATA_RESOURCE, &user,
                          NULL);
    g_assert(user == user1);

    /* Try changing the source property, check that the rule-property gets unset */

    value.type = DDM_DATA_STRING;
    value.u.string = "JohnDoe2";

    ddm_data_resource_update_property(buddy1,
                                      ddm_qname_get("online-desktop://p/o/buddy", "name"),
                                      DDM_DATA_UPDATE_REPLACE, DDM_DATA_CARDINALITY_1,
                                      FALSE, NULL,
                                      &value);

    test_flush();

    ddm_data_resource_get(buddy1,
                          "user", DDM_DATA_RESOURCE, &user,
                          NULL);
    g_assert(user == NULL);
    
    /* Change it again in a way that will cause it to get reset */

    value.type = DDM_DATA_STRING;
    value.u.string = "SSCoolJ";

    ddm_data_resource_update_property(buddy1,
                                      ddm_qname_get("online-desktop://p/o/buddy", "name"),
                                      DDM_DATA_UPDATE_REPLACE, DDM_DATA_CARDINALITY_1,
                                      FALSE, NULL,
                                      &value);

    test_flush();

    ddm_data_resource_get(buddy1,
                          "user", DDM_DATA_RESOURCE, &user,
                          NULL);
    g_assert(user == user2);

    /* Test our list valued rule */
    
    ddm_data_resource_get(ddm_data_model_get_global_resource(model),
                          "aimBuddies", DDM_DATA_RESOURCE | DDM_DATA_LIST, &aimBuddies,
                          NULL);

    g_assert(g_slist_length(aimBuddies) == 1);
    g_assert(aimBuddies->data == buddy1);
    
    return 0;
}
