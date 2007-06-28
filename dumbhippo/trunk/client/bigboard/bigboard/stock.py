import os, logging, xml.dom.minidom

import gobject

import hippo

import big_widgets
import mugshot
import libbig
from libbig.singletonmixin import Singleton

## FIXME remove these from the Stock class ... I can't figure out how to
## refer to them from outside a Stock instance with them there, anyway
SIZE_BEAR = 1
SIZE_BULL = 2
SIZE_BULL_CONTENT_PX = 200
SIZE_BEAR_CONTENT_PX = 36


class Stock(gobject.GObject):
    __gsignals__ = {
        "visible" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_BOOLEAN,))
    }
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
    
    def __init__(self, metainfo, panel=None):
        super(Stock, self).__init__()
        self._id = metainfo['id']
        self._ticker = metainfo['ticker']
        self._panel = panel
        self._bull_widgets = {}
        self._size = Stock.SIZE_BULL
        
        self.__more_link_cb = None
        
        # For use in subclasses as well
        self._logger = logging.getLogger('bigboard.stocks.' + self._id)  
        self._logger.debug("initializing")
        
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
        self._auth = False
        self.__have_contacts = False        
        self._mugshot_initialized = False
        self._dependent_handlers = []
        
        self._mugshot = mugshot.get_mugshot()
        self._mugshot.connect("initialized", lambda mugshot: self._on_mugshot_initialized())
        self._mugshot.connect("connection-status", lambda mugshot, auth, xmpp, contacts: self.__handle_mugshot_connection_status(auth, xmpp, contacts))  
        
        self.__cursize = None
        self.__box = hippo.CanvasBox()

    def __sync_content(self):
        self.__box.remove_all()        
        if self._auth:
            content = self.get_authed_content(self.__cursize)
            if not content:
                return None
            self.__box.append(content)
            return self.__box
        else:
            unauthed = self.get_unauthed_content(self.__cursize)
            if unauthed:
                self.__box.append(unauthed)
                return self.__box
            return None

    def get_unauthed_content(self, size):
        return None 

    def get_content(self, size):
        if size == self.__cursize:
            return self.__box
        self.__cursize = size        
        return self.__sync_content()
         
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
        if auth != self._auth:
            self._logger.debug("emitting visibility: %s", auth)
            self.emit("visible", auth)
        self._auth = auth
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
