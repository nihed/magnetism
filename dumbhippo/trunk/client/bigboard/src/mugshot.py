import logging, inspect, xml.dom, xml.dom.minidom

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
        "self-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "whereim-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "entity-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "my-top-apps-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
        }    
    
    def __init__(self, issingleton):
        gobject.GObject.__init__(self)
        if not issingleton == 42:
            raise Exception("use mugshot.get_mugshot()")
        self._my_apps_poll_id = 0
        self._global_apps_poll_id = 0
        
        self._app_poll_frequency_ms = 30 * 60 * 1000
                
        self._reset()
        
    def _reset(self):
        # Generic properties
        self._baseprops = None
        
        self._whereim = None
        self._self = None
        self._network = None
        self._entities = {}
        self._applications = {}
        self._my_top_apps = None
        self._global_top_apps = None
        self._proxy = None
        
        self._external_iqs = {} # int -> callback
        
        self._reset_my_apps_poll()
        self._reset_global_apps_poll()


    def _reset_my_apps_poll(self):
        if self._my_apps_poll_id > 0:
            gobject.source_remove(self._my_apps_poll_id)
            self._my_apps_poll_id = 0
        self._my_apps_poll_id = gobject.timeout_add(self._app_poll_frequency_ms, self._idle_poll_my_apps)
        
    def _reset_global_apps_poll(self):
        if self._global_apps_poll_id > 0:
            gobject.source_remove(self._global_apps_poll_id)
            self._global_apps_poll_id = 0
        self._global_apps_poll_id = gobject.timeout_add(self._app_poll_frequency_ms, self._idle_poll_global_apps)        
    
    def _whereimChanged(self, name, icon_url):
        logging.debug("whereimChanged: %s %s" % (name, icon_url))
        if self._whereim is None:
            self._whereim = {}
            self._network = {}
        attrs = {'name': name, 'icon_url': icon_url}
        if not self._whereim.has_key(name):
            self._whereim[name] = ExternalAccount(attrs)
            self.emit('whereim-added', self._whereim[name])     
        else:
            self._whereim[name].update(attrs)
        
    def _entityChanged(self, attrs):
        logging.debug("entityChanged: %s" % (attrs,))
        if self._network is None:
            self._network = {}
        
        guid = attrs[u'guid']
        if not self._entities.has_key(guid):
            self._entities[guid] = Entity(attrs)
            self.emit("entity-added", self._entities[guid])
        else:
            self._entities.update(attrs)
            
    def _externalIQReturn(self, id, content):
        logging.debug("got external IQ reply for %d", id)
        if self._external_iqs.has_key(id):
            self._external_iqs[id](content)
            del self._external_iqs[id]
    
    def _get_proxy(self):
        if self._proxy is None:
            bus = dbus.SessionBus()
            self._proxy = bus.get_object('org.mugshot.Mugshot', '/org/mugshot/Mugshot')
            self._proxy.connect_to_signal('WhereimChanged', _log_cb(self._whereimChanged))
            self._proxy.connect_to_signal('EntityChanged', _log_cb(self._entityChanged))
            self._proxy.connect_to_signal('ExternalIQReturn', _log_cb(self._externalIQReturn))
        return self._proxy
    
    def get_entity(self, guid):
        return self._entites[guid]
    
    def _on_dbus_error(self, err):
        # TODO - could schedule a "reboot" of this class here to reload
        # information
        logging.error("D-BUS error: %s" % (err,))
    
    def _on_get_self(self, myself):
        logging.debug("self changed: %s" % (myself,))
        self._self = Entity(myself)
        self.emit("self-changed", self._self)
    
    def _on_get_baseprops(self, props):
        self._baseprops = {}
        for k,v in props.items():
            self._baseprops[str(k)] = v
        self.emit("initialized")
    
    def _get_baseprop(self, name):
        if self._baseprops is None:
            proxy = self._get_proxy()
            proxy.GetBaseProperties(reply_handler=_log_cb(self._on_get_baseprops), error_handler=self._on_dbus_error)
            return None
        return self._baseprops[name]
    
    def get_baseurl(self):
        return self._get_baseprop('baseurl')
    
    def get_self(self):
        if self._self is None:
            proxy = self._get_proxy()
            proxy.GetSelf(reply_handler=_log_cb(self._on_get_self), error_handler=self._on_dbus_error)
            return None
        return self._self
    
    def get_whereim(self):
        if (self._whereim is None):
            proxy = self._get_proxy()
            proxy.NotifyAllWhereim()
            return None
        return self._whereim.values()
    
    def get_network(self):
        if self._network is None:
            proxy = self._get_proxy()
            proxy.NotifyAllNetwork()
            return None
        return self._network.values()

    def _do_external_iq(self, name, xmlns, content, cb):
        """Sends a raw IQ request to Mugshot server, indirecting
        via D-BUS to client."""
        proxy = self._get_proxy()        
        logging.debug("sending external IQ request: %s %s (%d bytes)", name, xmlns, len(content))
        id = proxy.SendExternalIQ(False, name, xmlns, content)
        self._external_iqs[id] = cb
    
    def _load_app_from_xml(self, node):
        id = node.getAttribute("id")
        logging.debug("parsing application id=%s", id)
        attrs = libbig.snarf_attributes_from_xml_node(node, ['id', 'rank', 'usage-count', 'icon-url', 'description', 'name'])
        app = None
        if not self._applications.has_key(attrs['id']):
            app = Application(attrs)
            self._applications[attrs['id']] = app
        else:
            app = self._applications[attrs['id']]    
        app.update(attrs)            
        return app
    
    def _parse_app_set(self, expected_name, xml_str):
        doc = xml.dom.minidom.parseString(xml_str)
        root = doc.documentElement
        if not root.nodeName == expected_name:
            logging.warn("invalid root node, expected %s", expected_name)
            return
        apps = []
        for node in root.childNodes:
            if not (node.nodeType == xml.dom.Node.ELEMENT_NODE):
                continue
            app = self._load_app_from_xml(node)
            apps.append(app)
        return apps
            
    def _on_my_top_applications(self, xml_str):     
        self._my_top_apps = self._parse_app_set('myTopApplications', xml_str)
        logging.debug("emitting my-top-apps-changed")
        self.emit("my-top-apps-changed", self._my_top_apps)
        
    def _on_top_applications(self, xml_str):     
        self._global_top_apps = self._parse_app_set('topApplications', xml_str)
        logging.debug("emitting global-top-apps-changed")
        self.emit("global-top-apps-changed", self._global_top_apps)        
    
    def _request_my_top_apps(self):
        self._do_external_iq("myTopApplications", "http://dumbhippo.com/protocol/applications", "",
                             self._on_my_top_applications)
    
    def _request_global_top_apps(self):
        self._do_external_iq("topApplications", "http://dumbhippo.com/protocol/applications", "",
                             self._on_top_applications)        
            
    def _idle_poll_my_apps(self):
        self._request_my_top_apps()
        return True
    
    def _idle_poll_global_apps(self):
        self._request_global_top_apps()
        return True    
    
    def get_my_top_apps(self):
        if self._my_top_apps is None:
            self._request_my_top_apps()
            self._reset_my_apps_poll()
            return None
        return self._my_top_apps
    
    def get_global_top_apps(self):
        if self._global_top_apps is None:
            self._request_global_top_apps()
            self._reset_global_apps_poll()
            return None    
        return self._global_top_apps
    
mugshot_inst = None
def get_mugshot():
    global mugshot_inst
    if mugshot_inst is None:
        mugshot_inst = Mugshot(42)
    return mugshot_inst
