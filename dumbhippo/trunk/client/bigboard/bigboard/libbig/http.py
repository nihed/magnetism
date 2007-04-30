import threading, logging, urllib2, cookielib, sys, urllib
import xml.dom.minidom

import gobject

import bigboard.libbig.xmlquery as xmlquery
import bigboard.libbig.httpcache as httpcache
from bigboard.libbig.singletonmixin import Singleton

class AsyncHTTPFetcher(Singleton):
    """Asynchronously fetch objects over HTTP, invoking
       callbacks using the GLib main loop."""
   
    def __init__(self):
        self.__logger = logging.getLogger("bigboard.AsyncHTTPFetcher")
        
        self.__worker_count = 3
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
       
    def fetch(self, url, cb, errcb, cookies=None, data=None, nocache=False):        
        self.__work_lock.acquire()
        self.__work_queue.append((url, cb, errcb, cookies, data, nocache))
        self.__work_cond.notify()
        self.__work_lock.release()

    def xml_method(self, url, params, cb, normerrcb, errcb):
        formdata = urllib.urlencode(params)
        self.__logger.debug("doing XML method request '%s' params: '%s'", url, formdata)
        self.fetch(url,
                   lambda url, data: self.__handle_xml_method_return(url, data, cb, normerrcb),
                   errcb,
                   cookies=None,
                   data=formdata,
                   nocache=not not params)

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
            (url, cb, errcb, cookies, data, nocache) = self.__work_queue.pop(0)
            self.__work_lock.release()            
            self.__do_fetch(url, cb, errcb, cookies, data, nocache)

    def __do_fetch(self, url, cb, errcb, cookies, data, nocache):
        self.__logger.debug("in thread fetch of %s (%s)" % (url, data))
        try:
            (fname, fdata) = httpcache.load(url, cookies, data=data, nocache=nocache)
            gobject.idle_add(lambda: self.__emit_results(url, data, cb, fname, fdata))
        except Exception, e:
            self.__logger.info("caught error for fetch of %s: %s" % (url, e))
            # in my experience sys.exc_info() is some kind of junk here, while "e" is useful
            gobject.idle_add(lambda: errcb(url, sys.exc_info()) and False)

    def __emit_results(self, url, data, cb, fname, fdata):
        self.__logger.debug("fetch of %s (%s): results %s %s" % (url, data, fname, fdata))
        cb(url, fdata or open(fname, 'rb').read())
    
