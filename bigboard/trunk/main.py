#!/usr/bin/python

import os, sys, threading, getopt, logging, StringIO, stat, signal
import xml.dom.minidom

import gobject, gtk, pango
import gnome.ui, gconf
# We need to import this early before gnome_program_init() is called
import gnomeapplet
import dbus
import dbus.service
import dbus.glib

import hippo

import bigboard
import bigboard.big_widgets
from bigboard.big_widgets import Sidebar, CommandShell, CanvasHBox, CanvasVBox, ActionLink, Button
from bigboard.stock import Stock
import bigboard.libbig
try:
    import bigboard.bignative as bignative
except:
    import bignative
import bigboard.globals
import bigboard.google
import bigboard.presence
from bigboard.libbig.gutil import *
from bigboard.libbig.logutil import log_except
import bigboard.libbig.dbusutil
import bigboard.libbig.logutil
import bigboard.libbig.xmlquery
import bigboard.libbig.stdout_logger
import bigboard.keybinder

BUS_NAME_STR='org.gnome.BigBoard'
BUS_IFACE=BUS_NAME_STR
BUS_IFACE_PANEL=BUS_IFACE + ".Panel"

GCONF_PREFIX = '/apps/bigboard/'

REEXEC_CMD = os.getenv('BB_REEXEC') or '/usr/bin/bigboard'
REEXEC_CMD = os.path.abspath(REEXEC_CMD)

BB_DATADIR = os.getenv('BB_DATADIR')
if BB_DATADIR:
    BB_DATADIR = os.path.abspath(BB_DATADIR)

def _find_in_datadir(fname):
    if BB_DATADIR:
        return os.path.join(BB_DATADIR, fname)
    datadir_env = os.getenv('XDG_DATA_DIRS')
    if datadir_env:
        datadirs = datadir_env.split(':')
    else:
        datadirs = ['/usr/share/']
    datadirs = map(lambda x: os.path.join(x, 'bigboard'), datadirs)
    for dir in datadirs:
        fpath = os.path.join(dir, fname)
        if os.access(fpath, os.R_OK):
            return fpath
    return None

_logger = logging.getLogger("bigboard.Main")

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
        
class FirstTimeMinimizeDialog(gtk.Dialog):
    def __init__(self, show_windows):
        super(FirstTimeMinimizeDialog, self).__init__("Minimizing Sidebar",
                                                      None,
                                                      gtk.DIALOG_MODAL,
                                                      ('Undo', gtk.RESPONSE_CANCEL,
                                                       gtk.STOCK_OK, gtk.RESPONSE_ACCEPT))
        self.set_has_separator(False)
        hbox = gtk.HBox(spacing=8)
        self.vbox.add(hbox)
        if show_windows:
            img_filename = 'windows_key.png'
        else:
            img_filename = 'ctrl_esc_keys.png'
        img_filename = _find_in_datadir(img_filename)
        _logger.debug("using img %s", img_filename)            
        img = gtk.Image()
        img.set_from_file(img_filename)
        hbox.add(img)
        hbox.add(gtk.Label('''The sidebar is now hidden; press the key shown on the left to pop it
back up temporarily.'''))
        self.connect('response', self.__on_response)
        
    def __on_response(self, self2, id):
        if id != gtk.RESPONSE_CANCEL:
            gconf.client_get_default().set_bool(GCONF_PREFIX + 'first_time_minimize_seen', True)
        else:
            # Avoid set of same value on notify receipt
            gobject.timeout_add(100, self.__idle_undo_visible)
        self.destroy()
        
    def __idle_undo_visible(self):
        gconf.client_get_default().set_bool(GCONF_PREFIX + 'visible', True)
        return False        
        
class PrelistedStock(object):
    def __init__(self, id, stockdir):
        self.__id = id
        self.__stockdir = stockdir
        self.__logger = logging.getLogger('bigboard.StockReader')        
        
    def get_id(self):
        return self.__id
        
    def get(self, panel=None):
        listing = os.path.join(self.__stockdir, 'listing.xml')
        if not os.access(listing, os.R_OK):
            raise Exception("stock listing %s vanished!" % listing)
        self.__logger.debug("parsing %s", listing)
        doc = xml.dom.minidom.parse(listing)
        stock = doc.documentElement
        fmt_version = int(stock.getAttribute("version") or "0")            
        class_name = self.__id.split('.')[-1]
        try:
            ticker = bigboard.libbig.xmlquery.get_element_value(stock, 'ticker')
        except KeyError:
            ticker = ""
            
        sys.path.append(self.__stockdir)
        try:
            self.__logger.info("importing module %s (%s) %s", class_name, self.__id, self.__stockdir)
            module = __import__(class_name)
            class_constructor = getattr(module, class_name)
            self.__logger.debug("got constructor %s", class_constructor)
            
            stock = class_constructor({'id': self.__id, 'ticker': ticker}, panel=panel)
            return stock                  
        except:
            self.__logger.exception("failed to add stock %s", self.__id)        
            return None
        
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
                self.__logger.debug("parsing prelisting %s", listing)
                doc = xml.dom.minidom.parse(listing)
                stock = doc.documentElement
                id = bigboard.libbig.xmlquery.get_element_value(stock, 'id')

                self.emit("stock-added", PrelistedStock(id, stockdir))
                
class Exchange(hippo.CanvasBox):
    """A container for stocks."""
    
    def __init__(self, stock):
        hippo.CanvasBox.__init__(self,  
                                 orientation=hippo.ORIENTATION_VERTICAL,
                                 spacing=4)      
        self.__size = None
        self.__logger = logging.getLogger("bigboard.Panel")
        self.__stock = stock
        self.__ticker_text = None
        self.__ticker_container = None
        self.__mini_more_button = None
        self.__sep = Separator()
        self.append(self.__sep)
        self.__expanded = True
        if not stock.get_ticker() in ("-", ""):
            text = stock.get_ticker()
            self.__ticker_container = GradientHeader()
            self.__ticker_text = hippo.CanvasText(text=text, font="14px", xalign=hippo.ALIGNMENT_START)
            self.__ticker_text.connect("button-press-event", lambda text, event: self.__toggle_expanded())  
            self.__ticker_container.append(self.__ticker_text, hippo.PACK_EXPAND)
            
            if stock.has_more_button():
                more_button = Button(label='More', label_ypadding=-2)
                more_button.set_property('yalign', hippo.ALIGNMENT_CENTER)
                more_button.connect("activated", lambda l: stock.on_more_clicked())
                self.__ticker_container.append(more_button)
                self.__mini_more_button = Button(label='More', label_ypadding=-1)
                self.__mini_more_button.set_property('yalign', hippo.ALIGNMENT_CENTER)                   
                self.__mini_more_button.connect("activated", lambda l: stock.on_more_clicked())
                self.append(self.__mini_more_button)
            
            self.append(self.__ticker_container)
        self.__stock.connect("visible", lambda s, v: self.set_size(self.__size))
        self.__stockbox = hippo.CanvasBox()
        self.append(self.__stockbox)
    
    def __toggle_expanded(self):
        self.__expanded = not self.__expanded
        self.set_child_visible(self.__stockbox, self.__expanded)
    
    def get_stock(self):
        return self.__stock
    
    def set_size(self, size):
        self.__size = size
        self.__stockbox.remove_all()
        self.__stock.set_size(size)
        content = self.__stock.get_content(size) 
        if self.__ticker_container:
            self.set_child_visible(self.__ticker_container, not not content)
        self.set_child_visible(self.__sep,
                               (not not content) and \
                               ((self.__ticker_container and size == Stock.SIZE_BEAR) \
                                or (size == Stock.SIZE_BULL
                                    and ((not self.__ticker_container) or (self.__stock.get_ticker() == "-")))))
        if self.__mini_more_button:
            self.set_child_visible(self.__mini_more_button, size == Stock.SIZE_BEAR)
        self.set_child_visible(self.__stockbox, not not content)
        if not content:
            self.__logger.debug("no content for stock %s", self.__stock)
            return
        self.__stockbox.append(content)
        padding = 4
        if size == Stock.SIZE_BEAR:
            padding = 2  
        self.__stockbox.set_property("padding_left", padding)
        self.__stockbox.set_property("padding_right", padding)
        if self.__ticker_text:
            self.set_child_visible(self.__ticker_container, size == Stock.SIZE_BULL)

class BigBoardPanel(dbus.service.Object):
    def __init__(self, dirs, bus_name):
        dbus.service.Object.__init__(self, bus_name, '/bigboard/panel')
        
        self.__logger = logging.getLogger("bigboard.Panel")        
        self.__logger.info("constructing")
                
        self._dw = Sidebar(True, GCONF_PREFIX + 'visible')
        self._shown = False
        self.__shell = None
        
        gconf_client = gconf.client_get_default()

        self.__keybinding = "Super_L"
        bigboard.keybinder.tomboy_keybinder_bind(self.__keybinding, self.__on_focus)
    
        self.__autohide_id = 0
        self._dw.connect('enter-notify-event', self.__on_mouse_enter)        
        self._dw.connect('leave-notify-event', self.__on_mouse_leave)
        
        self.__stockreader = StockReader(dirs)
        self.__stockreader.connect("stock-added", lambda reader, stock: self.__on_stock_added(stock))
        gconf_client.notify_add(GCONF_PREFIX + 'listings', lambda *args: self.Reboot())        

        self._exchanges = []
        self.__prelisted = {}

        self._canvas = hippo.Canvas()
        self._dw.get_content().add(self._canvas)
        self._canvas.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))
                
        self._main_box = hippo.CanvasBox(border_right=1, border_color=0x999999FF, padding_bottom=4)
        self._canvas.set_root(self._main_box)
     
        self._header_box = GradientHeader()
        self._header_box.connect("button-press-event", self.__on_header_buttonpress)     
     
        self._title = hippo.CanvasText(text="My Desktop", font="Bold 14px", xalign=hippo.ALIGNMENT_START)
     
        self._header_box.append(self._title, hippo.PACK_EXPAND)
        
        #self._size_button = hippo.CanvasImage(xalign=hippo.ALIGNMENT_END, yalign=hippo.ALIGNMENT_START)
        #self._size_button.set_clickable(True)
        #self._size_button.connect("button-press-event", lambda text, event: self._toggle_size())
        #
        #self._header_box.append(self._size_button, hippo.PACK_END)
        self._size_button = None
        
        self._main_box.append(self._header_box)
        
        self._stocks_box = hippo.CanvasBox(spacing=4)
        
        self._main_box.append(self._stocks_box)
  
        gconf_client.notify_add(GCONF_PREFIX + 'expand', self._sync_size)
        self._sync_size()
        
        try:
            self.__screensaver_proxy = dbus.SessionBus().get_object('org.gnome.ScreenSaver', '/org/gnome/ScreenSaver')
            self.__screensaver_proxy.connect_to_signal('SessionIdleChanged',
                                                       self.__on_session_idle_changed)
        except dbus.DBusException, e:
            _logger.warn("Couldn't find screensaver")
            pass
        
        self.__stockreader.load()        

        try:
            search = self.get_stock('org.gnome.bigboard.SearchStock')
            search.connect('match-selected', self.__on_search_match_selected)
        except KeyError, e:
            pass

        gconf_client.notify_add(GCONF_PREFIX + 'visible', self.__sync_visible)
        self.__sync_visible()
  
        self._canvas.show()

        self.__queue_strut()
        
        gobject.timeout_add(1000, self.__idle_show_we_exist)
        
    @log_except()
    def __on_session_idle_changed(self, isidle):
        if not isidle:
            self.__idle_show_we_exist()

    def __on_header_buttonpress(self, box, e):
        self.__logger.debug("got shell header click: %s %s %s", e, e.button, e.modifiers)
        if e.button == 2:
            self.Shell()

    @log_except()
    def __idle_show_we_exist(self):
        self.__logger.debug("showing we exist")
        self.__handle_activation()
        self.__handle_deactivation()

    @log_except()
    def __on_focus(self):
        self.__logger.debug("got focus keypress")
        self.external_focus()
        
    def __on_stock_added(self, prestock):
        if not prestock.get_id() in gconf.client_get_default().get_list(GCONF_PREFIX + 'listings', gconf.VALUE_STRING):
            self.__logger.debug("ignoring unlisted stock %s", prestock.get_id())
            self.__prelisted[prestock.get_id()] = prestock
            return
        stock = prestock.get(panel=self)
        if not stock:
            self.__logger.debug("stock %s failed to load", prestock.get_id())
            return
        self.list(stock)
        
    def __get_size(self):
        return Stock.SIZE_BULL
    
        #client = gconf.client_get_default()
        #if client.get_bool(GCONF_PREFIX + 'expand'):
        #    return Stock.SIZE_BULL
        #return Stock.SIZE_BEAR
        
    def list(self, stock):
        """Add a stock to an Exchange and append it to the bigboard."""
        self.__logger.debug("listing stock %s", stock)
        container = Exchange(stock)
        container.set_size(self.__get_size())
        self._exchanges.append(container)
                
        last_matched = None
        for listed in gconf.client_get_default().get_list(GCONF_PREFIX + 'listings', gconf.VALUE_STRING):
            stockid = stock.get_id()
            if listed == stockid:
                self.__logger.debug("found stock %s in saved listing, inserting after %s", stock, last_matched)
                if last_matched is None:
                    self._stocks_box.prepend(container)
                else:
                    self._stocks_box.insert_after(container, last_matched)
                break
            for exchange in self._stocks_box.get_children():
                if listed == exchange.get_stock().get_id(): 
                    last_matched = exchange

    def get_stocks(self):
        return map(lambda e: e.get_stock(), self._exchanges)

    def get_stock(self, id):
        for xcg in self._exchanges:
            stock = xcg.get_stock()
            if stock.get_id() == id:
                return stock
        raise KeyError("Couldn't find stock %s" % (id,))

    @log_except()
    def __on_mouse_enter(self, w, e):
        self.__logger.debug("mouse enter %s", e)
        if self.__autohide_id > 0:
            self.__logger.debug("removing autohide timeout")
            gobject.source_remove(self.__autohide_id)
            self.__autohide_id = 0
    
    @log_except()
    def __on_search_match_selected(self, search):
        self.__logger.debug("search match selected")        
        self.__handle_deactivation(immediate=True)    
    
    @log_except()
    def __on_mouse_leave(self, w, e):
        self.__logger.debug("mouse leave %s", e)
        self.__handle_deactivation()
        
    def __handle_deactivation(self, immediate=False):
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        if not vis and self.__autohide_id == 0:
            self.__logger.debug("enqueued autohide timeout")            
            self.__autohide_id = gobject.timeout_add(immediate and 1 or 1500, self.__idle_do_hide)        
            
    @log_except()
    def __idle_do_hide(self):
        self.__logger.debug("in idle hide")
        self.__autohide_id = 0
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        if vis:
            return        
        self.__shown = False
        self._dw.hide()
        
    @log_except()
    def __sync_visible(self, *args):
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        self.__queue_strut()
        if vis:
            self._dw.show()
            self.ExpandedChanged(True)
        else:
            self._dw.hide()
            if not gconf.client_get_default().get_bool(GCONF_PREFIX + 'first_time_minimize_seen'):
                dialog = FirstTimeMinimizeDialog(True)
                dialog.show_all()        
            self.ExpandedChanged(False)
        
    @log_except()
    def _toggle_size(self):
        self.__logger.debug("toggling size")
        expanded = gconf.client_get_default().get_bool(GCONF_PREFIX + 'expand')
        gconf.client_get_default().set_bool(GCONF_PREFIX + 'expand', not expanded)
            
    def _sync_size(self, *args):       
        self._header_box.set_child_visible(self._title, self.__get_size() == Stock.SIZE_BULL)
        if self.__get_size() == Stock.SIZE_BEAR:
            if self._size_button:
                self._header_box.remove(self._size_button)
                self._header_box.append(self._size_button, hippo.PACK_EXPAND)
                self._size_button.set_property('image-name', 'bigboard-expand.png')
            self._canvas.set_size_request(Stock.SIZE_BEAR_CONTENT_PX, 42)
        else:
            if self._size_button:
                self._header_box.remove(self._size_button)
                self._header_box.append(self._size_button, hippo.PACK_END)            
                self._size_button.set_property('image-name', 'bigboard-collapse.png')       
            self._canvas.set_size_request(Stock.SIZE_BULL_CONTENT_PX, 42)
            
        for exchange in self._exchanges:
            self.__logger.debug("resizing exchange %s to %s", exchange, self.__get_size())
            exchange.set_size(self.__get_size())
        
        self.__logger.debug("queuing resize")
        self._dw.queue_resize()  
        self.__logger.debug("queuing strut")
        self.__queue_strut()
        self.__logger.debug("queuing strut complete")

    @log_except()
    def __idle_do_strut(self):
        self.__logger.debug("idle strut set")
        self._dw.do_set_wm_strut()
        self.__logger.debug("idle strut set complete")
        return False

    def __queue_strut(self):
        # TODO - this is kind of gross; we need the strut change to happen after
        # the resize, but that appears to be an ultra-low priority internally
        # so we can't easily queue something directly after.
        gobject.timeout_add(250, self.__idle_do_strut)
        
    def show(self):
        self.__sync_visible()

    def __handle_activation(self):
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        if not vis:
            self.__logger.debug("showing all")
            self._dw.show_all()
            self._shown = True
        if self.__get_size() == Stock.SIZE_BEAR:
            self._toggle_size()        

    def external_focus(self):
        self.__handle_activation()
        try:
            search = self.get_stock('org.gnome.bigboard.SearchStock')
        except KeyError, e:
            _logger.debug("Couldn't find search stock")
            return
        search.focus()

    def __do_unexpand(self):
        gconf.client_get_default().set_bool(GCONF_PREFIX + 'visible', False)

    @dbus.service.method(BUS_IFACE_PANEL)
    def Unexpand(self):
        self.__logger.debug("got unexpand method call")
        return self.__do_unexpand()

    def __do_expand(self):
        gconf.client_get_default().set_bool(GCONF_PREFIX + 'visible', True)

    def toggle_expand(self):
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        vis = not vis
        gconf.client_get_default().set_bool(GCONF_PREFIX + 'visible', vis)

    @dbus.service.method(BUS_IFACE_PANEL)
    def Expand(self):
        self.__logger.debug("got expand method call")
        return self.__do_expand()

    @dbus.service.method(BUS_IFACE_PANEL)
    def EmitExpandedChanged(self):
        self.__logger.debug("got emitExpandedChanged method call")
        self.ExpandedChanged(gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible'))

    @dbus.service.method(BUS_IFACE_PANEL)
    def Reboot(self):
        import subprocess
        args = [REEXEC_CMD]
        args.extend(sys.argv[1:])
        if not '--replace' in args:
            args.append('--replace')
        _logger.debug("Got Reboot, executing %s", args)
        subprocess.Popen(args)

    @dbus.service.method(BUS_IFACE_PANEL)
    def Logout(self):
        master = gnome.ui.master_client()
        master.request_save(gnome.ui.SAVE_GLOBAL,
                            True,
                            gnome.ui.INTERACT_ANY,
                            False,
                            True)


    def __create_scratch_window(self):
        w = hippo.CanvasWindow(gtk.WINDOW_TOPLEVEL)
        w.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))
        w.set_title('Scratch Window')
        box = CanvasVBox()
        w.set_root(box)
        w.connect('delete-event', lambda *args: w.destroy())
        w.show_all()
        w.present_with_time(gtk.get_current_event_time())
        return box

    @dbus.service.method(BUS_IFACE_PANEL)
    def Shell(self):
        if self.__shell:
            self.__shell.destroy()
        self.__shell = CommandShell({'panel': self,
                                     'scratch_window': self.__create_scratch_window})
        self.__shell.show_all()
        self.__shell.present_with_time(gtk.get_current_event_time())
        
    @dbus.service.method(BUS_IFACE_PANEL)
    def Kill(self):
        try:
            bigboard.keybinder.tomboy_keybinder_unbind(self.__keybinding)
        except KeyError, e:
            pass   
        # This is a timeout so we reply to the method call
        gobject.timeout_add(100, gtk.main_quit)
        
    @dbus.service.method(BUS_IFACE_PANEL)
    def Exit(self):
        gtk.main_quit()

    @dbus.service.signal(BUS_IFACE_PANEL,
                         signature='b')
    def ExpandedChanged(self, is_expanded):
        pass

def load_image_hook(img_name):
    if img_name.startswith('bigboard-'):
        img_name = _find_in_datadir(img_name)
    if img_name.find(os.sep) >= 0:
        pixbuf = gtk.gdk.pixbuf_new_from_file(img_name)
    else:
        theme = gtk.icon_theme_get_default()
        pixbuf = theme.load_icon(img_name, 60, gtk.ICON_LOOKUP_USE_BUILTIN)
    _logger.debug("loaded '%s': %s" % (img_name,pixbuf))        
    return hippo.cairo_surface_from_gdk_pixbuf(pixbuf)    

def on_name_lost(*args):
    name = str(args[0])
    logging.debug("Lost bus name " + name)
    if name == BUS_NAME_STR:
        gtk.main_quit()

def usage():
    print "%s [--debug] [--debug-modules=mod1,mod2...] [--info] [--no-autolaunch] [--stockdirs=dir1:dir2:...] [--help]" % (sys.argv[0])

def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hds", ["help", "debug", "na", "no-autolaunch", "info", "replace", "stockdirs=", "debug-modules=", "server=", "dogfood"])
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    info = False
    debug = False
    replace = False
    stockdirs = []
    debug_modules = []
    for o, v in opts:
        if o in ('-d', '--debug'):
            debug = True
        elif o in ('--dogfood',):
            bigboard.globals.set_server_name('dogfood.mugshot.org:9080')
        elif o in ('--info',):
            info = True
        elif o in ('--replace',):
            replace = True            
        elif o in ('--na', '--no-autolaunch'):
            bigboard.globals.set_do_autolaunch(False)
        elif o in ('--stockdirs',):
            stockdirs = map(os.path.abspath, v.split(':'))
        elif o in ('--debug-modules',):
            debug_modules = v.split(',')
        elif o in ('--server',):
            bigboard.globals.set_server_name(v)
        elif o in ("-h", "--help"):
            usage()
            sys.exit()

    signal.signal(signal.SIGINT, lambda i,frame: sys.stderr.write('Caught SIGINT, departing this dear world\n') or os._exit(0))

    if (not os.environ.has_key('OD_SESSION')):
        warn = gconf.client_get_default().get_without_default(GCONF_PREFIX + 'warn_outside_online_desktop')
        if warn == None or warn.get_bool():
            dialog = gtk.MessageDialog(type=gtk.MESSAGE_WARNING, message_format="Online desktop session isn't running")
            dialog.format_secondary_text("You should log into the online desktop session rather than running Big Board directly.")
            dialog.add_buttons("Exit", gtk.RESPONSE_CANCEL, "Continue", gtk.RESPONSE_OK)
            checkbutton = gtk.CheckButton("Don't show this warning again")
            checkbutton.show()
            dialog.vbox.pack_end(checkbutton)
            response = dialog.run()
            if checkbutton.get_active():
                warn = gconf.client_get_default().set_bool(GCONF_PREFIX + 'warn_outside_online_desktop', False)
            if response == gtk.RESPONSE_CANCEL:
                exit(1)

    def logger(domain, priority, msg):
        print msg

    gtk.gdk.threads_init()
    dbus.glib.threads_init()    

    gnome.program_init("bigboard", "0.3")
    
    default_log_level = 'ERROR'
    if info:
        default_log_level = 'INFO'
    if debug:
        default_log_level = 'DEBUG'

    bigboard.libbig.logutil.init(default_log_level, debug_modules, '')

    # Redirect sys.stdout to our logging framework
    sys.stdout = bigboard.libbig.stdout_logger.StdoutLogger()
    
    bignative.set_application_name("BigBoard")
    bignative.set_program_name("bigboard")
    bignative.install_focus_docks_hack()
    
    hippo.canvas_set_load_image_hook(load_image_hook)    

    bus = dbus.SessionBus() 
    bus_name = dbus.service.BusName(BUS_NAME_STR, bus=bus)

    _logger.debug("Requesting D-BUS name")
    try:
        bigboard.libbig.dbusutil.take_name(BUS_NAME_STR, replace, on_name_lost)
    except bigboard.libbig.dbusutil.DBusNameExistsException:
        print "Big Board already running; exiting"
        sys.exit(0)
        
    gconf.client_get_default().add_dir(GCONF_PREFIX[:-1], gconf.CLIENT_PRELOAD_RECURSIVE)

    gtk.rc_parse_string('''
style "bigboard-nopad-button" {
  xthickness = 0
  ythickness = 0
  GtkButton::inner-border = {0,0,0,0}
}
widget "*bigboard-nopad-button" style "bigboard-nopad-button"
''')

    listings = gconf.client_get_default().get_list(GCONF_PREFIX + 'listings', gconf.VALUE_STRING)

    ## we used to use ids with "org.mugshot" instead of "org.gnome", migrate them
    if listings:
        new_listings = []
        fixed_listing = False
        for id in listings:
            if 'org.mugshot' in id:
                new_id = id.replace('org.mugshot', 'org.gnome')
                _logger.debug("Replacing %s with %s" % (id, new_id))
                new_listings.append(new_id)
                fixed_listing = True
            else:
                new_listings.append(id)

        if fixed_listing:
            gconf.client_get_default().set_list(GCONF_PREFIX + 'listings', gconf.VALUE_STRING, new_listings)

        listings = new_listings
    
    ## this is a bad hack for now since we'll often not have schemas and there's no way
    ## to add stocks to a blank bigboard
    if not listings or len(listings) == 0:
        gconf.client_get_default().set_list(GCONF_PREFIX + 'listings', gconf.VALUE_STRING,
                                            ['org.gnome.bigboard.SelfStock','org.gnome.bigboard.SearchStock','org.gnome.bigboard.FilesStock','org.gnome.bigboard.AppsStock','org.gnome.bigboard.PeopleStock'])

    if not stockdirs:
        stockdirs = [os.path.join(os.path.dirname(bigboard.__file__), 'stocks')]
    _logger.debug("Creating panel")
    panel = BigBoardPanel(stockdirs, bus_name)
    
    panel.show()

    bigboard.google.get_google() # for side effect of creating the Google object
    #bigboard.presence.get_presence() # for side effect of creating Presence object
        
    gtk.gdk.threads_enter()
    _logger.debug("Enter mainloop")    
    gtk.main()
    gtk.gdk.threads_leave()

    _logger.debug("Exiting BigBoard")
    sys.exit(0)

if __name__ == "__main__":
    main()
