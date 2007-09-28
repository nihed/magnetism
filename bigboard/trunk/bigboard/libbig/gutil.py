import os, sys, logging, weakref, functools

import gobject

def _run_logging(f, logger, *args):
    try:    
        f(*args)
    except:
        logger.exception('Exception in idle')        
    return False

def call_timeout(timeout, func, *args, **kwargs):
    if 'logger' in kwargs:
        logger = kwargs['logger']
        del kwargs['logger']
    else:
        logger = logging
    sys.stderr.write("idle call t:'%s' f:'%s' l:'%s' a:'%s' k:'%s'\n" % (timeout, func, logger, args, kwargs))
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

__all__ = ['call_timeout', 'call_idle', 'call_timeout_once', 'call_idle_once']
