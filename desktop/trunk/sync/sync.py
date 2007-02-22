#! /usr/bin/python

import gconf
import dbus
import gobject
import sys

gconf_client = None
bus = None 
online_prefs = None

def on_gconf_key_changed(client, id, entry, data):
    print entry.get_value().get_int()
            
def main():

    try:
        # this is in newer versions of pydbus only
        dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    except:
        # this is how you used to have to do it
        import dbus.glib

    gconf_client = gconf.client_get_default()
    bus = dbus.SessionBus()

    #proxy_obj = bus.get_object('org.freedesktop.DBus', '/org/freedesktop/DBus')
    #dbus_iface = dbus.Interface(proxy_obj, 'org.freedesktop.DBus')
    #print dbus_iface.ListNames()

    prefs_proxy = bus.get_object('org.freedesktop.OnlinePreferencesManager', '/org/freedesktop/online_preferences')

    # print prefs_proxy.Introspect()

    online_prefs = dbus.Interface(prefs_proxy, 'org.freedesktop.Preferences')

    gconf_client.add_dir('/', gconf.CLIENT_PRELOAD_NONE)
    gconf_client.notify_add('/', on_gconf_key_changed)
    
    loop = gobject.MainLoop()

    loop.run()

main()
