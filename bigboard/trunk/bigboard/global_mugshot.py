import os,sys
import logging, inspect, xml.dom, xml.dom.minidom, functools
import StringIO, urlparse, urllib, subprocess, time, sha

import gobject, dbus

import libbig
from libbig.logutil import log_except
from libbig.http import AsyncHTTPFetcher
from libbig.xmlquery import query as xml_query, get_attrs as xml_get_attrs
from libbig.struct import AutoStruct, AutoSignallingStruct
import globals

_logger = logging.getLogger("bigboard.Mugshot")

class Mugshot(gobject.GObject):
    """This class is a wrapper for the non-data-model D-BUS API's we use from the data model
    engine. Access using the get_mugshot() module method."""
    
    def __init__(self, issingleton):
        gobject.GObject.__init__(self)

        self._logger = logging.getLogger('bigboard.Mugshot')
        
        if not issingleton == 42:
            raise Exception("use global_mugshot.get_mugshot()")

        self._logger.debug("connecting to session bus")            
        session_bus = dbus.SessionBus()
        bus_proxy = session_bus.get_object('org.freedesktop.DBus', '/org/freedesktop/DBus')
        self.__bus_proxy = bus_proxy.connect_to_signal("NameOwnerChanged",
                                                       self.__on_dbus_name_owner_changed)
        self.__create_proxy()
        self.__create_ws_proxy()

        self.__reset()        
        
    def __reset(self):
        self._logger.debug("reset")  
        # Generic properties

        self.__endpoint_id = None
        
    def __create_proxy(self):
         try:        
             bus = dbus.SessionBus()
             self._logger.debug("creating proxy for %s" % globals.bus_name)
             self.__proxy = bus.get_object(globals.bus_name, '/org/mugshot/Mugshot')
             self.__mugshot_dbus_proxy = bus.get_object(globals.bus_name, '/com/dumbhippo/client')             
             self.__mugshot_dbus_proxy.RegisterEndpoint(reply_handler=self.__on_register_endpoint, error_handler=self.__on_dbus_error)
            
         except dbus.DBusException, e:
             _logger.debug("Caught D-BUS exception while trying to create proxy", exc_info=True)
             self.__proxy = None

    def __create_ws_proxy(self):
         try:
             bus = dbus.SessionBus()
             self.__ws_proxy = bus.get_object(globals.bus_name, '/org/gnome/web_services')
         except dbus.DBusException:
            self.__ws_proxy = None        

    @log_except(_logger)
    def __on_dbus_name_owner_changed(self, name, prev_owner, new_owner):
        if name == globals.bus_name:
            if new_owner != '':
                self._logger.debug("owner for %s changed, recreating proxies", globals.bus_name)
                self.__create_proxy()
            else:
                self.__proxy = None
                self.__ws_proxy = None

    @log_except(_logger)
    def __on_register_endpoint(self, id):
        self.__endpoint_id = id

    @log_except(_logger)
    def __on_dbus_error(self, err):
        # TODO - could schedule a "reboot" of this class here to reload
        # information
        self._logger.error("D-BUS error: %s", err)
    
    def install_application(self, id, package_names, desktop_names):
        self._logger.debug("requesting install of app id %s", id)
        self.__mugshot_dbus_proxy.InstallApplication(self.__endpoint_id, id, package_names, desktop_names)


mugshot_inst = None
def get_mugshot():
    global mugshot_inst
    if mugshot_inst is None:
        mugshot_inst = Mugshot(42)
    return mugshot_inst
