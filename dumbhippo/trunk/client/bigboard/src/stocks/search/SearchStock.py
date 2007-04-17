import logging, os

import gobject, gtk, gnomeapplet, gconf

try:
    import deskbar, deskbar.DeskbarApplet
    deskbar_available = True    
except:
    deskbar_available = False

import hippo

import mugshot, libbig
from bigboard import Stock
from big_widgets import CanvasMugshotURLImage, CanvasEntry, CanvasVBox
        
class SearchStock(Stock):
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
        self.__deskbar = deskbar.DeskbarApplet.DeskbarApplet(self.__applet)
        self.__deskbar.loader.connect("modules-loaded", self.__override_modules_loaded)
        self.__applet.reparent(self.__vbox)
        
        self.__box.append(self.__widget)
        self.__empty_box = CanvasVBox()
        
    def get_content(self, size):
        return size == self.SIZE_BULL and self.__box or self.__empty_box

    def __override_modules_loaded(self, loader):
        self._logger.debug("got modules loaded")        
        gobject.idle_add(self.__idle_override_modules_loaded)

    def __idle_override_modules_loaded(self):
        self._logger.debug("idle override modules")
        enabled_handlers = gconf.Value(gconf.VALUE_LIST)
        def make_str_val(s):
            v = gconf.Value(gconf.VALUE_STRING)
            v.set_string(s)
            return v
        enabled_handlers.set_list_type(gconf.VALUE_STRING)
        enabled_handlers.set_list(map(make_str_val, ['ProgramsHandler', 'MozillaBookmarksHandler', 'YahooHandler']))
        self.__deskbar.on_config_handlers(enabled_handlers)  
        self._logger.debug("idle override modules complete")

       