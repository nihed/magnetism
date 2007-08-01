import logging, os

import gobject, gtk, gnomeapplet, gconf

import hippo

import bigboard.global_mugshot as global_mugshot
import bigboard.libbig as libbig
import bigboard.deskbar_embed as deskbar_embed
from bigboard.stock import Stock
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasVBox
        
class SearchStock(Stock):
    """Search.  It's what's for dinner."""
    __gsignals__ = {
        "match-selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, [])
    }    
    def __init__(self, *args, **kwargs):
        super(SearchStock,self).__init__(*args, **kwargs)
        
        self.__box = hippo.CanvasBox()
        
        self.__deskbar = deskbar_embed.Deskbar()
        self.__deskbar.connect('match-selected', lambda d: self.emit('match-selected'))
        self.__widget = hippo.CanvasWidget(widget=self.__deskbar)
        self.__box.append(self.__widget)
        self.__empty_box = CanvasVBox()

    def get_content(self, size):
        return size == self.SIZE_BULL and self.__box or self.__empty_box

    def focus(self):
        self.__deskbar.focus()
