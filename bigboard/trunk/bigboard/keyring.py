import logging, gnomekeyring

try:
    import bigboard.bignative as bignative
except:
    import bignative

_logger = logging.getLogger("bigboard.Keyring")

class Keyring:
    def __init__(self, is_singleton):
        if not is_singleton == 42:
            raise Exception("use keyring.get_keyring()")
        # self.__ids and self.__fallback are dictionaries, keyed off 'whatfor', with values
        # that are dictionaries, keyed off 'username'. The values in inner dictionaries in 
        # self.__ids are keyring ids. The values in inner dictionaries in self.__fallback 
        # are passwords. This works best for retrieving and storing username-password pairs
        # for a particular service. In case of Google, usernames are e-mails.
        self.__ids = {}
        self.__fallback = {}

    def is_available(self):
        return gnomekeyring.is_available()

    def get_logins(self, whatfor):
        username_password_dict = {} 
        if not self.is_available():
            if self.__fallback.has_key(whatfor):
                _logger.debug("using fallback")
                return self.__fallback[whatfor]
            else:
                return username_password_dict
       
        # we can use values from self.__ids if we are looking for a particular 
        # username, but just using this to get all logins wouldn't work, because
        # extra logins for the service might be stored in the keyring
        # don't try to use gnomekeyring.find_items_sync, it's broken
        found = bignative.keyring_find_items_sync(gnomekeyring.ITEM_GENERIC_SECRET,
                                                  { 'whatfor' : whatfor } )
        for f in found:
            if f.attributes.has_key("username"):
                _logger.debug("found attribute 'username': %s", f.attributes["username"])
                username = f.attributes["username"]
                username_password_dict[username] = f.secret

        return username_password_dict
        
    def remove_logins(self, whatfor):
        if self.__fallback.has_key(whatfor):
            del self.__fallback[whatfor]
        if self.__ids.has_key(whatfor):
            del self.__ids[whatfor]

        if self.is_available():       
            found = bignative.keyring_find_items_sync(gnomekeyring.ITEM_GENERIC_SECRET,
                                                      { 'whatfor' : whatfor } )
            for f in found:
                gnomekeyring.item_delete_sync('session', f.item_id)

    def store_login(self, whatfor, username, password):
        if not self.is_available():
            self.__fallback[whatfor] = (username, password)
            if self.__fallback.has_key(whatfor):
                self.__fallback[whatfor][username] = password
            else:
                self.__fallback[whatfor] = {username : password} 
        else:  
            keyring_item_id = gnomekeyring.item_create_sync('session',
                                                            gnomekeyring.ITEM_GENERIC_SECRET,
                                                            "BigBoard",
                                                            dict(appname="BigBoard", whatfor=whatfor, username=username),
                                                            password, True)
            if self.__ids.has_key(whatfor):
                self.__ids[whatfor][username] = keyring_item_id
            else:
                self.__ids[whatfor] = {username : keyring_item_id} 

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
    ring.store_login('frap', 'qxr', 'def')

    print "getting"
    print ring.get_login('frap')

    print "done"
    
