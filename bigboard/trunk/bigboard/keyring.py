import gnomekeyring

try:
    import bigboard.bignative as bignative
except:
    import bignative

class Keyring:
    def __init__(self, is_singleton):
        if not is_singleton == 42:
            raise Exception("use keyring.get_keyring()")
        self.__ids = {}

        self.__fallback = {}

    def is_available(self):
        return gnomekeyring.is_available()

    def get_login(self, whatfor):

        if not self.is_available():
            if self.__fallback.has_key(whatfor):
                return self.__fallback[whatfor]
            else:
                return (None, None)
            
        ## some nonworking attempt to use gnomekeyring follows...
        
        id = None
        if self.__ids.has_key(whatfor):
            id = self.__ids[whatfor]
        else:
            # don't try to use gnomekeyring.find_items_sync, it's broken
            found = bignative.keyring_find_items_sync(gnomekeyring.ITEM_GENERIC_SECRET,
                                                      { 'whatfor' : whatfor } )
            ids = []
            for f in found:
                ids.append(f.item_id)
            if len(ids) > 0:
                id = ids[0]

        if id == None:
            return (None, None)
            
        try:
            secret = gnomekeyring.item_get_info_sync('session', id).get_secret()
            username, password = secret.split('\n')
        except gnomekeyring.DeniedError:
            username = None
            password = None

        return (username, password)
        
    def store_login(self, whatfor, username, password):
        if not self.is_available():
            self.__fallback[whatfor] = (username, password)
        else:
            ## some nonworking attempt to use gnomekeyring follows...
        
            self.__ids[whatfor] = gnomekeyring.item_create_sync('session',
                                                                gnomekeyring.ITEM_GENERIC_SECRET,
                                                                "BigBoard",
                                                                dict(appname="BigBoard", whatfor=whatfor),
                                                                "\n".join((username, password)), True)


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
    
