import logging, gnomekeyring

try:
    import bigboard.bignative as bignative
except:
    import bignative

_logger = logging.getLogger("bigboard.Keyring")

class KeyringItem(object):
    def __init__(self, kind, username='', url='', password=''):
        super(KeyringItem, self).__init__()        
        self.__kind = kind
        self.__username = username
        self.__url = url
        self.__password = password

    def get_kind(self):
        return self.__kind

    def get_username(self):
        return self.__username

    def get_url(self):
        return self.__url

    def get_password(self):
        return self.__password

    def set_kind(self, kind):
        self.__kind = kind

    def set_username(self, username):
        self.__username = username

    def set_url(self, url):
        self.__url = url

    def set_password(self, password):
        self.__password = password

    def __repr__(self):
        return '{kind=%s username=%s url=%s len(password)=%d}' % (self.__kind, self.__username, self.__url, len(self.__password))

### The keyring is a map from the tuple (kind,username,url) to a password.
### In gnome-keyring itself we add to the tuple appname=BigBoard
class Keyring:
    def __init__(self, is_singleton):
        if not is_singleton == 42:
            raise Exception("use keyring.get_keyring()")

        ### an in-memory substitute for gnome-keyring, set of KeyringItem, used
        ### when the real keyring is not available
        self.__fallback_items = set()

    def is_available(self):
        return gnomekeyring.is_available()

    # Returns a set of KeyringItem
    def get_logins(self, kind, username, url):
        matches = set()

        if not self.is_available():
            for ki in self.__fallback_items:
                if ki.get_kind() == kind and \
                   ki.get_username() == username and \
                   ki.get_url() == url:
                    matches.add(ki)

        else:
            # don't try to use gnomekeyring.find_items_sync, it's broken
            try:
                found = bignative.keyring_find_items_sync(gnomekeyring.ITEM_GENERIC_SECRET,
                                                          dict(appname='BigBoard',
                                                               kind=kind,
                                                               username=username,
                                                               url=url))
            except TypeError:
                found = set()
                
            for f in found:
                ki = KeyringItem(kind=f.attributes['kind'],
                                 username=f.attributes['username'],
                                 url=f.attributes['url'],
                                 password=f.secret)
                matches.add(ki)

        return matches

    def get_password(self, kind, username, url):
      logins = self.get_logins(kind, username, url)
      _logger.debug("got logins: %s" % (str(logins)))
      if len(logins) > 0:
          return logins.pop().get_password()
      else:
          return None
        
    def remove_logins(self, kind, username, url):
        _logger.debug("removing login (%s, %s, %s)" % (kind, username, url))
        new_fallbacks = set()
        for ki in self.__fallback_items:
            if ki.get_kind() == kind and \
                   ki.get_username() == username and \
                   ki.get_url() == url:
                pass
            else:
                new_fallbacks.add(ki)
                
        self.__fallback_items = new_fallbacks

        if self.is_available():
            try:   
                found = bignative.keyring_find_items_sync(gnomekeyring.ITEM_GENERIC_SECRET,
                                                          dict(appname='BigBoard',
                                                               kind=kind,
                                                               username=username,
                                                               url=url))
            except TypeError:
                found = set()
                
            for f in found:
                gnomekeyring.item_delete_sync('session', f.item_id)
  
    def store_login(self, kind, username, url, password):

        if not password:
            self.remove_logins(kind, username, url)
            return

        _logger.debug("storing login (%s, %s, %s)" % (kind, username, url))
        if not self.is_available():
            found = None
            for ki in self.__fallback_items:
                if ki.get_kind() == kind and \
                       ki.get_username() == username and \
                       ki.get_url() == url:
                    found = ki

            if found:
                found.set_password(password)
            else:
                ki = KeyringItem(kind=kind,
                                 username=username,
                                 url=url,
                                 password=password)
                self.__fallback_items.add(ki)

        else:  
            keyring_item_id = gnomekeyring.item_create_sync('session',
                                                            gnomekeyring.ITEM_GENERIC_SECRET,
                                                            "BigBoard",
                                                            dict(appname="BigBoard",
                                                                 kind=kind,
                                                                 username=username,
                                                                 url=url),
                                                            password, True)

keyring_inst = None
def get_keyring():
    global keyring_inst
    if keyring_inst is None:
        keyring_inst = Keyring(42)
    return keyring_inst


if __name__ == '__main__':

    bignative.set_application_name("BigBoard")
    
    ring = get_keyring()

    print ring.is_available()

    print "storing"
    ring.store_login(kind='google', username='havoc.pennington+foo@gmail.com',
                     url='http://google.com/', password='frob')

    print "getting"
    print ring.get_logins(kind='google', username='havoc.pennington+foo@gmail.com',
                          url='http://google.com/')

    print "done"
    
