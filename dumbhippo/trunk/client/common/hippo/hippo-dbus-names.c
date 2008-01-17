/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <string.h>
#include <hippo/hippo-basics.h>
#include "hippo-dbus-names.h"

static char*
hippo_dbus_full_bus_name_internal(const char *server,
                                  const char *base_bus_name,
                                  gboolean    backward_hex)
{
    GString *str;
    const char *p;
    char *server_with_port;
    static const char hexdigits[16] =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
          'A', 'B', 'C', 'D', 'E', 'F' };

    /* we need the "80" canonically in here */
    if (strchr(server, ':') == NULL)
        server_with_port = g_strdup_printf("%s:%d", server, 80);
    else
        server_with_port = NULL;

    /* 
     * There some kind of very remote security implications if we map multiple domains
     * to the same dbus name, or vice versa, I guess, so we try to avoid that.
     * In practice (at least right now) we don't automatically start a dbus client
     * to some random server so it probably doesn't matter.
     *
     */
    str = g_string_new(base_bus_name);
    g_string_append_c(str, '.');

    if (server_with_port)
        p = server_with_port;
    else 
        p = server;
    while (*p) {
        /* only [a-z][A-Z][0-9]_ are allowed, not starting with a digit.
         * We encode any non-alphanumeric as _ followed by 2-digit hex
         * of the byte.
         */
        if ((*p >= 'a' && *p <= 'z') || 
            (*p >= 'A' && *p <= 'Z') ||
            (*p >= '0' && *p <= '9')) {
            g_string_append_c(str, *p);
        } else {
            g_string_append_c(str, '_');
            if (backward_hex) {
                /* Nibbles backwards */
                g_string_append_c(str, hexdigits[(*p) & 0xf]);
                g_string_append_c(str, hexdigits[(*p) >> 4]);
            } else {
                g_string_append_c(str, hexdigits[(*p) >> 4]);
                g_string_append_c(str, hexdigits[(*p) & 0xf]);
            }
        }
        ++p;
    }
    g_free(server_with_port);

    return g_string_free(str, FALSE);
}

char*
hippo_dbus_full_bus_name(const char *server)
{
    return hippo_dbus_full_bus_name_internal(server, HIPPO_DBUS_ENGINE_BASE_BUS_NAME, FALSE);
}

char*
hippo_dbus_full_bus_name_com_dumbhippo_with_forward_hex(const char *server)
{
    return hippo_dbus_full_bus_name_internal(server, HIPPO_DBUS_STACKER_BASE_BUS_NAME, FALSE);
}

char*
hippo_dbus_full_bus_name_com_dumbhippo_with_backward_hex(const char *server)
{
    return hippo_dbus_full_bus_name_internal(server, HIPPO_DBUS_STACKER_BASE_BUS_NAME, TRUE);
}
