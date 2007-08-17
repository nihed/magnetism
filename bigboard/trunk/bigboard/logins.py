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
        g = bigboard.google.get_google()
        google = g.connect('auth', self.__on_google_auth)
        self.__on_google_auth(g, g.have_auth())
        
    def __on_google_auth(self, g, isauth):
        if isauth:
            self.accounts['gmail.com'] = g.get_auth()
        else:
            if self.accounts.has_key('gmail.com'):
                del self.accounts['gmail.com']
        
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