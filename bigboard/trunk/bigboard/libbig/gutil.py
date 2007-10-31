import os, sys, logging, weakref, functools
from StringIO import StringIO

import gobject

def _run_logging(f, logger, *args):
    try:    
        f(*args)
    except:
        logger.exception('Exception in idle')        
    return False

class DisconnectSet(object):
    def __init__(self):
        super(DisconnectSet, self).__init__()
        self.__connections = set()

    def add(self, object, id):
        self.__connections.add((object, id))

    def disconnect_all(self):
        for (object, id) in self.__connections:
            object.disconnect(id)

def call_timeout(timeout, func, *args, **kwargs):
    if 'logger' in kwargs:
        logger = kwargs['logger']
        del kwargs['logger']
    else:
        logger = logging
    return gobject.timeout_add(timeout, functools.partial(_run_logging, func, logger, *args), **kwargs)

def call_idle(func, *args, **kwargs):
    return call_timeout(0, func, *args, **kwargs)

_global_call_once_funcs = {}
def _run_removing_from_call_once(f):
    try:
        f()
    finally:
        del _global_call_once_funcs[f]
    
def call_timeout_once(timeout, func, **kwargs):
    """Call given func exactly once in the next idle time; if func is already pending,
    it will not be queued multiple times."""

    if func in _global_call_once_funcs:
        return
    id = call_timeout(timeout, _run_removing_from_call_once, func, **kwargs)
    _global_call_once_funcs[func] = id
    return id
    
def call_idle_once(func, **kwargs):
    return call_timeout_once(0, func, **kwargs)

def defer_idle_func(timeout=100, **kwargs):
    def wrapped(f):
        return lambda *margs: call_timeout_once(timeout, functools.partial(f, *margs), **kwargs)
    return wrapped

def read_subprocess_idle(args, cb):
    import subprocess
    subp = subprocess.Popen(args, stdout=subprocess.PIPE, close_fds=True)
    buf = StringIO()
    watchid = None
    def handle_data_avail(src, condition):
        if (condition & gobject.IO_IN):
            buf.write(os.read(src, 8192))
        if ((condition & gobject.IO_HUP) or (condition & gobject.IO_ERR)):
            cb(buf.getvalue())
            os.close(src)
            subp.wait()
            gobject.source_remove(watchid)
            return False
        return True
    watchid = gobject.io_add_watch(subp.stdout.fileno(), gobject.IO_IN | gobject.IO_ERR | gobject.IO_HUP, handle_data_avail)
    return watchid

__all__ = ['call_timeout', 'call_idle', 'call_timeout_once', 'call_idle_once', 'defer_idle_func', 'read_subprocess_idle', 'DisconnectSet']
