import re,logging

import dbus
import dbus.service

from ddm.AbstractModel import *
from ddm.Query import *
from ddm.NotificationSet import *
from ddm.Resource import *

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

        self.__web_base_url = None
        
        self.server_name = server_name # server_name can be None for "whatever engine is running"

        bus = dbus.SessionBus()
        bus_proxy = bus.get_object('org.freedesktop.DBus', '/org/freedesktop/DBus')
        bus_proxy.connect_to_signal("NameOwnerChanged",
                                    self.__update_proxy,
                                    arg0=_make_bus_name(server_name))
        self.__update_proxy()

        self.callback = _DBusCallback(self, bus)

    def __update_proxy(self, *args):
        self._on_disconnected()

        bus = dbus.SessionBus()
        targetname = _make_bus_name(self.server_name)
        try:
            _logger.debug("Looking for engine %s", targetname)            
            self._proxy = bus.get_object(targetname, '/org/freedesktop/od/data_model')
        except dbus.DBusException:
            _logger.debug("Failed to get proxy for %s", targetname, exc_info=True)
            return
        
        _logger.debug("Found model, querying status")          
        # Order matters ... we want the self_id to be there before we call on_connect
        self._proxy.Get('org.freedesktop.od.Model', 'SelfId', reply_handler=self.__get_self_id_reply, error_handler=self.__on_dbus_error)

        self._proxy.Get('org.freedesktop.od.Model', 'WebBaseUrl', reply_handler=self.__get_web_base_url_reply, error_handler=self.__on_dbus_error)
        
        self._proxy.connect_to_signal("ConnectedChanged", self.__on_connected_changed, dbus_interface='org.freedesktop.od.Model')
        self._proxy.Get('org.freedesktop.od.Model', 'Connected', reply_handler=self.__get_connected_reply, error_handler=self.__on_dbus_error)        

    def __on_dbus_error(self, err):
        _logger.error("Caught D-BUS error: %s", err)

    def __on_connected_changed(self, connected, self_id):
        _logger.debug("Connected status changed: %s", connected)      
        self.connected = connected  
        if connected:
            self.__on_connected(self_id)
        else:
            self.__on_disconnected()

    def __on_connected(self, self_id):
        if self_id == '':
            self._set_self_id(None)
        else:
            self._set_self_id(self_id)
        self._on_connected()

    def __on_disconnected(self):
        self._on_disconnected()

    def __get_web_base_url_reply(self, baseurl):
        _logger.debug("Got base url %s", baseurl)
        if baseurl == '':
            baseurl = None
        self.__web_base_url = baseurl

    def __get_self_id_reply(self, self_id):
        _logger.debug("Got self id %s", self_id)  
        if self_id == '':
            self._set_self_id(None)
        else:
            self._set_self_id(self_id)

    def __get_connected_reply(self, connected):
         self.connected = connected 

         self._on_initialized() 
         # this is a hack -- we pretend that we are connected so that we can get
         # the information that has been cached by the mugshot client
         if self.self_id != None:
             self._on_connected()

    def _get_proxy(self):
        return self._proxy

    def get_web_base_url(self):
        return self.__web_base_url
        
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

    def execute(self):
        # FIXME: Would it be better to call the __on_error? Doing that sync could cause problems.
        #   If we decide to continue raising an exception here, we should use a subclass
        if not self.__model.connected and self.__model.self_id == None:
            raise Exception("Not connected")

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

    def execute(self):
        # FIXME: Would it be better to call the __on_error? Doing that sync could cause problems.
        #   If we decide to continue raising an exception here, we should use a subclass
        if not self.__model.connected and self.__model.self_id == None:
            raise Exception("Not connected")

        method_uri = self.__method[0] + "#" + self.__method[1]
        _logger.debug("executing update method: '%s' params: '%s'", method_uri, self._params)
        self.__model._get_proxy().Update(method_uri, self._params,
                                         dbus_interface='org.freedesktop.od.Model', reply_handler=self.__on_update_reply, error_handler=self.__on_update_error)
        
