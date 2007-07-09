import os,sys
import threading, logging, urllib2, cookielib, urllib, StringIO
import xml.dom.minidom

import gobject

import bigboard.libbig.xmlquery as xmlquery
from bigboard.libbig.singletonmixin import Singleton
import bigboard.httplib2 as httplib2

class ThreadsafeFileCache(httplib2.FileCache):
    __lock = threading.Lock()
    
    def get(self, *args, **kwargs):
        self.__lock.acquire()
        try:
            return httplib2.FileCache.get(self, *args, **kwargs)
        finally:
            self.__lock.release()
        
    def set(self, *args, **kwargs):
        self.__lock.acquire()
        try:
            return httplib2.FileCache.set(self, *args, **kwargs)
        finally:
            self.__lock.release()
        
    def delete(self, *args, **kwargs):
        self.__lock.acquire()
        try:
            return httplib2.FileCache.delete(self, *args, **kwargs)
        finally:
            self.__lock.release()
        
class HomeDirCache(ThreadsafeFileCache, Singleton):
    def __init__(self):
        path = os.path.expanduser('~/.bigboard/httpcache')
        try:
            os.makedirs(path)
        except OSError, e:
            pass
        httplib2.FileCache.__init__(self, path)
_cache = HomeDirCache()

class AsyncHTTPFetcher(Singleton):
    """Asynchronously fetch objects over HTTP, invoking
       callbacks using the GLib main loop."""
   
    def __init__(self):
        self.__logger = logging.getLogger("bigboard.AsyncHTTPFetcher")
        
        self.__worker_count = 6
        self.__work_lock = threading.RLock()
        self.__work_cond = threading.Condition(self.__work_lock)
        self.__work_queue = [] # <(str,func,func,cookies)>
        self.__workers = set() # <Thread>
        
        for i in range(self.__worker_count):
            self.__logger.debug('creating http worker thread')            
            t = threading.Thread(target=self.__worker, name="AsyncHTTPWorker%d"%(i,))
            self.__workers.add(t)
            t.setDaemon(True)
            t.start()        
       
    def fetch(self, url, cb, errcb, data=None, cookies=None):
        self.__work_lock.acquire()
        self.__work_queue.append((url, cb, errcb, data, cookies))
        self.__work_cond.notify()
        self.__work_lock.release()

    def xml_method(self, url, params, cb, normerrcb, errcb):
        formdata = urllib.urlencode(params)
        self.__logger.debug("doing XML method request '%s' params: '%s'", url, formdata)
        self.fetch(url,
                   lambda url, data: self.__handle_xml_method_return(url, data, cb, normerrcb),
                   errcb,
                   data=formdata)

    def __handle_xml_method_return(self, url, data, cb, normerrcb):
        doc = xml.dom.minidom.parseString(data) 
        resp = doc.documentElement
        stat = resp.getAttribute("stat")
        if stat == 'ok':
            if cb:
                self.__logger.debug("got XML method %s return ok", url)
                cb(url, resp.childNodes)
        else:
            self.__logger.debug("got XML method %s error")
            errnode = xmlquery.query(resp, 'err')
            code = errnode.getAttribute('code') or 'red'
            msg = errnode.getAttribute('msg') or 'Unknown error'
            self.__logger.debug("got XML method %s error %s %s", url, code, msg)
            if normerrcb:
                normerrcb(url, code, msg)
        
    def __worker(self):
        while True:
            self.__work_lock.acquire()
            while len(self.__work_queue) == 0:
                self.__work_cond.wait()
            args = self.__work_queue.pop(0)
            self.__work_lock.release()            
            self.__do_fetch(*args)

    def __do_fetch(self, url, cb, errcb, data, cookies):
        self.__logger.debug("in thread fetch of %s (%s)" % (url, data))
        h = httplib2.Http(cache=_cache)
        kwargs = {}
        if data:
            kwargs['method'] = 'POST'
            kwargs['body'] = data
        if cookies:
            headers={}
            kwargs['headers'] = headers
            # oddly, apparently there's no escaping here
            cookie_str = ','.join(["%s=%s" % x for x in cookies])
            headers['Cookie'] = cookie_str       
        (response, content) = h.request(url, **kwargs)
        if response.status == 200:
            gobject.idle_add(lambda: self.__emit_results(url, data, cb, content))
        else:
            self.__logger.info("caught error for fetch of %s (status: %s)" % (url, response.status))
            # in my experience sys.exc_info() is some kind of junk here, while "e" is useful
            gobject.idle_add(lambda: errcb(url, sys.exc_info()) and False)

    def __emit_results(self, url, data, cb, fdata):
        self.__logger.debug("fetch of %s complete with %d bytes" % (url,len(fdata)))
        cb(url, fdata)
    
