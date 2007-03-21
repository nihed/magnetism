import logging, inspect, xml.dom, xml.dom.minidom, StringIO

import gobject, dbus

import libbig
from libbig import _log_cb

class ExternalAccount(libbig.AutoSignallingStruct):
    pass
    
class Entity(libbig.AutoSignallingStruct):
    """A Mugshot entity such as person, group, or feed."""
    pass

class Application(libbig.AutoSignallingStruct):
    pass

class Mugshot(gobject.GObject):
    """A combination of a wrapper and cache for the Mugshot D-BUS API.  Access
    using the get_mugshot() module method."""
    __gsignals__ = {
        "initialized" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "connection-status": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT, gobject.TYPE_PYOBJECT, gobject.TYPE_PYOBJECT)),
        "self-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "whereim-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "entity-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "global-top-apps-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "my-top-apps-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "pinned-apps-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))        
        }    
    
    def __init__(self, issingleton):
        gobject.GObject.__init__(self)
        
        self._logger = logging.getLogger('bigboard.Mugshot')
        
        if not issingleton == 42:
            raise Exception("use mugshot.get_mugshot()")
        self.__my_apps_poll_id = 0
        self.__global_apps_poll_id = 0
        
        self.__app_poll_frequency_ms = 30 * 60 * 1000
        
        self._logger.debug("connecting to session bus")            
        session_bus = dbus.SessionBus()
        bus_proxy = session_bus.get_object('org.freedesktop.DBus', '/org/freedesktop/DBus')
        self.__bus_proxy = bus_proxy.connect_to_signal("NameOwnerChanged",
                                                       _log_cb(self.__on_dbus_name_owner_changed))
        self.__create_proxy()
        self.__create_ws_proxy()
        
        self.__reset()        
        
    def __reset(self):
        self._logger.debug("reset")  
        # Generic properties
        self.__baseprops = None
        
        self.__whereim = None # <str>,<ExternalAccount>
        self.__self_proxy = None
        self.__self = None
        self.__self_path = None
        self.__network = None
        self.__entities = {} # <str>,<Entity>
        self.__applications = {} # <str>,<Application>
        self.__my_top_apps = None # <Application>
        self.__my_app_usage_start = None
        self.__pinned_apps = None
        self.__global_top_apps = None # <Application>
        
        self.__external_iqs = {} # <int>,<function>
        
        self.__reset_my_apps_poll()
        self.__reset_global_apps_poll()

    def __reset_my_apps_poll(self):
        if self.__my_apps_poll_id > 0:
            gobject.source_remove(self.__my_apps_poll_id)
            self.__my_apps_poll_id = 0
        self.__my_apps_poll_id = gobject.timeout_add(self.__app_poll_frequency_ms, self.__idle_poll_my_apps)
        
    def __reset_global_apps_poll(self):
        if self.__global_apps_poll_id > 0:
            gobject.source_remove(self.__global_apps_poll_id)
            self.__global_apps_poll_id = 0
        self.__global_apps_poll_id = gobject.timeout_add(self.__app_poll_frequency_ms, self.__idle_poll_global_apps)        
        
    def __create_proxy(self):
        try:        
            bus = dbus.SessionBus()
            self._logger.debug("creating proxy for org.mugshot.Mugshot")
            self.__proxy = bus.get_object('org.mugshot.Mugshot', '/org/mugshot/Mugshot')
            self.__proxy.connect_to_signal('ConnectionStatusChanged', _log_cb(self.__on_connection_status_changed))          
            self.__proxy.connect_to_signal('WhereimChanged', _log_cb(self.__whereimChanged))
            self.__proxy.connect_to_signal('EntityChanged', _log_cb(self.__entityChanged))
            self.__proxy.connect_to_signal('ExternalIQReturn', _log_cb(self.__externalIQReturn))
            self.__get_connection_status()    
            self.__proxy.GetBaseProperties(reply_handler=_log_cb(self.__on_get_baseprops), error_handler=self.__on_dbus_error)            
        except dbus.DBusException:
            self.__proxy = None

    def __create_ws_proxy(self):
        try:
            bus = dbus.SessionBus()
            self.__ws_proxy = bus.get_object('org.mugshot.Mugshot', '/org/gnome/web_services')
        except dbus.DBusException:
            self.__ws_proxy = None        
        
    def __on_dbus_name_owner_changed(self, name, prev_owner, new_owner):
        if name == 'org.mugshot.Mugshot':
            if new_owner != '':
                self._logger.debug("owner for org.mugshot.Mugshot changed, recreating proxies")
                self.__create_proxy()
                self.__create_ws_proxy()
                self.__create_listener_proxy()
            else:
                self.__proxy = None
                self.__ws_proxy = None

    def __on_connection_status(self, has_auth, connected, contacts):
        self._logger.debug("connection status auth=%s connected=%s contacts=%s" % (has_auth, connected, contacts))
        self.emit("connection-status", has_auth, connected, contacts)         

    def __get_connection_status(self):
        self.__proxy.GetConnectionStatus(reply_handler=_log_cb(self.__on_connection_status),
                                         error_handler=_log_cb(self.__on_dbus_error))

    def __on_connection_status_changed(self):
        self._logger.debug("connection status changed")        
        self.__get_connection_status()
    
    def __whereimChanged(self, props):
        self._logger.debug("whereimChanged: %s" % (props,))
        if self.__whereim is None:
            self.__whereim = {}
            self.__network = {}
        name = props['name']
        if not self.__whereim.has_key(name):
            self.__whereim[name] = ExternalAccount(props)
            self.emit('whereim-added', self.__whereim[name])     
        else:
            self.__whereim[name].update(props)
        
    def __entityChanged(self, attrs):
        self._logger.debug("entityChanged: %s" % (attrs,))
        if self.__network is None:
            self.__network = {}
        
        guid = attrs[u'guid']
        if not self.__entities.has_key(guid):
            self.__entities[guid] = Entity(attrs)
            self.emit("entity-added", self.__entities[guid])
        else:
            self.__entities.update(attrs)
            
    def __externalIQReturn(self, id, content):
        if self.__external_iqs.has_key(id):
            self._logger.debug("got external IQ reply for %d (%d outstanding)", id, len(self.__external_iqs.keys())-1)            
            self.__external_iqs[id](content)
            del self.__external_iqs[id]
    
    def __do_proxy_call(self, ):
        if self.__proxy is None:
            bus = dbus.SessionBus()
        return self.__proxy
    
    def get_entity(self, guid):
        return self.__entites[guid]
    
    def __on_dbus_error(self, err):
        # TODO - could schedule a "reboot" of this class here to reload
        # information
        self._logger.exception("D-BUS error: %s" % (err,))
    
    def __on_self_changed(self):
        self.__self_proxy.GetProperties(reply_handler=_log_cb(self.__on_get_self_properties),
                                        error_handler=_log_cb(self.__on_dbus_error))        
    
    def __on_get_self(self, myself_path):
        self._logger.debug("got self path: %s" % (myself_path,))
        self.__self_proxy = dbus.SessionBus().get_object('org.mugshot.Mugshot', myself_path)
        self.__on_self_changed()
        self.__self_proxy.connect_to_signal("Changed", 
                                            _log_cb(self.__on_self_changed), 
                                            'org.mugshot.Mugshot.Entity')
        
    def __on_get_self_properties(self, myself):
        self._logger.debug("self properties: %s" % (myself,))
        if self.__self:
            self.__self.update(myself)
        else:
            self.__self = Entity(myself)
        self.emit("self-changed", self.__self)        
    
    def __on_get_baseprops(self, props):
        self.__baseprops = {}
        for k,v in props.items():
            self.__baseprops[str(k)] = v
        self.emit("initialized")
    
    def __get_baseprop(self, name):
        return self.__baseprops[name]
    
    def get_baseurl(self):
        return self.__get_baseprop('baseurl')
    
    def get_self(self):
        if self.__self is None:
            self.__proxy.GetSelf(reply_handler=_log_cb(self.__on_get_self), error_handler=self.__on_dbus_error)
            return None
        return self.__self
    
    def get_whereim(self):
        if (self.__whereim is None):
            self.__proxy.NotifyAllWhereim()
            return None
        return self.__whereim.values()
    
    def get_network(self):
        if self.__network is None:
            self.__proxy.NotifyAllNetwork()
            return None
        return self.__network.values()

    def get_cookies(self, url):
        cookies = self.__ws_proxy.GetCookiesToSend(url)
            
        #print cookies
        return cookies

    def __do_external_iq(self, name, xmlns, content, cb, is_set=False):
        """Sends a raw IQ request to Mugshot server, indirecting
        via D-BUS to client."""     
        if self.__proxy is None:
            self._logger.warn("No Mugshot active, not sending IQ")
            return
        self._logger.debug("sending external IQ request: set=%s %s %s (%d bytes)", is_set, name, xmlns, len(content))
        id = self.__proxy.SendExternalIQ(is_set, name, xmlns, content)
        self.__external_iqs[id] = cb
    
    def __load_app_from_xml(self, node):
        id = node.getAttribute("id")
        self._logger.debug("parsing application id=%s", id)
        attrs = libbig.snarf_attributes_from_xml_node(node, ['id', 'rank', 'usageCount', 
                                                             'iconUrl', 'description',
                                                             'category', 'tooltip',
                                                             'name', 'desktopNames'])
        app = None
        if not self.__applications.has_key(attrs['id']):
            app = Application(attrs)
            self.__applications[attrs['id']] = app
        else:
            app = self.__applications[attrs['id']]    
        app.update(attrs)            
        return app
    
    def __parse_app_set(self, expected_name, doc):
        root = doc.documentElement
        if not root.nodeName == expected_name:
            self._logger.warn("invalid root node, expected %s", expected_name)
            return None
        apps = []
        for node in root.childNodes:
            if not (node.nodeType == xml.dom.Node.ELEMENT_NODE):
                continue
            app = self.__load_app_from_xml(node)
            apps.append(app)
        return apps
            
    def __on_my_top_applications(self, xml_str):     
        doc = xml.dom.minidom.parseString(xml_str)        
        self.__my_top_apps = self.__parse_app_set('myTopApplications', doc)
        self.__my_app_usage_start = doc.documentElement.getAttribute("since")
        self._logger.debug("emitting my-top-apps-changed")
        self.emit("my-top-apps-changed", self.__my_top_apps)
        
    def __on_top_applications(self, xml_str):     
        doc = xml.dom.minidom.parseString(xml_str)        
        self.__global_top_apps = self.__parse_app_set('topApplications', doc)
        self._logger.debug("emitting global-top-apps-changed")
        self.emit("global-top-apps-changed", self.__global_top_apps)        
    
    def __request_my_top_apps(self):
        self.__do_external_iq("myTopApplications", "http://dumbhippo.com/protocol/applications", "",
                             self.__on_my_top_applications)
    
    def __request_global_top_apps(self):
        self.__do_external_iq("topApplications", "http://dumbhippo.com/protocol/applications", "",
                             self.__on_top_applications)        
            
    def __idle_poll_my_apps(self):
        self.__request_my_top_apps()
        return True
    
    def __idle_poll_global_apps(self):
        self.__request_global_top_apps()
        return True    
    
    def get_my_top_apps(self):
        if self.__my_top_apps is None:
            self.__request_my_top_apps()
            self.__reset_my_apps_poll()
            return None
        return self.__my_top_apps
    
    def get_my_app_usage_start(self):
        return self.__my_app_usage_start
    
    def get_global_top_apps(self):
        if self.__global_top_apps is None:
            self.__request_global_top_apps()
            self.__reset_global_apps_poll()
            return None    
        return self.__global_top_apps
    
    def __on_pinned_apps(self, xml_str):
        self._logger.debug("parsing pinned apps reply: %s", xml_str)
        doc = xml.dom.minidom.parseString(xml_str)        
        self.__pinned_apps = self.__parse_app_set('pinned', doc)
        self._logger.debug("emitting pinned-apps-changed")
        self.emit("pinned-apps-changed", self.__pinned_apps)        
    
    def get_pinned_apps(self):
        self.__do_external_iq("pinned", "http://dumbhippo.com/protocol/applications", "",
                              self.__on_pinned_apps)
        
    def set_pinned_apps(self, ids):
        iq = StringIO.StringIO()
        for id in ids:
            iq.write('<appId>')
            iq.write(id)
            iq.write('</appId>')
        self.__do_external_iq("pinned", "http://dumbhippo.com/protocol/applications", 
                              iq.getvalue(),
                              lambda *args: self._logger.debug("app pin set succeeded"), is_set=True)     
    
mugshot_inst = None
def get_mugshot():
    global mugshot_inst
    if mugshot_inst is None:
        mugshot_inst = Mugshot(42)
    return mugshot_inst
