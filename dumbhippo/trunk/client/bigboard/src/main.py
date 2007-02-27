#!/usr/bin/python

import os, sys, threading, getopt, logging

import gobject, gtk, dbus, dbus.glib

import hippo
from big_widgets import Sidebar, CommandShell
import bigboard,libbig

import stocks, stocks.self_stock

class BigBoardPanel:
    def __init__(self):
        self.dw = Sidebar(True)

        self._stocks = [stocks.self_stock.SelfStock()]

        self.canvas = hippo.Canvas()
        self.dw.get_content().add(self.canvas)
        
        self.main_box = hippo.CanvasBox()
        self.canvas.set_root(self.main_box)
        self.canvas.set_size_request(100, 600)
     
        self.title = hippo.CanvasText()
        self.title.set_property("text", "My Fedora")
     
        self.main_box.append(self.title)
     
        for stock in self._stocks:
            self.main_box.append(stock.get_content())
  
        self.canvas.show()
        
    def show(self):
        self.dw.show_all()

def load_image_hook(img_name):
    print "loading: %s" % (img_name,)
    pixbuf = gtk.gdk.pixbuf_new_from_file(img_name)
    return hippo.cairo_surface_from_gdk_pixbuf(pixbuf)

def usage():
    print "%s [--debug] [--help]" % (sys.argv[0])

def main(args):
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hd", ["help", "debug"])
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    debug = False
    for o, a in opts:
        if o in ('-d', '--debug'):
            debug = True
        if o in ("-h", "--help"):
            usage()
            sys.exit()
    
    def logger(domain, priority, msg):
        print msg

    libbig.set_log_handler(logger)    
    
    gtk.gdk.threads_init()
    dbus.glib.threads_init()    
    
    default_log_level = logging.WARNING
    if debug:
        default_log_level = logging.DEBUG
    logging.basicConfig(level=default_log_level)
    logging.debug("Initialized logging")
    
    hippo.canvas_set_load_image_hook(load_image_hook)    
    
    panel = BigBoardPanel()
    panel.show()
    
    if debug:
        cmdshell = CommandShell({'panel': panel})
        cmdshell.show_all()
        
    gtk.main()

if __name__ == "__main__":
    main(None)
