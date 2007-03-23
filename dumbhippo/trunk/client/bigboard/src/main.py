#!/usr/bin/python

import os, sys, threading, getopt, logging, StringIO, stat
import xml.dom.minidom

import gobject, gtk, pango, dbus, dbus.glib

import hippo
from big_widgets import Sidebar, CommandShell, CanvasHBox, ActionLink
from bigboard import Stock, PrefixedState
import libbig

class GradientHeader(hippo.CanvasGradient):
    def __init__(self, **kwargs):
        hippo.CanvasGradient.__init__(self, 
                                      orientation=hippo.ORIENTATION_HORIZONTAL,
                                      start_color=0xF4F4F4FF, 
                                      end_color=0xC7C7C7FF,
                                      padding_left=4,
                                      color=0x333333FF, **kwargs)

class Separator(hippo.CanvasBox):
    def __init__(self):
        hippo.CanvasBox.__init__(self, border_top=1, border_color=0x999999FF)
        
class StockReader(gobject.GObject):
    __gsignals__ = {
        "stock-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
    }
            
    def __init__(self, dirs):
        gobject.GObject.__init__(self)
        self.__logger = logging.getLogger('bigboard.StockReader')
        self.__dirs = dirs
        
    def load(self):
        self.__logger.debug("starting stock reading, cwd=%s", os.getcwd())
        for dir in self.__dirs:
            self.__logger.info("reading directory %s", dir)
            for direntry in os.listdir(dir):
                stockdir = os.path.join(dir, direntry)
                if not stat.S_ISDIR(os.stat(stockdir).st_mode):
                    continue
                
                listing = os.path.join(stockdir, 'listing.xml')
                if not os.access(listing, os.R_OK):
                    self.__logger.debug("ignoring missing %s", listing)
                    continue
                self.__logger.debug("parsing %s", listing)
                doc = xml.dom.minidom.parse(listing)
                
                stock = doc.documentElement
                fmt_version = int(stock.getAttribute("version") or "0")
                id = libbig.get_xml_element_value(stock, 'id')
                class_name = id.split('.')[-1]
                try:
                    ticker = libbig.get_xml_element_value(stock, 'ticker')
                except KeyError:
                    ticker = ""
                    
                sys.path.append(stockdir)
                try:
                    self.__logger.info("importing module %s (%s) %s", class_name, id, stockdir)
                    module = __import__(class_name)
                    class_constructor = getattr(module, class_name)
                    self.__logger.debug("got constructor %s", class_constructor)
                    
                    stock = class_constructor({'id': id, 'ticker': ticker})
                    self.emit("stock-added", stock)                    
                except:
                    self.__logger.exception("failed to add stock %s", id)
                
                
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
        if stock.get_ticker() == "-":
            sep = Separator()
            self.append(sep)            
        elif not stock.get_ticker() == "":
            text = stock.get_ticker()
            self.__ticker_container = GradientHeader()
            self.__ticker_text = hippo.CanvasText(text=text, font="14px", xalign=hippo.ALIGNMENT_START)
            self.__ticker_text.connect("button-press-event", lambda text, event: self.__toggle_expanded())  
            self.__ticker_container.append(self.__ticker_text, hippo.PACK_EXPAND)
            
            if stock.has_more_link():
                more_link = ActionLink(xalign=hippo.ALIGNMENT_END, 
                                       text=u"More \u00BB")
                more_link.connect("button-press-event", lambda link, event: stock.on_more_clicked())
                self.__ticker_container.append(more_link)
            
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
    def __init__(self, dirs):
        self._dw = Sidebar(True)
        
        self.__logger = logging.getLogger("bigboard.Panel")
        
        self.__logger.info("constructing")
        
        self.__state = PrefixedState('/panel/')  
        
        self.__state.set_default('listed', 'org.mugshot.bigboard.SelfStock;org.mugshot.bigboard.SearchStock;org.mugshot.bigboard.AppsStock;org.mugshot.bigboard.PhotosStock')
                       
        self.__size_str = libbig.BiMap("size", "str", {Stock.SIZE_BULL: u'bull', Stock.SIZE_BEAR: u'bear'})
        self.__state.set_default('size', self.__size_str['size'][Stock.SIZE_BULL])

        self.__stockreader = StockReader(dirs)
        self.__stockreader.connect("stock-added", lambda reader, stock: self.__on_stock_added(stock))

        self._exchanges = []

        self._canvas = hippo.Canvas()
        self._dw.get_content().add(self._canvas)
        self._canvas.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))
                
        self._main_box = hippo.CanvasBox(border_right=1, border_color=0x999999FF)
        self._canvas.set_root(self._main_box)
     
        self._header_box = GradientHeader()
     
        self._title = hippo.CanvasText(text="My Fedora", font="Bold 14px", xalign=hippo.ALIGNMENT_START)
     
        self._header_box.append(self._title, hippo.PACK_EXPAND)
        
        self._size_button = hippo.CanvasLink(xalign=hippo.ALIGNMENT_CENTER)
        self._size_button.connect("button-press-event", lambda text, event: self._toggle_size())
        
        self._header_box.append(self._size_button, hippo.PACK_END)
        
        self._main_box.append(self._header_box)
        
        self._stocks_box = hippo.CanvasBox(spacing=4)
        
        self._main_box.append(self._stocks_box)
  
        self._sync_size()
        
        self.__stockreader.load()        
  
        self._canvas.show()
        
    def __on_stock_added(self, stock):
        self.list(stock)
        
    def __get_size(self):
            return self.__size_str['str'][self.__state['size']]
        
    def list(self, stock):
        """Add a stock to an Exchange and append it to the bigboard."""
        self.__logger.debug("listing stock %s", stock)
        container = Exchange(stock)
        container.set_size(self.__get_size())
        self._exchanges.append(container)
                
        last_matched = None
        for listed in self.__state['listed'].split(';'):
            if listed == stock.get_id():
                self.__logger.debug("found stock %s in saved listing, inserting after %s", stock, last_matched)
                if last_matched is None:
                    self._stocks_box.prepend(container)
                else:
                    self._stocks_box.insert_after(container, last_matched)
                break
            for exchange in self._stocks_box.get_children():
                if listed == exchange.get_stock().get_id(): 
                    last_matched = exchange
        
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
    print "%s [--debug] [--debug-modules=mod1,mod2...] [--shell] [--stockdirs=dir1:dir2:...] [--help]" % (sys.argv[0])

def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hds", ["help", "debug", "info", "shell", "stockdirs=", "debug-modules="])
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    info = False
    debug = False
    shell = False
    stockdirs = []
    debug_modules = []
    for o, v in opts:
        if o in ('-d', '--debug'):
            debug = True
        elif o in ('--info',):
            info = True
        elif o in ('-s', '--shell'):
            shell = True
        elif o in ('--stockdirs',):
            stockdirs = map(os.path.abspath, v.split(':'))
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
    
    panel = BigBoardPanel(stockdirs)
    
    panel.show()
    
    if shell:
        cmdshell = CommandShell({'panel': panel})
        cmdshell.show_all()
        
    gtk.main()

if __name__ == "__main__":
    main()
