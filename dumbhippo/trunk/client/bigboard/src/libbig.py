import os, code, sys, traceback, urllib2, logging, logging.config, StringIO, cookielib
import re, tempfile, xml.dom.minidom, urlparse, threading

import cairo, gtk, gobject
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

def set_application_name(name):
    bignative.set_application_name(name)

def set_program_name(name):
    bignative.set_program_name(name)

def gnome_keyring_find_items_sync(type, attributes):
    return bignative.keyring_find_items_sync(type, attributes)
    
def get_bigboard_config_file(name):
    basepath = os.path.expanduser("~/.bigboard")
    try:
        os.mkdir(basepath)
    except OSError, e:
        pass
    return os.path.join(basepath, name)

def get_attr_or_none(dict, attr):
    if dict.has_key(attr):
        return dict[attr]
    else:
        return None
    
_stud_re = re.compile(r'[A-Z]\w')
def studly_to_underscore(str):
    match = _stud_re.search(str)
    result = ''
    while match:
        prev = str[:match.start()]
        result += prev
        if prev != '':
            result += '_'
        result += match.group().lower()
        str = str[match.end():]
        match = _stud_re.search(str)
    result += str
    return result
    
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

# Taken from http://www.python.org/dev/peps/pep-0318/
def singleton(cls):
    instances = {}
    def getinstance():
        if cls not in instances:
            instances[cls] = cls()
        return instances[cls]
    return getinstance

class BiMap(object):
    def __init__(self, a_name, b_name, initval):
        self.__a_to_b = initval
        self.__b_to_a = {}
        self.__a_name = a_name
        self.__b_name = b_name
        for k, v in initval.iteritems():
            self.__b_to_a[v] = k
    
    def __getitem__(self, key):
        if key == self.__a_name:
            return self.__a_to_b
        elif key == self.__b_name:
            return self.__b_to_a
        else:
            raise ValueError("Unknown bimap set name %s" % (key,))    
        
def _traverse_nodes(node, matches, index): 
    gather = index+1 == len(matches)
    gather_result = []
    if node.nodeType == xml.dom.Node.ELEMENT_NODE:
        for subnode in node.childNodes:
            if subnode.nodeType == xml.dom.Node.ELEMENT_NODE and subnode.tagName == matches[index]:
                if gather:
                    gather_result.append(subnode)
                else:
                    _traverse_nodes(subnode, matches, index+1)
    if not gather:
        raise KeyError("Couldn't find path %s from node %s" % ('/'.join(matches), node))
    return gather_result
        
def xml_query(node, *queries):
    """A braindead-simple xpath-like minilanguage for querying an xml node.
    Examples:
    
    foo/bar/baz*    Returns all baz subelements of foo/bar
    foo/bar/baz     Returns the first baz subelement of foo/bar
    \@date          Returns the date attribute of the current node
    bar#            Returns the text content the first node named bar
    baz\@moo        Returns the moo attribute of the first node named baz
    
    A query may also be a tuple of (querystr, conversion_func).  So for example:
    
    (timestamp, link, contents) = xml_query(node, ("\@timestamp", int), "\@link", "content#")"""
    
    def gather_node_value(node):
        result = ""
        for subnode in node.childNodes:
            if subnode.nodeType == xml.dom.Node.TEXT_NODE:
                result += subnode.nodeValue
        return result
    
    def first_or_lose(query, elt_name, nodeset):
        if len(nodeset) == 0:
            raise KeyError("Couldn't find element %s from query %s" % (elt_name, query))   
        return nodeset[0]
    
    results = []
    for query in queries:
        conversion_func = lambda x: x
        if type(query) == tuple:
            (query_str, conversion_func) = query
        else:
            query_str = query
            
        query_components = query_str.split('/')
        last_query = query_components[-1]
        
        get_first_element_func = lambda results: first_or_lose(query_str, last_query, results)
        
        attr_pos = last_query.find('@')
        hash_pos = last_query.find('#')
        star_pos = last_query.find('*')
        if attr_pos > 0:
            query_func = lambda results: get_first_element_func(results).getAttribute(last_query[attr_pos+1:])
            last_query = last_query[:attr_pos]            
        elif hash_pos > 0:
            query_func = lambda results: gather_node_value(get_first_element_func(results))
            last_query = last_query[:hash_pos]
        elif star_pos > 0:
            query_func = lambda results: results
            last_query = last_query[:star_pos]
        else:
            query_func = get_first_element_func
        node_matches = query_components[:-1]
        node_matches.append(last_query)
        resultset = _traverse_nodes(node, node_matches, 0)
        result = conversion_func(query_func(resultset))      
        results.append(result)
    if len(results) == 1:
        return results[0]
    else:
        return results

def xml_gather_attrs(node, attrlist):
    attrs = {}
    for attr in attrlist:
        attrs[attr] = node.getAttribute(attr)
        if not attrs[attr]:
            raise KeyError("Failed to find attribute %s of node %s" %(attr, node))
    return attrs    
        
def get_xml_element(node, path):
    """Traverse a path like foo/bar/baz from a DOM node, using the first matching
    element."""
    return xml_query(node, path)

def get_xml_element_value(node, path):
    return xml_query(node, path+"#")
    
class AutoStruct(object):
    """Kind of like a dictionary, except the values are accessed using
    normal method calls, i.e. get_VALUE(), and the keys are determined
    by arguments passed to the constructor (and are immutable thereafter).
    
    Dictionary keys should be alphanumeric.  Transformation rules are
    applied to make the key more friendly to the get_VALUE syntax.  
    First, hyphens (-) are transformed to underscore (_).  Second,
    studlyCaps style names are replaced by their underscored versions;
    e.g. fooBarBaz is transformed to foo_bar_baz.    
    """
    def __init__(self, values):    
        self._struct_values = {}
        self._struct_values.update(self._transform_values(values))
        
    def __getattribute__(self, name):
        try:
            return object.__getattribute__(self, name)
        except AttributeError:
            if name[0:4] == 'get_':
                attr = name[4:] # skip over get_
                return lambda: get_attr_or_none(self._struct_values, attr)
            raise AttributeError,name
    
    def _get_keys(self):
        return self._struct_values.keys()
    
    def _get_value(self, name):
        return self._struct_values[name]
    
    def _transform_values(self, values):
        temp_args = {}
        for k,v in values.items():
            if type(k) == unicode:
                k = str(k)
            k = k.replace('-','_')
            k = studly_to_underscore(k)
            temp_args[k] = v    
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
        values = self._transform_values(values)
        for k,v in values.items():
            # FIXME use deep comparison here
            if self._get_value(k) != v:
                changed = True
        AutoStruct.update(self, values)
        if changed:
            self.emit("changed")    
    
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
    
class URLImageCache(Singleton):
    def __init__(self):
        self._cache = {}
        self._loads = {}
        self.__logger = logging.getLogger('bigboard.URLImageCache')
        self._fetcher = AsyncHTTPFetcher.getInstance()
    
    def get(self, url, cb, errcb):
        if self._cache.has_key(url): # TODO expire
            surface = self._cache[url]
            cb(url, surface)
            return

        cbdata = (cb, errcb)
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
            surface = hippo.cairo_surface_from_gdk_pixbuf(pixbuf)
            self.__logger.debug("invoking callback for %s url='%s'" % (self, url))
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
 

def init_logging(default_level, debug_modules):
    
    logging_config = StringIO.StringIO()
    logging_config.write("""
[loggers]
keys=%s

""" % (','.join(['root'] + debug_modules)))
    logging_config.write("""    
[formatters]
keys=base

[handlers]
keys=stderr
    
[formatter_base]
class=logging.Formatter
format="%%(asctime)s [%%(thread)d] %%(name)s %%(levelname)s %%(message)s"
datefmt=%%H:%%M:%%S

[handler_stderr]
class=StreamHandler
level=NOTSET
formatter=base
args=(sys.stderr,)

[logger_root]
level=%s
handlers=stderr

        """ % (default_level,))
    for module in debug_modules:
        logging_config.write("""
[logger_%s]
level=DEBUG
handlers=stderr
propagate=0
qualname=bigboard.%s

        """ % (module,module)
        )
    logging.config.fileConfig(StringIO.StringIO(logging_config.getvalue()))
    
    logging.debug("Initialized logging")
    
if __name__ == '__main__':
    a = AutoSignallingStruct({'foo': 10, 'bar': 20})
    assert(a.get_foo() == 10)
    assert(a.get_bar() == 20)
    try:
        v = a.get_baz()
        assert(True)
    except AttributeError:
        assert(False)
    
    class Bar(AutoSignallingStruct):
        def __init__(self, *args, **kwargs):
            super(Bar, self).__init__(*args, **kwargs)
            
        def get_baz(self):
            return 30
        
    b = Bar({'foo': 10, 'bar': 20})
    assert(b.get_baz() == 30)
