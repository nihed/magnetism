#!/usr/bin/python

import os, sys, threading, getopt, logging, StringIO, stat, signal
import xml.dom.minidom, urllib2, urlparse, subprocess, weakref

# This line makes jhbuild find the jhbuilt pygtk
import pygtk; pygtk.require ('2.0')
import gobject, gtk, pango, cairo
import gnome.ui, gconf
# We need to import this early before gnome_program_init() is called
import gnomeapplet
import dbus
import dbus.service
import dbus.glib

import hippo

import pyonlinedesktop
import pyonlinedesktop.widget

import bigboard
import bigboard.big_widgets
from bigboard.big_widgets import Sidebar, CanvasHBox, CanvasVBox, ActionLink, ThemedText
from bigboard.big_widgets import Button, GradientHeader, ThemedWidgetMixin, ThemeManager, Header
from bigboard.stock import Stock
import bigboard.libbig
try:
    import bigboard.bignative as bignative
except:
    import bignative
import bigboard.globals
import bigboard.google
from bigboard.libbig.gutil import *
from bigboard.libbig.logutil import log_except
import bigboard.libbig.dbusutil
import bigboard.libbig.logutil
import bigboard.libbig.xmlquery
import bigboard.libbig.stdout_logger
import bigboard.keybinder

_logger = logging.getLogger("bigboard.Main")

_logger.debug("starting main")

BUS_NAME_STR='org.gnome.BigBoard'
BUS_IFACE=BUS_NAME_STR
BUS_IFACE_PANEL=BUS_IFACE + ".Panel"

GCONF_PREFIX = '/apps/bigboard/'

REEXEC_CMD = os.getenv('BB_REEXEC') or '/usr/bin/bigboard'
REEXEC_CMD = os.path.abspath(REEXEC_CMD)

BB_DATADIR = os.getenv('BB_DATADIR')
if BB_DATADIR:
    BB_DATADIR = os.path.abspath(BB_DATADIR)

# We do this early because there is some breakage when fork()ing after we're doing
# Python threading and/or gnomevfs stuff.
try:    
    DESKTOP_PATH = subprocess.Popen(['xdg-user-dir', 'DESKTOP'], stdout=subprocess.PIPE).communicate()[0].strip()
    _logger.debug("got desktop path %s", DESKTOP_PATH)
except OSError, e:
    _logger.debug("caught error reading desktop path", exc_info=True)
    DESKTOP_PATH = os.path.expanduser('~/Desktop') 

def _get_datadirs():
    datadir_env = os.getenv('XDG_DATA_DIRS')
    if datadir_env:
        datadirs = datadir_env.split(':')
    else:
        datadirs = ['/usr/share/']
    return map(lambda x: os.path.join(x, 'bigboard'), datadirs)

def _find_in_datadir(fname):
    if BB_DATADIR:
        return os.path.join(BB_DATADIR, fname)
    datadirs = _get_datadirs()
    for dir in datadirs:
        fpath = os.path.join(dir, fname)
        if os.access(fpath, os.R_OK):
            return fpath
    return None

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
    
class StockManager(gobject.GObject):
    __gsignals__ = {
        "listings-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),
    }    
    def __init__(self, hardcoded_urls):
        super(StockManager, self).__init__()
        self.__stockdir = os.path.join(os.path.dirname(bigboard.__file__), 'stocks')
        self.__widget_environ = widget_environ = pyonlinedesktop.widget.WidgetEnvironment()
        widget_environ['google_apps_auth_path'] = ''
        self.__listing_key = GCONF_PREFIX + 'url_listings'
        gconf.client_get_default().notify_add(self.__listing_key, self.__on_listings_change)
        self.__metainfo_cache = {}   
        self.__hardcoded_urls = hardcoded_urls                        

    def set_listed(self, url, dolist):
        curlist = list(self.get_listed_urls())
        if (url in curlist) and dolist:
            _logger.debug("attempting to list currently listed stock %s", url)
            return
        elif (url not in curlist) and (not dolist):
            _logger.debug("attempting to delist currently unlisted stock %s", url)            
            return
        elif dolist:
            _logger.debug("listing %s", url)              
            curlist.append(url)
        elif not dolist:
            _logger.debug("delisting %s", url)               
            curlist.remove(url)
        gconf.client_get_default().set_list(self.__listing_key, gconf.VALUE_STRING, curlist)                        

    def move_listing(self, url, isup):
        curlist = list(self.get_listed_urls())
        curlen = len(curlist)
        pos = curlist.index(url)
        if pos < 0:
            _logger.debug("couldn't find url in listings: %s", url)
            return
        if isup and pos == 0:
            return
        elif (not isup) and pos == (curlen-1):
            return
        del curlist[pos]        
        pos += (isup and -1 or 1)
        curlist.insert(pos, url)
        gconf.client_get_default().set_list(self.__listing_key, gconf.VALUE_STRING, curlist)        

    def get_all_builtin_urls(self):
        for fname in os.listdir(self.__stockdir):
            fpath = os.path.join(self.__stockdir, fname)
            if fpath.endswith('.xml'):
                url = 'builtin://' + fname
                if url not in self.__hardcoded_urls:
                    yield url
            
    def get_hardcoded_urls(self):
        return self.__hardcoded_urls
            
    def get_all_builtin_metadata(self):
        for url in self.get_all_builtin_urls():
            yield self.load_metainfo(url)
            
    def get_listed_urls(self):
        for url in gconf.client_get_default().get_list(self.__listing_key, gconf.VALUE_STRING):
            if url not in self.__hardcoded_urls:
                yield url
    
    def get_listed(self):
        for url in self.get_listed_urls():
            yield self.load_metainfo(url)        

    def load_metainfo(self, url):
        try:
            return self.__metainfo_cache[url]
        except KeyError, e:
            pass        
        _logger.debug("loading stock url %s", url)
        builtin_scheme = 'builtin://'
        srcurl = url
        if url.startswith(builtin_scheme):
            srcurl = 'file://' + os.path.join(self.__stockdir, url[len(builtin_scheme):])
            baseurl = 'file://' + self.__get_moddir_for_builtin(url)
        else:
            baseurl = os.path.dirname(url)
        try:
            metainfo = pyonlinedesktop.widget.WidgetParser(url, urllib2.urlopen(srcurl), self.__widget_environ, baseurl=baseurl)
            ## FIXME this is a hack - we need to move async processing into Exchange probably
            url_contents = {}
            for url in metainfo.get_required_urls():
                url_contents[url] = urllib2.urlopen(url).read()
            metainfo.process_urls(url_contents)
        except urllib2.HTTPError, e:
            _logger.warn("Failed to load %s", url, exc_info=True)
            
        self.__metainfo_cache[url] = metainfo
        return metainfo 
    
    def render(self, module, **kwargs):
        (content_type, content_data) = module.content
        pymodule = None
        if content_type == 'online-desktop-builtin':
            pymodule = self.__load_builtin(module, **kwargs)
            if not pymodule: 
                return None
        return Exchange(module, self.__widget_environ, pymodule=pymodule, is_notitle=(module.srcurl in self.__hardcoded_urls), panel=kwargs['panel'])
        
    def render_url(self, url, **kwargs):
        return self.render(self.load_metainfo(url), **kwargs)
        
    def __get_moddir_for_builtin(self, url):
        modpath = urlparse.urlparse(url).path
        modfile = os.path.basename(modpath)
        dirname = modfile[:modfile.rfind('.')]
        return os.path.join(self.__stockdir, dirname) + "/"
                
    def __load_builtin(self, metainfo, notitle=False, panel=None):
        dirpath = self.__get_moddir_for_builtin(metainfo.srcurl)
        modpath = urlparse.urlparse(metainfo.srcurl).path
        modfile = os.path.basename(modpath)
        dirname = modfile[:modfile.rfind('.')]        
        _logger.debug("appending to path: %s", dirpath)
        sys.path.append(dirpath)
        pfxidx = modfile.find('_')
        if pfxidx >= 0:
            classname = dirname[pfxidx+1:]
        else:
            classname = dirname
        classname = classname[0].upper() + classname[1:] + 'Stock'
        try:
            _logger.info("importing module %s (title: %s) from dir %s", classname, metainfo.title, dirpath)
            pymodule = __import__(classname)
            class_constructor = getattr(pymodule, classname)
            _logger.debug("got constructor %s", class_constructor)
            if notitle:
                title = ''
            else:
                title = metainfo.title
            stock = class_constructor(metainfo, title=title, panel=panel)
            return stock                  
        except:
            _logger.exception("failed to add stock %s", classname)
            return None
                
    @defer_idle_func(timeout=100)     
    def __on_listings_change(self, *args):
        _logger.debug("processing listings change")
        self.emit("listings-changed")
         
class GoogleGadgetContainer(hippo.CanvasWidget):
    def __init__(self, metainfo, env):
        super(GoogleGadgetContainer, self).__init__()
        from pyonlinedesktop import ggadget        
        self.widget = ggadget.Gadget(metainfo, env)
        self.widget.show_all() 
        self.set_property('widget', self.widget)

class HeaderButton(hippo.CanvasBox, ThemedWidgetMixin):
    def __init__(self):
        hippo.CanvasBox.__init__(self, box_width=40, xalign=hippo.ALIGNMENT_END,
                                 background_color=0x00000001)
        self.set_clickable(True)
        ThemedWidgetMixin.__init__(self, theme_hints=['header-text'])
        self.append(hippo.CanvasText(text=" "))

    def do_paint_below_children(self, cr, dmgbox):
        area = self.get_background_area()
        self.get_theme().draw_more_button(cr, area)
gobject.type_register(HeaderButton)
         
class Exchange(hippo.CanvasBox, ThemedWidgetMixin):
    """A renderer for stocks."""
    
    def __init__(self, metainfo, env, pymodule=None, is_notitle=False, panel=None):
        hippo.CanvasBox.__init__(self,  
                                 orientation=hippo.ORIENTATION_VERTICAL,
                                 spacing=4)
        self.__size = None
        self.__metainfo = metainfo
        self.__env = env
        self.__pymodule = pymodule
        self.__panel = panel
        self.__ticker_text = None
        self.__ticker_container = None
        self.__mini_more_button = None
        self.__sep = Separator()
        self.append(self.__sep)
        self.__expanded = True
        if not is_notitle:
            self.__ticker_container = Header()
            self.__ticker_text = ThemedText(theme_hints=['header'], text=metainfo.title, font="14px Bold", xalign=hippo.ALIGNMENT_START)
            self.__ticker_text.connect("button-press-event", lambda text, event: self.__toggle_expanded())  
            self.__ticker_container.append(self.__ticker_text, hippo.PACK_EXPAND)
            
            if pymodule and pymodule.has_more_button():
                more_button = HeaderButton()
                more_button.connect("activated", lambda l: pymodule.on_more_clicked())
                self.__ticker_container.append(more_button)
            
            self.append(self.__ticker_container)
        self.__stockbox = hippo.CanvasBox()
        self.append(self.__stockbox)
        if pymodule:
            pymodule.connect('visible', self.__render_pymodule)
            self.__render_pymodule()
        else:
            self.__render_google_gadget()    

    def on_delisted(self):
        _logger.debug("on_delisted exchange %s" % (str(self)))
        self.__unrender_pymodule()

    def on_popped_out_changed(self, popped_out):
        self.__pymodule.on_popped_out_changed(popped_out)
    
    def __toggle_expanded(self):
        self.__expanded = not self.__expanded
        self.set_child_visible(self.__stockbox, self.__expanded)
    
    def get_metainfo(self):
        return self.__metainfo
    
    def get_pymodule(self):
        return self.__pymodule
    
    def __render_google_gadget(self):
        rendered = GoogleGadgetContainer(self.__metainfo, self.__env)
        self.__stockbox.append(rendered)
    
    def __render_pymodule(self, *args):
        self.__size = size = Stock.SIZE_BULL
        self.__stockbox.remove_all()
        self.__pymodule.set_size(size)
        content = self.__pymodule.get_content(size) 
        if self.__ticker_container:
            self.set_child_visible(self.__ticker_container, not not content)
        self.set_child_visible(self.__sep,
                               (not not content) and \
                               ((self.__ticker_container and size == Stock.SIZE_BEAR) \
                                or (size == Stock.SIZE_BULL
                                    and ((not self.__ticker_container) or (self.__pymodule.get_ticker() == "-")))))
        if self.__mini_more_button:
            self.set_child_visible(self.__mini_more_button, size == Stock.SIZE_BEAR)
        self.set_child_visible(self.__stockbox, not not content)
        if not content:
            _logger.debug("no content for stock %s", self.__pymodule)
            return
        self.__stockbox.append(content)
        padding = 4
        self.__stockbox.set_property("padding_left", padding)
        self.__stockbox.set_property("padding_right", padding)
        if self.__ticker_text:
            self.set_child_visible(self.__ticker_container, size == Stock.SIZE_BULL)

    def __unrender_pymodule(self):
        if not self.__pymodule:
            _logger.debug("Not a pymodule exchange")
            return

        _logger.debug("delisting pymodule %s" % (str(self.__pymodule)))
        self.__pymodule.on_delisted()
        self.__pymodule = None

class BigBoardPanel(dbus.service.Object):
    def __init__(self, bus_name):
        dbus.service.Object.__init__(self, bus_name, '/bigboard/panel')
          
        _logger.info("constructing")
                
        self.__popped_out = False
        self.__shell = None
        
        gconf_client = gconf.client_get_default()
        self._dw = Sidebar(GCONF_PREFIX + 'visible')
        gconf_client.notify_add(GCONF_PREFIX + 'orientation', self.__sync_orient)        
        self.__sync_orient()            

        self.__keybinding = gconf_client.get_string('/apps/bigboard/focus_key')
        if self.__keybinding:
            bigboard.keybinder.tomboy_keybinder_bind(self.__keybinding, self.__on_focus)
    
        self.__autohide_id = 0
        
        self._exchanges = {} ## metainfo.srcurl to Exchange

        self._canvas = canvas = hippo.Canvas()
        self._dw.get_content().add(self._canvas)
        cwin = self._dw
        self.__compositing = gtk.gdk.display_get_default().supports_composite()
        _logger.debug("compositing: %s", self.__compositing)
#            screen = cwin.get_screen()
#            rgba = screen.get_rgba_colormap()
#            cwin.set_colormap(rgba)
#            cwin.set_app_paintable(True)
#            def fitty_opacity(w, e):
#                ctx = w.window.cairo_create()
#                ctx.set_source_pixmap(canvas.window, canvas.allocation.x, canvas.allocation.y)
#                region = gtk.gdk.region_rectangle(canvas.allocation)
#                region.intersect(gtk.gdk.region_rectangle(e.area))
#                print >>sys.stderr, "e: %s" % (region,)            
#                ctx.region(region)
#                ctx.clip()
#            
#                ctx.set_operator(cairo.OPERATOR_OVER)
#                ctx.paint_with_alpha(0.5)
#            cwin.connect_after('expose-event', fitty_opacity)
#            cwin.realize()
#            cwin.window.set_composited(True)
        
        self._main_box = hippo.CanvasBox(border_right=1, border_color=0x999999FF, padding_bottom=4)
        self._canvas.set_root(self._main_box)
     
        self._header_box = Header()
        self._header_box.connect("button-press-event", self.__on_header_buttonpress)             

        self.__unpopout_button = Button(label='Hide', label_ypadding=-2)
        self.__unpopout_button.set_property('yalign', hippo.ALIGNMENT_CENTER)
        self.__unpopout_button.connect("activated", lambda button: self.__do_unpopout())
        self._header_box.append(self.__unpopout_button, hippo.PACK_END)
     
        self._title = ThemedText(text="My Desktop", font="Bold 14px", xalign=hippo.ALIGNMENT_START, padding_left=8)
     
        self._header_box.append(self._title, hippo.PACK_EXPAND)
        
        self._size_button = None
        
        self._main_box.append(self._header_box)
        
        self._stocks_box = hippo.CanvasBox(spacing=4)
        
        self._main_box.append(self._stocks_box)
        
        self.__theme_mgr = ThemeManager.getInstance()
        self.__theme_mgr.connect('theme-changed', self.__sync_theme)
        self.__sync_theme()    
  
        gconf_client.notify_add(GCONF_PREFIX + 'expand', self._sync_size)
        self._sync_size()
        
        try:
            self.__screensaver_proxy = dbus.SessionBus().get_object('org.gnome.ScreenSaver', '/org/gnome/ScreenSaver')
            self.__screensaver_proxy.connect_to_signal('SessionIdleChanged',
                                                       self.__on_session_idle_changed)
        except dbus.DBusException, e:
            _logger.warn("Couldn't find screensaver")
            pass
        
        self.__stock_manager = StockManager(['builtin://self.xml', 'builtin://search.xml'])
        self.__stock_manager.connect("listings-changed", lambda *args: self.__sync_listing())
        
        # These are hardcoded as it isn't really sensible to remove them
        self.__hardcoded_stocks = self.__stock_manager.get_hardcoded_urls()
        hardcoded_metas = map(lambda url: self.__stock_manager.load_metainfo(url), self.__hardcoded_stocks)
        for metainfo in hardcoded_metas:
            self.__append_metainfo(metainfo, notitle=True)    
        self.__self_stock = self._exchanges[self.__hardcoded_stocks[0]].get_pymodule()
        self.__search_stock = self._exchanges[self.__hardcoded_stocks[1]].get_pymodule()
        gobject.idle_add(self.__sync_listing)

        if self.__self_stock.info_loaded:
            self.__initial_appearance()
        else:
            self.__self_stock.connect('info-loaded', lambda *args: self.__initial_appearance())

        ## visible=True means we never hide, visible=False means we "autohide" and popout
        ## when the hotkey or applet is used
        gconf_client.notify_add(GCONF_PREFIX + 'visible', self.__sync_visible_mode)
        self.__sync_visible_mode()
        
    @log_except(_logger)
    def __initial_appearance(self):
        ## This function is where we show the canvas internally; we only want this to 
        ## happen after we've loaded information intially to avoid showing a partially-loaded
        ## state.
        self._canvas.show() 
        self.__queue_strut()
        self.__idle_show_we_exist()
        
    @log_except()
    def __on_session_idle_changed(self, isidle):
        if not isidle:
            self.__idle_show_we_exist()

    def __on_header_buttonpress(self, box, e):
        _logger.debug("got shell header click: %s %s %s", e, e.button, e.modifiers)
        if e.button == 2:
            self.Shell()

    @log_except()
    def __idle_show_we_exist(self):
        _logger.debug("showing we exist")
        self.__enter_popped_out_state()
        self.__leave_popped_out_state()

    @log_except()
    def __on_focus(self):
        _logger.debug("got focus keypress")
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        ts = bigboard.keybinder.tomboy_keybinder_get_current_event_time()
        if vis:
            self.__do_focus_search(ts)
        else:
            self.toggle_popout(ts)

    def __append_metainfo(self, metainfo, **kwargs):
        try:
            exchange = self._exchanges[metainfo.srcurl]
        except KeyError, e:
            exchange = self.__stock_manager.render(metainfo, panel=self, **kwargs)
            _logger.debug("rendered %s: %s", metainfo.srcurl, exchange)
            if exchange:
                self._exchanges[metainfo.srcurl] = exchange
        if not exchange:
            _logger.debug("failed to load stock from %s", metainfo.srcurl)
            return
        _logger.debug("adding stock %s", exchange)
        self._stocks_box.append(exchange)
        
    @log_except(_logger)
    def __sync_listing(self):
        _logger.debug("doing stock listing sync")
        new_listed = list(self.__stock_manager.get_listed())
        new_listed_srcurls = map(lambda mi: mi.srcurl, new_listed)
        for exchange in list(self._stocks_box.get_children()):
            if exchange.get_metainfo().srcurl in self.__hardcoded_stocks:
                continue

            _logger.debug("unrendering %s", exchange)
            
            self._stocks_box.remove(exchange)

            if exchange.get_metainfo().srcurl not in new_listed_srcurls:
                _logger.debug("removing %s", exchange)                
                del self._exchanges[exchange.get_metainfo().srcurl]
                exchange.on_delisted()
            
        for metainfo in new_listed:
            self.__append_metainfo(metainfo)
        _logger.debug("done with stock load")            
        
    def get_stock_manager(self):
        return self.__stock_manager
        
    def __get_size(self):
        return Stock.SIZE_BULL

    ## If the user performs an action such as launching an app,
    ## that should close a popped-out sidebar, call this
    def action_taken(self):
        _logger.debug("action taken")
        self.__leave_popped_out_state(immediate=True)
        
    @log_except()
    def __sync_orient(self, *args):
        orient = gconf.client_get_default().get_string(GCONF_PREFIX + 'orientation')
        if not orient:
            orient = 'west'
        if orient.lower() == 'west':
            gravity = gtk.gdk.GRAVITY_WEST
        else:
            gravity = gtk.gdk.GRAVITY_EAST
        self._dw.set_gravity(gravity)
        self.__queue_strut()
        
    @log_except()
    def _toggle_size(self):
        _logger.debug("toggling size")
        expanded = gconf.client_get_default().get_bool(GCONF_PREFIX + 'expand')
        gconf.client_get_default().set_bool(GCONF_PREFIX + 'expand', not expanded)
            
    def _sync_size(self, *args):
        # This function should be deleted basically; we no longer support size changes.
                   
        self._canvas.set_size_request(Stock.SIZE_BULL_CONTENT_PX, 42)
        
        _logger.debug("queuing resize")
        self._dw.queue_resize()  
        _logger.debug("queuing strut")
        self.__queue_strut()
        _logger.debug("queuing strut complete")
        
    def __sync_theme(self, *args):
        theme = self.__theme_mgr.get_theme()
        _logger.debug("syncing with theme %r", theme)
        if self.__compositing:
            self._dw.realize()
            self._dw.set_opacity(1.0)
            self._dw.set_opacity(theme.opacity)        
        self._canvas.modify_bg(gtk.STATE_NORMAL, gtk.gdk.color_parse("#%6X" % (theme.background >> 8,)))
        self._dw.queue_draw_area(0,0,-1,-1)

    def get_theme(self):
        return self.__theme

    @log_except()
    def __idle_do_strut(self):
        _logger.debug("setting strut in idle")
        self._dw.do_set_wm_strut()
        return False

    def __queue_strut(self):
        # TODO - this is kind of gross; we need the strut change to happen after
        # the resize, but that appears to be an ultra-low priority internally
        # so we can't easily queue something directly after.
        call_timeout_once(250, self.__idle_do_strut)
        
    def get_desktop_path(self):
        return DESKTOP_PATH        

    ## There are two aspects to the sidebar state:
    ## the "visible" gconf key is like the old gnome-panel "autohide"
    ## preference. i.e. if !visible, the sidebar is normally collapsed
    ## and you have to use a hotkey or the applet to pop it out.
    ## So the second piece of state is self.__popped_out, which is whether
    ## the sidebar is currently popped out. If visible=True, the sidebar
    ## is always popped out, i.e. self.__popped_out should be True always.

    def __notify_stocks_of_popped_out(self):
        for e in self._exchanges.values():
            e.on_popped_out_changed(self.__popped_out)

    ## Shows the sidebar
    def __enter_popped_out_state(self):
        if not self.__popped_out:
            _logger.debug("popping out")

            self._dw.show()
            # we would prefer to need this, if iconify() worked on dock windows
            #self._dw.deiconify()
            self.__queue_strut()
            self.__popped_out = True

            self.__notify_stocks_of_popped_out()

            self.EmitPoppedOutChanged()

    ## Hides the sidebar, possibly after a delay, only if visible mode is False
    def __leave_popped_out_state(self, immediate=False):
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        if self.__popped_out and not vis and self.__autohide_id == 0:
            _logger.debug("enqueued autohide timeout")            
            self.__autohide_id = gobject.timeout_add(immediate and 1 or 1500, self.__idle_do_hide) 
            
    @log_except()
    def __idle_do_hide(self):
        _logger.debug("in idle hide")
        self.__autohide_id = 0
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        if vis or not self.__popped_out:
            return  

        _logger.debug("unpopping out")
        self.__popped_out = False
        ## would be better to iconify, not hide - hide withdraws the
        ## window, iconify should leave bigboard in the Ctrl+Alt+Tab
        ## order.
        ## Unfortunately, it appears metacity disallows minimize on
        ## dock windows.
        #self._dw.iconify()
        self._dw.hide()
        self.__queue_strut()

        self.__notify_stocks_of_popped_out()        

        self.EmitPoppedOutChanged()

    ## syncs our current state to a change in the gconf setting for visible mode
    @log_except()
    def __sync_visible_mode(self, *args):
        ## unpopout button is only visible if unpopout is allowed
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        self.__unpopout_button.set_visible(not vis)
        
        if vis and not self.__popped_out:
            self.__enter_popped_out_state()
        elif not vis:
            self.__leave_popped_out_state()
            if not gconf.client_get_default().get_bool(GCONF_PREFIX + 'first_time_minimize_seen'):
                dialog = FirstTimeMinimizeDialog(True)
                dialog.show_all()

        ## this is needed because the Sidebar widget knows about the 'visible' gconf key,
        ## and if we're not in visible mode (in autohide mode), it never sets the strut.
        ## However the Sidebar widget does not itself listen for changes on the gconf key.
        self.__queue_strut()
        
    ## Pops out the sidebar, and focuses it (if the sidebar is in visible mode, only has to focus)
    def __do_popout(self, xtimestamp):
        if not self.__popped_out:
            _logger.debug("popout requested")
            self.__enter_popped_out_state()
        self.__do_focus_search(xtimestamp)
            
    def __do_focus_search(self, xtimestamp):
        ## focus even if we were already shown
        _logger.debug("presenting with ts %s", xtimestamp)
        self._dw.present_with_time(xtimestamp)
        self.__search_stock.focus()

    ## Hides the sidebar, only if not in visible mode
    def __do_unpopout(self):
        if self.__popped_out:
            _logger.debug("unpopout requested")
            self.__leave_popped_out_state(True)

    def toggle_popout(self, xtimestamp):
        if self.__popped_out:
            self.__do_unpopout()
        else:
            self.__do_popout(xtimestamp)

    def __set_visible_mode(self, setting):
        vis = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')
        if setting != vis:
            gconf.client_get_default().set_bool(GCONF_PREFIX + 'visible', setting)

    @dbus.service.method(BUS_IFACE_PANEL)
    def EmitPoppedOutChanged(self):
        _logger.debug("got emitPoppedOutChanged method call")        
        self.PoppedOutChanged(self.__popped_out)
        
    @dbus.service.method(BUS_IFACE_PANEL)
    def Popout(self, xtimestamp):
        _logger.debug("got popout method call")
        return self.__do_popout(xtimestamp)

    @dbus.service.method(BUS_IFACE_PANEL)
    def Unpopout(self):
        _logger.debug("got unpopout method call")

        ## force us into autohide mode, since otherwise unpopout would not make sense
        self.__set_visible_mode(False)

        return self.__do_unpopout()

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
        import bigboard.pyshell
        self.__shell = bigboard.pyshell.CommandShell({'panel': self,
                                     '               scratch_window': self.__create_scratch_window},
                                                     savepath=os.path.expanduser('~/.bigboard/pyshell.py'))
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
    def PoppedOutChanged(self, is_popped_out):
        pass

# TODO: figure out an algorithm for removing pixbufs from the cache
_pixbufcache = {}
def load_image_hook(img_name):
    try:
        pixbuf = _pixbufcache[img_name]
    except KeyError, e:
        pixbuf = None
    if not pixbuf:
        if img_name.find(os.sep) >= 0:
            pixbuf = gtk.gdk.pixbuf_new_from_file(img_name)
            _logger.debug("loaded from file '%s': %s" % (img_name,pixbuf))               
        else:
            theme = gtk.icon_theme_get_default()
            pixbuf = theme.load_icon(img_name, 60, gtk.ICON_LOOKUP_USE_BUILTIN)
            _logger.debug("loaded from icon theme '%s': %s" % (img_name,pixbuf))
    _pixbufcache[img_name] = pixbuf        
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

    signal.signal(signal.SIGINT, lambda i,frame: sys.stderr.write('Caught SIGINT\n') or os._exit(0))

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
    
    icon_datadir = None
    for path in _get_datadirs():
        if os.path.isdir(path):
            icon_datadir = path
            break
    if icon_datadir:
        _logger.debug("adding to icon theme path: %s", icon_datadir)
    gtk.icon_theme_get_default().prepend_search_path(icon_datadir)

    bus = dbus.SessionBus() 

    if replace:
        try:
            bb = bus.get_object(BUS_NAME_STR, '/bigboard/panel')
            bb.Kill()
        except dbus.DBusException, e:
            pass
        
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
    
    _logger.debug("Creating panel")
    panel = BigBoardPanel(bus_name)

    bigboard.google.init()
        
    gtk.gdk.threads_enter()
    _logger.debug("Enter mainloop")
    gtk.main()
    gtk.gdk.threads_leave()

    _logger.debug("Exiting BigBoard")
    sys.exit(0)

if __name__ == "__main__":
    main()
