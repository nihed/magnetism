import logging, os

import gobject, gtk, gnomeapplet

try:
    import deskbar, deskbar.DeskbarApplet
    deskbar_available = True    
except:
    deskbar_available = False

import hippo

import mugshot
import libbig
from bigboard import AbstractMugshotStock
from big_widgets import CanvasMugshotURLImage, CanvasEntry
        
class SearchStock(AbstractMugshotStock):
    """Search.  It's what's for dinner."""
    def __init__(self, *args, **kwargs):
        super(SearchStock,self).__init__(*args, **kwargs)
        
        self.__box = hippo.CanvasBox()
        
        if not deskbar_available:
            self.__box.append(hippo.CanvasText(text="Deskbar not installed"))
            return
        
        self.__vbox = gtk.VBox()
        self.__widget = hippo.CanvasWidget(widget=self.__vbox)
        
        self.__applet = gnomeapplet.Applet()
        self.__applet.get_orient = lambda: gnomeapplet.ORIENT_DOWN
        deskbar.DeskbarApplet.DeskbarApplet(self.__applet)        
        self.__applet.reparent(self.__vbox)
        
        self.__box.append(self.__widget)
        
    def _on_mugshot_initialized(self):
        super(SearchStock, self)._on_mugshot_initialized()
        
    def get_content(self, size):
        return self.__box
    
    def set_size(self, size):
        super(SearchStock, self).set_size(size)
        
    def __queue_search(self):
        text = self.__input.get_property("text")
        self._logger.debug("initiating query for %s" % (text,))
        self.__deskbar.query(text,
                             self.__handle_result)
        
    def __handle_result(self, qstring, matches):
        self._logger.debug("got matches for %s: %s", qstring, matches)

