#! /usr/bin/python

import gconf
import dbus
import gobject
import sys
import fnmatch
import getopt

def warn(str):
    print str # fixme

# complex globs don't really work, only trailing "*"
simple_sync_whitelist = [
    '/desktop/gnome/background/*'
    ]

gconf_client = None
bus = None 
online_prefs = None
copy_local_state = False

def extract_gconf_value(gconf_value):
    type = gconf_value.type
    value = None
    if type == gconf.VALUE_INT:
        value = gconf_value.get_int()
    elif type == gconf.VALUE_STRING:
        value = gconf_value.get_string()
    elif type == gconf.VALUE_BOOL:
        value = gconf_value.get_bool()
    elif type == gconf.VALUE_FLOAT:
        value = gconf_value.get_float()
    elif type == gconf.VALUE_LIST:
        value = []
        list = gconf_value.get_list()
        for item in list:
            value.append(extract_gconf_value(item))
    elif type == gconf.VALUE_SCHEMA or type == gconf.VALUE_PAIR or \
         type == gconf.VALUE_INVALID:
        pass
    else:
        warn("ignoring gconf value of unknown type " + str(type))
        pass

    return value

def server_key_from_gconf_key(gconf_key):
    return "/gconf" + gconf_key

def send_gconf_to_server(gconf_key, value):
    dbus_key = server_key_from_gconf_key(gconf_key)
    online_prefs.SetPreference(dbus_key, value)    

def on_gconf_key_changed(client, id, entry, data):
    key = entry.key

    value = extract_gconf_value(entry.get_value())

    if not value:
        return

    for pattern in simple_sync_whitelist:
        if fnmatch.fnmatch(key, pattern):
            send_gconf_to_server(key, value)
            break

def sync_gconf_key_to_server(key):
    gconf_value = gconf_client.get_without_default(key)
    value = None
    if gconf_value:
        value = extract_gconf_value(gconf_value)
    if value:
        send_gconf_to_server(key, value)

def initialize_server_from_gconf_state():
    for pattern in simple_sync_whitelist:
        if pattern.endswith("/*"):
            entries = gconf_client.all_entries(pattern.replace("/*", ""))
            for e in entries:
                sync_gconf_key_to_server(e.key)
        else:
            sync_gconf_key_to_server(pattern)

def dbus_signature_from_gconf_type(type, list_type):
    if type == gconf.VALUE_INT:
        return 'i'
    elif type == gconf.VALUE_STRING:
        return 's'
    elif type == gconf.VALUE_BOOL:
        return 'b'
    elif type == gconf.VALUE_FLOAT:
        return 'f'
    elif type == gconf.VALUE_LIST:
        element_type = dbus_signature_from_gconf_type(list_type)
        return 'a' + element_type        
    elif type == gconf.VALUE_SCHEMA or type == gconf.VALUE_PAIR or \
         type == gconf.VALUE_INVALID:
        return None
    else:
        warn("ignoring unknown gconf type " + str(type))
        return None

def sync_gconf_key_from_server(key):
    schema_key = "/schemas" + key # this is wrong, but schema.get_type() isn't in older gnome-python, only in svn head
    schema = gconf_client.get_schema(schema_key)
    if not schema:
        warn("no schema for key, can't sync from server: " + key)
        return

    # for some reason schema.get_type() appears to not exist
    dvalue = schema.get_default_value()
    gconf_type = dvalue.type
    gconf_list_type = None
    if gconf_type == gconf.VALUE_LIST:
        gconf_list_type = dvalue.get_list_type()
    
    signature = dbus_signature_from_gconf_type(gconf_type, gconf_list_type)
    try:
        value = online_prefs.GetPreference(server_key_from_gconf_key(key), signature)
    except dbus.DBusException, e:
        warn("failed to get pref from server: " + key + ": " + str(e))
        return

    gconf_value = gconf.Value(gconf_type)
    if gconf_type == gconf.VALUE_INT:
        gconf_value.set_int(value)
    elif gconf_type == gconf.VALUE_STRING:
        gconf_value.set_string(value)
    elif gconf_type == gconf.VALUE_BOOL:
        gconf_value.set_bool(value)
    elif gconf_type == gconf.VALUE_FLOAT:
        gconf_value.set_float(value)
    elif gconf_type == gconf.VALUE_LIST:
        gconf_value.set_list_type(gconf_list_type)
    elif gconf_type == gconf.VALUE_SCHEMA or gconf_type == gconf.VALUE_PAIR or \
         gconf_type == gconf.VALUE_INVALID:
        warn("should not be able to get here...")
    else:
        warn("ignoring unknown gconf type " + str(gconf_type))

    gconf_client.set(key, gconf_value)

def initialize_gconf_from_server_state():
    for pattern in simple_sync_whitelist:
        if pattern.endswith("/*"):
            entries = gconf_client.all_entries(pattern.replace("/*", ""))
            for e in entries:
                sync_gconf_key_from_server(e.key)
        else:
            sync_gconf_key_from_server(pattern)

def on_pref_changed(key):
    if key.startswith("/gconf/"):
        key = key.replace("/gconf", "")
    sync_gconf_key_from_server(key)

def on_prefs_ready(is_ready=False):
    online_prefs.connect_to_signal('PreferenceChanged', on_pref_changed)
    if copy_local_state:
        initialize_server_from_gconf_state()
    else:
        initialize_gconf_from_server_state()
    
def usage():
    print "bad arguments"
            
def main():

    global gconf_client
    global bus
    global copy_local_state
    global online_prefs
    
    try:
        options, remaining = getopt.getopt(sys.argv[1:], '', 'upload')
    except getopt.GetoptError:
        usage()
        sys.exit(1)

    for opt, val in options:
        if opt == "--upload":
            copy_local_state = True

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

    #print prefs_proxy.Introspect()

    online_prefs = dbus.Interface(prefs_proxy, 'org.freedesktop.Preferences')

    gconf_client.add_dir('/', gconf.CLIENT_PRELOAD_NONE)
    gconf_client.notify_add('/', on_gconf_key_changed)

    if online_prefs.IsReady():
        on_prefs_ready()
    else:
        online_prefs.connect_to_signal('ReadyChanged', on_prefs_ready)
    
    loop = gobject.MainLoop()

    loop.run()

main()
