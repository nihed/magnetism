import os, logging

import gnome, hippo

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
