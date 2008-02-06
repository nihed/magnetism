#!/usr/bin/python

def log_except(logger=None, text=''):
    def annotate(func):
        def _exec_cb(*args, **kwargs):
            try:
                return func(*args, **kwargs)
            except:
                log_target = logger or logging
                log_target.exception('Exception in callback%s', text and (': '+text) or '')
        return _exec_cb
    return annotate
