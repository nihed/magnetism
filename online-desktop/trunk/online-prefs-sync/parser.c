/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n.h>
#include <string.h>
#include "parser.h"

typedef struct {

    /* not sorted by priority yet */
    GSList *entries;

} ParseData;

typedef struct {
    const char *name;
    int value;
} StringEnumPair;

static const StringEnumPair priorities[] = {
    { "lowest", ENTRY_PRIORITY_LOWEST },
    { "provided-by-app", ENTRY_PRIORITY_PROVIDED_BY_APP },
    { "provided-by-os-vendor", ENTRY_PRIORITY_PROVIDED_BY_OS_VENDOR },
    { "provided-by-local-site", ENTRY_PRIORITY_PROVIDED_BY_LOCAL_SITE },
    { "provided-by-user", ENTRY_PRIORITY_PROVIDED_BY_USER },
    { "provided-by-local-site-and-locked-down", ENTRY_PRIORITY_PROVIDED_BY_LOCAL_SITE_AND_LOCKED_DOWN },
    { NULL, 0 }

};

static const StringEnumPair scopes[] = {
    { "not-saved-remotely", KEY_SCOPE_NOT_SAVED_REMOTELY },
    { "saved-per-machine", KEY_SCOPE_SAVED_PER_MACHINE },
    { "saved-per-user", KEY_SCOPE_SAVED_PER_USER },
    { NULL, 0 }
};

static void
set_error (GError             **err,
           GMarkupParseContext *context,
           int                  error_domain,
           int                  error_code,
           const char          *format,
           ...)
{
    int line, ch;
    va_list args;
    char *str;

    g_markup_parse_context_get_position (context, &line, &ch);

    va_start (args, format);
    str = g_strdup_vprintf (format, args);
    va_end (args);

    g_set_error (err, error_domain, error_code,
                 _("Line %d character %d: %s"),
                 line, ch, str);

    g_free (str);
}

static char*
values_as_string(const StringEnumPair *pairs)
{
    int i;
    GString *str;

    str = g_string_new(NULL);

    for (i = 0; pairs[i].name != NULL; ++i) {
        g_string_append(str, pairs[i].name);
        g_string_append(str, " ");
    }

    return g_string_free(str, FALSE);
}

static gboolean
parse_enum(GMarkupParseContext  *context,
           const StringEnumPair *pairs,
           const char           *s,
           int                  *value_p,
           GError              **error)
{
    int i;

    for (i = 0; pairs[i].name != NULL; ++i) {
        if (strcmp(s, pairs[i].name) == 0) {
            *value_p = pairs[i].value;
            return TRUE;
        }
    }

    if (error) {
        char *values_str;
        values_str = values_as_string(pairs);
        g_set_error(error, G_MARKUP_ERROR, G_MARKUP_ERROR_INVALID_CONTENT,
                    "Invalid value '%s'; valid values are one of: (%s)",
                    s, values_str);
        g_free(values_str);
    }
    return FALSE;
}

static gboolean
parse_priority(GMarkupParseContext  *context,
               const char           *s,
               EntryPriority        *priority_p,
               GError              **error)
{
    return parse_enum(context, priorities, s, (int*) priority_p, error);
}

static gboolean
parse_scope(GMarkupParseContext  *context,
            const char           *s,
            KeyScope             *scope_p,
            GError              **error)
{
    return parse_enum(context, scopes, s, (int*) scope_p, error);
}

typedef struct
{
    const char  *name;
    const char **retloc;
} LocateAttr;

static gboolean
locate_attributes (GMarkupParseContext *context,
                   const char  *element_name,
                   const char **attribute_names,
                   const char **attribute_values,
                   GError     **error,
                   const char  *first_attribute_name,
                   const char **first_attribute_retloc,
                   ...)
{
    va_list args;
    const char *name;
    const char **retloc;
    int n_attrs;
#define MAX_ATTRS 24
    LocateAttr attrs[MAX_ATTRS];
    gboolean retval;
    int i;

    g_return_val_if_fail (first_attribute_name != NULL, FALSE);
    g_return_val_if_fail (first_attribute_retloc != NULL, FALSE);

    retval = TRUE;

    n_attrs = 1;
    attrs[0].name = first_attribute_name;
    attrs[0].retloc = first_attribute_retloc;
    *first_attribute_retloc = NULL;

    va_start (args, first_attribute_retloc);

    name = va_arg (args, const char*);
    retloc = va_arg (args, const char**);

    while (name != NULL) {
        g_return_val_if_fail (retloc != NULL, FALSE);

        g_assert (n_attrs < MAX_ATTRS);

        attrs[n_attrs].name = name;
        attrs[n_attrs].retloc = retloc;
        n_attrs += 1;
        *retloc = NULL;

        name = va_arg (args, const char*);
        retloc = va_arg (args, const char**);
    }

    va_end (args);

    if (!retval)
        return retval;

    i = 0;
    while (attribute_names[i]) {
        int j;
        gboolean found;

        found = FALSE;
        j = 0;
        while (j < n_attrs) {
            if (strcmp (attrs[j].name, attribute_names[i]) == 0) {
                retloc = attrs[j].retloc;

                if (*retloc != NULL) {
                    set_error (error, context,
                               G_MARKUP_ERROR,
                               G_MARKUP_ERROR_PARSE,
                               _("Attribute \"%s\" repeated twice on the same <%s> element"),
                               attrs[j].name, element_name);
                    retval = FALSE;
                    goto out;
                }

                *retloc = attribute_values[i];
                found = TRUE;
            }

            ++j;
        }

        if (!found) {
            set_error (error, context,
                       G_MARKUP_ERROR,
                       G_MARKUP_ERROR_PARSE,
                       _("Attribute \"%s\" is invalid on <%s> element in this context"),
                       attribute_names[i], element_name);
            retval = FALSE;
            goto out;
        }

        ++i;
    }

 out:
    return retval;
}

static gboolean
check_no_attributes (GMarkupParseContext *context,
                     const char  *element_name,
                     const char **attribute_names,
                     const char **attribute_values,
                     GError     **error)
{
    if (attribute_names[0] != NULL) {
        set_error (error, context,
                   G_MARKUP_ERROR,
                   G_MARKUP_ERROR_PARSE,
                   _("Attribute \"%s\" is invalid on <%s> element in this context"),
                   attribute_names[0], element_name);
        return FALSE;
    }

    return TRUE;
}


static void
element_key(GMarkupParseContext *context,
            const gchar         *element_name,
            const gchar        **attribute_names,
            const gchar        **attribute_values,
            ParseData           *pd,
            GError             **error)
{
    const char *name_attr;
    const char *scope_attr;
    const char *priority_attr;
    gboolean exact_match_only;
    KeyScope scope;
    EntryPriority priority;
    const char *star;
    ParsedEntry *entry;
    
    if (!locate_attributes (context, element_name, attribute_names, attribute_values,
                            error,
                            "name", &name_attr, "scope", &scope_attr, "priority", &priority_attr,
                            NULL))
        return;

    if (name_attr == NULL) {
        set_error(error, context, G_MARKUP_ERROR, G_MARKUP_ERROR_PARSE,
                  _("No \"name\" attribute on element <%s>"), element_name);
        return;
    }

    scope = KEY_SCOPE_SAVED_PER_USER;
    if (scope_attr != NULL) {
        if (!parse_scope(context, scope_attr, &scope, error))
            return;
    }

    priority = ENTRY_PRIORITY_LOWEST;
    if (priority_attr != NULL) {
        if (!parse_priority(context, priority_attr, &priority, error))
            return;
    }

    star = strstr(name_attr, "*");
    if (star == NULL) {
        exact_match_only = TRUE;
    } else {
        int len;
        len = strlen(name_attr);
        if (star == (name_attr + len - 1)) {
            exact_match_only = FALSE;
        } else {
            set_error(error, context, G_MARKUP_ERROR, G_MARKUP_ERROR_INVALID_CONTENT,
                      _("A '*' is only allowed at the end of a string; arbitrary globs such as '%s' are not supported"),
                      name_attr);
            return;
        }
    }

    entry = g_new0(ParsedEntry, 1);

    if (exact_match_only)
        entry->key = g_strdup(name_attr);
    else
        entry->key = g_strndup(name_attr, strlen(name_attr) - 2);

    entry->scope = scope;
    entry->priority = priority;
    entry->exact_match_only = exact_match_only;

    pd->entries = g_slist_prepend(pd->entries, entry);
}

/* Called for open tags <foo bar="baz"> */
static void
handle_start_element(GMarkupParseContext *context,
                     const gchar         *element_name,
                     const gchar        **attribute_names,
                     const gchar        **attribute_values,
                     gpointer             user_data,
                     GError             **error)
{
    ParseData *pd = user_data;
    
    if (strcmp(element_name, "online_sync") == 0) {
        /* root element, do nothing except set an error if it has attributes */
        check_no_attributes(context, element_name,
                            attribute_names, attribute_values,
                            error);
    } else if (strcmp(element_name, "key") == 0) {
        return element_key(context, element_name,
                           attribute_names, attribute_values,
                           pd, error);
    } else {
        set_error(error, context, G_MARKUP_ERROR,
                  G_MARKUP_ERROR_UNKNOWN_ELEMENT,
                  "Unknown element '%s'", element_name);
    }
}

/* Called for close tags </foo> */
static void
handle_end_element(GMarkupParseContext *context,
                   const gchar         *element_name,
                   gpointer             user_data,
                   GError             **error)
{

}

/* Called for character data */
/* text is not nul-terminated */
static void
handle_text(GMarkupParseContext *context,
            const gchar         *text,
            gsize                text_len,
            gpointer             user_data,
            GError             **error)
{

}

/* Called for strings that should be re-saved verbatim in this same
 * position, but are not otherwise interpretable.  At the moment
 * this includes comments and processing instructions.
 */
/* text is not nul-terminated. */
static void
handle_passthrough(GMarkupParseContext *context,
                   const gchar         *passthrough_text,
                   gsize                text_len,
                   gpointer             user_data,
                   GError             **error)
{

}


/* Called on error, including one set by other
 * methods in the vtable. The GError should not be freed.
 */
static void
handle_error(GMarkupParseContext *context,
             GError              *error,
             gpointer             user_data)
{

}

static GMarkupParser parser = {
    handle_start_element,
    handle_end_element,
    handle_text,
    handle_passthrough,
    handle_error
};

gboolean
parse_entries(const char    *filename,
              ParsedEntry ***entries_p,
              int           *entries_len_p)
{
    char *contents;
    gsize contents_len;
    GMarkupParseContext *context;
    GError *error;
    gboolean success;
    ParseData pd;
    ParsedEntry **entries;
    int entries_len;
    int i;
    
    success = FALSE;

    /* ignore error here since we want to silently ignore file open failures */
    if (!g_file_get_contents(filename, &contents, &contents_len, NULL))
        return FALSE;

    pd.entries = NULL;

    context = g_markup_parse_context_new(&parser, G_MARKUP_TREAT_CDATA_AS_TEXT,
                                         &pd, NULL);

    error = NULL;
    if (!g_markup_parse_context_parse(context, contents, contents_len, &error)) {
        g_warning("Error parsing '%s': %s", filename, error->message);
        g_error_free(error);
        goto out;
    }
    
    pd.entries = g_slist_reverse(pd.entries);
    entries_len = g_slist_length(pd.entries);
    entries = g_new0(ParsedEntry*, entries_len + 1);

    i = 0;
    while (pd.entries != NULL) {
        ParsedEntry *entry = pd.entries->data;
        pd.entries = g_slist_remove(pd.entries, pd.entries->data);
        entries[i] = entry;
        ++i;
    }
    g_assert(i == entries_len);
    
    *entries_p = entries;
    *entries_len_p = entries_len;
    
    success = TRUE;

 out:
    g_free(contents);
    if (context)
        g_markup_parse_context_free(context);
    return success;
}

void
parsed_entry_free(ParsedEntry *entry)
{
    g_free(entry->key);    
    g_free(entry);
}
