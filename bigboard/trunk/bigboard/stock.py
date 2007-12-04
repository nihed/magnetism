import os, logging, xml.dom.minidom

import gobject

import hippo

import big_widgets
import global_mugshot
import libbig
from libbig.singletonmixin import Singleton
from bigboard.libbig.gutil import *
from bigboard.libbig.logutil import log_except

## FIXME remove these from the Stock class ... I can't figure out how to
## refer to them from outside a Stock instance with them there, anyway
SIZE_BEAR = 1
SIZE_BULL = 2
SIZE_BULL_CONTENT_PX = 200
SIZE_BEAR_CONTENT_PX = 36

_logger = logging.getLogger("bigboard.Stock")

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
    
    def __init__(self, metainfo, title=None, panel=None):
        super(Stock, self).__init__()
        self._metainfo = metainfo
        self._id = metainfo.srcurl
        self._ticker = title
        self._panel = panel
        self._bull_widgets = {}
        self._size = Stock.SIZE_BULL
        
        self.__more_button_cb = None
        
        # For use in subclasses as well
        self._logger = logging.getLogger('bigboard.stocks.' + self._id)  
        _logger.debug("initializing")

    def on_delisted(self):
        """Called when stock is shut down (removed)"""
        _logger.debug("on_delisted stock %s" % (self._id))
        self._on_delisted() ## saves derived classes the need to chain up

    def _on_delisted(self):
        """The most-derived concrete stock object can override this instead of on_delisted, then not chain up"""
        pass

    def on_popped_out_changed(self, popped_out):
        """Called when the sidebar is shown or hidden"""
        pass
        
    def get_id(self):
        return self._id
    
    def get_metainfo(self):
        return self._metainfo
    
    def get_ticker(self):
        return self._ticker

    def _add_more_button(self, cb):
        self.__more_button_cb = cb
        
    def has_more_button(self):
        return bool(self.__more_button_cb)
    
    def on_more_clicked(self):
        assert(self.__more_button_cb)
        self._panel.action_taken()
        self.__more_button_cb()

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
    method on this class is connect_mugshot_handler. This is deprecated since the
    old mugshot API in mugshot.py should no longer be used, use the data model."""
    def __init__(self, *args, **kwargs):
        super(AbstractMugshotStock, self).__init__(*args, **kwargs)
        self._auth = False
        self.__have_contacts = False        
        self._mugshot_initialized = False
        self._dependent_handlers = []
        
        self._mugshot = global_mugshot.get_mugshot()
        self.__connections = DisconnectSet()

        id = self._mugshot.connect("initialized", lambda mugshot: self._on_mugshot_initialized())
        self.__connections.add(self._mugshot, id)
        if self._mugshot.get_initialized():
            call_idle(self.__invoke_mugshot_initialized)

        id = self._mugshot.connect("connection-status", lambda mugshot, auth, xmpp, contacts: self.__handle_mugshot_connection_status(auth, xmpp, contacts))
        self.__connections.add(self._mugshot, id)        

        call_idle(self.__handle_mugshot_connection_status, *self._mugshot.current_connection_status())  
        
        self.__cursize = None
        self.__box = hippo.CanvasBox()

    def on_delisted(self):
        self.__connections.disconnect_all()

        super(AbstractMugshotStock, self).on_delisted()

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
        
    @log_except(_logger)
    def __invoke_mugshot_initialized(self):
        self._on_mugshot_initialized()        
        
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

    @log_except(_logger)
    def __handle_mugshot_connection_status(self, auth, xmpp, contacts):
        if auth != self._auth:
            _logger.debug("emitting visibility: %s", auth)
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
