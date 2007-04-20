import dbus

def bus_proxy(bus=None):
    target_bus = bus or dbus.Bus()
    return target_bus.get_object('org.freedesktop.DBus', '/org/freedesktop/DBus')

class DBusNameExistsException(Exception):
    pass

def take_name(name, bus=None):
    target_bus = bus or dbus.Bus()
    proxy = bus_proxy(bus=target_bus)
    if not proxy.RequestName(name, dbus.UInt32(4)) in (1,4):
        raise DBusNameExistsException("Couldn't get D-BUS name %s: Name exists")
