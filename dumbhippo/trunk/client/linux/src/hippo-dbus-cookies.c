/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include "hippo-cookies-linux.h"
#include "hippo-dbus-server.h"
#include "hippo-dbus-web.h"
#include "hippo-dbus-cookies.h"
#include "main.h"

/* This is its own file primarily because it likely needs a cache if ever really
 * used much, which would mean a bit more code in here
 */

DBusMessage*
hippo_dbus_handle_get_cookies_to_send(HippoDBus   *dbus,
                                      DBusMessage *message)
{
    DBusMessage *reply;
    const char *url;
    GSList *cookies;
    char *host;
    int port;
    gboolean is_https;
    DBusMessageIter iter;
    DBusMessageIter array_iter;
    
    url = NULL;
    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_STRING, &url,
                               DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Expected one argument, the URL to get cookies for"));
    }


    is_https = FALSE;
    host = NULL;
    port = -1;
    if (!hippo_parse_http_url(url, &is_https, &host, &port)) {
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Invalid URL, only http/https URLs understood"));
    }
    
    if (port < 0) {
        if (is_https)
            port = 443;
        else
            port = 80;
    }

    cookies = hippo_load_cookies(host, port, NULL);
    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "(ss)",
                                     &array_iter);
    
    while (cookies != NULL) {
        HippoCookie *c = cookies->data;
        GSList *next = cookies->next;
        const char *s;

        DBusMessageIter struct_iter;
        
        g_slist_free_1(cookies);
        cookies = next;
        
        if (hippo_cookie_get_secure_connection_required(c) && !is_https) {
            continue;
        }
        
        dbus_message_iter_open_container(&array_iter, DBUS_TYPE_STRUCT,
                                         NULL, &struct_iter);

        s = hippo_cookie_get_name(c);
        dbus_message_iter_append_basic(&struct_iter, DBUS_TYPE_STRING, &s);

        s = hippo_cookie_get_value(c);
        dbus_message_iter_append_basic(&struct_iter, DBUS_TYPE_STRING, &s);

        dbus_message_iter_close_container(&array_iter, &struct_iter);

        hippo_cookie_unref(c);
    }

    dbus_message_iter_close_container(&iter, &array_iter);
    
    g_free(host);

    return reply;
}
