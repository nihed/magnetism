/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <glib.h>
#include <stdlib.h>
#include <string.h>
#include "json.h"


#ifdef BUILD_TESTS
#include <errno.h>
static gboolean
hippo_parse_int32(const char *s,
                                 int        *result)
{
    /*
     * We accept values of the form '\s*\d+\s+'
     */
    
    char *end;
    long v;

    while (g_ascii_isspace(*s))
        ++s;
    
    if (*s == '\0')
        return FALSE;
    
    end = NULL;
    errno = 0;
    v = strtol(s, &end, 10);

    if (errno == ERANGE)
        return FALSE;

    while (g_ascii_isspace(*end))
        end++;

    if (*end != '\0')
        return FALSE;

    *result = v;

    return TRUE;
}
#else /* !BUILD_TESTS */
#include <engine/hippo-engine-basics.h>
#endif


static void
skip_whitespace(const char **p_p)
{
    while (**p_p && g_ascii_isspace(**p_p)) {
        (*p_p) ++;
    }
}

/* this is pretty vulnerable to hostile input, should not crash but
 * probably won't error out properly either
 */
static void
skip_string(const char **p_p)
{
    const char *p;

    p = *p_p;
    
    skip_whitespace(&p);

    if (*p && *p == '"') {
        ++p;
        while (*p) {
            if (*p == '\\') {
                ++p;
                if (*p == 'u') {
                    int i;
                    ++p;
                    for (i = 0; *p && i < 4; ++i)
                        ++p;
                } else {
                    ++p;
                }
            } else if (*p == '"') {
                ++p;
                break;
            } else {
                ++p;
            }
        }
    }
    
    *p_p = p;
}

char**
json_array_split (const char  *s,
                  GError     **error)
{
    GPtrArray *elements;
    const char *p;
    const char *value_start;

    p = s;
    skip_whitespace(&p);

    if (*p != '[') {
        /* lame hack to steal GMarkup's error codes... */
        g_set_error(error, G_MARKUP_ERROR,
                    G_MARKUP_ERROR_PARSE,
                    "No [ at start of array");
        return NULL;
    }

    ++p;

    elements = g_ptr_array_new();
    
    skip_whitespace(&p);    
    while (*p && *p != ']') {
        char *value;
        
        value_start = p;
        if (*p == '"') {
            skip_string(&p);
        }

        while (*p && *p != ',' && *p != ']' && !g_ascii_isspace(*p))
            ++p;

        value = g_strndup(value_start, p - value_start);
        g_ptr_array_add(elements, value);

        if (*p == ',')
            ++p;
        
        skip_whitespace(&p);

        if (*p == ',')
            ++p;
    }

    if (*p != ']') {
        g_set_error(error, G_MARKUP_ERROR, G_MARKUP_ERROR_PARSE,
                    "No ] at end of array");
        g_ptr_array_free(elements, TRUE);
        return NULL;
    }

    g_ptr_array_add(elements, NULL);

    return (char**) g_ptr_array_free(elements, FALSE);
}

/* this is pretty vulnerable to hostile input, should not crash but
 * probably won't error out properly either
 */
char*
json_string_parse (const char  *s,
                   GError     **error)
{
    const char *p;
    GString *str;

    str = g_string_new(NULL);

    p = s;
    skip_whitespace(&p);

    if (*p && *p == '"') {
        ++p;
        while (*p) {
            if (*p == '\\') {
                ++p;
                if (*p == 'u') {
                    int i;
                    const char *end;
                    char buf[5];
                    int unicode_value;
                    
                    ++p;
                    end = p;
                    for (i = 0; *end && i < 4; ++i) {
                        buf[i] = *end;
                        ++end;
                    }
                    buf[4] = '\0';
                    if ((end - p) != 4) {
                        g_set_error(error, G_MARKUP_ERROR,
                                    G_MARKUP_ERROR_PARSE,
                                    "Invalid Unicode escape sequence");
                        g_string_free(str, TRUE);
                        return NULL;
                    }

                    p = end;
                    
                    if (!hippo_parse_int32(buf, &unicode_value)) {
                        g_set_error(error, G_MARKUP_ERROR,
                                    G_MARKUP_ERROR_PARSE,
                                    "Invalid Unicode escape sequence");
                        g_string_free(str, TRUE);
                        return NULL;
                    }

                    if (!g_unichar_validate(unicode_value)) {
                        g_set_error(error, G_MARKUP_ERROR,
                                    G_MARKUP_ERROR_PARSE,
                                    "Invalid Unicode codepoint %d", unicode_value);
                        g_string_free(str, TRUE);
                        return NULL;
                    }
                    
                    g_string_append_unichar(str, unicode_value);                    
                } else {
                    char c = '\0';
                    
                    switch (*p) {
                    case '"':
                    case '\\':
                    case '/':
                        c = *p;
                        break;
                    case 'b':
                        c = '\b';
                        break;
                    case 'f':
                        c = '\f';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    }

                    if (c == '\0') {
                        g_set_error(error, G_MARKUP_ERROR,
                                    G_MARKUP_ERROR_PARSE,
                                    "Invalid escape sequence");
                        g_string_free(str, TRUE);
                        return NULL;
                    }

                    g_string_append_c(str, c);
                    
                    ++p;
                }
            } else if (*p == '"') {
                ++p;
                break;
            } else {
                g_string_append_c(str, *p);
                ++p;
            }
        }
    }

    return g_string_free(str, FALSE);
}

char*
json_string_escape (const char *s)
{
    GString *escaped;
    const char *p;

    escaped = g_string_new(NULL);

    g_string_append_c(escaped, '"');

    p = s;
    while (*p) {        
        gunichar c;
        
        c = g_utf8_get_char(p);
        p = g_utf8_next_char(p);

        if (c < 128) {
            switch (c) {
            case '"':
                g_string_append_c(escaped, '\\');
                g_string_append_c(escaped, '"');
                break;
            case '\\':
                g_string_append_c(escaped, '\\');
                g_string_append_c(escaped, '\\');
                break;
            case '/':
                g_string_append_c(escaped, '\\');
                g_string_append_c(escaped, '/');
                break;
            case '\b':
                g_string_append_c(escaped, '\\');
                g_string_append_c(escaped, 'b');
                break;
            case '\f':
                g_string_append_c(escaped, '\\');
                g_string_append_c(escaped, 'f');
                break;
            case '\n':
                g_string_append_c(escaped, '\\');
                g_string_append_c(escaped, 'n');
                break;
            case '\r':
                g_string_append_c(escaped, '\\');
                g_string_append_c(escaped, 'r');
                break;
            case '\t':
                g_string_append_c(escaped, '\\');
                g_string_append_c(escaped, 't');
                break;
            default:
                g_string_append_unichar(escaped, c);
                break;
            }
        } else {
            g_string_append_printf(escaped, "\\u%04d", c);
        }
    }
    
    g_string_append_c(escaped, '"');

    return g_string_free(escaped, FALSE);
}

#ifdef BUILD_TESTS
/* too confusing when done as a string literal */
static const char escaped_quote[] = { '"', '\\', '"', '"', '\0' };

static const char*
valid_json_strings[] = {
    "\"\"",
    "\"a\"",
    "\"abc\"",
    escaped_quote,
    "\"abc\\u2620def\"",
    "\"\\u2620\"",
    "\"\\u2620def\"",
    "\"abc\\u2620def\"",
    "\"\\u0155\"" /* to check escaping with leading 0, i.e. %04d not %4d */
};

typedef struct {
    const char *text;
    int n_elements;
} ValidJsonArray;

static ValidJsonArray
valid_json_arrays[] = {
    { "[]", 0 },
    { "[ 1 ]", 1 },
    { "[ 1, 2 ]", 2 },
    { "[ 1, 2, 3 ]", 3 },
    { "[ \"abc\", 2, 3 ]", 3 },
    { "[1]", 1 },
    { "[1,2]", 2 },
    { "[1,2,3]", 3},
    { "[\"abc\",2,3]", 3 },
    { "[1.0,2.0]", 2 }
};

/* cc -DBUILD_TESTS -I../config -Wall -ggdb -O2 `pkg-config --cflags --libs glib-2.0` json.c -o json-test && ./json-test */
int
main(int argc, char **argv)
{
    int i;

    for (i = 0; i < (int) G_N_ELEMENTS(valid_json_strings); ++i) {
        const char *s = valid_json_strings[i];
        int len = strlen(s);
        GString *str;
        const char *p;

        g_print("%s\n", s);
        
        /* check skipping a valid string */
        p = s;
        skip_string(&p);
        if ((p - s) != len) {
            g_error("Did not correctly skip %s, skipped %d bytes", s, (int) (p - s));
        }

        /* check skipping with leading whitespace */
        str = g_string_new(s);
        g_string_prepend(str, "   ");

        p = str->str;
        skip_string(&p);
        if ((p - str->str) != (len+3)) {
            g_error("Did not correctly skip %s, skipped %d bytes", str->str, (int) (p - str->str));
        }

        g_string_free(str, TRUE);

        /* check that skipping with trailing space does not skip trailing space */
        str = g_string_new(s);
        g_string_append(str, "   ");

        p = str->str;
        skip_string(&p);
        if ((p - str->str) != len) {
            g_error("Did not correctly skip %s, skipped %d bytes", str->str, (int) (p - str->str));
        }
        
        g_string_free(str, TRUE);

        /* check parsing and escaping */
        {
            GError *error = NULL;
            char *unescaped = json_string_parse(s, &error);
            char *reescaped;
            
            if (unescaped == NULL) {
                g_error("Failed to unescape '%s': %s",
                        s, error->message);
            }

            if (!g_utf8_validate(unescaped, -1, NULL)) {
                g_error("Unescaping led to invalid UTF-8");
            }
            
            g_print("   = '%s'\n", unescaped);

            reescaped = json_string_escape(unescaped);
            if (strcmp(reescaped, s) != 0) {
                g_error("Re-escaping gave '%s' not '%s'",
                        reescaped, s);
            } else {
                g_print("   (re-escaped '%s')\n", reescaped);
            }
            
            g_free(reescaped);
            g_free(unescaped);
        }
    }

    for (i = 0; i < (int) G_N_ELEMENTS(valid_json_arrays); ++i) {
        const ValidJsonArray *a = &valid_json_arrays[i];
        char **split;
        int count;
        GError *error;

        g_print("%s\n", a->text);
        
        error = NULL;
        split = json_array_split(a->text, &error);
        if (split == NULL) {
            g_assert(error != NULL);
            g_error("Failed to split '%s': %s", a->text, error->message);
        }

        for (count = 0; split[count] != NULL; ++count) {
            ; /* do nothing, just counting */
        }

        if (count != a->n_elements) {
            g_error("Failed to split '%s': got %d elements expecting %d", a->text, count, a->n_elements);
        }

        for (count = 0; split[count] != NULL; ++count) {
            g_print("  %s\n", split[count]);
        }
        
        g_strfreev(split);
    }

    g_print("Tests passed\n");
    return 0;
}
#endif

