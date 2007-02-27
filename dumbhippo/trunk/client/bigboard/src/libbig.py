import os, code, sys, traceback, urllib, logging, StringIO, tempfile

import cairo, gtk, gobject, threading

import hippo

from libgimmie import DockWindow
from singletonmixin import Singleton
import bignative

def set_log_handler(handler):
    bignative.set_log_handler(handler)
    
def get_bigboard_config_file(name):
    basepath = os.path.expanduser("~/.bigboard")
    try:
        os.mkdir(basepath)
    except OSError, e:
        pass
    return os.path.join(basepath, name)
    
class AutoStruct:
    def __init__(self, **kwargs):    
        self._struct_values = {}
        self._struct_values.update(kwargs)
        
    def __getattr__(self, name):
        if name[0:4] == 'get_':
            attr = name[4:] # skip over get_
            return lambda: self._struct_values[attr]
        return None
    
    def _get_keys(self):
        return self._struct_values.keys()
    
    def _get_value(self, name):
        return self._struct_values[name]
    
    def _update(self, args):
        self._struct_values.update(args)
        
    def __str__(self):
        return "autostruct values=%s" % (self._struct_values,)
    
class AutoSignallingStruct(gobject.GObject, AutoStruct):
    __gsignals__ = {
        "changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())        
        }
    
    def __init__(self, **kwargs):
        gobject.GObject.__init__(self)
        AutoStruct.__init__(self, **kwargs)
            
    def update(self, **kwargs):
        changed = False
        for k,v in kwargs.items():
            if self._get_value(k) != v:
                changed = True
        self._update(kwargs)
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
        self._loads[url] = (cb, errcb)
        logging.debug("adding url='%s' to pending loads (%d outstanding)" % (url, len(self._loads.keys())))        
        self._fetcher.fetch(url, self._do_load, self._do_load_error)
        
    def _do_load(self, url, data):
        try:
            # Why doesn't gdk-pixbuf have a sensible _new_from_memory_stream ?
            (tmpfd, tmpf_name) = tempfile.mkstemp()
            tmpf = os.fdopen(tmpfd, 'w')
            tmpf.write(data)
            tmpf.close()
            pixbuf = gtk.gdk.pixbuf_new_from_file(tmpf_name)
            os.unlink(tmpf_name)
            surface = hippo.cairo_surface_from_gdk_pixbuf(pixbuf)
            logging.debug("invoking callback for %s url='%s'" % (self, url))
            self._cache[url] = surface
            self._loads[url][0](url, surface)
        except:
            self._loads[url][1](url, sys.exc_info())
        del self._loads[url]            
        
    def _do_load_error(self, url, exc_info):
        self._loads[url][1](url, exc_info)
        del self._loads[url]        
 