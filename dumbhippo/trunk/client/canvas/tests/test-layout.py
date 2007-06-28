import copy
import random

import pygtk
pygtk.require('2.0')
import gobject
import gtk
import hippo
from overview_layout import OverviewLayout

class TestItem(hippo.CanvasBox):
    def __init__(self):
        hippo.CanvasBox.__init__(self, border=1, xalign=hippo.ALIGNMENT_CENTER, yalign=hippo.ALIGNMENT_CENTER, border_color=0x000000ff)
        self.__min_size = random.randint(25, 75)
        self.__natural_size = random.randint(self.__min_size, 100)

    def do_paint_below_children(self, cr, damaged):
        (width, height) = self.get_allocation()

        # subtract off border
        width -= 2 
        height -= 2
        
        x,y,width,height = self.align(min(width, self.__natural_size), min(height, self.__natural_size))
        cr.set_source_rgb(1., 0., 0.)
        cr.rectangle(x,y,width,height)
        cr.fill()

        x,y,width,height = self.align(self.__min_size, self.__min_size)
        cr.set_source_rgb(1., 1., 0.)
        cr.rectangle(x,y,width,height)
        cr.fill()

    def do_get_content_width_request(self):
        return (self.__min_size, self.__natural_size)
        
    def do_get_content_height_request(self, for_width):
        return (self.__min_size, self.__natural_size)

gobject.type_register(TestItem)

######################################################################    
    
def add_test_section(layout, title, n_items):
    header = hippo.CanvasText(text=title, xalign=hippo.ALIGNMENT_START, font="Sans 15")
    layout.add(header, is_header=True)

    for i in range(0, n_items):
        layout.add(TestItem())
                
window = gtk.Window()

canvas = hippo.Canvas()
window.add(canvas)
canvas.show()

box = hippo.CanvasBox(background_color=0xffffffff)
layout = OverviewLayout(column_spacing=10, row_spacing=10)
box.set_layout(layout)
canvas.set_root(box)

add_test_section(layout, "This is the first section", 5)
add_test_section(layout, "This section is the second section", 20)
add_test_section(layout, "And another", 10)

window.set_size_request(100, 100)
window.set_default_size(500, 500)

window.show()

gtk.main()
