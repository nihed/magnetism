import os,sys

import gtk,cairo

from bigboard.themes.default import DefaultTheme

class FedoraTheme(DefaultTheme):
    def __init__(self):
        super(FedoraTheme, self).__init__()
        self.background = 0x345B75FF  
        self.foreground = 0xFFFFFFFF
        self.header_fg = self._rgba_to_cairo(self.foreground)
        self.subforeground = 0x95A9B6FF
        self.header_top    = self._rgba_to_cairo(0xCBD5DCFF)        
        self.header_start  = self._rgb_to_cairo(0x436A85)
        self.header_end    = self._rgb_to_cairo(0x59809C)
        self.header_bottom = self._rgba_to_cairo(0x244155FF)
        self.prelight = 0x59809CFF
        self.more_1 = self._rgb_to_cairo(0x496D87)
        self.more_2 = self._rgba_to_cairo(0xA9BCCA99)
        self.more_start = self._rgb_to_cairo(0x66859C)
        self.more_end = self._rgb_to_cairo(0x8BA6BA)
        
    def draw_header(self, cr, area):
        cr.set_source_rgba(*self.header_top)
        cr.rectangle(area.x, area.y, area.width, 1)
        cr.fill()
        gradient_y_start = area.y+1
        gradient_y_height = gradient_y_start+area.height-1
        pat = cairo.LinearGradient(area.x, gradient_y_start,
                                   area.x, gradient_y_height)
        pat.add_color_stop_rgb(0.0, *self.header_start)
        pat.add_color_stop_rgb(1.0, *self.header_end)
        cr.set_source(pat)
        cr.rectangle(area.x, gradient_y_start, area.width, gradient_y_height)
        cr.fill()
        cr.set_source_rgba(*self.header_bottom)
        cr.rectangle(area.x, gradient_y_height, area.width, 1)
        cr.fill()
        
    def draw_more_button(self, cr, area):
        gradient_y_start = area.y+1
        gradient_y_height = gradient_y_start+area.height-2
        pat = cairo.LinearGradient(area.x, gradient_y_start,
                                   area.x, gradient_y_height)
        pat.add_color_stop_rgb(0.0, *self.more_start)
        pat.add_color_stop_rgb(1.0, *self.more_end)
        cr.set_source(pat)
        cr.rectangle(area.x, gradient_y_start, area.width, gradient_y_height)
        cr.fill()
        super(FedoraTheme, self).draw_more_button(cr, area)
        
def getInstance():
    return FedoraTheme.getInstance()