import sys

import gobject

import bigboard.google

class LoginSystem(gobject.GObject):
    __gsignals__ = {
        "changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),     
    }
    
    def __init__(self):
        super(LoginSystem, self).__init__()
        self.accounts = {}
        googles = bigboard.google.get_googles()
        for g in googles:
            g.connect('auth', self.__on_google_auth)
            self.__on_google_auth(g, g.have_auth())
        
    def __on_google_auth(self, g, isauth):
        if isauth:
            self.accounts[g.get_storage_key()] = g.get_auth()
        else:
            if self.accounts.has_key(g.get_storage_key()):
                del self.accounts[g.get_storage_key()]
        
    def get_login(self, name):
        return self.accounts[name]
        
    def iter_logins(self):
        return self.accounts.iteritems() 
    
_singleton = None
def get_logins():
    global _singleton
    if _singleton is None:
        _singleton = LoginSystem()
    return _singleton
