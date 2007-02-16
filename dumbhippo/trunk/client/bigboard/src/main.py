#!/usr/bin/python

import os, threading

import gobject
import gtk

import hippo
from libbig import Sidebar
import bigboard,libbig

import thingy_self

thingies = [thingy_self.SelfThingy()]
        
class BigBoardPanel:
    def __init__(self):
        self.dw = Sidebar(True)

        self.canvas = hippo.Canvas()
        self.dw.get_content().add(self.canvas)
        
        self.main_box = hippo.CanvasBox()
        self.canvas.set_root(self.main_box)
        self.canvas.set_size_request(100, 600)
     
        self.title = hippo.CanvasText()
        self.title.set_property("text", "My Fedora")
     
        self.main_box.append(self.title)
     
        for thingy in thingies:
            self.main_box.append(thingy.get_content())
  
        self.canvas.show()
        
    def show(self):
        self.dw.show_all()

def load_image_hook(img_name):
    print "loading: %s" % (img_name,)
    pixbuf = gtk.gdk.pixbuf_new_from_file(img_name)
    return hippo.cairo_surface_from_gdk_pixbuf(pixbuf)

def main(args):
    def logger(domain, priority, msg):
        print msg

    libbig.set_log_handler(logger)    
    hippo.canvas_set_load_image_hook(load_image_hook)    
    
    panel = BigBoardPanel()
    panel.show()
    
    cmdshell = libbig.CommandShell()
    cmdshell.show_all()
    gtk.main()

if __name__ == "__main__":
    main(None)
