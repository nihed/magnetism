import logging, inspect

import gobject, dbus

import libbig

class ExternalAccount:
    def __init__(self, name, icon_url):        
        self._name = name
        self._icon_url = icon_url
        
    def get_name(self):
        return self._name
    
    def get_icon_url(self):
        return self._icon_url
    
class Entity(libbig.AutoSignallingStruct):
    pass

class Mugshot(gobject.GObject):
    __gsignals__ = {
        "self-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "whereim-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "entity-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
        }    
    
    def __init__(self, issingleton):
        gobject.GObject.__init__(self)
        if not issingleton == 42:
            raise Exception("use mugshot.get_mugshot()")       
        self._whereim = None
        self._self = None
        self._network = None
        self._entities = {}
        self._mugshot = None
    
    def _whereimChanged(self, name, icon_url):
        logging.debug("whereimChanged: %s" % (inspect.getargvalues(inspect.currentframe())[3],))
        acct = ExternalAccount(name, icon_url)
        if self._whereim is None:
            self._whereim = {}
            self._network = {}
        self._whereim[name] = acct
        self.emit('whereim-changed', acct)
        
    def _entityChanged(self, attrs):
        logging.debug("entityChanged: %s" % (inspect.getargvalues(inspect.currentframe())[3],))
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
    
    def _get_mugshot(self):
        if self._mugshot is None:
            bus = dbus.SessionBus()
            self._mugshot = bus.get_object('org.mugshot.Mugshot', '/org/mugshot/Mugshot')
            self._mugshot.connect_to_signal('WhereimChanged', self._whereimChanged)
            self._mugshot.connect_to_signal('EntityChanged', self._entityChanged)
        return self._mugshot
    
    def get_entity(self, guid):
        return self._entites[guid]
    
    def _on_dbus_error(self, err):
        logging.error("D-BUS error: %s" % (err,))
    
    def _on_get_self(self, myself):
        logging.debug("self changed: %s" % (myself,))
        self._self = Entity(guid=myself['guid'], name=myself['name'], home_url=myself['home-url'], photo_url=myself['photo-url'])
        self.emit("self-changed", self._self)
    
    def get_self(self):
        if self._self is None:
            mugshot = self._get_mugshot()
            mugshot.GetSelf(reply_handler=self._on_get_self, error_handler=self._on_dbus_error)
            return None
        return self._self
    
    def get_whereim(self):
        if (self._whereim is None):
            proxy = self._get_mugshot()
            proxy.NotifyAllWhereim()
            return None
        return self._whereim.values()
    
    def get_network(self):
        if self._network is None:
            proxy = self._get_mugshot()
            proxy.NotifyAllNetwork()
            return None
        return self._network.values()
    
mugshot_inst = None
def get_mugshot():
    global mugshot_inst
    if mugshot_inst is None:
        mugshot_inst = Mugshot(42)
    return mugshot_inst
