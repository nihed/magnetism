from mugshot.AbstractModel import *
from mugshot.Query import *
from mugshot.NotificationSet import *
from mugshot.Resource import *
import dbus
import dbus.service
import re

def _escape_byte(m):
    return "_%02X" % ord(m.group(0))

def _escape_server_name(server_name):
    if (server_name.index(":") < 0):
        server_name = server_name + ":80"
        
    return re.sub(r"[^a-zA-Z0-9]", _escape_byte, server_name.encode("UTF-8"))

def _make_bus_name(server_name):
    if server_name == None:
        return "org.mugshot.Mugshot";

    return "com.dumbhippo.Client." + _escape_server_name(server_name)

class DataModel(AbstractModel):
    singletons = {}
    
    """
    The data model for the Mugshot desktop session.

    There is a singleton DataModel object for each server session, retrieved by
    calling the constructor DataModel(server_name). server_name is optional, and if
    omitted, the session will be for the official official mugshot.org server.
    
    """
    def __new__(cls, server_name=None):
        try:
            return DataModel.singletons[server_name];
        except KeyError:
            model = AbstractModel.__new__(cls)
            model.__real_init(server_name)
            DataModel.singletons[server_name] = model

            return model

    def __init__(cls, server_name):
        # Don't chain up here, this is called for each singleton
        pass

    def __real_init(self, server_name):
        AbstractModel.__init__(self)
        
        self.server_name = server_name

        # FIXME: catch dbus.DBusException and connect to NameOwnerChanged
        bus = dbus.SessionBus()
        self._proxy = bus.get_object(_make_bus_name(server_name), '/org/mugshot/data_model')

        self.callback = _DBusCallback(self, bus)

    def _get_proxy(self):
        return self._proxy
        
    def query(self, method, fetch=None, single_result=False, **kwargs):
        return _DBusQuery(self, method, fetch, single_result, kwargs)

    def __update_property_from_dbus(self, resource, property_struct, notifications):
        property_uri, property_name, update_byte, type_byte, cardinality_byte, value = property_struct

        if update_byte == ord('a'):
            update = UPDATE_ADD
        elif update_byte == ord('r'):
            update = UPDATE_REPLACE
        elif update_byte == ord('d'):
            update = UPDATE_DELETE
        elif update_byte == ord('c'):
            update = UPDATE_CLEAR
                    
        if type_byte == ord('r'):
            try:
                value = self._get_resource(value)
            except KeyError:
                raise Exception("Resource-valued element points to a resource we don't know about")
        elif type_byte == ord('s'):
            value = value.__str__()
            
        if cardinality_byte == ord('.'):
            cardinality = CARDINALITY_1
        elif cardinality_byte == ord('?'):
            cardinality = CARDINALITY_01
        elif cardinality_byte == ord('*'):
            cardinality = CARDINALITY_N

        resource._update_property((property_uri, property_name), update, cardinality, value, notifications=notifications)

    def _update_resource_from_dbus(self, resource_struct, notifications=None):
        resource_id, class_id, indirect, properties = resource_struct
        
        resource = self._ensure_resource(resource_id, class_id)
        for property_struct in properties:
            self.__update_property_from_dbus(resource, property_struct, notifications=notifications)

        return (resource,indirect)

class _DBusCallback(dbus.service.Object):
    def __init__(self, model, bus):
        self.__model = model

        if model.server_name != None:
            path = "/org/mugshot/callback/" + _escape_server_name(model.server_name)
        else:
            path = "/org/mugshot/callback/_default"

        self.path = dbus.ObjectPath(path)
        
        dbus.service.Object.__init__(self, bus, self.path)

    @dbus.service.method("org.mugshot.dm.Client",
                         in_signature='a(ssba(ssyyyv))', out_signature='')
    def Notify(self, resources):
        notifications = NotificationSet(self.__model)
        for resource_struct in resources:
            self.__model._update_resource_from_dbus(resource_struct, notifications=notifications)
        notifications.send()
    
class _DBusQuery(Query):
    def __init__(self, model, method, fetch, single_result, params):
        Query.__init__(self, single_result)
        self.__model = model
        self.__method = method
        self.__fetch = fetch
        self.__single_result = single_result
        self.__params = params

    def __on_reply(self, resources):
        result = []
        for resource_struct in resources:
            (resource,indirect) = self.__model._update_resource_from_dbus(resource_struct)
            if resource != None and not indirect:
                result.append(resource)
                
        self._on_success(result)

    def __on_error(self, *args):
        print args

    def execute(self):
        self.__model._get_proxy().Query(self.__model.callback.path, self.__method[0] + "#" + self.__method[1], self.__fetch, self.__params,
                                        dbus_interface='org.mugshot.dm.Model', reply_handler=self.__on_reply, error_handler=self.__on_error)
