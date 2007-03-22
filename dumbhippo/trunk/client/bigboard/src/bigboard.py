import os, logging, xml.dom.minidom

import gobject

import hippo

from singletonmixin import Singleton
import big_widgets, mugshot, libbig

class State(Singleton):
    """Extremely simple state saving mechanism.  Intended to be
    used for stocks to store simple options, as well as non-preference
    state.  For now, machine-local semantics may be assumed.  
    
    Only unicode strings, numbers, and booleans may be stored."""
    def __init__(self):
        self._logger = logging.getLogger("bigboard.State")
        self.__path = libbig.get_bigboard_config_file("state.xml")
        self.__defaults = {}
        self.__state = {}
        self.__idle_save_id = 0
        self.__type_name = libbig.BiMap('pytype', 'name', 
                                        {unicode: "string", 
                                         int: "number",
                                         long: "number",
                                         bool: "boolean"})
        self.__type_read = {bool: lambda val: val.lower() == 'true'}
        self.__type_write = {}
        self.__read()
        
    def __validate_key(self, key):
        if isinstance(key, str):
            key = unicode(key)
        if not isinstance(key, unicode):
            raise ValueError("Keys must be unicode")
        return key        
        
    def set_default(self, key, value):
        key = self.__validate_key(key)
        self._logger.debug("set default of %s: %s", key, value)
        self.__defaults[key] = value
        
    def __getitem__(self, key):
        key = self.__validate_key(key)
        try:
            val = self.__state[key]
        except KeyError:
            val = self.__defaults[key]
        self._logger.debug("lookup of %s: %s", key, val)
        return val 

    def __setitem__(self, key, value):
        self._logger.debug("set %s: %s", key, value)
        key = self.__validate_key(key)
        if not key.startswith("/"):
            raise ValueError("Keys must begin with /")
        if not self.__type_name['pytype'].has_key(type(value)):
            raise ValueError("Can't store value of type '%s'", type(value))
        self.__state[key] = value
        self.__queue_save()
        
    def __read(self):
        self._logger.info("reading state file %s", self.__path)
        if not os.access(self.__path, os.R_OK):
            self._logger.info("no state file %s", self.__path)            
            return
        try:
            f = file(self.__path, 'r')
            doc = xml.dom.minidom.parse(f)
            for child in doc.documentElement.childNodes:
                if not (child.nodeType == xml.dom.Node.ELEMENT_NODE and child.tagName == 'key'):
                    continue
                name = child.getAttribute("name")
                type_str = child.getAttribute("type")
                type_val = self.__type_name['name'][type_str]
                valnode = child.firstChild
                if not valnode.nodeType == xml.dom.Node.TEXT_NODE:
                    continue
                value = valnode.nodeValue
                if self.__type_read.has_key(type_val):
                    parsed_val = self.__type_read[type_val](value)
                else:
                    parsed_val = unicode(value)
                self._logger.debug("read value %s %s '%s'", name, type_val, parsed_val)
                self.__state[unicode(name)] = parsed_val
        except:
            self._logger.exception("Failed to read state")
        
    def __queue_save(self):
        if self.__idle_save_id == 0:
            self.__idle_save_id = gobject.timeout_add(3000, self.__idle_save)
        
    def __idle_save(self):
        # TODO: if we're feeling ambitious, do this save in a separate thread to not block
        # ui.  Note you must handle locking on self.__state, maybe just make a copy
        self._logger.info("in idle state save")
        doc = xml.dom.minidom.getDOMImplementation().createDocument(None, "bigboard-state", None)
        doc.documentElement.setAttribute("version", "0")
        for k,v in self.__state.iteritems():
            elt = doc.createElement("key")
            elt.setAttribute("name", k)
            typename = self.__type_name['pytype'][type(v)]
            elt.setAttribute("type", typename)
            self._logger.debug("saving value %s %s '%s'", k, typename, v)
            if self.__type_write.has_key(type(v)):
                serialized_val = self.__type_write(type(v))[v]
            else:
                serialized_val = unicode(v)
            elt.appendChild(doc.createTextNode(serialized_val))
            doc.documentElement.appendChild(elt)
        tmpf_path = self.__path + ".tmp"
        tmpf = file(tmpf_path, 'w')
        doc.writexml(tmpf)
        tmpf.close()
        os.rename(tmpf_path, self.__path)
        self._logger.info("idle state save complete")
        self.__idle_save_id = 0
        
class PrefixedState(object):
    def __init__(self, prefix):
        self.__prefix = prefix
        self.__state = State.getInstance()

    def set_default(self, key, value):
        return self.__state.set_default(self.__prefix + key, value)
        
    def __getitem__(self, key):
        return self.__state[self.__prefix + key]

    def __setitem__(self, key, value):
        self.__state[self.__prefix + key] = value

class Stock(object):
    """An item that can be placed on the big board.  Has
    two primary properties: id and ticker.  The id is
    an internal unique identifier.  The ticker is the title
    displayed to the user.
    
    Stocks should support being displayed at two sizes:
    SIZE_BEAR and SIZE_BULL.  Pixel values for these are
    not yet fixed, but assume roughly 60 and 200.
    There are two ways your widget can implement size
    changes.  First, the get_content method is called with
    a size parameter.  You can use this to return a
    different widget hierarchy for different sizes.  
    Second, the set_size method will be called; you could
    for example hide or show specific widgets."""
    SIZE_BEAR = 1
    SIZE_BULL = 2

    SIZE_BULL_CONTENT_PX = 200
    SIZE_BEAR_CONTENT_PX = 36
    
    def __init__(self, metainfo):
        self._id = metainfo['id']
        self._ticker = metainfo['ticker']
        self._bull_widgets = {}
        self._size = Stock.SIZE_BULL
        
        self.__more_link_cb = None
        
        # For use in subclasses as well
        self._logger = logging.getLogger('bigboard.stocks.' + self._id)  
        self._state = PrefixedState('/stock/' + self._id)
        
    def get_id(self):
        return self._id
    
    def get_ticker(self):
        return self._ticker

    def _add_more_link(self, cb):
        self.__more_link_cb = cb
        
    def has_more_link(self):
        return bool(self.__more_link_cb)
    
    def on_more_clicked(self):
        assert(self.__more_link_cb)
        self.__more_link_cb()

    def append_bull(self, box, item):
        """Adds item to box, recording that this widget should
        only be displayed in "bull" size."""
        self._bull_widgets[item] = box
        box.append(item)
        
    def set_size(self, size):
        for item, box in self._bull_widgets.items():
            box.set_child_visible(item, size == Stock.SIZE_BULL)

    def get_size(self):
        return self._size

    # get the preferred width of the content at the current size
    def get_content_width(self):
        if self.get_size() == Stock.SIZE_BEAR:
            return Stock.SIZE_BEAR_CONTENT_PX
        else:
            return Stock.SIZE_BULL_CONTENT_PX
    
    def get_content(self, size):
        raise NotImplementedError()

class AbstractMugshotStock(Stock):
    """An abstract class for stocks which use Mugshot.  The most useful
    method on this class is connect_mugshot_handler."""
    def __init__(self, *args, **kwargs):
        super(AbstractMugshotStock, self).__init__(*args, **kwargs)
        self.__auth = False
        self.__have_contacts = False        
        self._mugshot_initialized = False
        self._dependent_handlers = []
        
        self._mugshot = mugshot.get_mugshot()
        self._mugshot.connect("initialized", lambda mugshot: self._on_mugshot_initialized())
        self._mugshot.connect("connection-status", lambda mugshot, auth, xmpp, contacts: self.__handle_mugshot_connection_status(auth, xmpp, contacts))  
        
        self.__cursize = None
        self.__box = hippo.CanvasBox()
        
        self.__signin = big_widgets.ActionLink(text="Sign in to Mugshot")
        self.__signin.connect("button-press-event", lambda signin, event: self.__on_signin_press())
        
    def __sync_content(self):
        self.__box.remove_all()        
        if self.__auth:
            self.__box.append(self.get_authed_content(self.__cursize))
        else:
            self.__box.append(self.__signin)
            
    def get_content(self, size):
        if size == self.__cursize:
            return self.__box
        self.__cursize = size        
        self.__sync_content()
        return self.__box
         
    # protected
    def get_mugshot_initialized(self):
        return self._mugshot_initialized
        
    def _on_mugshot_initialized(self):
        logging.debug("mugshot intialized, hooking up %d handlers", len(self._dependent_handlers))
        self._mugshot_initialized = True
        for object, signal, handler in self._dependent_handlers:
            object.connect(signal, handler)
        self.__check_ready()            
            
    def _on_mugshot_ready(self):
        """Should be overridden by subclasses to handle the state where mugshot
        is initialized and connected."""
        pass
            
    def __handle_mugshot_connection_status(self, auth, xmpp, contacts):
        self.__auth = auth
        self.__sync_content()        
        self.__have_contacts = contacts
        self.__check_ready()
            
    def __check_ready(self):
        if self._mugshot_initialized and self.__have_contacts:
            self._on_mugshot_ready()
        
    # protected
    def connect_mugshot_handler(self, object, signal, handler):
        """Hook up a GObject signal handler only after the Mugshot
        object is initialized.  This is useful if your signal handler
        depends on Mugshot properties such as the base URL."""
        if self.get_mugshot_initialized():
            object.connect(signal, handler)
        else:
            self._dependent_handlers.append((object, signal, handler))
            
    def __on_signin_press(self):
        baseurl = self._mugshot.get_baseurl()
        libbig.show_url(baseurl + "/who-are-you")        
