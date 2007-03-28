import logging, inspect, xml.dom, xml.dom.minidom, StringIO

import gobject, dbus

import libbig
from libbig import _log_cb

class ExternalAccount(libbig.AutoSignallingStruct):
    pass

class ExternalAccountThumbnail(libbig.AutoStruct):
    pass
    
class Entity(libbig.AutoSignallingStruct):
    """Abstract superclass of a Mugshot entity such as person, group, or feed; see
    subclasses."""
    pass
    
class Person(Entity):
    def __init__(self, *args, **kwargs):
        super(Person, self).__init__(*args, **kwargs)
        self.__requesting_accts = False
        self.__external_accounts = None
    
    def get_external_accounts(self):
        # FIXME - server needs to notify us
        if self.__external_accounts is not None:
            return self.__external_accounts
        elif not self.__requesting_accts:
            self.__requesting_accts = True
            mugshot = get_mugshot()
            mugshot.get_person_accounts(self)
            
    def set_external_accounts(self, accts):
        self.__requesting_accts = False
        self.__external_accounts = accts
        self.emit("changed")
        
class Group(Entity):
    pass

class Resource(Entity):
    pass

class Feed(Entity):
    pass

class Application(libbig.AutoSignallingStruct):
    pass

class Mugshot(gobject.GObject):
    """A combination of a wrapper and cache for the Mugshot D-BUS API.  Access
    using the get_mugshot() module method."""
    __gsignals__ = {
        "initialized" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "connection-status": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT, gobject.TYPE_PYOBJECT, gobject.TYPE_PYOBJECT)),
        "self-known" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "network-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
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
        
        self.__self_proxy = None
        self.__self = None
        self.__self_path = None
        self.__network = None
        self.__entities = {} # <str>,<Entity>
        self.__applications = {} # <str>,<Application>
        self.__my_top_apps = None # <Application>
        self.__my_app_usage_start = None
        self.__apps_enabled = None
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
            
    def __externalIQReturn(self, id, content):
        if self.__external_iqs.has_key(id):
            self._logger.debug("got external IQ reply for %d (%d outstanding)", id, len(self.__external_iqs.keys())-1)            
            self.__external_iqs[id](content)
            del self.__external_iqs[id]
    
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
            self.__self = Person(myself)
            self._logger.debug("emitting self known")
            self.emit("self-known")  
    
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
    
    def __on_get_network_entity_props(self, proxy, attrs):
        self._logger.debug("entity properties: %s" % (attrs,))
        
        guid = attrs[u'guid']
        if not self.__network.has_key(guid):
            entity_type = attrs['type']
            entity_class = {'person': Person, 'group': Group, 
                            'resource': Resource, 'feed': Feed}
            self.__network[guid] = entity_class[attrs['type']](attrs)
            self.emit("network-changed")
        else:
            self.__network[guid].update(attrs)        
        if proxy:
            proxy.connect_to_signal("Changed", 
                                    _log_cb(lambda props: self.__on_get_network_entity_props(None, props)), 
                                    'org.mugshot.Mugshot.Entity')
        
    
    def __on_get_network(self, opaths):
        self.__network = {}
        for opath in opaths:
            proxy = dbus.SessionBus().get_object('org.mugshot.Mugshot', opath)
            proxy.GetProperties(reply_handler=_log_cb(lambda props: self.__on_get_network_entity_props(proxy, props)),
                                error_handler=_log_cb(self.__on_dbus_error))              
    
    def get_network(self):
        if self.__network is None:
            self.__proxy.GetNetwork(reply_handler=_log_cb(self.__on_get_network), error_handler=self.__on_dbus_error)
            return None
        return self.__network.itervalues()

    def get_cookies(self, url):
        cookies = self.__ws_proxy.GetCookiesToSend(url)
            
        #print cookies
        return cookies

    def __do_external_iq(self, name, xmlns, cb, attrs=None, content="", is_set=False):
        """Sends a raw IQ request to Mugshot server, indirecting
        via D-BUS to client."""     
        if self.__proxy is None:
            self._logger.warn("No Mugshot active, not sending IQ")
            return
        self._logger.debug("sending external IQ request: set=%s name=%s xmlns=%s attrs=%s (%d bytes)", is_set, name, xmlns, attrs, len(content))
        if attrs is None:
            attrs = {}
        attrs['xmlns'] = xmlns
        flattened_attrs = []
        for k,v in attrs.iteritems():
            flattened_attrs.append(k)
            flattened_attrs.append(v)
        id = self.__proxy.SendExternalIQ(is_set, name, flattened_attrs, content)
        self.__external_iqs[id] = cb
        return id
        
    def __on_get_person_accounts(self, person, xml_str):
        doc = xml.dom.minidom.parseString(xml_str) 
        accts = []
        for child in libbig.xml_query(doc.documentElement, 'externalAccount*'):
            attrs = libbig.xml_gather_attrs(child, ['type', 
                                                    'sentiment', 
                                                    'icon'])
            accttype = attrs['type']
            if attrs['sentiment'] == 'love':
                attrs['link'] = child.getAttribute('link')
            thumbnails = []            
            try:
                thumbnails_node = libbig.xml_query(child, 'thumbnails')
            except KeyError:
                thumbnails_node = None
            if thumbnails_node:
                for thumbnail in libbig.xml_query(thumbnails_node, 'thumbnail*'):
                    subattrs = libbig.xml_gather_attrs(thumbnail, ['src', 'title', 'href'])
                    thumbnails.append(ExternalAccountThumbnail(subattrs)) 
                self._logger.debug("%d thumbnails found for account %s (user %s)" % (len(thumbnails), accttype, person)) 
                attrs['thumbnails'] = thumbnails
            acct = ExternalAccount(attrs)
            accts.append(acct)
        self._logger.debug("setting %d accounts for user %s" % (len(accts), person))
        person.set_external_accounts(accts)
        
    def get_person_accounts(self, person):
        return self.__do_external_iq("whereim", "http://dumbhippo.com/protocol/whereim",
                                     lambda node: self.__on_get_person_accounts(person, node),
                                     attrs={'who': person.get_guid()})
    
    def __load_app_from_xml(self, node):
        id = node.getAttribute("id")
        self._logger.debug("parsing application id=%s", id)
        attrs = libbig.xml_gather_attrs(node, ['id', 'rank', 'usageCount', 
                                               'iconUrl', 'description',
                                               'category',
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
            return []
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
        self.__apps_enabled = doc.documentElement.getAttribute("enabled").lower() == 'true'
        self._logger.debug("apps enabled: %s", self.__apps_enabled)        
        self._logger.debug("emitting my-top-apps-changed")
        self.emit("my-top-apps-changed", self.__my_top_apps)
        
    def __on_top_applications(self, xml_str):     
        doc = xml.dom.minidom.parseString(xml_str)        
        self.__global_top_apps = self.__parse_app_set('topApplications', doc)
        self._logger.debug("emitting global-top-apps-changed")
        self.emit("global-top-apps-changed", self.__global_top_apps)        
    
    def __request_my_top_apps(self):
        self.__do_external_iq("myTopApplications", "http://dumbhippo.com/protocol/applications",
                             self.__on_my_top_applications)
    
    def __request_global_top_apps(self):
        self.__do_external_iq("topApplications", "http://dumbhippo.com/protocol/applications",
                             self.__on_top_applications)        
            
    def __idle_poll_my_apps(self):
        self.__request_my_top_apps()
        return True
    
    def __idle_poll_global_apps(self):
        self.__request_global_top_apps()
        return True    
    
    def get_app(self, guid):
        return self.__applications[guid]
    
    def get_my_top_apps(self):
        if self.__my_top_apps is None:
            self.__my_top_apps = []
            self.__request_my_top_apps()
            self.__reset_my_apps_poll()
            return None
        return self.__my_top_apps
    
    def get_my_app_usage_start(self):
        return self.__my_app_usage_start
    
    def get_global_top_apps(self):
        if self.__global_top_apps is None:
            self.__global_top_apps = []
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
    
    def get_apps_enabled(self):
        return self.__apps_enabled
    
    def get_pinned_apps(self):
        if self.__pinned_apps is None:
            self.__pinned_apps = []
            self.__do_external_iq("pinned", "http://dumbhippo.com/protocol/applications",
                                  self.__on_pinned_apps)
            return None
        return self.__pinned_apps
        
    def set_pinned_apps(self, ids, cb):
        iq = StringIO.StringIO()
        for id in ids:
            iq.write('<appId>')
            iq.write(id)
            iq.write('</appId>')
        self.__do_external_iq("pinned", "http://dumbhippo.com/protocol/applications", 
                              lambda *args: cb(), 
                              content=iq.getvalue(),
                              is_set=True)     
    
mugshot_inst = None
def get_mugshot():
    global mugshot_inst
    if mugshot_inst is None:
        mugshot_inst = Mugshot(42)
    return mugshot_inst
