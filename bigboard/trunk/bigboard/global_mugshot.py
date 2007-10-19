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

class ExternalAccount(AutoSignallingStruct):
    pass

class ExternalAccountThumbnail(AutoStruct):
    pass
    
class Entity(AutoSignallingStruct):
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

class Mugshot(gobject.GObject):
    """A combination of a wrapper and cache for the Mugshot D-BUS API.  Access
    using the get_mugshot() module method."""
    __gsignals__ = {
        "initialized" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "connection-status": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT, gobject.TYPE_PYOBJECT, gobject.TYPE_PYOBJECT)),
        "self-known" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "network-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "pref-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT, gobject.TYPE_PYOBJECT))
        }    
    
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
        self.__create_im_proxy()

        self.__reset()        
        
        self.__iqcachedir = os.path.expanduser("~/.bigboard/iqcache")
        try:
            os.makedirs(self.__iqcachedir)
        except OSError, e:
            pass        
        
    def __reset(self):
        self._logger.debug("reset")  
        # Generic properties
        self.__baseprops = None
        self.__connection_status = (None, None, None)

        self.__self_proxy = None
        self.__self = None
        self.__self_path = None
        self.__prefs = {}
        self.__network = None
        self.__entities = {} # <str>,<Entity>

        self.__endpoint_id = None
        
        self.__external_iqs = {} # <int>,<function>

    def get_im_proxy(self):
        return self.__im_proxy
        
    def __create_proxy(self):
         try:        
             bus = dbus.SessionBus()
             self._logger.debug("creating proxy for %s" % globals.bus_name)
             self.__proxy = bus.get_object(globals.bus_name, '/org/mugshot/Mugshot')
             self.__proxy.connect_to_signal('ConnectionStatusChanged', self.__on_connection_status_changed)
             self.__proxy.connect_to_signal('PrefChanged', self.__on_pref_changed)            
             self.__proxy.connect_to_signal('ExternalIQReturn', self.__externalIQReturn)
             self.__get_connection_status()    
             self.__proxy.GetBaseProperties(reply_handler=self.__on_get_baseprops, error_handler=self.__on_dbus_error)
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

    def __create_im_proxy(self):
         try:
             bus = dbus.SessionBus()
             self.__im_proxy = bus.get_object(globals.bus_name, '/org/freedesktop/od/im')
         except dbus.DBusException:
            self.__im_proxy = None
        
    @log_except(_logger)
    def __on_dbus_name_owner_changed(self, name, prev_owner, new_owner):
        if name == globals.bus_name:
            if new_owner != '':
                self._logger.debug("owner for %s changed, recreating proxies", globals.bus_name)
                self.__create_proxy()
                self.__create_ws_proxy()
                self.__create_im_proxy()
            else:
                self.__proxy = None
                self.__ws_proxy = None
                self.__im_proxy = None

    @log_except(_logger)
    def __on_register_endpoint(self, id):
        self.__endpoint_id = id

    @log_except(_logger)
    def __on_connection_status(self, has_auth, connected, contacts):
        self._logger.debug("connection status auth=%s connected=%s contacts=%s" % (has_auth, connected, contacts))
        self.__connection_status = (has_auth, connected, contacts)
        self.emit("connection-status", has_auth, connected, contacts)         

    def __get_connection_status(self):
        self.__proxy.GetConnectionStatus(reply_handler=self.__on_connection_status,
                                         error_handler=self.__on_dbus_error)

    @log_except(_logger)
    def __on_connection_status_changed(self):
        self._logger.debug("connection status changed")        
        self.__get_connection_status()
        
    def current_connection_status(self):
        return self.__connection_status
    
    def get_pref(self, key):
        if self.__prefs.has_key(key):
            return self.__prefs[key]
        return None
        
    @log_except(_logger)
    def __on_pref_changed(self, key, value):
        self._logger.debug("pref %s changed: %s", key, value)  
        changed = False
        if not self.__prefs.has_key(key):
            changed = True            
            self.__prefs[key] = value
        elif self.__prefs[key] != value:
            changed = True
            self.__prefs[key] = value
        if changed:
            self.emit("pref-changed", key, value)
            
    @log_except(_logger)
    def __externalIQReturn(self, id, content):
        if self.__external_iqs.has_key(id):
            #self._logger.debug("got external IQ reply for %d (%d outstanding)", id, len(self.__external_iqs.keys())-1)
            (cb, iqkey) = self.__external_iqs[id]
            if iqkey:
                iqfile = os.path.join(self.__iqcachedir, iqkey)
                open(iqfile, 'w').write(content)
            cb(content)            
            del self.__external_iqs[id]
    
    def get_entity(self, guid):
        return self.__entites[guid]
    
    @log_except(_logger)
    def __on_dbus_error(self, err):
        # TODO - could schedule a "reboot" of this class here to reload
        # information
        self._logger.error("D-BUS error: %s", err)
    
    @log_except(_logger)
    def __on_self_changed(self):
        self.__self_proxy.GetProperties(reply_handler=self.__on_get_self_properties,
                                        error_handler=self.__on_dbus_error)        
    
    @log_except(_logger)
    def __on_get_self(self, myself_path):
        self._logger.debug("got self path: %s" % (myself_path,))
        self.__self_proxy = dbus.SessionBus().get_object(globals.bus_name, myself_path)
        self.__on_self_changed()
        self.__self_proxy.connect_to_signal("Changed", 
                                            self.__on_self_changed, 
                                            'org.mugshot.Mugshot.Entity')
        
    @log_except(_logger)
    def __on_get_self_properties(self, myself):
        self._logger.debug("self properties: %s" % (myself,))
        if self.__self:
            self.__self.update(myself)
        else:
            self.__self = Person(myself)
            self._logger.debug("emitting self known")
            self.emit("self-known")  
    
    @log_except(_logger)
    def __on_get_baseprops(self, props):
        self.__baseprops = {}
        for k,v in props.items():
            self.__baseprops[str(k)] = v
        self.emit("initialized")
    
    def __get_baseprop(self, name):
        return self.__baseprops and self.__baseprops[name] or None
    
    def get_baseurl(self):
        return globals.get_baseurl()
    
    def get_initialized(self):
        return True
    
    def get_self(self):
        if self.__self is None:
            self.__proxy.GetSelf(reply_handler=self.__on_get_self, error_handler=self.__on_dbus_error)
            return None
        return self.__self
    
    @log_except(_logger)
    def __on_get_network_entity_props(self, proxy, attrs, connect=False):
        self._logger.debug("entity properties: %s", attrs)
        
        guid = attrs[u'guid']
        if not self.__network.has_key(guid):
            entity_type = attrs['type']
            entity_class = {'person': Person, 'group': Group, 
                            'resource': Resource, 'feed': Feed}
            self.__network[guid] = entity_class[attrs['type']](attrs)
            self.emit("network-changed")
        else:
            self.__network[guid].update(attrs)        
        if connect:
            proxy.connect_to_signal("Changed", 
                                    functools.partial(self.__on_get_network_entity_props, None),
                                    'org.mugshot.Mugshot.Entity')
        
    
    @log_except(_logger)
    def __on_get_network(self, opaths):
        self._logger.debug("got network reply %s", opaths)
        self.__network = {}
        for opath in opaths:
            proxy = dbus.SessionBus().get_object(globals.bus_name, opath)
            proxy.GetProperties(reply_handler=functools.partial(self.__on_get_network_entity_props, proxy, connect=True),
                                error_handler=self.__on_dbus_error)              
    
    def get_network(self):
        if self.__network is None:
            self.__proxy.GetNetwork(reply_handler=self.__on_get_network, error_handler=self.__on_dbus_error)
            return None
        return self.__network.itervalues()

    def get_cookies(self, url):
        cookies = self.__ws_proxy.GetCookiesToSend(url)
            
        #print cookies
        return cookies

    def __iq_key(self, name, xmlns, attrs):
        return sha.new(name+xmlns+repr(attrs)).hexdigest()

    def __do_external_iq(self, name, xmlns, cb, attrs=None, content="", is_set=False):
        """Sends a raw IQ request to Mugshot server, indirecting
        via D-BUS to client."""
        if not is_set:
            # Check the cache
            iqkey = self.__iq_key(name, xmlns, attrs)
            iqfile = os.path.join(self.__iqcachedir, iqkey)
            if os.access(iqfile, os.R_OK):
                cachedata = open(iqfile).read()
                gobject.idle_add(log_except(_logger)(cb), cachedata)
        else:
            iqkey = None
        gobject.idle_add(log_except(_logger)(functools.partial(self.__do_external_iq_uncached, iqkey, name, xmlns, cb, attrs=attrs, content=content, is_set=is_set)))
    
    def __do_external_iq_uncached(self, iqkey, name, xmlns, cb, attrs=None, content="", is_set=False):
        if self.__proxy is None:
            self._logger.warn("No Mugshot active, not sending IQ")
            return
        #self._logger.debug("sending external IQ request: set=%s name=%s xmlns=%s attrs=%s (%d bytes)", is_set, name, xmlns, attrs, len(content))
        if attrs is None:
            attrs = {}
        attrs['xmlns'] = xmlns
        flattened_attrs = []
        for k,v in attrs.iteritems():
            flattened_attrs.append(k)
            flattened_attrs.append(v)
        id = self.__proxy.SendExternalIQ(is_set, name, flattened_attrs, content)
        self.__external_iqs[id] = (cb, iqkey)
        
    def __on_get_person_accounts(self, person, xml_str):
        doc = xml.dom.minidom.parseString(xml_str) 
        accts = []
        for child in xml_query(doc.documentElement, 'externalAccount*'):
            attrs = xml_get_attrs(child, ['type', 
                                          'sentiment', 
                                          'icon'])
            accttype = attrs['type']
            if attrs['sentiment'] == 'love':
                attrs['link'] = child.getAttribute('link')
            thumbnails = []            
            try:
                thumbnails_node = xml_query(child, 'thumbnails')
            except KeyError:
                thumbnails_node = None
            if thumbnails_node:
                for thumbnail in xml_query(thumbnails_node, 'thumbnail*'):
                    subattrs = xml_get_attrs(thumbnail, ['src', ('title', True), 'href'])
                    thumbnails.append(ExternalAccountThumbnail(subattrs)) 
                #self._logger.debug("%d thumbnails found for account %s (user %s)" % (len(thumbnails), accttype, person)) 
                attrs['thumbnails'] = thumbnails
            feeds = []
            try:
                feeds_node = xml_query(child, 'feeds')
            except KeyError:
                feeds_node = None
            if feeds_node:
                for feed in xml_query(feeds_node, 'feed*'):
                    feeds.append(feed.getAttribute('src'))   
                attrs['feeds'] = feeds          
            acct = ExternalAccount(attrs)
            accts.append(acct)
        #self._logger.debug("setting %d accounts for user %s" % (len(accts), person))
        person.set_external_accounts(accts)
        
    def get_person_accounts(self, person):
        self.__do_external_iq("whereim", "http://dumbhippo.com/protocol/whereim",
                              lambda node: self.__on_get_person_accounts(person, node),
                              attrs={'who': person.get_guid()})
    
    def install_application(self, id, package_names, desktop_names):
        self._logger.debug("requesting install of app id %s", id)
        self.__mugshot_dbus_proxy.InstallApplication(self.__endpoint_id, id, package_names, desktop_names)


mugshot_inst = None
def get_mugshot():
    global mugshot_inst
    if mugshot_inst is None:
        mugshot_inst = Mugshot(42)
    return mugshot_inst
