import os, logging

import gnome, hippo

def run_program(name, args):
    pid = os.fork()
    if pid == 0:
        os.execvp(name, [name] + args)
        os._exit(0)
        
def show_url(url):
    gnome.url_show(url)

def get_bigboard_config_file(name):
    basepath = os.path.expanduser("~/.bigboard")
    try:
        os.mkdir(basepath)
    except OSError, e:
        pass
    return os.path.join(basepath, name)

def get_xdg_datadirs():
    result = []
    default = '/usr/share'
    try:
        for x in os.environ['XDG_DATA_DIRS'].split(':'):
            result.append(x)
    except KeyError, e:
        pass
    result.append(default)
    return result

def _log_cb(func, errtext=None):
    """Wraps callbacks in a function that catches exceptions and logs them
    to the logging system."""
    def exec_cb(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except:
            if errtext:
                logging.exception(text)
            else:
                logging.exception("Caught exception in callback %s", func)
    return exec_cb

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
