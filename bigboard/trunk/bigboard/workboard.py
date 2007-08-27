import os, sys, logging, weakref

import gobject

from bigboard.libbig.singletonmixin import Singleton
from bigboard.libbig.logutil import log_except

_logger = logging.getLogger("bigboard.WorkBoard")

class WorkBoard(Singleton):
    """A decoupled method invocation facility, limited to in-process calls.
    Use this if you need one class to asynchronously invoke a method on another without
    any knowledge of each other's names or order of construction.  To emulate return values,
    pass a callback function in the arguments."""
    __queue = []
    __observers = []
    
    def append(self, name, *args, **kwargs):
        _logger.debug("appending message name='%s' args='%s', kwargs='%s'", name, args, kwargs)
        self.__queue.append((name, args, kwargs))
        self.__signal()
        
    def observe(self, func, *names):
        _logger.debug("adding observer for names='%s': %s", names, func)        
        self.__observers.append((names, weakref.ref(func)))
        self.__signal()
        
    @log_except(_logger)
    def __emit_observer(self, argdata):             
        (func, args, kwargs) = argdata
        _logger.debug("Idle calling %s, %d args, %d kwargs", func, len(args), len(kwargs))             
        func(*args, **kwargs)
        return False
        
    def __signal(self):
        if len(self.__queue) == 0:
            return
        deleted = set()
        observers_deleted = set()
        for i,(onames, funcref) in enumerate(self.__observers):
            for j,(msg, args, kwargs) in enumerate(self.__queue):
                if j in deleted:
                    continue
                if msg in onames:
                    func = funcref()
                    if func:
                        _logger.debug("Giving msg %s to %s", msg, func)                             
                        gobject.idle_add(self.__emit_observer, (func, args, kwargs))
                        deleted.add(j)
                    else:
                        observers_deleted.add(i)
        for i,delidx in enumerate(deleted):
            del self.__queue[delidx-i]
        for i,delidx in enumerate(observers_deleted):
            del self.__observers[delidx-i]
        _logger.debug("Processing complete, %d messages remain and %d observers", len(self.__queue), len(self.__observers))
           