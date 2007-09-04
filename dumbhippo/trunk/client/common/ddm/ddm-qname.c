/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "ddm-qname.h"

/* Don't make these public, once you ddm_qname_get(), you can use
 * g_direct_hash() and ==
 */
static guint
hash_qname(DDMQName *qname)
{
    return g_str_hash(qname->uri) * 11 + g_str_hash(qname->uri) * 17;
}

static gboolean
equal_qname(DDMQName *qname,
            DDMQName *other)
{
    return strcmp(qname->uri, other->uri) == 0 && strcmp(qname->name, other->name) == 0;
}

static GHashTable *qname_hash;

DDMQName *
ddm_qname_get(const char *uri,
              const char *name)
{
    DDMQName tmp_qname;
    DDMQName *qname;

    if (qname_hash == NULL)
        qname_hash = g_hash_table_new((GHashFunc)hash_qname, (GEqualFunc)equal_qname);

    tmp_qname.uri = uri;
    tmp_qname.name = name;

    qname = g_hash_table_lookup(qname_hash, &tmp_qname);
    if (qname == NULL) {
        qname = g_new(DDMQName, 1);
        qname->uri = g_intern_string(uri);
        qname->name = g_intern_string(name);

        g_hash_table_insert(qname_hash, qname, qname);
    }

    return qname;
}

DDMQName *
ddm_qname_from_uri(const char *full_uri)
{
    const char *hash;
    const char *name;
    char *uri;
    DDMQName *qname;

    hash = strchr(full_uri, '#');
    if (hash == NULL) {
        g_warning("URI '%s' representing qualified name doesn't have a fragment", full_uri);
        return NULL;
    }

    name = hash + 1;
    uri = g_strndup(full_uri, hash - full_uri);
    qname = ddm_qname_get(uri, name);
    g_free(uri);

    return qname;
}
