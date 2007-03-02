import os, code, sys, traceback, urllib, logging, StringIO, tempfile

import cairo, gtk, gobject, threading
import gnome

import hippo

from libgimmie import DockWindow
from singletonmixin import Singleton
import bignative

def run_program(name, args):
    pid = os.fork()
    if pid == 0:
        os.execvp(name, [name] + args)
        os._exit(0)
        
def show_url(url):
    gnome.url_show(url)

def set_log_handler(handler):
    bignative.set_log_handler(handler)
    
def get_bigboard_config_file(name):
    basepath = os.path.expanduser("~/.bigboard")
    try:
        os.mkdir(basepath)
    except OSError, e:
        pass
    return os.path.join(basepath, name)

def snarf_attributes_from_xml_node(node, attrlist):
    attrs = {}
    for attr in attrlist:
        attrs[attr] = node.getAttribute(attr)
    return attrs

def get_attr_or_none(dict, attr):
    if dict.has_key(attr):
        return dict[attr]
    else:
        return None
    
def _log_cb(func, errtext=None):
    """Wraps callbacks in a function that catches exceptions and logs them
    to the logging system."""
    def exec_cb(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except:
            if errtext:
                text = errtext
            else:
                text = "Caught exception in callback"
            logging.exception(text)
    return exec_cb
    
class AutoStruct:
    """Kind of like a dictionary, except the values are accessed using
    normal method calls, i.e. get_VALUE(), and the keys are determined
    by arguments passed to the constructor (and are immutable thereafter).
    
    Dictionary keys should be alphanumeric, with the addition that 
    hyphens (-) are transformed to underscore (_).
    """
    def __init__(self, values):    
        self._struct_values = {}
        self._struct_values.update(self._transform_values(values))
        
    def __getattr__(self, name):
        if name[0:4] == 'get_':
            attr = name[4:] # skip over get_
            return lambda: get_attr_or_none(self._struct_values, attr)
        return None
    
    def _get_keys(self):
        return self._struct_values.keys()
    
    def _get_value(self, name):
        return self._struct_values[name]
    
    def _transform_values(self, values):
        temp_args = {}
        for k,v in values.items():
            if type(k) == unicode:
                k = str(k)
            temp_args[k.replace('-','_')] = v    
        return temp_args
    
    def update(self, values):
        for k in values.keys():
            if not self._struct_values.has_key(k):
                raise Exception("Unknown key '%s' added to %s" % (k, self))
        self._struct_values.update(self._transform_values(values))
        
    def __str__(self):
        return "autostruct values=%s" % (self._struct_values,)
    
class AutoSignallingStruct(gobject.GObject, AutoStruct):
    """An AutoStruct that also emits a "changed" signal when
    its values change."""
    __gsignals__ = {
        "changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())        
        }
    
    def __init__(self, values):
        gobject.GObject.__init__(self)
        AutoStruct.__init__(self, values)
            
    def update(self, values):
        changed = False
        for k,v in values.items():
            if self._get_value(k) != v:
                changed = True
        self.update(values)
        if changed:
            self.emit("changed")    
    
class AsyncHTTPFetcher:
    """Asynchronously fetch objects over HTTP, invoking
       callbacks using the GLib main loop."""
    def fetch(self, url, cb, errcb):
        logging.debug('creating async HTTP request thread for %s' % (url,))
        thread = threading.Thread(target=self._do_fetch, name="AsyncHTTPFetch", args=(url, cb, errcb))
        thread.setDaemon(True)
        thread.start()
        
    def _do_fetch(self, url, cb, errcb):
        logging.debug("in thread fetch of %s" % (url,))
        try:
            data = urllib.urlopen(url).read()
            gobject.idle_add(lambda: cb(url, data) and False)
        except:
            logging.debug("caught error for fetch of %s: %s" % (url, sys.exc_info()))
            gobject.idle_add(lambda: errcb(url, sys.exc_info()) and False)
    
class URLImageCache(Singleton):
    def __init__(self):
        self._cache = {}
        self._loads = {}
        self._fetcher = AsyncHTTPFetcher()
    
    def get(self, url, cb, errcb):
        if self._cache.has_key(url): # TODO expire
            return self._cache[url]
        cbdata = (cb, errcb)
        if self._loads.has_key(url):
            self._loads[url].append(cbdata)
        else:
            self._loads[url] = [cbdata]
            logging.debug("adding url='%s' to pending loads (%d outstanding)" % (url, len(self._loads.keys())))        
            self._fetcher.fetch(url, self._do_load, self._do_load_error)

    def _pixbuf_from_data(self, data):

        return loader.get_pixbuf()
        
    def _do_load(self, url, data):
        try:
            loader = gtk.gdk.PixbufLoader()
            # the write and close can both throw
            loader.write(data)
            loader.close()            
            pixbuf = loader.get_pixbuf()
            surface = hippo.cairo_surface_from_gdk_pixbuf(pixbuf)
            logging.debug("invoking callback for %s url='%s'" % (self, url))
            self._cache[url] = surface
            for cb, errcb in self._loads[url]:
                cb(url, surface)
        except:
            for cb, errcb in self._loads[url]:
                errcb(url, sys.exc_info())
        del self._loads[url]            
        
    def _do_load_error(self, url, exc_info):
        for cb,errcb in self._loads[url]:
            errcb(url, exc_info)
        del self._loads[url]        
 
