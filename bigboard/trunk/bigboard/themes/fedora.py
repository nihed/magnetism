import os,sys

import gtk

from bigboard.themes.default import DefaultTheme

class FedoraTheme(DefaultTheme):
    def __init__(self):
        super(FedoraTheme, self).__init__()
        self.background = 0x345B75FF  
        self.foreground = 0xFFFFFFFF
        self.subforeground = 0x95A9B6FF
        self.header_top    = self._rgba_to_cairo(0xCBD5DCFF)        
        self.header_start  = self._rgb_to_cairo(0x436A85)
        self.header_end    = self._rgb_to_cairo(0x59809C)
        self.header_bottom = self._rgba_to_cairo(0x244155FF)
        self.prelight = 0x59809CFF     
        
def getInstance():
    return FedoraTheme.getInstance()