/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_SQLITE_UTILS_H__
#define __HIPPO_SQLITE_UTILS_H__

#include <glib-object.h>
#include <ddm/ddm.h>
#ifdef HAVE_SQLITE
#include <sqlite3.h>
#endif

G_BEGIN_DECLS

#ifdef HAVE_SQLITE

gboolean
hippo_sqlite_bind_parameter(sqlite3      *db,
		                    sqlite3_stmt *stmt,
		                    const char  *name,
		                    GValue       *val);

gboolean
hippo_sqlite_bind_sql_parameters(sqlite3        *db,
                                 sqlite3_stmt   *stmt,
                                 va_list        *vap);

gboolean
hippo_sqlite_get_row_datav(sqlite3_stmt *stmt,
                           va_list      *vap);

gboolean
hippo_sqlite_get_row_data(sqlite3_stmt *stmt,
                          ...);

sqlite3_stmt *
hippo_sqlite_prepare_sqlv(sqlite3          *db,
                          const char     *sql,
                          va_list          *vap);

sqlite3_stmt *
hippo_sqlite_prepare_sql(sqlite3         *db,
                         const char     *sql,
                         ...);

gboolean
hippo_sqlite_execute_sql(sqlite3          *db,
                         const char     *sql,
                         ...);

gboolean
hippo_sqlite_execute_sql_single_result(sqlite3         *db,
                                       const char     *sql,
                                       ...);

#endif

G_END_DECLS

#endif /* __HIPPO_SQLITE_UTILS_H__ */
