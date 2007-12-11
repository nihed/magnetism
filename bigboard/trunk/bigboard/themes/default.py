import sys

import hippo, cairo

from bigboard.libbig.singletonmixin import Singleton

class DefaultTheme(Singleton):
    def __init__(self):
        super(DefaultTheme, self).__init__()
        self.background = 0xFFFFFFFF
        self.prelight = 0xE2E2E2FF
        self.foreground = 0x000000FF
        self.subforeground = 0x666666FF
        
        self.header_top = self._rgba_to_cairo(self.foreground)
        self.header_start = self._rgb_to_cairo(0xC7C7C7)        
        self.header_end = self._rgb_to_cairo(0xF4F4F4)
        self.header_bottom = self._rgba_to_cairo(self.foreground)
    
    def _rgba_to_cairo(self, color):
        return map(lambda c: c/255.0,
                   ((color & 0xFF000000) >> 24,
                    (color & 0x00FF0000) >> 16,
                    (color & 0x0000FF00) >> 8,                                        
                    (color & 0x000000FF) >> 0))
        
    def _rgb_to_cairo(self, color):
        return map(lambda c: c/255.0,
                   ((color & 0x00FF0000) >> 16,
                    (color & 0x0000FF00) >> 8,                                        
                    (color & 0x000000FF) >> 0))        
        
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
        
    def set_properties(self, widget):
        if isinstance(widget, hippo.CanvasText) or \
            isinstance(widget, hippo.CanvasLink):
            hints = widget.get_theme_hints()
            if 'subforeground' in hints:
                widget.set_properties(color=self.subforeground)
            else:
                widget.set_properties(color=self.foreground)
        
def getInstance():
    return DefaultTheme.getInstance()