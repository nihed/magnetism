/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-http.h"

static void
handle_http_name_owned(DBusConnection *connection,
                       void           *data)
{


}

static void
handle_http_name_not_owned(DBusConnection *connection,
                           void           *data)
{
    

}

static HippoDBusNameOwner http_name_owner = {
    handle_http_name_owned,
    handle_http_name_not_owned
};

void
hippo_dbus_init_http(DBusConnection *connection)
{
    hippo_dbus_helper_register_name_owner(connection,
                                          HIPPO_DBUS_HTTP_BUS_NAME,
                                          HIPPO_DBUS_NAME_OWNED_OPTIONALLY,
                                          &http_name_owner,
                                          NULL);
}
