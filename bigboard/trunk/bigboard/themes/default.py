import sys

import hippo, cairo, pangocairo, pango

from bigboard.libbig.singletonmixin import Singleton
from bigboard.big_widgets import ThemedWidgetMixin

class DefaultTheme(Singleton):
    def __init__(self):
        super(DefaultTheme, self).__init__()
        self.background = 0xFFFFFFFF
        self.prelight = 0xE2E2E2FF
        self.foreground = 0x000000FF
        self.subforeground = 0x666666FF
        
        self.header_fg = self._rgba_to_cairo(self.foreground)
        self.header_top = self._rgb_to_cairo(0x9EA3A5)
        self.header_top2 = self._rgb_to_cairo(0xFFFFFF)
        self.header_start = self._rgb_to_cairo(0xdEE6EB)        
        self.header_end = self._rgb_to_cairo(0xFFFFFF)
        self.more_1 = self._rgb_to_cairo(0xFFFFFF)
        self.more_2 = self._rgba_to_cairo(0xBBBFC299)
    
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
        cr.set_source_rgba(*self.header_top2)
        cr.rectangle(area.x, area.y+1, area.width, 1)
        cr.fill()        
        gradient_y_start = area.y+2
        gradient_y_height = gradient_y_start+area.height-1
        pat = cairo.LinearGradient(area.x, gradient_y_start,
                                   area.x, gradient_y_height)
        pat.add_color_stop_rgb(0.0, *self.header_start)
        pat.add_color_stop_rgb(1.0, *self.header_end)
        cr.set_source(pat)
        cr.rectangle(area.x, gradient_y_start, area.width, gradient_y_height)
        cr.fill()
        
    def draw_more_button(self, cr, area):
        y = area.y+2
        height = area.height-2
        cr.set_source_rgb(*self.more_1)
        cr.rectangle(area.x, y, 1, height)
        cr.fill()
        cr.set_source_rgba(*self.more_2)
        cr.rectangle(area.x+1, area.y, 1, area.height)
        cr.fill()
        cr.set_source_rgb(0.0, 0.0, 0.0)
        cr.translate(4, 0)
        ctx = pangocairo.CairoContext(cr)
        layout = ctx.create_layout()
        layout.set_text('More')
        desc = pango.FontDescription('Sans 12')
        layout.set_font_description(desc)
        cr.set_source_rgba(*self.header_fg)
        ctx.update_layout(layout)
        ctx.show_layout(layout)
        
    def set_properties(self, widget):
        if isinstance(widget, ThemedWidgetMixin) \
           and isinstance(widget, hippo.CanvasText):
            hints = widget.get_theme_hints()
            if 'subforeground' in hints:
                widget.set_properties(color=self.subforeground)
            else:
                widget.set_properties(color=self.foreground)
        
def getInstance():
    return DefaultTheme.getInstance()