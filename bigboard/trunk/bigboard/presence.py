import logging

import gobject, dbus

import libbig
from libbig.struct import AutoStruct, AutoSignallingStruct

import global_mugshot
    
class Buddy(AutoStruct):
    """An IM buddy."""
    def __init__(self):
        AutoStruct.__init__(self, { 'protocol' : 'unknown', 'name' : None,
                                    'status' : 'away', 'online' : False })

class Presence:
    
    def __init__(self, issingleton):
        self._logger = logging.getLogger('bigboard.Presence')
        
        if not issingleton == 42:
            raise Exception("use presence.get_presence()")

        self.__proxy = None
        self.__buddies = []
        global_mugshot.get_mugshot().connect("initialized", lambda mugshot: self.__on_reset())

    def __reload_buddy_list(self):
        if not self.__proxy:
            return
        self.__buddies = []
        buddies = self.__proxy.GetBuddyList()
        for b in buddies:
            buddy = Buddy()
            buddy.update(b)
            self.__buddies.append(buddy)

    def __on_buddy_changed(self, buddy):
        self._logger.debug("buddy changed %s" % str(buddy))

    def __on_reset(self):
        if self.__proxy:
            self.__proxy.disconnect_from_signal("BuddyChanged")
            self.__proxy.disconnect_from_signal("BuddyListChanged")
        self.__proxy = global_mugshot.get_mugshot().get_im_proxy()
        if self.__proxy:
            self.__proxy.connect_to_signal("BuddyChanged", self.__on_buddy_changed)
            self.__proxy.connect_to_signal("BuddyListChanged", self.__reload_buddy_list)
            self.__reload_buddy_list()

    def get_buddies(self):
        return self.__buddies

presence_inst = None
def get_presence():
    global presence_inst
    if presence_inst is None:
        presence_inst = Presence(42)
    return presence_inst
