import re,logging

import dbus
import dbus.service

from ddm.AbstractModel import *
from ddm.Query import *
from ddm.NotificationSet import *
from ddm.Resource import *

# For idle handling
import gobject

_logger = logging.getLogger('mugshot.DataModel')

def _escape_byte(m):
    return "_%02X" % ord(m.group(0))

def _escape_server_name(server_name):
    if (server_name.find(":") < 0):
        server_name = server_name + ":80"
        
    return re.sub(r"[^a-zA-Z0-9]", _escape_byte, server_name.encode("UTF-8"))

def _make_bus_name(server_name):
    if server_name == None:
        return "org.freedesktop.od.Engine";

    return "org.freedesktop.od.Engine." + _escape_server_name(server_name)

class DataModel(AbstractModel):
    singletons = {}
    
    """
    The data model for the Mugshot desktop session.

    There is a singleton DataModel object for each server session, retrieved by
    calling the constructor DataModel(server_name). server_name is optional, and if
    omitted, the session will be for whichever server the org.freedesktop.od.Engine
    bus name owner owns.
    
    """
    def __new__(cls, server_name=None):
        try:
            return DataModel.singletons[server_name];
        except KeyError:
            model = AbstractModel.__new__(cls)
            model.__real_init(server_name)
            DataModel.singletons[server_name] = model

            return model

    def __init__(cls, server_name=None):
        # Don't chain up here, this is called for each singleton
        pass

    def __real_init(self, server_name):
        AbstractModel.__init__(self)

        self.server_name = server_name # server_name can be None for "whatever engine is running"

        bus = dbus.SessionBus()
        bus_proxy = bus.get_object('org.freedesktop.DBus', '/org/freedesktop/DBus')
        bus_proxy.connect_to_signal("NameOwnerChanged",
                                    self.__on_name_owner_changed,
                                    arg0=_make_bus_name(server_name))
        self._proxy = None        
        self.__update_proxy()

        self.callback = _DBusCallback(self, bus)

    def __on_name_owner_changed(self, name, old_owner, new_owner):
        self._proxy = None
        if new_owner != '':
            self.__update_proxy()
        else:
            self.__go_offline()

    def __update_proxy(self, *args):
        bus = dbus.SessionBus()
        targetname = _make_bus_name(self.server_name)
        try:
            _logger.debug("Looking for engine %s", targetname)            
            self._proxy = bus.get_object(targetname, '/org/freedesktop/od/data_model')
        except dbus.DBusException:
            # Probably means the engine couldn't be activated
            _logger.debug("Failed to get proxy for %s", targetname, exc_info=True)
            self.__go_offline()
            return
        
        _logger.debug("Found new model, querying ready state")

        self._proxy.connect_to_signal("Ready", self.__on_ready, dbus_interface='org.freedesktop.od.Model')
        self._proxy.Get('org.freedesktop.od.Model', 'Ready', reply_handler=self.__get_ready_reply, error_handler=self.__get_ready_error)

    def __get_ready_reply(self, ready):
        _logger.debug("Got reply for ready state, Ready=%s", ready)
        if ready:
            self.__on_ready()

    def __get_ready_error(self, err):
        # Probably means that the engine died
        _logger.error("Caught D-BUS error asking for Ready: %s", err)
        self.__go_offline()

    def __on_ready(self):
        self._reset()

        _logger.debug("Doing initial query")
        
        # don't add global object properties you need here unless pyddm needs them - 
        # instead, put another query in your app
        query = self.query_resource("online-desktop:/o/global", "self +;webBaseUrl;online;ddmProtocolVersion")
        query.add_handler(self.__on_initial_query_success)
        query.add_error_handler(self.__on_initial_query_error)
        query.execute()
            
    def __on_initial_query_success(self, resource):
        self._on_ready()
            
    def __on_initial_query_error(self, code, message):
        # Probably means that the engine died
        _logger.error("Got an error response to the initial query: %s", message)
        self.__go_offline()

    def __go_offline(self):
        # Common handling if an error occurs in the initialization path or we lose the
        # connection to the server; we change the state to be offline and if we were
        # still in the "not yet ready" state, signal the end of initialization
        #
        if self.global_resource == None:
            self._reset()
        notifications = NotificationSet(self)
        self.global_resource._update_property(("online-desktop:/p/o/global", "online"),
                                              UPDATE_REPLACE, CARDINALITY_1, False,
                                              notifications)
        notifications.send()

        if not self.ready:
            self._on_ready()

    def _get_proxy(self):
        return self._proxy

    def query(self, method, fetch=None, single_result=False, **kwargs):
        _logger.debug("doing query: %s fetch=%s, single_result=%s", method, fetch, single_result)
        return _DBusQuery(self, method, fetch, single_result, kwargs)

    def update(self, method, **kwargs):
        _logger.debug("doing update: %s", method)        
        return _DBusUpdate(self, method, kwargs)

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
                raise Exception("Resource-valued element points to a resource we don't know about: " + str(value))
        elif type_byte == ord('s') or type_byte == ord('u'):
            value = value.__str__()
        elif type_byte == ord('b'):
            value = bool(value)
            
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
            path = "/org/freedesktop/od/callback/" + _escape_server_name(model.server_name)
        else:
            path = "/org/freedesktop/od/callback/_default"

        self.path = dbus.ObjectPath(path)
        
        dbus.service.Object.__init__(self, bus, self.path)

    @dbus.service.method("org.freedesktop.od.ModelClient",
                         in_signature='a(ssba(ssyyyv))', out_signature='')
    def Notify(self, resources):
        _logger.debug("got notify for resources: %s", resources)
        notifications = NotificationSet(self.__model)
        for resource_struct in resources:
            try:
                self.__model._update_resource_from_dbus(resource_struct, notifications=notifications)
            except Exception, e:
                _logger.error("Failed to update resource from a Notify", e)

        notifications.send()
    
class _DBusQuery(Query):
    def __init__(self, model, method, fetch, single_result, params):
        Query.__init__(self, params, single_result)
        self.__model = model
        self.__method = method
        self.__fetch = fetch
        self.__single_result = single_result

    def __on_query_reply(self, resources):
        result = []
        notifications = NotificationSet(self.__model)        
        for resource_struct in resources:
            try:
                (resource,indirect) = self.__model._update_resource_from_dbus(resource_struct, notifications=notifications)
                if resource != None and not indirect:
                    result.append(resource)
            except Exception, e:
                _logger.error("Failed to update resource from a query reply: " + e.message)

        notifications.send()
        self._on_success(result)

    def __on_query_error(self, err):
        # FIXME: As of dbus-python-0.80, exception handling for is very, very, limited
        # all we get is the message, so we can't do anything special for the defined
        # DataModel errors. This is fixed in later versions of dbus-python, where we can
        # get the exception type and the args
        #
        _logger.error('Caught error: %s', err.message)
        self._on_error(ERROR_FAILED, err.message)

    def __async_no_connection_error(self):
        self._on_error(ERROR_NO_CONNECTION, "No connection to engine to data model engine")
        return False

    def execute(self):
        if self.__model._proxy == None:
            self._on_error(ERROR_NO_CONNECTION, "No connection to data model engine")
            return

        method_uri = self.__method[0] + "#" + self.__method[1]
        #_logger.debug("executing query method: '%s' fetch: '%s' params: '%s'", method_uri, self.__fetch, self._params)
        self.__model._get_proxy().Query(self.__model.callback.path, method_uri, self.__fetch, self._params,
                                        dbus_interface='org.freedesktop.od.Model', reply_handler=self.__on_query_reply, error_handler=self.__on_query_error)
        

class _DBusUpdate(Query):
    def __init__(self, model, method, params):
        Query.__init__(self, params)
        self.__model = model
        self.__method = method

    def __on_update_reply(self):
        self._on_success()

    def __on_update_error(self, err):
        # FIXME: As of dbus-python-0.80, exception handling is very, very, limited
        # all we get is the message, so we can't do anything special for the defined
        # DataModel errors. This is fixed in later versions of dbus-python, where we can
        # get the exception type and the args
        #
        _logger.error('Caught error: %s', err.message)
        self._on_error(ERROR_FAILED, err.message)

    def __async_no_connection_error(self):
        self._on_error(ERROR_NO_CONNECTION, "No connection to engine")
        return False

    def execute(self):
        if self.__model._proxy == None:
            gobject.idle_add(self.__async_no_connection_error)
            return

        method_uri = self.__method[0] + "#" + self.__method[1]
        _logger.debug("executing update method: '%s' params: '%s'", method_uri, self._params)
        self.__model._get_proxy().Update(method_uri, self._params,
                                         dbus_interface='org.freedesktop.od.Model', reply_handler=self.__on_update_reply, error_handler=self.__on_update_error)
        
