/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <stdlib.h>
#include <string.h>

#include <ddm/ddm-data-fetch.h>
#include "ddm-data-resource-internal.h"

typedef struct _FetchProperty FetchProperty;

struct _FetchProperty {
    DDMQName *qname;
    char *name;

    DDMDataFetch *children;
};

struct _DDMDataFetch
{
    guint ref_count;

    gboolean include_default;

    int n_properties;
    FetchProperty *properties;
};

static gboolean ddm_data_fetch_is_contained(DDMDataFetch *fetch, DDMDataFetch *other);

static gboolean fetch_from_string_internal(const char      *str,
                                           const char     **pos_inout,
                                           gboolean         allow_multiple,
                                           DDMDataFetch **result);

static int
fetch_property_compare(const FetchProperty *a, const FetchProperty *b)
{
    if (a->qname) {
        if (b->qname) {
            /* Pointer comparison works fine, which just need some well defined ordering */
            return a->qname < b->qname ? -1 : (a->qname == b->qname ? 0 : 1);
        } else {
            return -1;
        }
    } if (a->qname) {
        return 1;
    }

    return strcmp(a->name, b->name);
}

static void
fetch_property_copy(FetchProperty *property, FetchProperty *from)
{
    property->name = g_strdup(from->name);
    property->qname = from->qname;
    if (from->children)
        property->children = ddm_data_fetch_ref(from->children);
    else
        property->children = NULL;
}

static void
fetch_property_merge(FetchProperty *property, FetchProperty *a, FetchProperty *b)
{
    property->name = g_strdup(a->name);
    property->qname = a->qname;

    if (a->children && b->children)
        property->children = ddm_data_fetch_merge(a->children, b->children);
    else if (a->children)
        property->children = ddm_data_fetch_ref(a->children);
    else if (b->children)
        property->children = ddm_data_fetch_ref(b->children);
    else
        property->children = NULL;
}

static gboolean
fetch_property_subtract(FetchProperty *property, FetchProperty *a, FetchProperty *b)
{
    DDMDataFetch *difference;

    if (a->children == NULL)
        return FALSE;

    if (b->children == NULL) {
        fetch_property_copy(property, a);

        return TRUE;
    }

    difference = ddm_data_fetch_subtract(a->children, b->children);
    if (difference == NULL)
        return FALSE;

    property->name = g_strdup(a->name);
    property->qname = a->qname;

    property->children = difference;

    return TRUE;
}

static gboolean
fetch_property_is_contained(FetchProperty *a, FetchProperty *b)
{
    if (a->children == NULL)
        return TRUE;

    if (b->children == NULL)
        return FALSE;

    return ddm_data_fetch_is_contained(a->children, b->children);
}

static void
fetch_property_free(FetchProperty *property)
{
    g_free(property->name);
    if (property->children)
        ddm_data_fetch_unref(property->children);
}

DDMDataFetch *
ddm_data_fetch_ref(DDMDataFetch *fetch)
{
    fetch->ref_count++;

    return fetch;
}

void
ddm_data_fetch_unref(DDMDataFetch *fetch)
{
    int i;

    fetch->ref_count--;
    if (fetch->ref_count == 0) {
        for (i = 0; i < fetch->n_properties; i++)
            fetch_property_free(&fetch->properties[i]);

        g_free(fetch->properties);
        g_free(fetch);
    }
}

static void
skip_whitespace(const char **pos_inout)
{
    const char *p = *pos_inout;

    while (g_ascii_isspace(*p))
        p++;

    *pos_inout = p;
}

static inline gboolean
is_name_start(char c) {
    return ((c >= 'A' && c <= 'Z') ||
            (c >= 'a' && c <= 'z') ||
            (c == '_'));
}

static inline gboolean
is_name_continue(char c) {
    return ((c >= 'A' && c <= 'Z') ||
            (c >= 'a' && c <= 'z') ||
            (c >= '0' && c <= '9') ||
            (c == '_'));
}

static gboolean
parse_property_name(const char **pos_inout,
                    char **name)
{
    const char *p = *pos_inout;
    const char *start = p;

    if (!is_name_start(*start))
        return FALSE;
    do {
        p++;
    } while (is_name_continue(*p));

    if (name)
        *name = g_strndup(start, p - start);

    *pos_inout = p;
    return TRUE;
}

/* World's worse URI parse. It's an URI if it starts with <word>:, and it ends at whitespace.
 * We also require exactly one '#', since we want a fragment
 */
static gboolean
parse_property_qname(const char **pos_inout,
                     DDMQName **qname)
{
    const char *p = *pos_inout;
    const char *start = p;
    const char *hash_pos = NULL;
    char *name;
    char *uri;

    if (!parse_property_name(&p, NULL))
        return FALSE;

    if (*p != ':')
        return FALSE;
    p++;

    while (TRUE) {
        if (*p == '\0' || g_ascii_isspace(*p))
            break;

        if (*p == '#') {
            if (hash_pos != NULL)
                return FALSE;
            hash_pos = p;
        }

        p++;
    }

    if (hash_pos == NULL || hash_pos == p - 1)
        return FALSE;

    name = g_strndup(hash_pos + 1, p - (hash_pos + 1));
    uri = g_strndup(start, hash_pos - start);
    *qname = ddm_qname_get(name, uri);
    g_free(name);
    g_free(uri);

    *pos_inout = p;
    return TRUE;
}

static gboolean
fetch_property_from_string(const char     *str,
                           const char    **pos_inout,
                           FetchProperty  *property)
{
    const char *p = *pos_inout;
    char *name = NULL;
    DDMQName *qname = NULL;
    DDMDataFetch *children = NULL;

    skip_whitespace(&p);

    /* First thing is always a property name or property URI */

    if (!parse_property_qname(&p, &qname) && !parse_property_name(&p, &name)) {
        g_warning("Couldn't parse '%s': at position %ld, expected <name> or <uri>#<name>", str, (long)(p - str));
        goto error;
    }

    skip_whitespace(&p);

#if 0
    /* FIXME: Implement */

    /* Next might be attributes */

    skip_whitespace(&p);
#endif

    /* After that, we have either a child fetch, possibly in '[...]', or the end of this property */

    if (!(*p == '\0' || *p == ']' || *p == ';')) {
        gboolean bracketed = FALSE;

        if (*p == '[') {
            bracketed = TRUE;
            p++;
        }

        if (!fetch_from_string_internal(str, &p, bracketed, &children)) {
            goto error;
        }

        if (bracketed) {
            skip_whitespace(&p);
            if (*p != ']') {
                g_warning("Couldn't parse '%s': at position %ld, expected ']'", str, (long)(p - str));
                goto error;
            }
            p++;
        }
    }

    property->name = name;
    property->qname = qname;
    property->children = children;

    *pos_inout = p;

    return TRUE;

 error:
    g_free(name);
    if (children)
        ddm_data_fetch_unref(children);

    return FALSE;
}

static gboolean
fetch_from_string_internal(const char      *str,
                           const char     **pos_inout,
                           gboolean         allow_multiple,
                           DDMDataFetch **result)
{
    GArray *properties = g_array_new(FALSE, FALSE, sizeof(FetchProperty));
    const char *p = *pos_inout;
    DDMDataFetch *fetch;
    gboolean include_default = FALSE;
    guint i;

    while (TRUE) {
        FetchProperty property;

        skip_whitespace(&p);

        if (*p == '\0' || *p == ']')
            break;

        if (!allow_multiple && *p == ';')
            break;

        if (properties->len > 0 || include_default) {
            if (*p != ';') {
                g_warning("Couldn't parse '%s': at position %ld, expected ';'", str, (long)(p - str));
                goto error;
            }
            p++;

            skip_whitespace(&p);
        }

        if (*p == '+') {
            include_default = TRUE;
            p++;
        } else {
            if (!fetch_property_from_string(str, &p, &property))
                goto error;
            g_array_append_val(properties, property);
        }
    }

    fetch = g_new0(DDMDataFetch, 1);
    fetch->ref_count = 1;

    fetch->include_default = include_default;
    fetch->n_properties = properties->len;
    /* We memdup rather than g_array_free(..., TRUE) to save a bit of memory */
    fetch->properties = g_memdup(properties->data, sizeof(FetchProperty) * properties->len);
    g_array_free(properties, TRUE);

    qsort(fetch->properties, fetch->n_properties, sizeof(FetchProperty),
          (int(*)(const void *, const void *))fetch_property_compare);

    *result = fetch;
    *pos_inout = p;

    return TRUE;
 error:
    for (i = 0; i < properties->len; i++)
        fetch_property_free(&g_array_index(properties, FetchProperty, i));

    g_array_free(properties, TRUE);

    return FALSE;
}

DDMDataFetch *
ddm_data_fetch_from_string(const char *str)
{
    DDMDataFetch *result = NULL;
    const char *p = str;

    if (!fetch_from_string_internal(str, &p, TRUE, &result))
        return NULL;

    skip_whitespace(&p);

    if (*p != '\0') {
        g_warning("Couldn't parse '%s': at position %ld, expected <EOF>", str, (long)(p - str));
        ddm_data_fetch_unref(result);
        return NULL;
    }

    return result;
}

static void
ddm_data_fetch_to_string_internal(DDMDataFetch *fetch,
                                  GString        *out)
{
    int i;

    for (i = 0; i < fetch->n_properties; i++) {
        FetchProperty *property = &fetch->properties[i];

        if (i != 0)
            g_string_append_c(out, ';');

        if (property->qname) {
            g_string_append(out, property->qname->uri);
            g_string_append_c(out, '#');
            g_string_append(out, property->qname->name);
        } else {
            g_string_append(out, property->name);
        }

        if (property->children) {
            if (property->children->n_properties + (fetch->include_default ? 1 : 0) > 0) {
                g_string_append_c(out, '[');
                ddm_data_fetch_to_string_internal(property->children, out);
                g_string_append_c(out, ']');
            } else {
                ddm_data_fetch_to_string_internal(property->children, out);
            }
        }
    }

    if (fetch->include_default) {
        if (i != 0)
            g_string_append_c(out, ';');

        g_string_append_c(out, '+');
    }
}

char *
ddm_data_fetch_to_string(DDMDataFetch *fetch)
{
    GString *out;

    g_return_val_if_fail(fetch != NULL, NULL);

    out = g_string_new(NULL);

    ddm_data_fetch_to_string_internal(fetch, out);

    return g_string_free(out, FALSE);
}

DDMDataFetch *
ddm_data_fetch_merge(DDMDataFetch *fetch,
                     DDMDataFetch *other)
{
    DDMDataFetch *result;
    int i, j;
    int total_properties;
    gboolean include_default = fetch->include_default || other->include_default;

    i = 0; j = 0; total_properties = 0;
    while (i < fetch->n_properties || j < other->n_properties) {
        int cmp;

        if (i == fetch->n_properties)
            cmp = 1;
        else if (j == other->n_properties)
            cmp = -1;
        else
            cmp = fetch_property_compare(&fetch->properties[i], &other->properties[j]);

        if (cmp == -1) {
            i++;
        } else if (cmp == 1) {
            j++;
        } else {
            i++;
            j++;
        }

        total_properties++;
    }

    result = g_new(DDMDataFetch, 1);
    result->ref_count = 1;
    result->include_default = include_default;
    result->n_properties = total_properties;
    result->properties = g_new(FetchProperty, total_properties);

    i = 0; j = 0; total_properties = 0;
    while (i < fetch->n_properties || j < other->n_properties) {
        int cmp;

        if (i == fetch->n_properties)
            cmp = 1;
        else if (j == other->n_properties)
            cmp = -1;
        else
            cmp = fetch_property_compare(&fetch->properties[i], &other->properties[j]);

        if (cmp == -1) {
            fetch_property_copy(&result->properties[total_properties], &fetch->properties[i]);
            i++;
        } else if (cmp == 1) {
            fetch_property_copy(&result->properties[total_properties], &other->properties[j]);
            j++;
        } else {
            fetch_property_merge(&result->properties[total_properties], &fetch->properties[i], &other->properties[j]);
            i++;
            j++;
        }

        total_properties++;
    }

    return result;
}

static gboolean
ddm_data_fetch_is_contained(DDMDataFetch *fetch,
                            DDMDataFetch *other)
{
    int i, j;

    if (fetch->include_default && !other->include_default)
        return FALSE;

    i = 0; j = 0;
    while (i < fetch->n_properties || j < other->n_properties) {
        int cmp;

        if (i == fetch->n_properties)
            cmp = 1;
        else if (j == other->n_properties)
            cmp = -1;
        else
            cmp = fetch_property_compare(&fetch->properties[i], &other->properties[j]);

        if (cmp == -1) {
            return FALSE;
        } else if (cmp == 1) {
            j++;
        } else {
            if (!fetch_property_is_contained(&fetch->properties[i], &other->properties[j]))
                return FALSE;
            i++;
            j++;
        }
    }

    return TRUE;
}

DDMDataFetch *
ddm_data_fetch_subtract(DDMDataFetch *fetch,
                        DDMDataFetch *other)
{
    DDMDataFetch *result;
    int i, j;
    int total_properties;
    gboolean include_default = fetch->include_default && !other->include_default;

    i = 0; j = 0; total_properties = 0;
    while (i < fetch->n_properties || j < other->n_properties) {
        int cmp;

        if (i == fetch->n_properties)
            cmp = 1;
        else if (j == other->n_properties)
            cmp = -1;
        else
            cmp = fetch_property_compare(&fetch->properties[i], &other->properties[j]);

        if (cmp == -1) {
            total_properties++;
            i++;
        } else if (cmp == 1) {
            j++;
        } else {
            if (!fetch_property_is_contained(&fetch->properties[i], &other->properties[j]))
                total_properties++;
            i++;
            j++;
        }
    }

    if (total_properties == 0 && !include_default)
        return NULL;

    if (total_properties == fetch->n_properties && include_default == fetch->include_default)
        return ddm_data_fetch_ref(fetch);

    result = g_new(DDMDataFetch, 1);
    result->ref_count = 1;
    result->include_default = include_default;
    result->n_properties = total_properties;
    result->properties = g_new(FetchProperty, total_properties);

    i = 0; j = 0; total_properties = 0;
    while (i < fetch->n_properties || j < other->n_properties) {
        int cmp;

        if (i == fetch->n_properties)
            cmp = 1;
        else if (j == other->n_properties)
            cmp = -1;
        else
            cmp = fetch_property_compare(&fetch->properties[i], &other->properties[j]);

        if (cmp == -1) {
            fetch_property_copy(&result->properties[total_properties], &fetch->properties[i]);
            total_properties++;
            i++;
        } else if (cmp == 1) {
            j++;
        } else {
            if (fetch_property_subtract(&result->properties[total_properties], &fetch->properties[i], &other->properties[j]))
                total_properties++;
            i++;
            j++;
        }
    }

    return result;
}

static void
ddm_data_fetch_iter_advance(DDMDataFetchIter *iter)
{
    if (iter->property_index < iter->fetch->n_properties) {
        while (TRUE) {
            iter->property_index++;
            if (iter->property_index == iter->fetch->n_properties)
                break;

            if (iter->fetch->properties[iter->property_index].qname) {
                iter->next_property = ddm_data_resource_get_property_by_qname(iter->resource, iter->fetch->properties[iter->property_index].qname);
                if (iter->next_property) {
                    iter->next_children = iter->fetch->properties[iter->property_index].children;
                    return;
                }
            } else {
                iter->next_property = ddm_data_resource_get_property(iter->resource, iter->fetch->properties[iter->property_index].name);
                if (iter->next_property) {
                    iter->next_children = iter->fetch->properties[iter->property_index].children;
                    return;
                }
            }
        }
    }

    if (iter->default_properties) {
        iter->next_property = iter->default_properties->data;
        iter->next_children = ddm_data_property_get_default_children(iter->next_property);
        iter->default_properties = g_slist_delete_link(iter->default_properties, iter->default_properties);
    } else {
        iter->next_property = NULL;
    }
}

void
ddm_data_fetch_iter_init(DDMDataFetchIter *iter,
                         DDMDataResource  *resource,
                         DDMDataFetch     *fetch)
{
    iter->fetch = fetch;
    iter->resource = resource;
    iter->property_index = -1;
    if (fetch->include_default)
        iter->default_properties = _ddm_data_resource_get_default_properties(resource);
    else
        iter->default_properties = NULL;
    iter->next_property = NULL;

    ddm_data_fetch_iter_advance(iter);
}

void
ddm_data_fetch_iter_clear(DDMDataFetchIter *iter)
{
    g_slist_free(iter->default_properties);
}

gboolean
ddm_data_fetch_iter_has_next(DDMDataFetchIter *iter)
{
    return iter->next_property != NULL;
}

void
ddm_data_fetch_iter_next(DDMDataFetchIter *iter,
                         DDMDataProperty **property,
                         DDMDataFetch    **children)
{
    g_return_if_fail(ddm_data_fetch_iter_has_next(iter));

    if (property)
        *property = iter->next_property;
    if (children)
        *children = iter->next_children;
    iter->default_properties = g_slist_remove(iter->default_properties, iter->next_property);

    ddm_data_fetch_iter_advance(iter);
}
