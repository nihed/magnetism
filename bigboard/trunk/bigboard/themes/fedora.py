
import gtk

from bigboard.themes.default import DefaultTheme

class FedoraTheme(DefaultTheme):
    def __init__(self):
        super(DefaultTheme, self).__init__()
        self.background = 0x345B75FF
        self.foreground = 0xFFFFFFFF
        
    def draw_header(self, cr, area):
        cr.set_source_rgb(1.0, 1.0, 1.0)
        cr.rectangle(area.x, area.y, area.width, area.height)
        cr.fill()
        
def getInstance():
    return FedoraTheme.getInstance()