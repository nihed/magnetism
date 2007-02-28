#!/usr/bin/python

import os, sys, threading, getopt, logging

import gobject, gtk, pango, dbus, dbus.glib

import hippo
from big_widgets import Sidebar, CommandShell
from bigboard import Stock
import libbig

import stocks, stocks.self_stock, stocks.people_stock

class Exchange(hippo.CanvasBox):
    """A container for stocks."""
    
    def __init__(self, stock):
        hippo.CanvasBox.__init__(self,  
                                 spacing=4, 
                                 padding_left=4,
                                 padding_top=6,
                                 orientation=hippo.ORIENTATION_VERTICAL)
        self._stock = stock
        if not stock.get_ticker() is None:
            text = stock.get_ticker() + "                  "
            self._ticker_text = hippo.CanvasText(text=text, font="14px", xalign=hippo.ALIGNMENT_START)
            attrs = pango.AttrList()
            attrs.insert(pango.AttrUnderline(pango.UNDERLINE_SINGLE, 0, 32767))
            self._ticker_text.set_property("attributes", attrs)
            self.append(self._ticker_text)
        self._stockbox = hippo.CanvasBox()
        self.append(self._stockbox)
        # wait to append until set_size is called
    
    def get_stock(self):
        return self._stock
    
    def set_size(self, size):
        self._stockbox.remove_all()
        self._stock.set_size(size)
        self._stockbox.append(self._stock.get_content(size))

class BigBoardPanel(object):
    def __init__(self):
        self._dw = Sidebar(True)
        
        self._size = Stock.SIZE_BULL

        self._stocks = [stocks.self_stock.SelfStock(), stocks.people_stock.PeopleStock()]
        self._exchanges = []

        self._canvas = hippo.Canvas()
        self._dw.get_content().add(self._canvas)
        self._canvas.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))
        
        self._main_box = hippo.CanvasBox()
        self._canvas.set_root(self._main_box)
     
        self._header_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
     
        self._title = hippo.CanvasText(text="My Fedora", font="Bold 12px")
     
        self._header_box.append(self._title)
        
        self._size_button = hippo.CanvasLink(xalign=hippo.ALIGNMENT_END)
        self._size_button.connect("button-press-event", lambda text, event: self._toggle_size())
        
        self._header_box.append(self._size_button, hippo.PACK_END)
        
        self._main_box.append(self._header_box)
        
        self._stocks_box = hippo.CanvasBox()
        
        self._main_box.append(self._stocks_box)
        
        for stock in self._stocks:
            self.list(stock)        
  
        self._sync_size()
  
        self._canvas.show()
        
    def list(self, stock):
        container = Exchange(stock)
        self._stocks_box.append(container)
        container.set_size(self._size)
        self._exchanges.append(container)
        
    def _toggle_size(self):
        if self._size == Stock.SIZE_BULL:
            self._size = Stock.SIZE_BEAR
        else:
            self._size = Stock.SIZE_BULL
        self._sync_size()
            
    def _sync_size(self):       
        self._header_box.set_child_visible(self._title, self._size == Stock.SIZE_BULL)
        if self._size == Stock.SIZE_BEAR:
            self._size_button.set_property("text", u"\u00bb large")
            self._canvas.set_size_request(55, 42)             
        else:
            self._size_button.set_property("text", u"\u00ab small")
            self._canvas.set_size_request(210, 42)             
            
        for exchange in self._exchanges:
            exchange.set_size(self._size)
        
        self._dw.queue_resize()  
        # TODO - this is kind of gross; we need the strut change to happen after
        # the resize, but that appears to be an ultra-low priority internally
        # so we can't easily queue something directly after.
        gobject.timeout_add(42, lambda : self._dw.do_set_wm_strut() and False)
        
    def show(self):
        self._dw.show_all()

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

    #libbig.set_log_handler(logger)    
    
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
