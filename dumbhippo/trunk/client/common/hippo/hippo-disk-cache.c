/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#ifdef HAVE_SQLITE
#include <sqlite3.h>
#endif

#include <ddm/ddm.h>
#include "hippo-connection.h"
#include "hippo-disk-cache.h"
#include "hippo-data-cache.h"

/* How often to touch a field in the database indicating the last time the
 * current session was used
 */
#define DATABASE_HEARTBEAT_INTERVAL 5 * 60 * 1000 /* 5 minutes */

/* Database schema version, used to know when to run upgrade steps
 */
#define SCHEMA_VERSION 0

static void      hippo_disk_cache_init                (HippoDiskCache       *model);
static void      hippo_disk_cache_class_init          (HippoDiskCacheClass  *klass);

static void      hippo_disk_cache_finalize            (GObject              *object);

struct _HippoDiskCache {
    GObject parent;

    HippoDataCache *data_cache;
    DDMDataModel *model;

#ifdef HAVE_SQLITE
    sqlite3 *db;
    gint64 db_session;
    guint heartbeat_id;
#endif
};

struct _HippoDiskCacheClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoDiskCache, hippo_disk_cache, G_TYPE_OBJECT);

static void
hippo_disk_cache_init(HippoDiskCache *cache)
{
}

static void
hippo_disk_cache_class_init(HippoDiskCacheClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->finalize = hippo_disk_cache_finalize;
}

static void
hippo_disk_cache_finalize(GObject *object)
{
    HippoDiskCache *cache = HIPPO_DISK_CACHE(object);

    _hippo_disk_cache_close(cache);

    G_OBJECT_CLASS(hippo_disk_cache_parent_class)->finalize(object);
}

#ifdef HAVE_SQLITE
static gboolean
bind_sql_parameters(HippoDiskCache *cache,
                    sqlite3_stmt   *stmt,
                    va_list        *vap)
{
    while (TRUE) {
        const char *spec = va_arg(*vap, const char *);
        const char *colon;
        int index;
        int sql_result;

        if (spec == NULL)
            break;
        
        colon = strchr(spec, ':');
        
        if (colon == NULL || *(colon + 1) == '\0' || colon != spec + 1) {
            g_warning("Parameter specification %s should be <type_char>:<name>", spec);
            return FALSE;
        }

        index = sqlite3_bind_parameter_index(stmt, colon);
        if (index == 0) {
            g_warning("Parameter '%s' not found", colon);
            return FALSE;
        }

        switch (*spec) {
        case 'i':
            {
                int value = va_arg(*vap, int);
                sql_result = sqlite3_bind_int(stmt, index, value);
                break;
            }
        case 'l':
            {
                gint64 value = va_arg(*vap, gint64);
                sql_result = sqlite3_bind_int64(stmt, index, value);
                break;
            }
        case 'f':
            {
                double value = va_arg(*vap, double);
                sql_result = sqlite3_bind_double(stmt, index, value);
                break;
            }
        case 's':
            {
                const char *value = va_arg(*vap, const char *);
                if (value)
                    sql_result = sqlite3_bind_text(stmt, index, g_strdup(value), strlen(value), (void(*)(void*))g_free);
                else
                    sql_result = SQLITE_OK;
                break;
            }
        default:
            g_warning("Unknown type character '%c'", *spec);
            return FALSE;
        }

        if (sql_result != SQLITE_OK) {
            g_warning("Error binding parameter '%s': %s'", colon, sqlite3_errmsg(cache->db));
            return FALSE;
        }
    }

    return TRUE;
}

static gboolean
get_row_datav(sqlite3_stmt *stmt,
              va_list      *vap)
{
    const char *types = va_arg(*vap, const char *);
    const char *p;
    GSList *to_free = NULL;

    if (types == NULL)
        return TRUE;

    if ((int)strlen(types) > sqlite3_column_count(stmt)) {
        g_warning("Got %d columns from query, expected at least %d", sqlite3_column_count(stmt),
                  (int) strlen(types));
        goto error;
    }

    for (p = types; *p; p++) {
        switch (*p) {
        case 'i':
            {
                int *l = va_arg(*vap, int *);
                *l = sqlite3_column_int(stmt, p - types);
                break;
            }
        case 'l':
            {
                gint64 *l = va_arg(*vap, gint64 *);
                *l = sqlite3_column_int64(stmt, p - types);
                break;
            }
        case 'f':
            {
                double *l = va_arg(*vap, double *);
                *l = sqlite3_column_double(stmt, p - types);
                break;
            }
        case 's':
            {
                char **l = va_arg(*vap, char **);
                const unsigned char *value = sqlite3_column_text(stmt, p - types);
                *l = g_strdup((const char *)value);
                to_free = g_slist_prepend(to_free, *l);
                break;
            }
        default:
            g_warning("Unknown type character '%c'", *p);
            goto error;
        }
    }

    return TRUE;

 error:
    g_slist_foreach(to_free, (GFunc)g_free, NULL);
    return FALSE;
}

static gboolean
get_row_data(sqlite3_stmt *stmt,
             ...)
{
    gboolean result;
    va_list vap;

    va_start(vap, stmt);
    result = get_row_datav(stmt, &vap);
    va_end(vap);

    return result;
}

static sqlite3_stmt *
prepare_sqlv(HippoDiskCache *cache,
             const char     *sql,
             va_list        *vap)
{
    sqlite3_stmt *stmt;
    
    if (sqlite3_prepare(cache->db, sql, -1, &stmt, NULL) != SQLITE_OK) {
        g_warning("Failed to prepare SQL command %s: %s\n", sql, sqlite3_errmsg(cache->db));
        return FALSE;
    }

    if (!bind_sql_parameters(cache, stmt, vap)) {
        sqlite3_finalize(stmt);
        return NULL;
    }

    return stmt;
}

static sqlite3_stmt *
prepare_sql(HippoDiskCache *cache,
            const char     *sql,
            ...)
{
    va_list vap;
    sqlite3_stmt *stmt;
    
    va_start(vap, sql);
    stmt = prepare_sqlv(cache, sql, &vap);
    va_end(vap);

    return stmt;
}

static gboolean
execute_sql(HippoDiskCache *cache,
            const char     *sql,
            ...)
{
    sqlite3_stmt *stmt;
    int sql_result;
    gboolean result = FALSE;
    va_list vap;

    va_start(vap, sql);
    stmt = prepare_sqlv(cache, sql, &vap);
    va_end(vap);
    
    if (stmt == NULL)
        return FALSE;

    sql_result = sqlite3_step(stmt);
    if (sql_result != SQLITE_DONE && sql_result != SQLITE_ROW) {
        g_warning("Failed to execute SQL command: %s\n", sqlite3_errmsg(cache->db));
        goto out;
    }

    result = TRUE;

 out:
    sqlite3_finalize(stmt);
    return result;
}

static gboolean
execute_sql_single_result(HippoDiskCache *cache,
                          const char     *sql,
                          ...)
{
    sqlite3_stmt *stmt;
    int sql_result;
    gboolean result = FALSE;
    va_list vap;
    
    va_start(vap, sql);

    stmt = prepare_sqlv(cache, sql, &vap);
    if (stmt == NULL)
        goto out;
    
    sql_result = sqlite3_step(stmt);
    if (sql_result != SQLITE_DONE && sql_result != SQLITE_ROW) {
        g_warning("Failed to execute SQL command: %s\n", sqlite3_errmsg(cache->db));
        goto out;
    }

    if (sql_result != SQLITE_ROW) {
        goto out;
    }

    if (!get_row_datav(stmt, &vap))
        goto out;

    result = TRUE;

 out:
    va_end(vap);

    if (stmt)
        sqlite3_finalize(stmt);
    
    return result;
}

static char *
make_db_name(HippoDiskCache *cache)
{
    HippoConnection *connection = hippo_data_cache_get_connection(cache->data_cache);
    HippoPlatform *platform = hippo_connection_get_platform(connection);
    char *web_server;
    char *path;
    const char *user_id;

    user_id = hippo_connection_get_self_guid(connection);
    if (user_id == NULL)
        return NULL;

    web_server = hippo_platform_get_web_server(platform,
                                               hippo_connection_get_auth_server_type(connection));

    path = hippo_platform_make_cache_filename(platform, web_server, user_id);
    
    g_free(web_server);
    
    return path;
}

static void
close_database(HippoDiskCache *cache)
{
    if (cache->db) {
        sqlite3_close(cache->db);
        cache->db = NULL;
    }
}

static void
open_database(HippoDiskCache *cache)
{
    static const char *create_statements[] = {
        "CREATE TABLE IF NOT EXISTS CacheProperties (key TEXT UNIQUE, value)",
        "CREATE TABLE IF NOT EXISTS Session (id INTEGER PRIMARY KEY, connectTime INTEGER, disconnectTime INTEGER, heartbeatTime INTEGER)",
        "CREATE TRIGGER IF NOT EXISTS cascade_session_delete DELETE ON Session "
        "  BEGIN "
        "   DELETE FROM Query WHERE session = old.id; "
        "   DELETE FROM Property WHERE session = old.id; "
        "  END ",
        "CREATE TABLE IF NOT EXISTS Query (id INTEGER PRIMARY KEY, session INTEGER, timestamp INTEGER, uri TEXT, params TEXT)",
        "CREATE TRIGGER IF NOT EXISTS cascade_delete_query DELETE ON Query "
        "  BEGIN "
        "   DELETE FROM QueryResult WHERE query = old.id; "
        "  END ",
        "CREATE TABLE IF NOT EXISTS QueryResult (query INTEGER, resourceId TEXT)",
        "CREATE TABLE IF NOT EXISTS Property (session INTEGER, timestamp INTEGER, resourceId TEXT, propertyId TEXT, type TEXT, defaultChildren TEXT, value)",
        NULL
    };

    int old_version;
    int i;
    char *filename;
    int sqlite_result;

    filename = make_db_name(cache);
    if (filename == NULL)
        return;
    
    sqlite_result = sqlite3_open(filename, &cache->db);
    g_free(filename);

    if (sqlite_result != SQLITE_OK) {
        g_warning("Could not open data cache cache %s: %s", filename, sqlite3_errmsg(cache->db));
        sqlite3_close(cache->db);
        g_free(filename);

        return;
    }

    for (i = 0; create_statements[i]; i++) {
        if (!execute_sql(cache, create_statements[i], NULL)) {
            goto error;
        }
    }

    if (execute_sql_single_result(cache,
                                  "SELECT value from CacheProperties WHERE key = 'schemaVersion'",
                                  NULL,
                                  "i", &old_version)) {
        if (old_version > SCHEMA_VERSION) {
            g_warning("Database version %d newer than we understand", old_version);
            goto error;
        } else if (old_version < SCHEMA_VERSION) {
            /* Migration steps go here
             * 
             * if (old_version < 1)
             *     run_migration_01(cache);
             * if (old_version < 2)
             *     run_migration_12(cache);
              */
        }
    }

    if (!execute_sql(cache,
                     "INSERT OR REPLACE INTO CacheProperties (key, value) VALUES ('schemaVersion', :version)",
                     "i:version", SCHEMA_VERSION, NULL)) {
        goto error;
    }

    return;

 error:
    close_database(cache);
}

static guint64
get_current_timestamp()
{
    GTimeVal timeval;

    g_get_current_time(&timeval);

    return (guint64)timeval.tv_sec * 1000 + timeval.tv_usec / 1000;
}

static void
on_connection_connected_changed(HippoConnection *connection,
                                gboolean         connected,
                                HippoDiskCache  *cache)
{
    if (!cache->db)
        return;

    if (connected) {
        if (!execute_sql(cache,
                         "INSERT INTO Session (connectTime, disconnectTime, heartbeatTime) VALUES (:timestamp, -1, :timestamp)",
                         "l:timestamp", get_current_timestamp(),
                         NULL)) {
            return;
        }
        cache->db_session = sqlite3_last_insert_rowid(cache->db);
    } else if (cache->db_session != -1) {
        execute_sql(cache,
                    "UPDATE Session SET heartbeatTime = :timestamp, disconnectTime = :timestamp WHERE id = :session",
                    "l:timestamp", get_current_timestamp(),
                    "l:session", cache->db_session,
                    NULL);
        cache->db_session = -1;
    }
}

static gboolean
database_heartbeat(void *data)
{
    HippoDiskCache *cache = data;

    if (cache->db && cache->db_session != -1) {
        execute_sql(cache,
                    "UPDATE Session SET heartbeatTime = :timestamp WHERE id = :session",
                    "l:timestamp", get_current_timestamp(),
                    "l:session", cache->db_session,
                    NULL);
    }

    return TRUE;
}

static char *
make_type_string(DDMDataProperty *property)
{
    char type[4];
    char *p = type;

    if (ddm_data_property_get_default_include(property))
        *(p++) = '+';
    
    switch (ddm_data_property_get_type(property)) {
    case DDM_DATA_BOOLEAN:
        *(p++) = 'b';
        break;
    case DDM_DATA_INTEGER:
        *(p++) = 'i';
        break;
    case DDM_DATA_LONG:
        *(p++) = 'l';
        break;
    case DDM_DATA_FLOAT:
        *(p++) = 'f';
        break;
    case DDM_DATA_STRING:
        *(p++) = 's';
        break;
    case DDM_DATA_RESOURCE:
        *(p++) = 'r';
        break;
    case DDM_DATA_URL:
        *(p++) = 'u';
        break;
    case DDM_DATA_NONE:
        *(p++) = 's'; /* Used only for empty lists, tpye doesn't matter */
        break;
    case DDM_DATA_LIST:
        g_assert_not_reached();
        break;
    }

    switch (ddm_data_property_get_cardinality(property)) {
    case DDM_DATA_CARDINALITY_01:
        *(p++) = '?';
        break;
    case DDM_DATA_CARDINALITY_1:
        break;
    case DDM_DATA_CARDINALITY_N:
        *(p++) = '*';
        break;
    }

    *(p++) = '\0';

    return g_strdup(type);
}

static char *
make_default_children_string(DDMDataProperty *property)
{
    DDMDataFetch *default_children = ddm_data_property_get_default_children(property);
    if (default_children == NULL)
        return NULL;
    else {
        return ddm_data_fetch_to_string(default_children);
    }
}

static void
save_class_id_to_disk(HippoDiskCache    *cache,
                      DDMDataResource *resource,
                      gint64             timestamp)
{
    const char *resource_id = ddm_data_resource_get_resource_id(resource);
    static const char *property_id = "http://mugshot.org/p/system#classId";
    const char *class_id = ddm_data_resource_get_class_id(resource);
    
    execute_sql(cache,
                "DELETE FROM Property WHERE resourceId = :resourceId AND propertyId = :propertyId",
                "s:resourceId", resource_id,
                "s:propertyId", property_id,
                NULL);
    
    execute_sql(cache,
                "INSERT INTO Property (session, timestamp, resourceId, propertyId, type, value)"
                " VALUES (:session, :timestamp, :resourceId, :propertyId, :type, :value)",
                "l:session", cache->db_session,
                "l:timestamp", timestamp,
                "s:resourceId", resource_id,
                "s:propertyId", property_id,
                "s:type", "u",
                "s:value", class_id,
                NULL);
}

static void
save_property_value_to_disk(HippoDiskCache    *cache,
                            const char        *resource_id,
                            const char        *property_id,
                            DDMDataProperty *property,
                            DDMDataValue    *value,
                            const char        *type,
                            const char        *default_children,
                            gint64             timestamp)
{
    gboolean is_float = FALSE;
    double value_float = 0;
    gboolean is_long = FALSE;
    gint64 value_long = 0;
    gboolean is_string = FALSE;
    const char *value_string = NULL;

    switch (ddm_data_property_get_type(property)) {
    case DDM_DATA_BOOLEAN:
        is_long = TRUE;
        value_long = value->u.boolean;
        break;
    case DDM_DATA_INTEGER:
        is_long = TRUE;
        value_long = value->u.integer;
        break;
    case DDM_DATA_LONG:
        is_long = TRUE;
        value_long = value->u.long_;
        break;
    case DDM_DATA_FLOAT:
        is_float = TRUE;
        value_float = value->u.float_;
        break;
    case DDM_DATA_STRING:
        is_string = TRUE;
        value_string = value->u.string;
        break;
    case DDM_DATA_RESOURCE:
        is_string = TRUE;
        value_string = ddm_data_resource_get_resource_id(value->u.resource);
        break;
    case DDM_DATA_URL:
        is_string = TRUE;
        value_string = value->u.string;
        break;
    case DDM_DATA_LIST:
    case DDM_DATA_NONE:
        g_assert_not_reached();
        break;
    }

    if (is_long)
        execute_sql(cache,
                    "INSERT INTO Property (session, timestamp, resourceId, propertyId, type, defaultChildren,  value)"
                    " VALUES (:session, :timestamp, :resourceId, :propertyId, :type, :defaultChildren, :value)",
                    "l:session", cache->db_session,
                    "l:timestamp", timestamp,
                    "s:resourceId", resource_id,
                    "s:propertyId", property_id,
                    "s:type", type,
                    "s:defaultChildren", default_children,
                    "l:value", value_long,
                    NULL);
    else if (is_float)
        execute_sql(cache,
                    "INSERT INTO Property (session, timestamp, resourceId, propertyId, type, defaultChildren,  value)"
                    " VALUES (:session, :timestamp, :resourceId, :propertyId, :type, :defaultChildren, :value)",
                    "l:session", cache->db_session,
                    "l:timestamp", timestamp,
                    "s:resourceId", resource_id,
                    "s:propertyId", property_id,
                    "s:type", type,
                    "s:defaultChildren", default_children,
                    "f:value", value_float,
                    NULL);
    else if (is_string)
        execute_sql(cache,
                    "INSERT INTO Property (session, timestamp, resourceId, propertyId, type, defaultChildren,  value)"
                    " VALUES (:session, :timestamp, :resourceId, :propertyId, :type, :defaultChildren, :value)",
                    "l:session", cache->db_session,
                    "l:timestamp", timestamp,
                    "s:resourceId", resource_id,
                    "s:propertyId", property_id,
                    "s:type", type,
                    "s:defaultChildren", default_children,
                    "s:value", value_string,
                    NULL);
}

typedef struct {
    DDMDataResource *resource;
    DDMQName *property_qname;
    DDMDataCardinality cardinality;
    gboolean default_include;
    char *default_children;
} QueuedResourceProperty;

typedef struct {
    char *resource_id;

    GSList *properties;
} QueuedResource;

typedef struct {
    GHashTable *resources_to_fetch;
    GHashTable *fetched_resources;
} ResourceTracking;

static QueuedResourceProperty *
queued_resource_property_new(DDMDataResource   *resource,
                             DDMQName          *property_qname,
                             DDMDataCardinality cardinality,
                             gboolean             default_include,
                             const char          *default_children)
{
    QueuedResourceProperty *queued_property = g_new0(QueuedResourceProperty, 1);

    queued_property->resource = resource;
    queued_property->property_qname = property_qname;
    queued_property->cardinality = cardinality;
    queued_property->default_include = default_include;
    queued_property->default_children = g_strdup(default_children);

    return queued_property;
}

static void
queued_resource_property_free(QueuedResourceProperty *queued_property)
{
    g_free(queued_property->default_children);
    g_free(queued_property);
}
    
static QueuedResource *
queued_resource_new(const char *resource_id)
{
    QueuedResource *queued = g_new0(QueuedResource, 1);
    queued->resource_id = g_strdup(resource_id);

    return queued;
}

static void
queued_resource_free(QueuedResource *queued)
{
    g_free(queued->resource_id);

    g_slist_foreach(queued->properties, (GFunc)queued_resource_property_free, NULL);
    g_slist_free(queued->properties);
    
    g_free(queued);
}

static ResourceTracking *
resource_tracking_new(void)
{
    ResourceTracking *tracking = g_new(ResourceTracking, 1);
    tracking->resources_to_fetch = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                         NULL, (GDestroyNotify)queued_resource_free);
    tracking->fetched_resources = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                        (GDestroyNotify)g_free, NULL);
    
    return tracking;
}

static void
resource_tracking_free(ResourceTracking *tracking)
{
    g_hash_table_destroy(tracking->resources_to_fetch);
    g_hash_table_destroy(tracking->fetched_resources);

    g_free(tracking);
}

static DDMDataResource *
resource_tracking_lookup_resource(ResourceTracking       *tracking,
                                  const char             *resource_id)
{
    return g_hash_table_lookup(tracking->fetched_resources, resource_id);
}

/* Assumes ownership of property */
static void
resource_tracking_queue_resource(ResourceTracking       *tracking,
                                 const char             *resource_id,
                                 QueuedResourceProperty *property)
{
    QueuedResource *queued;

    queued = g_hash_table_lookup(tracking->resources_to_fetch, resource_id);
    if (queued == NULL) {
        queued = queued_resource_new(resource_id);
        g_hash_table_insert(tracking->resources_to_fetch, queued->resource_id, queued);
    }

    if (property)
        queued->properties = g_slist_prepend(queued->properties, property);
}

static DDMDataResource *
load_resource_from_db(HippoDiskCache   *cache,
                      ResourceTracking *tracking,
                      const char       *resource_id)
{
    DDMDataResource *resource;
    sqlite3_stmt *stmt = NULL;
    gboolean result = FALSE;
    GHashTable *seen_properties = g_hash_table_new(g_direct_hash, NULL);
    DDMQName *class_id_qname = ddm_qname_get("http://mugshot.org/p/system", "classId");

    stmt  = prepare_sql(cache,
                        "SELECT propertyId, type,defaultChildren, value FROM Property WHERE resourceId = :resourceId",
                        "s:resourceId", resource_id,
                        NULL);
    
    if (stmt == NULL)
        goto out;

    /* We'll set the class_id later when we find the property for it */
    resource = ddm_data_model_ensure_resource(cache->model, resource_id, NULL);
    
    while (TRUE) {
        char *property_id;
        char *type_string;
        char *default_children;

        DDMQName *property_qname;
        
        DDMDataType type;
        DDMDataCardinality cardinality;
        gboolean default_include;

        gboolean is_float = FALSE;
        double value_float = 0;
        gboolean is_long = FALSE;
        gint64 value_long = 0;
        gboolean is_string = FALSE;
        char *value_string = NULL;

        int sql_result;

        sql_result = sqlite3_step(stmt);
        if (sql_result != SQLITE_DONE && sql_result != SQLITE_ROW) {
            g_warning("Failed to execute SQL command: %s\n", sqlite3_errmsg(cache->db));
            goto out;
        }
        
        if (sql_result != SQLITE_ROW)
            break;
        
        if (!get_row_data(stmt, "sss", &property_id, &type_string, &default_children))
            goto next;

        property_qname = ddm_qname_from_uri(property_id);
        if (property_qname == NULL)
            goto next;

        if (!ddm_data_parse_type(type_string, &type, &cardinality, &default_include))
            goto next;
        
        switch (type) {
        case DDM_DATA_BOOLEAN:
        case DDM_DATA_INTEGER:
        case DDM_DATA_LONG:
            break;
        case DDM_DATA_FLOAT:
            is_float = TRUE;
            break;
        case DDM_DATA_STRING:
        case DDM_DATA_RESOURCE:
        case DDM_DATA_URL:
            is_string = TRUE;
            break;
        case DDM_DATA_LIST:
        case DDM_DATA_NONE:
            g_assert_not_reached();
            break;
        }

        if (is_long)
            value_long = sqlite3_column_int64(stmt, 3);
        else if (is_float)
            value_float = sqlite3_column_double(stmt, 3);
        else if (is_string) {
            const unsigned char *value = sqlite3_column_text(stmt, 3);
            value_string = g_strdup((const char *)value);
        } else {
            g_assert_not_reached();
        }

        if (cardinality == DDM_DATA_CARDINALITY_N) {
            if (g_hash_table_lookup(seen_properties, property_qname) == NULL) {
                g_hash_table_insert(seen_properties, property_qname, property_qname);
                
                ddm_data_resource_update_property(resource, property_qname,
                                                     DDM_DATA_UPDATE_CLEAR,
                                                     cardinality,
                                                     default_include, default_children,
                                                     NULL);
            }
        }

        if (type == DDM_DATA_RESOURCE) {
            DDMDataResource *referenced_resource;
            referenced_resource = resource_tracking_lookup_resource(tracking, value_string);
            if (referenced_resource) {
                DDMDataValue value;

                value.type = type;
                value.u.resource = referenced_resource;
                
                ddm_data_resource_update_property(resource, property_qname,
                                                     (cardinality == DDM_DATA_CARDINALITY_N) ? DDM_DATA_UPDATE_ADD : DDM_DATA_UPDATE_REPLACE,
                                                     cardinality,
                                                     default_include, default_children,
                                                     &value);
            } else {
                QueuedResourceProperty *property = queued_resource_property_new(resource, property_qname,
                                                                                cardinality,
                                                                                default_include, default_children);
                resource_tracking_queue_resource(tracking, value_string, property);
            }
        } else {
            DDMDataValue value;

            value.type = type;
        
            switch (type) {
            case DDM_DATA_BOOLEAN:
                value.u.boolean = value_long != 0;
                break;
            case DDM_DATA_INTEGER:
                value.u.integer = (int)value_long;
                break;
            case DDM_DATA_LONG:
                value.u.long_ = value_long;
                break;
            case DDM_DATA_FLOAT:
                value.u.float_ = value_float;
                break;
            case DDM_DATA_STRING:
            case DDM_DATA_URL:
                value.u.string = value_string;
                break;
            case DDM_DATA_RESOURCE:
            case DDM_DATA_LIST:
            case DDM_DATA_NONE:
                g_assert_not_reached();
                break;
            }

            if (property_qname == class_id_qname) {
                if (type == DDM_DATA_URL) {
                    ddm_data_resource_set_class_id(resource, value.u.string);
                } else {
                    g_warning("class_id property must have URL type");
                }
            } else {
                ddm_data_resource_update_property(resource, property_qname,
                                                     (cardinality == DDM_DATA_CARDINALITY_N) ? DDM_DATA_UPDATE_ADD : DDM_DATA_UPDATE_REPLACE,
                                                     cardinality,
                                                     default_include, default_children,
                                                     &value);
            }
        }
        
    next:
        g_free(property_id);
        g_free(type_string);
        g_free(default_children);
        
        g_free(value_string);
    }

    if (ddm_data_resource_get_class_id(resource) == NULL)
        goto out;
    
    result = TRUE;
        
 out:
    g_hash_table_destroy(seen_properties);
    
    if (stmt)
        sqlite3_finalize(stmt);

    if (result)
        return resource;
    else
        return NULL;
}

static void
get_values_foreach(gpointer key,
                   gpointer value,
                   gpointer data)
{
    GSList **values = data;

    *values = g_slist_prepend(*values, value);
}

static GSList *
hash_table_get_values(GHashTable *hash_table)
{
    GSList *values = NULL;

    g_hash_table_foreach(hash_table, get_values_foreach, &values);

    return values;
}

static void
resource_tracking_resolve(ResourceTracking *tracking,
                          HippoDiskCache   *cache)
{
    while (g_hash_table_size(tracking->resources_to_fetch) != 0) {
        GSList *to_fetch = hash_table_get_values(tracking->resources_to_fetch);
        GSList *l;

        for (l = to_fetch; l; l = l->next) {
            QueuedResource *queued = l->data;
            DDMDataResource *resource = load_resource_from_db(cache, tracking, queued->resource_id);

            if (resource != NULL) {
                GSList *properties;

                for (properties = queued->properties; properties; properties = properties->next) {
                    QueuedResourceProperty *queued_property = properties->data;
                    DDMDataValue value;
                    
                    value.type = DDM_DATA_RESOURCE;
                    value.u.resource = resource;

                    ddm_data_resource_update_property(queued_property->resource, queued_property->property_qname,
                                                         (queued_property->cardinality == DDM_DATA_CARDINALITY_N) ? DDM_DATA_UPDATE_ADD : DDM_DATA_UPDATE_REPLACE,
                                                         queued_property->cardinality,
                                                         queued_property->default_include, queued_property->default_children,
                                                         &value);
                }
                    
                g_hash_table_insert(tracking->fetched_resources, g_strdup(queued->resource_id), resource);
            }

            /* FIXME: if loading the resource failed, then we don't insert it into fetched_resources,
             * so that we'll try to load it repeatedly
             */
        }

        for (l = to_fetch; l; l = l->next) {
            QueuedResource *queued = l->data;
            
            g_hash_table_remove(tracking->resources_to_fetch, queued->resource_id);
        }
        
        g_slist_free(to_fetch);
    }
}

static void
get_param_names_foreach(void *key,
                       void  *value,
                       void  *data)
{
    const char *name = key;
    GSList **param_names = data;

    *param_names = g_slist_prepend(*param_names, (char *)name);
}

static void
append_escaped(GString    *str,
               const char *s)
{
    const char *p;

    for (p = s; *p; p++) {
        if (*p == '=' || *p == '&')
            g_string_append_printf(str, "%%%02x", *p);
        else
            g_string_append_c(str, *p);
    }
}

static char *
build_param_string(DDMDataQuery *query)
{
    GHashTable *params = ddm_data_query_get_params(query);
    GSList *param_names = NULL;
    GSList *l;
    GString *str = g_string_new(NULL);

    g_hash_table_foreach(params, get_param_names_foreach, &param_names);
    param_names = g_slist_sort(param_names, (GCompareFunc)strcmp);

    for (l = param_names; l; l = l->next) {
        const char *name = l->data;
        const char *value = g_hash_table_lookup(params, name);

        if (str->len > 0 )
            g_string_append_c(str, '&');

        append_escaped(str, name);
        g_string_append_c(str, '=');
        append_escaped(str, value);
    }

    g_slist_free(param_names);

    return g_string_free(str, FALSE);
}

static char *
build_query_uri(DDMDataQuery *query)
{
    DDMQName *qname = ddm_data_query_get_qname(query);
    return g_strconcat(qname->uri, "#", qname->name, NULL);
}
#endif /* HAVE_SQLITE */

void
_hippo_disk_cache_do_query(HippoDiskCache *cache,
                           DDMDataQuery *query)
{
#ifdef HAVE_SQLITE
    char *params = build_param_string(query);
    char *uri = build_query_uri(query);
    gint64 query_id;
    sqlite3_stmt *stmt = NULL;
    GSList *resource_ids = NULL;
    GSList *resources = NULL;
    GSList *l;
    ResourceTracking *tracking;

    if (!execute_sql_single_result(cache,
                                  "SELECT id from Query WHERE uri = :uri AND params = :params",
                                   "s:uri", uri,
                                   "s:params", params,
                                   NULL,
                                   "l", &query_id))
    {
        ddm_data_query_error(query,
                                DDM_DATA_ERROR_NO_CONNECTION,
                                "No connection and query is not cached");
        goto out;
    }

    stmt  = prepare_sql(cache,
                        "SELECT resourceId FROM QueryResult WHERE query = :queryId",
                        "l:queryId", query_id,
                        NULL);
    
    if (stmt == NULL) {
        ddm_data_query_error(query,
                                DDM_DATA_ERROR_INTERNAL,
                                "Error reading cached result form database");
        goto out;
    }

    while (TRUE) {
        char *resource_id;
        int sql_result;
        
        sql_result = sqlite3_step(stmt);
        if (sql_result != SQLITE_DONE && sql_result != SQLITE_ROW) {
            g_warning("Failed to execute SQL command: %s\n", sqlite3_errmsg(cache->db));
            goto out;
        }
        
        if (sql_result != SQLITE_ROW)
            break;
        
        if (!get_row_data(stmt, "s", &resource_id))
            goto out;

        resource_ids = g_slist_prepend(resource_ids, resource_id);
    }

    tracking = resource_tracking_new();

    for (l = resource_ids; l; l = l->next)
        resource_tracking_queue_resource(tracking, l->data, NULL);

    resource_tracking_resolve(tracking, cache);

    /* Two g_slist_prepend reverses gets things in the right order
     */
    for (l = resource_ids; l; l = l->next) {
        DDMDataResource *resource = resource_tracking_lookup_resource(tracking, l->data);
        if (resource)
            resources = g_slist_prepend(resources, resource);
    }
    
    resource_tracking_free(tracking);
        
    ddm_data_query_response(query, resources);
    
 out:
    if (stmt)
        sqlite3_finalize(stmt);

    g_slist_foreach(resource_ids, (GFunc)g_free, NULL);
    g_slist_free(resource_ids);
    g_slist_free(resources);
    g_free(params);
    g_free(uri);
#endif /* HAVE_SQLITE */
}

void
_hippo_disk_cache_save_properties_to_disk(HippoDiskCache    *cache,
                                          DDMDataResource *resource,
                                          GSList            *properties,
                                          gint64             timestamp)
{
#ifdef HAVE_SQLITE
    const char *resource_id;
    GSList *l;
    
    if (cache->db_session == -1)
        return;

    resource_id = ddm_data_resource_get_resource_id (resource);
    save_class_id_to_disk(cache, resource, timestamp);
    
    for (l = properties; l; l = l->next) {
        DDMQName *qname = l->data;
        char *property_id = g_strdup_printf("%s#%s", qname->uri, qname->name);
        DDMDataProperty *property;
        
        execute_sql(cache,
                    "DELETE FROM Property WHERE resourceId = :resourceId AND propertyId = :propertyId",
                    "s:resourceId", resource_id,
                    "s:propertyId", property_id,
                    NULL);
        
        property = ddm_data_resource_get_property_by_qname(resource, qname);
        if (property) {
            DDMDataValue value;
            char *type = make_type_string(property);
            char *default_children = make_default_children_string(property);

            ddm_data_property_get_value(property, &value);
            
            if (ddm_data_property_get_cardinality(property) == DDM_DATA_CARDINALITY_N) {
                GSList *ll;

                for (ll = value.u.list; ll; ll = ll->next) {
                    DDMDataValue element;
                    
                    ddm_data_value_get_element(&value, ll, &element);

                    save_property_value_to_disk(cache, resource_id, property_id,
                                                property, &element, type, default_children, timestamp);
                }
            } else {
                save_property_value_to_disk(cache, resource_id, property_id,
                                            property, &value, type, default_children, timestamp);
            }

            g_free(type);
            g_free(default_children);
        }
    }
#endif    
}

void
_hippo_disk_cache_save_update_to_disk(HippoDiskCache       *cache,
                                      HippoNotificationSet *properties)
{
#ifdef HAVE_SQLITE
    execute_sql(cache,
                "BEGIN TRANSACTION",
                NULL);
    
    _hippo_notification_set_save_to_disk(properties, get_current_timestamp());
    
    execute_sql(cache,
                "END TRANSACTION",
                NULL);
#endif    
}

void
_hippo_disk_cache_save_query_to_disk(HippoDiskCache       *cache,
                                     DDMDataQuery         *query,
                                     GSList               *resources,
                                     HippoNotificationSet *properties)
{
#ifdef HAVE_SQLITE
    char *uri;
    char *params;
    gint64 query_id;
    GSList *l;
    gint64 timestamp = get_current_timestamp();
    
    execute_sql(cache,
                "BEGIN TRANSACTION",
                NULL);

    _hippo_notification_set_save_to_disk(properties, timestamp);
    
    uri = build_query_uri(query); 
    params = build_param_string(query);

    execute_sql(cache,
                "DELETE FROM Query WHERE uri = :uri AND params = :params",
                "s:uri", uri,
                "s:params", params,
                NULL);

    
    execute_sql(cache,
                "INSERT INTO Query (session, timestamp, uri, params) VALUES (:session, :timestamp, :uri, :params)",
                "l:session", cache->db_session,
                "l:timestamp", timestamp,
                "s:uri", uri,
                "s:params", params,
                NULL);

    query_id = sqlite3_last_insert_rowid(cache->db);

    for (l = resources; l; l = l->next) {
        DDMDataResource *resource = l->data;

        execute_sql(cache,
                    "INSERT INTO QueryResult (query, resourceId) VALUES (:query, :resourceId)",
                    "l:query", query_id,
                    "s:resourceId", ddm_data_resource_get_resource_id(resource),
                    NULL);
    }
    
    execute_sql(cache,
                "END TRANSACTION",
                NULL);
    
    g_free(uri);
    g_free(params);
#endif    
}

HippoDiskCache *
_hippo_disk_cache_new(HippoDataCache *data_cache)
{
#ifdef HAVE_SQLITE    
    HippoDiskCache *cache = g_object_new(HIPPO_TYPE_DISK_CACHE, NULL);
    HippoConnection *connection = hippo_data_cache_get_connection(data_cache);

    cache->data_cache = data_cache;
    cache->model = hippo_data_cache_get_model(data_cache);

    open_database(cache);
    if (!cache->db) {
        g_object_unref(cache);
        return NULL;
    }
    
    g_signal_connect(connection, "connected-changed",
                     G_CALLBACK(on_connection_connected_changed), cache);
    
    cache->heartbeat_id = g_timeout_add(DATABASE_HEARTBEAT_INTERVAL, database_heartbeat, cache);
    
    return cache;
#else
    return NULL;
#endif    
}

void
_hippo_disk_cache_close(HippoDiskCache *cache)
{
#ifdef HAVE_SQLITE
    close_database(cache);
    
    if (cache->heartbeat_id) {
        g_source_remove(cache->heartbeat_id);
        cache->heartbeat_id = 0;
    }
#endif    
}
