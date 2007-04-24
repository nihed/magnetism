import threading, logging, urllib2, cookielib, sys, urllib
import xml.dom.minidom

import gobject

import libbig.xmlquery
from singletonmixin import Singleton

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
       
    def fetch(self, url, cb, errcb, cookies=None, data=None):        
        self.__work_lock.acquire()
        self.__work_queue.append((url, cb, errcb, cookies, data))
        self.__work_cond.notify()
        self.__work_lock.release()

    def xml_method(self, url, params, cb, normerrcb, errcb):
        formdata = urllib.urlencode(params)
        self.__logger.debug("doing XML method request '%s' params: '%s'", url, formdata)
        self.fetch(url,
                   lambda url, data: self.__handle_xml_method_return(url, data, cb, normerrcb),
                   errcb, params)

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
            errnode = libbig.xmlquery.query(resp, 'err')
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
            (url, cb, errcb, cookies, data) = self.__work_queue.pop(0)
            self.__work_lock.release()            
            self.__do_fetch(url, cb, errcb, cookies, data)

    def __do_fetch(self, url, cb, errcb, cookies, data):
        self.__logger.debug("in thread fetch of %s" % (url,))
        try:
            request = urllib2.Request(url, data)
            # set our cookies
            if cookies:
                for c in cookies:
                    header = c[0] + "=" + c[1] # oddly, apparently there's no escaping here
                    request.add_header("Cookie", header)
            # this cookie stuff is an attempt to be sure we use Set-Cookie cookies during this request,
            # e.g. JSESSIONID, but not sure it's needed/correct
            cj = cookielib.CookieJar()
            opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(cookiejar=cj))
            data = opener.open(request).read()
            gobject.idle_add(lambda: cb(url, data) and False)
        except Exception, e:
            self.__logger.error("caught error for fetch of %s: %s" % (url, e))
            # in my experience sys.exc_info() is some kind of junk here, while "e" is useful
            gobject.idle_add(lambda: errcb(url, sys.exc_info()) and False)
    