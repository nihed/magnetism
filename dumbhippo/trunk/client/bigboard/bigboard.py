#!/usr/bin/python

import gobject
import gtk

import hippo
from libgimmie import EdgeWindow
import libbig

class Thingy:
    def __init__(self, name):
        self.name = name
    
    def get_content(self):
        raise NotImplementedError()
        
class SelfThingy(Thingy):
    DEFAULT_PHOTO_PATH = '/usr/share/pixmaps/nobody.png'
    
    def __init__(self):
        Thingy.__init__(self, "Self")
        self.box = hippo.CanvasBox()
        self.photo = hippo.CanvasImage()          
        self.photo.set_property("image-name", self.DEFAULT_PHOTO_PATH)
        self.box.append(self.photo)
        
        self.name = hippo.CanvasText()
        self.name.set_property("text", "Bigboard")
        self.box.append(self.name)
        
    def get_content(self):
        return self.box
        
thingies = [SelfThingy()]
        
class BigBoardPanel:
    def __init__(self):
        self.dw = EdgeWindow(gtk.gdk.GRAVITY_WEST)

        self.canvas = hippo.Canvas()
        self.dw.get_content().add(self.canvas)
        
        self.main_box = hippo.CanvasBox()
        self.canvas.set_root(self.main_box)
        self.canvas.set_size_request(100, 600)
     
        for thingy in thingies:
            self.main_box.append(thingy.get_content())
  
        self.canvas.show()

    def show(self):
        self.dw.show_all()

panel = BigBoardPanel()
panel.show()

gtk.main()