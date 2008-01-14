/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "hippo-sqlite-utils.h"

gboolean
hippo_sqlite_bind_parameter(sqlite3      *db,
		                        sqlite3_stmt *stmt,
		                        const char  *name,
		                        GValue       *val)
{
	int index;
    int sql_result;
    
    index = sqlite3_bind_parameter_index(stmt, name);
    if (index == 0) {
        g_warning("Parameter '%s' not found", name);
        return FALSE;
    }
    
    if (G_VALUE_HOLDS_INT(val)) {
        sql_result = sqlite3_bind_int(stmt, index, g_value_get_int(val));
    } else if (G_VALUE_HOLDS_STRING(val)) {
    	const char *value = g_value_get_string(val);
        if (value)
        	sql_result = sqlite3_bind_text(stmt, index, g_strdup(value), strlen(value), (void(*)(void*))g_free);
        else
            sql_result = SQLITE_OK;
    } else {
    	g_assert_not_reached();
    }
    if (sql_result != SQLITE_OK) {
        g_warning("Error binding parameter '%s': %s'", name, sqlite3_errmsg(db));
        return FALSE;
    }
    return TRUE;
}

gboolean
hippo_sqlite_bind_sql_parameters(sqlite3        *db,
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
            g_warning("Error binding parameter '%s': %s'", colon, sqlite3_errmsg(db));
            return FALSE;
        }
    }

    return TRUE;
}

gboolean
hippo_sqlite_get_row_datav(sqlite3_stmt *stmt,
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

gboolean
hippo_sqlite_get_row_data(sqlite3_stmt *stmt,
                              ...)
{
    gboolean result;
    va_list vap;

    va_start(vap, stmt);
    result = hippo_sqlite_get_row_datav(stmt, &vap);
    va_end(vap);

    return result;
}

sqlite3_stmt *
hippo_sqlite_prepare_sqlv(sqlite3          *db,
                              const char     *sql,
                              va_list          *vap)
{
    sqlite3_stmt *stmt;
    
    if (sqlite3_prepare(db, sql, -1, &stmt, NULL) != SQLITE_OK) {
        g_warning("Failed to prepare SQL command %s: %s\n", sql, sqlite3_errmsg(db));
        return FALSE;
    }

    if (!hippo_sqlite_bind_sql_parameters(db, stmt, vap)) {
        sqlite3_finalize(stmt);
        return NULL;
    }

    return stmt;
}

sqlite3_stmt *
hippo_sqlite_prepare_sql(sqlite3         *db,
                             const char     *sql,
                             ...)
{
    va_list vap;
    sqlite3_stmt *stmt;
    
    va_start(vap, sql);
    stmt = hippo_sqlite_prepare_sqlv(db, sql, &vap);
    va_end(vap);

    return stmt;
}

gboolean
hippo_sqlite_execute_sql(sqlite3          *db,
                             const char     *sql,
                             ...)
{
    sqlite3_stmt *stmt;
    int sql_result;
    gboolean result = FALSE;
    va_list vap;

    va_start(vap, sql);
    stmt = hippo_sqlite_prepare_sqlv(db, sql, &vap);
    va_end(vap);
    
    if (stmt == NULL)
        return FALSE;

    sql_result = sqlite3_step(stmt);
    if (sql_result != SQLITE_DONE && sql_result != SQLITE_ROW) {
        g_warning("Failed to execute SQL command: %s\n", sqlite3_errmsg(db));
        goto out;
    }

    result = TRUE;

 out:
    sqlite3_finalize(stmt);
    return result;
}

gboolean
hippo_sqlite_execute_sql_single_result(sqlite3         *db,
                                             const char     *sql,
                                             ...)
{
    sqlite3_stmt *stmt;
    int sql_result;
    gboolean result = FALSE;
    va_list vap;
    
    va_start(vap, sql);

    stmt = hippo_sqlite_prepare_sqlv(db, sql, &vap);
    if (stmt == NULL)
        goto out;
    
    sql_result = sqlite3_step(stmt);
    if (sql_result != SQLITE_DONE && sql_result != SQLITE_ROW) {
        g_warning("Failed to execute SQL command: %s\n", sqlite3_errmsg(db));
        goto out;
    }

    if (sql_result != SQLITE_ROW) {
        goto out;
    }

    if (!hippo_sqlite_get_row_datav(stmt, &vap))
        goto out;

    result = TRUE;

 out:
    va_end(vap);

    if (stmt)
        sqlite3_finalize(stmt);
    
    return result;
}
