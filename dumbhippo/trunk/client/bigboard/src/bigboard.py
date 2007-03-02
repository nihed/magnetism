import logging

import mugshot

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
    
    def __init__(self, id, ticker=None):
        self._id = id
        if ticker == None:
            self._ticker = id
        else:
            self._ticker = ticker
        self._bull_widgets = {}
        self._size = Stock.SIZE_BULL
        
    def get_id(self):
        return self._id
    
    def get_ticker(self):
        return self._ticker

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
    def __init__(self, id, ticker=None):
        super(AbstractMugshotStock, self).__init__(id, ticker)
        self._mugshot_initialized = False
        self._dependent_handlers = []
        
        self._mugshot = mugshot.get_mugshot()
        self._mugshot.connect("initialized", lambda mugshot: self._on_mugshot_initialized())
        
    # protected
    def get_mugshot_initialized(self):
        return self._mugshot_initialized
        
    def _on_mugshot_initialized(self):
        logging.debug("mugshot intialized, hooking up %d handlers", len(self._dependent_handlers))
        self._mugshot_initialized = True
        for object, signal, handler in self._dependent_handlers:
            object.connect(signal, handler)
        
    # protected
    def connect_mugshot_handler(self, object, signal, handler):
        """Hook up a GObject signal handler only after the Mugshot
        object is initialized.  This is useful if your signal handler
        depends on Mugshot properties such as the base URL."""
        if self.get_mugshot_initialized():
            object.connect(signal, handler)
        else:
            self._dependent_handlers.append((object, signal, handler))
