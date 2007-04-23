import threading, logging, urllib2, cookielib, sys

import gobject

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
       
    def fetch(self, url, cb, errcb, cookies=None):        
        self.__work_lock.acquire()
        self.__work_queue.append((url, cb, errcb, cookies))
        self.__work_cond.notify()
        self.__work_lock.release()        
        
    def __worker(self):
        while True:
            self.__work_lock.acquire()
            while len(self.__work_queue) == 0:
                self.__work_cond.wait()
            (url, cb, errcb, cookies) = self.__work_queue.pop(0)
            self.__work_lock.release()            
            self.__do_fetch(url, cb, errcb, cookies)

    def __do_fetch(self, url, cb, errcb, cookies):
        self.__logger.debug("in thread fetch of %s" % (url,))
        try:
            request = urllib2.Request(url)
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
    
