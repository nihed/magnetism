import logging, os

import gobject, gtk, gnomeapplet, gconf

import hippo

import bigboard.mugshot as mugshot
import bigboard.libbig as libbig
import bigboard.deskbar_embed as deskbar_embed
from bigboard.stock import Stock
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasVBox
        
class SearchStock(Stock):
    """Search.  It's what's for dinner."""
    def __init__(self, *args, **kwargs):
        super(SearchStock,self).__init__(*args, **kwargs)
        
        self.__box = hippo.CanvasBox()
        
        self.__deskbar = deskbar_embed.Deskbar()
        self.__widget = hippo.CanvasWidget(widget=self.__deskbar)
        self.__box.append(self.__widget)
        self.__empty_box = CanvasVBox()

    def focus(self):
        self.__deskbar.get_deskbar().on_keybinding_button_press(None, gtk.get_current_event_time())
        
    def get_content(self, size):
        return size == self.SIZE_BULL and self.__box or self.__empty_box

