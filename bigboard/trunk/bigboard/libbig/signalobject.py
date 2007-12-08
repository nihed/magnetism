import weakref

## Adapted from:
## http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/87056
## by Patrick O'Brien, under the Python License

class SignalObject(object):
    """An object which can emit signals, similar in style
    to GObject but usable as a non-meta class.  Not threadsafe."""
    def __init__(self):
        self.__weakmethods = weakref.WeakKeyDictionary()
        self.__signal_handlers = {}
        
    def connect(self, signame, handler, *args):
        if signame not in self.__class__.pysignals:
            raise ValueError("Unknown signal %s", signame)
        try:
            handlers = self.__signal_handlers.get(signame)
        except KeyError, e:
            handlers = []
            self.__signal_handlers[signame] = handlers
        funcref = self.__get_method_ref(handler)
        argref = weakref.ref(args)
        handlers.append((funcref, argref))
        
    def emit(self, signame, *args):
        dead = []
        handlers = self.__signal_handlers.get(signame, [])
        for funcref, argref in handlers:
            func = funcref()
            fargs = argref()
            if (func is None) or (fargs is None):
                dead.append((funcref, argref))
                continue
            func(self, *(args + fargs))
        for pair in dead:
            handlers.remove(pair)

    def __get_method_ref(self, obj):
        """Return a *safe* weak reference to a callable object."""
        if hasattr(object, 'im_self'):
            if object.im_self is not None:
                # Turn a bound method into a BoundMethodWeakref instance.
                # Keep track of these instances for lookup by disconnect().
                selfkey = object.im_self
                funckey = object.im_func
                if selfkey not in self.__weakmethods:
                    self.__weakmethods[selfkey] = weakref.WeakKeyDictionary()
                if funckey not in self.__weakmethods[selfkey]:
                    self.__weakmethods[selfkey][funckey] = BoundMethodWeakref(boundMethod=obj)
                return self.__weakmethods[selfkey][funckey]
        return weakref.ref(object)

class BoundMethodWeakref:
    """BoundMethodWeakref class."""
    def __init__(self, boundMethod):
        """Return a weak-reference-like instance for a bound method."""
        self.isDead = False
        self.weakSelf = weakref.ref(boundMethod.im_self, self.__set_dead)
        self.weakFunc = weakref.ref(boundMethod.im_func, self.__set_dead)
    
    def __set_dead(self):
        self.isDead = True    
    
    def __repr__(self):
        """Return the closest representation."""
        return repr(self.weakFunc)
    def __call__(self):
        """Return a strong reference to the bound method."""
        if self.isDead:
            return None
        else:
            object = self.weakSelf()
            method = self.weakFunc().__name__
            return getattr(object, method)
 
__all__ = ['SignalObject']
