/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "static-file-backend.h"
#include "ddm-rule.h"

static gboolean result = TRUE;

static void
do_test(const char      *input,
        DDMDataResource *target,
        DDMDataResource *source,
        gboolean         expected_result)
{
    DDMCondition *condition;
    DDMCondition *reduced;
    gboolean matches;

    condition = ddm_condition_from_string(input);
    if (condition == NULL) {
        g_warning("FAILED: %s: parse/syntax error", input);
        result = FALSE;
        return;
    }

    reduced = ddm_condition_reduce_target(condition, target);
    matches = ddm_condition_matches_source(reduced, source);
    if (!matches != !expected_result) {
        g_warning("FAILED: %s, target=%s, source=%s: Got: %s, expected: %s",
                  input,
                  ddm_data_resource_get_resource_id(target),
                  ddm_data_resource_get_resource_id(source),
                  matches ? "true" : "false",
                  expected_result ? "true" : "false");
        result = FALSE;
    }
    ddm_condition_free(reduced);

    reduced = ddm_condition_reduce_source(condition, source);
    matches = ddm_condition_matches_target(reduced, target);
    if (!matches != !expected_result) {
        g_warning("FAILED: %s, source=%s, target=%s: Got: %s, expected: %s",
                  input,
                  ddm_data_resource_get_resource_id(source),
                  ddm_data_resource_get_resource_id(target),
                  matches ? "true" : "false",
                  expected_result ? "true" : "false");
        result = FALSE;
    }
    
    ddm_condition_free(condition);
}

int
main(int argc, char **argv)
{
    GError *error = NULL;
    DDMDataModel *model;
    const char *srcdir;
    char *filename;

    DDMDataResource *user1;
    DDMDataResource *user2;

    g_type_init();

    model = ddm_data_model_new_no_backend();

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

    do_test("target.name = 'John Doe'", user1, user2, TRUE);
    do_test("target.name = 'John Doe'", user2, user1, FALSE);
    do_test("source.name = 'Sally Smith'", user1, user2, TRUE);
    do_test("source.name = 'Sally Smith'", user2, user1, FALSE);

    /* Some tests of missing values */
    do_test("source.unset = false",        user1, user2, TRUE);
    do_test("source.unset = true",         user1, user2, FALSE);
    do_test("source.unset = target.unset", user2, user1, TRUE);

    /* target.<property> = source should result in a property identical to
     * to <resource>
     */
    do_test("target.contacts = source", user1, user2, TRUE);
    do_test("target.contacts = source", user2, user1, FALSE);

    /* source.<property> = target is a more useful inverse-mapping
     */
    do_test("source.contacts = target", user2, user1, TRUE);
    do_test("source.contacts = target", user2, user1, TRUE);
    
    return result ? 0 : 1;
}
