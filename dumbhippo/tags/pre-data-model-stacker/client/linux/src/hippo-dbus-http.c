/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-http.h"
#include "hippo-http.h"
#include "main.h"

typedef struct {
    DBusConnection *connection;
    char *requestor;
    char *url;
    char *sink_path;
} Request;

typedef struct {
    gint64        start_offset;
    gint64        estimated_remaining;
    const char   *bytes;
    int           bytes_len;
} ByteBuffer;

static dbus_bool_t
append_bytes(DBusMessage *message,
             void        *data)
{
    ByteBuffer *buf = data;
    DBusMessageIter toplevel_iter, array_iter;

    dbus_message_iter_init_append(message, &toplevel_iter);

    if (!dbus_message_iter_append_basic(&toplevel_iter, DBUS_TYPE_INT64,
                                        &buf->start_offset))
        return FALSE;

    if (!dbus_message_iter_append_basic(&toplevel_iter, DBUS_TYPE_INT64,
                                        &buf->estimated_remaining))
        return FALSE;
    
    if (!dbus_message_iter_open_container(&toplevel_iter, DBUS_TYPE_ARRAY, "y", &array_iter))
        return FALSE;
    
    if (!dbus_message_iter_append_fixed_array(&array_iter, DBUS_TYPE_BYTE,
                                              &buf->bytes,
                                              buf->bytes_len))
        return FALSE;
    
    if (!dbus_message_iter_close_container(&toplevel_iter, &array_iter))
        return FALSE;

    return TRUE;
}

static void
send_bytes_chunk(HippoDBusProxy *sink,
                 gint64          start_offset,
                 gint64          estimated_remaining,
                 const char     *bytes,
                 int             bytes_len)
{
    ByteBuffer buf;

    buf.start_offset = start_offset;
    buf.estimated_remaining = estimated_remaining;
    buf.bytes = bytes;
    buf.bytes_len = bytes_len;
    
    hippo_dbus_proxy_call_method_async_appender(sink,
                                                "Data",
                                                NULL, NULL, NULL,
                                                append_bytes,
                                                &buf);
}

/* content_type == NULL on error */
static void
http_result(const char *content_type,
            GString    *content_or_error,
            void       *data)
{
    Request *r = data;
    HippoDBusProxy *sink;

    sink = hippo_dbus_proxy_new(r->connection,
                                r->requestor, r->sink_path,
                                HIPPO_DBUS_HTTP_DATA_SINK_INTERFACE);
    
    if (content_type == NULL) {
        hippo_dbus_proxy_call_method_async(sink,
                                           "Error",
                                           NULL, NULL, NULL,
                                           DBUS_TYPE_STRING,
                                           &content_or_error->str,
                                           DBUS_TYPE_INVALID);
    } else {
        int bytes_to_send;
        int bytes_sent;

        hippo_dbus_proxy_call_method_async(sink,
                                           "Begin",
                                           NULL, NULL, NULL,
                                           DBUS_TYPE_STRING,
                                           &content_type,
                                           /* this is only an ESTIMATED bytes remaining in
                                            * the API contract though here it is precise
                                            */
                                           DBUS_TYPE_INT64,
                                           &content_or_error->len,
                                           DBUS_TYPE_INVALID);

        /* FIXME the chunk size here should be larger for efficiency, this is small
         * to start in order to reveal bugs
         */
#define CHUNK_SIZE 1024
        bytes_to_send = content_or_error->len;
        bytes_sent = 0;

        while (bytes_to_send > 0) {
            int chunk;

            chunk = MIN(bytes_to_send, CHUNK_SIZE);

            send_bytes_chunk(sink,
                             bytes_sent,
                             bytes_to_send,
                             content_or_error->str + bytes_sent,
                             chunk);
            bytes_sent += chunk;
            bytes_to_send -= chunk;
        }
        g_assert(bytes_sent == (int) content_or_error->len);
        
        hippo_dbus_proxy_call_method_async(sink,
                                           "End",
                                           NULL, NULL, NULL,
                                           DBUS_TYPE_INVALID);
    }

    dbus_connection_unref(r->connection);
    g_free(r->requestor);
    g_free(r->url);
    g_free(r->sink_path);
    g_free(r);
}

static void
request_begin(DBusConnection *connection,
              const char     *requestor,
              const char     *url,
              const char     *sink_path)
{
    Request *r;

    r = g_new0(Request, 1);
    r->connection = connection;
    dbus_connection_ref(r->connection);
    r->requestor = g_strdup(requestor);
    r->url = g_strdup(url);
    r->sink_path = g_strdup(sink_path);

    hippo_http_get(r->url, http_result, r);
}

static DBusMessage*
handle_request (void            *object,
                DBusMessage     *message,
                DBusError       *error)
{
    const char *url;
    const char *sink_path;
    DBusConnection *connection;

    connection = object;
    
    url = NULL;
    sink_path = NULL;
    if (!dbus_message_get_args(message, error,
                               DBUS_TYPE_STRING, &url,
                               DBUS_TYPE_OBJECT_PATH, &sink_path,
                               DBUS_TYPE_INVALID))
        return NULL;
    
    request_begin(connection, dbus_message_get_sender(message), url, sink_path);

    return dbus_message_new_method_return(message);
}

static const HippoDBusMember http_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "Request", "so", "", handle_request },
    { 0, NULL }
};

static const HippoDBusProperty http_properties[] = {
    { NULL }
};

static void
handle_http_name_owned(DBusConnection *connection,
                       void           *data)
{
    g_debug("Http name owned");

    hippo_dbus_helper_register_object(connection,
                                      HIPPO_DBUS_HTTP_PATH,
                                      connection,
                                      HIPPO_DBUS_HTTP_INTERFACE,
                                      NULL);
}

static void
handle_http_name_not_owned(DBusConnection *connection,
                           void           *data)
{
    g_debug("Http name not owned");
    
    hippo_dbus_helper_unregister_object(connection,
                                        HIPPO_DBUS_HTTP_PATH);
}

static HippoDBusNameOwner http_name_owner = {
    handle_http_name_owned,
    handle_http_name_not_owned
};

void
hippo_dbus_init_http(DBusConnection *connection)
{

    hippo_dbus_helper_register_interface(connection,
                                         HIPPO_DBUS_HTTP_INTERFACE,
                                         http_members,
                                         http_properties);
    
    hippo_dbus_helper_register_name_owner(connection,
                                          HIPPO_DBUS_HTTP_BUS_NAME,
                                          HIPPO_DBUS_NAME_OWNED_OPTIONALLY,
                                          &http_name_owner,
                                          NULL);
}
