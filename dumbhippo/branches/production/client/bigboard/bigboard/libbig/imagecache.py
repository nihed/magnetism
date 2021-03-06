import sys,os,logging

import gtk, hippo

from bigboard.libbig.http import AsyncHTTPFetcher
from bigboard.libbig.singletonmixin import Singleton

class URLImageCache(Singleton):
    def __init__(self):
        self._cache = {}
        self._loads = {}
        self.__logger = logging.getLogger('bigboard.URLImageCache')
        self._fetcher = AsyncHTTPFetcher.getInstance()
    
    def get(self, url, cb, errcb, format='surface'):
        if self._cache.has_key(url): # TODO expire
            pixbuf = self._cache[url]
            cb(url, format == 'surface' and hippo.cairo_surface_from_gdk_pixbuf(pixbuf) or pixbuf)
            return

        cbdata = (cb, errcb, format)
        if self._loads.has_key(url):
            self._loads[url].append(cbdata)
        else:
            self._loads[url] = [cbdata]
            self.__logger.debug("adding url='%s' to pending loads (%d outstanding)" % (url, len(self._loads.keys())))        
            self._fetcher.fetch(url, self._do_load, self._do_load_error)
        
    def _do_load(self, url, data):
        try:
            loader = gtk.gdk.PixbufLoader()
            # the write and close can both throw
            loader.write(data)
            loader.close()            
            pixbuf = loader.get_pixbuf()
            self.__logger.debug("invoking callback for %s url='%s'" % (self, url))
            self._cache[url] = pixbuf
            for cb, errcb, fmt in self._loads[url]:
                cb(url, fmt == 'surface' and hippo.cairo_surface_from_gdk_pixbuf(pixbuf) or pixbuf)
        except:
            for cb, errcb, fmt in self._loads[url]:
                errcb(url, sys.exc_info())
        del self._loads[url]            
        
    def _do_load_error(self, url, exc_info):
        for cb,errcb in self._loads[url]:
            errcb(url, exc_info)
        del self._loads[url]        
