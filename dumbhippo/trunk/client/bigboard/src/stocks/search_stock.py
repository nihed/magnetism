import logging, os

import gobject

try:
    import deskbar, deskbar.ui
    from deskbar.ModuleList import ModuleList
    from deskbar.ModuleLoader import ModuleLoader
    deskbar_available = True    
except:
    deskbar_available = False

import hippo

import identity_spider, mugshot
import libbig
from bigboard import AbstractMugshotStock
from big_widgets import CanvasMugshotURLImage, CanvasEntry

class DeskbarAPI(gobject.GObject):
    """Wrapper class for programatically searching via Deskbar.
       * Hides the difference between sync and async modules - everything is async.
       * Doesn't directly depend on deskbar UI or GConf, though it probably does indirectly.
       * Deals with initializing modules, ignoring disabled ones, etc.  All you have
         to do is ask it to search.
       
       Based on DeskbarApplet.py from deskbar.
    """
    __gsignals__ = {
        "initialized" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
    }    
    
    def __init__(self):
        gobject.GObject.__init__(self)

        self.__logger = logging.getLogger('bigboard.stocks.SearchStock')
        
        self.__module_list = []
        
        self.__initialized_module_count = 0
        self.__loaded_module_count = 0
        
        self.__loader = ModuleLoader(deskbar.MODULES_DIRS)
        self.__loader.connect("modules-loaded", self.__on_modules_loaded)
                        
        self.__loader.connect("module-loaded", self.__on_module_loaded)
        self.__loader.connect("module-initialized", self.__on_module_initialized)
        self.__loader.connect("module-not-initialized", self.__on_module_initialized) # er, what?
        
        self.__enabled_module_names = ['ProgramsHandler', 'FileFolderHandler']
        
        self.__loader.load_all_async()        
        
    def set_enabled_modules(self, enabled):
        self.__enabled_module_names = enabled
    
    def __on_module_initialized(self, sender, context):
        self.__logger.debug("module %s initalized", context)
        self.__initialized_module_count += 1
        if context.module.is_async():
            context.module.connect ('query-ready', 
                                    lambda sender, qstring, matches: self.__handle_matches(self.__filter_matches([(qstring, match) for match in matches])))
        if self.__initialized_module_count == self.__loaded_module_count:
            self.emit("initialized")            
    
    def __get_module_by_name(self, name):
        for module in self.__module_list:
            if module.handler == name:
                return module
        return None
    
    def __on_module_loaded(self, sender, context):
        self.__logger.debug("loaded module %s", context)
        self.__module_list.append(context)
    
    def __on_modules_loaded(self, loader):
       for modname in self.__enabled_module_names:
           module = self.__get_module_by_name(modname)
           self.__logger.debug("async initializing module %s", modname)
           self.__loader.initialize_module_async(module)
           self.__loaded_module_count += 1
    
    def query(self, qstring, cb):
        self.__match_hashes = {}
        self.__qstring = qstring
        self.__cb = cb
        results = []
        for modctx in self.__module_list:
            if not modctx.enabled:
                    continue
            if modctx.module.is_async():
                    modctx.module.query_async(qstring)
            else:
                try:
                    matches = modctx.module.query(qstring)
                except TypeError:
                    matches = modctx.module.query(qstring, deskbar.DEFAULT_RESULTS_PER_HANDLER)               
                results.extend(self.__filter_matches(qstring, matches))
        
        if len(results) > 0:
            gobject.idle_add(lambda: self.__handle_matches(results))
            
    def __filter_matches(self, qstring, matches):
        results = []
        for match in matches:
            text, match = qstring, match
            if type(match) is tuple:
                text, match = match

            hsh = match.get_hash(text)
            if hsh != None:
                if hsh in self.__match_hashes:
                    continue        
                results.append((text,match))
            else:
                results.append((text,match))    
        return results           
            
    def __handle_matches(self, matches):
        self.__cb(self.__qstring, matches)
        
class SearchStock(AbstractMugshotStock):
    """Search.  It's what's for dinner."""
    def __init__(self):
        super(SearchStock,self).__init__("Search", ticker="")

        self.__logger = logging.getLogger('bigboard.stocks.SearchStock')

        if not deskbar_available:
            self.append(hippo.CanvasText(text="Deskbar not installed"))
            return
        
        self.__deskbar = DeskbarAPI()
        
        self.__box = hippo.CanvasBox()
        
        self.__input = CanvasEntry()
        self.__input.connect("notify::text", lambda *args: self.__queue_search())
        
        self.__box.append(self.__input)
        
    def _on_mugshot_initialized(self):
        super(SearchStock, self)._on_mugshot_initialized()
        
    def get_content(self, size):
        return self.__box
    
    def set_size(self, size):
        super(SearchStock, self).set_size(size)
        
    def __queue_search(self):
        text = self.__input.get_property("text")
        self.__logger.debug("initiating query for %s" % (text,))
        self.__deskbar.query(text,
                             self.__handle_result)
        
    def __handle_result(self, qstring, matches):
        self.__logger.debug("got matches for %s: %s", qstring, matches)

