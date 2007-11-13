/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-pidgin.h"
#include "hippo-dbus-im-client.h"

#define GAIM_BUS_NAME "net.sf.gaim.GaimService"
#define GAIM_OBJECT_NAME "/net/sf/gaim/GaimObject"
#define GAIM_INTERFACE_NAME "net.sf.gaim.GaimInterface"

#define PIDGIN_BUS_NAME "im.pidgin.purple.PurpleService"
#define PIDGIN_OBJECT_NAME "/im/pidgin/purple/PurpleObject"
#define PIDGIN_INTERFACE_NAME "im.pidgin.purple.PurpleInterface"

void
hippo_dbus_init_pidgin(DBusConnection *connection)
{
    hippo_dbus_im_client_add(connection,
                             PIDGIN_BUS_NAME,
                             "pidgin");
    hippo_dbus_im_client_add(connection,
                             GAIM_BUS_NAME,
                             "gaim");
}

#if 0

/* cc -Wall -ggdb -O2 `pkg-config --cflags --libs dbus-glib-1 glib-2.0 dbus-1` -I ../build/config hippo-dbus-pidgin.c hippo-dbus-helper.c -o foo && ./foo */

#include <dbus/dbus-glib-lowlevel.h>

int
main(int argc, char **argv)
{
    GMainLoop *loop;
    DBusConnection *connection;

    connection = dbus_bus_get(DBUS_BUS_SESSION, NULL);
    dbus_connection_setup_with_g_main(connection, NULL);

    hippo_dbus_init_pidgin(connection);
    
    loop = g_main_loop_new(NULL, FALSE);
    
    g_main_loop_run(loop);

    return 0;
}

#endif
