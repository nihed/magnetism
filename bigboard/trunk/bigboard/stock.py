import os, logging, xml.dom.minidom

import gobject

import hippo

from ddm import DataModel
import big_widgets
import libbig
from libbig.singletonmixin import Singleton
from bigboard.libbig.gutil import *
from bigboard.libbig.logutil import log_except
import bigboard.globals as globals

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
    """An abstract class for stocks which use Mugshot.."""
    def __init__(self, *args, **kwargs):
        super(AbstractMugshotStock, self).__init__(*args, **kwargs)

        self._model = DataModel(globals.server_name)

        # There is a minor danger of calling on_ready twice if we start up and then get a
        # ready notification before going idle; this could be protected with a flag variable
        self._model.add_ready_handler(self._on_ready)
        if self._model.ready:
            call_idle(self.__invoke_on_ready)

        self.__cursize = None
        self.__box = hippo.CanvasBox()

    def __sync_content(self):
        self.__box.remove_all()        
        if self._model.self_resource:
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
         
    @log_except(_logger)
    def __invoke_on_ready(self):
        if self._model.ready:
            self._on_ready()

    def _on_ready(self):
        """Should be overridden by subclasses to handle the state where we
        have connected to the data model (or tried to connected an failed."""
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
