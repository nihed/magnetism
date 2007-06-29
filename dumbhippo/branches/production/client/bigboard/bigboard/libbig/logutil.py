import logging, logging.config, StringIO

def log_except(logger=None, text='Caught exception in callback'):
    def annotate(func):
        def _exec_cb(*args, **kwargs):
            try:
                return func(*args, **kwargs)
            except:
                log_target = logger or logging
                log_target.exception(text)
        return _exec_cb
    return annotate

def init(default_level, debug_modules, prefix=None):
    
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
qualname=%s%s

        """ % (module, prefix or "",module)
        )
    logging.config.fileConfig(StringIO.StringIO(logging_config.getvalue()))
    
    logging.debug("Initialized logging")
