#!/usr/bin/python

import os, sys, threading, getopt, logging, StringIO

import gobject, gtk, pango, dbus, dbus.glib

import hippo
from big_widgets import Sidebar, CommandShell, CanvasHBox
from bigboard import Stock, PrefixedState
import libbig

import stocks, stocks.self_stock, stocks.people_stock, stocks.apps_stock, stocks.search_stock, stocks.docs_stock, stocks.calendar_stock

class GradientHeader(hippo.CanvasGradient):
    def __init__(self, **kwargs):
        hippo.CanvasGradient.__init__(self, 
                                      start_color=0xF4F4F4FF, 
                                      end_color=0xC7C7C7FF,
                                      padding_left=4,
                                      color=0x333333FF, **kwargs)

class Separator(hippo.CanvasBox):
    def __init__(self):
        hippo.CanvasBox.__init__(self, border_top=1, border_color=0x999999FF)

class Exchange(hippo.CanvasBox):
    """A container for stocks."""
    
    def __init__(self, stock):
        hippo.CanvasBox.__init__(self,  
                                 orientation=hippo.ORIENTATION_VERTICAL,
                                 spacing=4)      
        self.__stock = stock
        self.__ticker_text = None
        self.__state = PrefixedState('/panel/stock/' + stock.get_id() + "/") 
        self.__state.set_default('expanded', True)
        if not stock.get_ticker() == "":
            text = stock.get_ticker()
            self.__ticker_container = GradientHeader()
            self.__ticker_text = hippo.CanvasText(text=text, font="14px", xalign=hippo.ALIGNMENT_START)
            self.__ticker_text.connect("button-press-event", lambda text, event: self.__toggle_expanded())  
            self.__ticker_container.append(self.__ticker_text)
            self.append(self.__ticker_container)
        self.__stockbox = hippo.CanvasBox()
        self.append(self.__stockbox)
        self.__sync_expanded()
        # wait to append stock until set_size is called
    
    def __toggle_expanded(self):
        self.__state['expanded'] = not self.__state['expanded']
        self.__sync_expanded()
        
    def __sync_expanded(self):
        self.set_child_visible(self.__stockbox, self.__state['expanded'])
    
    def get_stock(self):
        return self.__stock
    
    def set_size(self, size):
        self.__stockbox.remove_all()
        self.__stock.set_size(size)
        self.__stockbox.append(self.__stock.get_content(size))
        padding = 4
        if size == Stock.SIZE_BEAR:
            padding = 2  
        self.__stockbox.set_property("padding_left", padding)
        self.__stockbox.set_property("padding_right", padding)
        if self.__ticker_text:
            self.set_child_visible(self.__ticker_container, size == Stock.SIZE_BULL)

class BigBoardPanel(object):
    def __init__(self):
        self._dw = Sidebar(True)
        
        self.__logger = logging.getLogger("bigboard.Panel")
        
        self.__logger.info("constructing")
        
        self.__state = PrefixedState('/panel/')  
                       
        self.__size_str = libbig.BiMap("size", "str", {Stock.SIZE_BULL: u'bull', Stock.SIZE_BEAR: u'bear'})
        self.__state.set_default('size', self.__size_str['size'][Stock.SIZE_BULL])

        self._stocks = [stocks.self_stock.SelfStock(), 
                        stocks.search_stock.SearchStock(),
                        stocks.people_stock.PeopleStock(), 
                        stocks.apps_stock.AppsStock()
#                        stocks.calendar_stock.CalendarStock(),
#                        stocks.docs_stock.DocsStock()
                        ]
        self._exchanges = []

        self._canvas = hippo.Canvas()
        self._dw.get_content().add(self._canvas)
        self._canvas.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))
                
        self._main_box = hippo.CanvasBox(border_right=1, border_color=0x999999FF)
        self._canvas.set_root(self._main_box)
     
        self._header_container = GradientHeader()
        self._header_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self._header_container.append(self._header_box)
     
        self._title = hippo.CanvasText(text="My Fedora", font="Bold 14px")
     
        self._header_box.append(self._title)
        
        self._size_button = hippo.CanvasLink(xalign=hippo.ALIGNMENT_CENTER)
        self._size_button.connect("button-press-event", lambda text, event: self._toggle_size())
        
        self._header_box.append(self._size_button, hippo.PACK_END)
        
        self._main_box.append(self._header_container)
        
        self._stocks_box = hippo.CanvasBox(spacing=4)
        
        self._main_box.append(self._stocks_box)
        
        for stock in self._stocks:
            self.list(stock)        
  
        self._sync_size()
  
        self._canvas.show()
        
    def __get_size(self):
            return self.__size_str['str'][self.__state['size']]
        
    def list(self, stock):
        """Add a stock to an Exchange and append it to the bigboard."""
        self.__logger.debug("listing stock %s", stock)
        if stock.get_ticker() == "" and len(self._stocks_box.get_children()) > 0:
            sep = Separator()
            self._stocks_box.append(sep)
        container = Exchange(stock)
        self._stocks_box.append(container)
        container.set_size(self.__get_size())
        self._exchanges.append(container)
        
    def _toggle_size(self):
        self.__logger.debug("toggling size")
        if self.__get_size() == Stock.SIZE_BULL:
            self.__state['size'] = self.__size_str['size'][Stock.SIZE_BEAR]
        else:
            self.__state['size']= self.__size_str['size'][Stock.SIZE_BULL]
        self._sync_size()
            
    def _sync_size(self):       
        self._header_box.set_child_visible(self._title, self.__get_size() == Stock.SIZE_BULL)
        if self.__get_size() == Stock.SIZE_BEAR:
            self._header_box.remove(self._size_button)
            self._header_box.append(self._size_button, hippo.PACK_EXPAND)
            self._size_button.set_property("text", u"\u00bb large")
            self._canvas.set_size_request(Stock.SIZE_BEAR_CONTENT_PX, 42)
        else:
            self._header_box.remove(self._size_button)
            self._header_box.append(self._size_button, hippo.PACK_END)            
            self._size_button.set_property("text", u"\u00ab small")        
            self._canvas.set_size_request(Stock.SIZE_BULL_CONTENT_PX, 42)
            
        for exchange in self._exchanges:
            self.__logger.debug("resizing exchange %s to %s", exchange, self.__get_size())
            exchange.set_size(self.__get_size())
        
        self._dw.queue_resize()  
        # TODO - this is kind of gross; we need the strut change to happen after
        # the resize, but that appears to be an ultra-low priority internally
        # so we can't easily queue something directly after.
        gobject.timeout_add(250, lambda : self._dw.do_set_wm_strut() and False)
        
    def show(self):
        self._dw.show_all()

def load_image_hook(img_name):
    print "loading: %s" % (img_name,)
    pixbuf = gtk.gdk.pixbuf_new_from_file(img_name)
    return hippo.cairo_surface_from_gdk_pixbuf(pixbuf)

def usage():
    print "%s [--debug] [--debug-modules=mod1,mod2...] [--shell] [--help]" % (sys.argv[0])

def main(args):
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hds", ["help", "debug", "info", "shell", "debug-modules="])
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    info = False
    debug = False
    shell = False
    debug_modules = []
    for o, v in opts:
        if o in ('-d', '--debug'):
            debug = True
        elif o in ('--info',):
            info = True
        elif o in ('-s', '--shell'):
            shell = True
        elif o in ('--debug-modules'):
            debug_modules = v.split(',')
        elif o in ("-h", "--help"):
            usage()
            sys.exit()
    
    def logger(domain, priority, msg):
        print msg

    #libbig.set_log_handler(logger)    
    
    gtk.gdk.threads_init()
    dbus.glib.threads_init()    
    
    default_log_level = 'ERROR'
    if info:
        default_log_level = 'INFO'
    if debug:
        default_log_level = 'DEBUG'

    libbig.init_logging(default_log_level, debug_modules)

    libbig.set_application_name("BigBoard")
    libbig.set_program_name("bigboard")
    
    hippo.canvas_set_load_image_hook(load_image_hook)    
    
    panel = BigBoardPanel()
    
    panel.show()
    
    if shell:
        cmdshell = CommandShell({'panel': panel})
        cmdshell.show_all()
        
    gtk.main()

if __name__ == "__main__":
    main(None)
