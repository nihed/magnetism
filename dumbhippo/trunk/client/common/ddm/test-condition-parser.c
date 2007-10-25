/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "ddm-rule.h"

static gboolean result = TRUE;

static void
do_test(const char *input,
        const char *expected_output)
{
    DDMCondition *condition;
    char *output;

    condition = ddm_condition_from_string(input);
    if (condition == NULL) {
        g_warning("FAILED: %s: parse/syntax error", input);
        result = FALSE;
        return;
    }
    
    output = ddm_condition_to_string(condition);
    if (strcmp(output, expected_output) != 0) {
        g_warning("FAILED: %s: Got '%s', expected '%s'", input, output, expected_output);
        result = FALSE;
        return;
    }

    ddm_condition_free(condition);

    /*    g_debug("SUCCESS: %s", input); */
}

static void
do_test_i(const char *input)
{
    do_test(input, input);
}

int main(void)
{
    do_test_i("source.s = \"Parking Lot\"");
    do_test_i("target.s = \"Parking Lot\"");
    do_test_i("source.b = true");
    do_test_i("source.b = false");
    do_test_i("source.i = 1");
    do_test_i("source.i = 1");
    do_test_i("source.a = 1 or source.b = 2");
    do_test_i("source.a = 1 and source.b = 2");
    do_test_i("not source.a = 1");
    do_test("source.a", "source.a = true");
    do_test("true", "true");
    do_test("false", "false");
    do_test("target.a = 1 and target.b = 2 or not source.c = 3",
            "(target.a = 1 and target.b = 2) or (not source.c = 3)");
    
    return result ? 0 : 1;
}
