import logging, inspect

import gobject, dbus

import libbig

class ExternalAccount(libbig.AutoSignallingStruct):
    pass
    
class Entity(libbig.AutoSignallingStruct):
    """A Mugshot entity such as person, group, or feed."""
    pass

class Mugshot(gobject.GObject):
    """A combination of a wrapper and cache for the Mugshot D-BUS API.  Access
    using the get_mugshot() module method."""
    __gsignals__ = {
        "initialized" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "self-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "whereim-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "entity-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
        }    
    
    def __init__(self, issingleton):
        gobject.GObject.__init__(self)
        if not issingleton == 42:
            raise Exception("use mugshot.get_mugshot()")
        
        # Generic properties
        self._baseprops = None
        
        self._whereim = None
        self._self = None
        self._network = None
        self._entities = {}
        self._proxy = None
    
    def _whereimChanged(self, name, icon_url):
        logging.debug("whereimChanged: %s %s" % (name, icon_url))
        if self._whereim is None:
            self._whereim = {}
            self._network = {}
        attrs = {'name': name, 'icon_url': icon_url}
        if not self._whereim.has_key(name):
            self._whereim[name] = ExternalAccount(**attrs)
            self.emit('whereim-added', self._whereim[name])     
        else:
            self._whereim[name].update(**attrs)
        
    def _entityChanged(self, attrs):
        logging.debug("entityChanged: %s" % (attrs,))
        if self._network is None:
            self._network = {}

        ## python keywords have to be byte arrays (binary strings) not unicode strings
        ## also over dbus we name the keys in the dict with hyphen, and the keywords
        ## are with underscore
        kwattrs = {}
        for k in attrs.keys():
            kwattrs[str(k).replace('-','_')] = attrs[k]
        
        guid = kwattrs['guid']
        if not self._entities.has_key(guid):
            self._entities[guid] = Entity(**kwattrs)
            self.emit("entity-added", self._entities[guid])
        else:
            self._entities.update(**kwattrs)
    
    def _get_proxy(self):
        if self._proxy is None:
            bus = dbus.SessionBus()
            self._proxy = bus.get_object('org.mugshot.Mugshot', '/org/mugshot/Mugshot')
            self._proxy.connect_to_signal('WhereimChanged', self._whereimChanged)
            self._proxy.connect_to_signal('EntityChanged', self._entityChanged)
        return self._proxy
    
    def get_entity(self, guid):
        return self._entites[guid]
    
    def _on_dbus_error(self, err):
        # TODO - could schedule a "reboot" of this class here to reload
        # information
        logging.error("D-BUS error: %s" % (err,))
    
    def _on_get_self(self, myself):
        logging.debug("self changed: %s" % (myself,))
        self._self = Entity(guid=myself['guid'], name=myself['name'], home_url=myself['home-url'], photo_url=myself['photo-url'])
        self.emit("self-changed", self._self)
    
    def _on_get_baseprops(self, props):
        self._baseprops = {}
        for k,v in props.items():
            self._baseprops[str(k)] = v
        self.emit("initialized")
    
    def _get_baseprop(self, name):
        if self._baseprops is None:
            proxy = self._get_proxy()
            proxy.GetBaseProperties(reply_handler=self._on_get_baseprops, error_handler=self._on_dbus_error)
            return None
        return self._baseprops['baseurl']
    
    def get_baseurl(self):
        return self._get_baseprop('baseurl')
    
    def get_self(self):
        if self._self is None:
            proxy = self._get_proxy()
            proxy.GetSelf(reply_handler=self._on_get_self, error_handler=self._on_dbus_error)
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
    
mugshot_inst = None
def get_mugshot():
    global mugshot_inst
    if mugshot_inst is None:
        mugshot_inst = Mugshot(42)
    return mugshot_inst
