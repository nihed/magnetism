import copy
import gobject
import logging
import re

from ddm import DataModel
from ddm.NotificationSet import NotificationSet
import ddm.Resource
import bigboard.globals
from libbig.singletonmixin import Singleton

try:
    import bigboard.bignative as bignative
except:
    import bignative

_logger = logging.getLogger("bigboard.PeopleTracker")

def _canonicalize_aim(aim):
    return aim.replace(" ", "").lower()
        
class UserList(gobject.GObject):
    """A list of users with change notification

    The UserList object represents an (unordered) set of users with change notification
    when users are added and removed from it via the 'added' and 'removed' GObject signals.
    The object also supports the standard Python iteration protocol.

    """
    __gsignals__ = {
        "added": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "removed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
    }

    def __init__(self):
        super(UserList, self).__init__()

        self.__users = {}

    def _update(self, users):
        old_users = copy.copy(self.__users)

        for user in users:
            if user.resource_id in old_users:
                del old_users[user.resource_id]
            else:
                self.__users[user.resource_id] = user

        for id in old_users:
            del self.__users[id]
            
        for id in self.__users:
            if not id in old_users:
                self.emit('added', self.__users[id])
            
        for id in old_users:
            self.emit('removed', old_users[id])

    def _add(self, user):
        self.__users[user.resource_id] = user
        self.emit('added', user)

    def _remove(self, user):
        del self.__users[user.resource_id]
        self.emit('removed', user)

    def __str__(self):
        return self.__users.values().__str__()

    def __iter__(self):
        return self.__users.itervalues()

class _MultiDict(object):
    """A map from key => [ value, value, ... ]"""
    
    def __init__(self):
        self.__forward = {}
        self.__reverse = {}

    def add(self, key, value):
        try:
            self.__forward[key].append(value)
        except KeyError:
            self.__forward[key] = [ value ]
            
        self.__reverse[value] = key

    def contains_value(self, value):
        return value in self.__reverse

    def contains_key(self, key):
        return key in self.__forward

    def remove_value(self, value):
        key = self.__reverse[value]
        values = self.__forward[key]
        values.remove(value)
        if len(values) == 0:
            del self.__forward[key]
        del self.__reverse[value]

    def iterkeys(self):
        return self.__forward.iterkeys()
        
    def itervalues(self):
        return self.__reverse.iterkeys()

    def lookup_value(self, value):
        return self.__reverse[value]
        
    def lookup(self, key):
        try:
            return self.__forward[key]
        except KeyError:
            return []

    def lookup_first(self, key):
        try:
            return self.__forward[key][0]
        except KeyError:
            return None
        except IndexError:
            return None

    def clear(self):
        self.__forward = {}
        self.__reverse = {}

    def __str__(self):
        return self.__forward.__str__()

class PeopleTracker(Singleton):
    """Singleton object for tracking available users and contacts

    The PeopleTracker is responsible for doing bookkeeping associated with figuring
    out who is around.

    Lists of different types of users are available as the contacts, aim_users and local_users
    attributes of the singleton; these are UserList objects, and have signals for tracking
    changes as well as allowing iteration through the users.

    The PeopleTracker object also injects artificial aimBuddy and localBuddy resource
    properties into the user objects it tracks, as if they were provided from the
    data model.
    """
    
    def __init__(self):
        self.__model = DataModel(bigboard.globals.server_name)
        self.__model.add_ready_handler(self.__on_ready)

        self.__myself = None
        self.__globalResource = None

        self.contacts = UserList()
        self.aim_users = UserList()
        self.local_users = UserList()

        self.__aim_buddies = _MultiDict()
        self.__local_buddies = _MultiDict()

        self.__users_by_aim = _MultiDict()
        self.__users_by_resource_id = _MultiDict()
        
        if self.__model.ready:
            self.__on_ready()
        
    def __on_ready(self):

        # When we disconnect from the server we freeze existing content, then on reconnect
        # we clear everything and start over.
        
        self.__clear_users()
        
        if self.__myself != None:
            self.__set_new_contacts([])
            self.__myself.disconnect(self.__on_contacts_changed)

            self.__myself = None
        
        query = self.__model.query_resource(self.__model.self_resource, "contacts [+;aim;email;contactStatus]")
        query.add_handler(self.__on_got_self)
        query.execute()
        
        if self.__globalResource != None:
            self.__set_new_buddies([])
            self.__globalResource.disconnect(self.__on_buddies_changed)

            self.__globalResource = None

        query = self.__model.query_resource(self.__model.global_resource, "onlineBuddies +")
        query.add_handler(self.__on_got_buddies)
        query.execute()
        
    def __on_got_self(self, myself):
        self.__myself = myself
        myself.connect(self.__on_contacts_changed, "contacts")
        self.__on_contacts_changed(myself)

    def __on_contacts_changed(self, myself):
        self.__set_new_contacts(myself.contacts)

    def __set_new_contacts(self, contacts):
        for user in contacts:
            self.__add_user(user)
            
        self.contacts._update(contacts)
        
    def __on_got_buddies(self, globalResource):
        self.__globalResource = globalResource
        globalResource.connect(self.__on_buddies_changed, "onlineBuddies")
        self.__on_buddies_changed(globalResource)

    def __on_buddies_changed(self, globalResource):
        try:
            self.__set_new_buddies(globalResource.onlineBuddies)
        except AttributeError, e:
            ## globalResource.onlineBuddies may not exist yet before the query reply arrives
            pass

    def __set_new_buddies(self, buddies):
        _logger.debug("Got new buddy list from the server: %s", buddies)
        
        old_aim_buddies = self.__aim_buddies
        old_local_buddies = self.__local_buddies
        new_aim_buddies = _MultiDict()
        new_local_buddies = _MultiDict()

        for buddy in buddies:
            if buddy.protocol == "aim":
                new_aim_buddies.add(_canonicalize_aim(buddy.name), buddy)
            elif buddy.protocol == "mugshot-local":
                new_local_buddies.add(buddy.name, buddy)

        self.__aim_buddies = new_aim_buddies
        self.__local_buddies = new_local_buddies

        for aim in new_aim_buddies.iterkeys():
            first_buddy = new_aim_buddies.lookup_first(aim)
            
            if not old_aim_buddies.contains_key(aim) or \
               old_aim_buddies.lookup_first(aim) != first_buddy:
                self.__update_buddy_for_aim(aim)
            
        for aim in old_aim_buddies.iterkeys():
            if not new_aim_buddies.contains_key(aim):
                self.__update_buddy_for_aim(aim)
        
        for resource_id in new_local_buddies.iterkeys():
            first_buddy = new_local_buddies.lookup_first(resource_id)
            
            if not old_local_buddies.contains_key(resource_id) or \
               old_local_buddies.lookup_first(resource_id) != first_buddy:
                self.__update_buddy_for_resource_id(resource_id)
            
                query = self.__model.query_resource(resource_id, "+;aim;email")
                query.add_handler(self.__on_got_local_user)
                query.execute()
                
        for resource_id in old_local_buddies.iterkeys():
            if not new_local_buddies.contains_key(resource_id):
                self.__update_buddy_for_resource_id(resource_id)

        _logger.debug("Aim buddy list is now %s", self.__aim_buddies)
        _logger.debug("Local buddy list is now %s", self.__local_buddies)

        self.__update_user_list(self.aim_users, self.__aim_buddies, self.__users_by_aim)
        _logger.debug("Aim user list is now %s", self.aim_users)
        self.__update_user_list(self.local_users, self.__local_buddies, self.__users_by_resource_id)
        _logger.debug("Local user list is now %s", self.local_users)
        
    def __on_got_local_user(self, user):
        self.__add_user(user)
        
    def __update_buddy_for_aim(self, aim):
        for user in self.__users_by_aim.lookup(aim):
            self.__update_user_aim_buddy(self, user)
    
    def __update_user_aim_buddy(self, user):
        try:
            buddy = self.__aim_buddies.lookup_first(_canonicalize_aim(user.aim))
        except AttributeError:
            buddy = None
            
        notifications = NotificationSet(self.__model)
        
        if buddy != None:
            user._update_property(('http://mugshot.org/p/bigboard/user', 'aimBuddy'),
                                  ddm.Resource.UPDATE_REPLACE, ddm.Resource.CARDINALITY_01,
                                  buddy, notifications)
        else:
            user._update_property(('http://mugshot.org/p/bigboard/user', 'aimBuddy'),
                                  ddm.Resource.UPDATE_CLEAR, ddm.Resource.CARDINALITY_01,
                                  None, notifications)
        notifications.send()

    def __update_buddy_for_resource_id(self, resource_id):
        for user in self.__users_by_resource_id.lookup(resource_id):
            self.__update_user_local_buddy(user)
    
    def __update_user_local_buddy(self, user):
        buddy = self.__local_buddies.lookup_first(user.resource_id)
        notifications = NotificationSet(self.__model)
        
        if buddy != None:
            user._update_property(('http://mugshot.org/p/bigboard/user', 'localBuddy'),
                                  ddm.Resource.UPDATE_REPLACE, ddm.Resource.CARDINALITY_01,
                                  buddy, notifications)
        else:
            user._update_property(('http://mugshot.org/p/bigboard/user', 'localBuddy'),
                                  ddm.Resource.UPDATE_CLEAR, ddm.Resource.CARDINALITY_01,
                                  None, notifications)
        notifications.send()

    def __update_user_list(self, list, buddies, key_to_user):
        new_users = []
        for key in buddies.iterkeys():
            user = key_to_user.lookup_first(key)
            if user != None:
                new_users.append(user)

        list._update(new_users)

    def __on_user_aim_changed(self, user):
        try:
            old_aim = self.__users_by_aim.lookup_value(user)
        except KeyError:
            old_aim = None

        try:
            new_aim = _canonicalize_aim(user.aim)
        except AttributeError:
            new_aim = None

        if old_aim != None:
            if self.__aim_buddies.contains_key(old_aim):
                old_first_user = self.__users_by_aim.lookup_first(old_aim)
            self.__users_by_aim.remove_value(user)
            if self.__aim_buddies.contains_key(old_aim):
                new_first_user = self.__users_by_aim.lookup_first(old_aim)

                if old_first_user != new_first_user:
                    if old_first_user != None:
                        self.aim_users._remove(old_first_user)
                    if new_first_user != None:
                        self.aim_users._add(new_first_user)
                    _logger.debug("aim users is now %s", self.aim_users)                        

        _logger.debug("aim for %s changed from %s to %s", user, old_aim, new_aim)
                
        if new_aim != None:
            if self.__aim_buddies.contains_key(new_aim):
                old_first_user = self.__users_by_aim.lookup_first(new_aim)
            self.__users_by_aim.add(new_aim, user)
            if self.__aim_buddies.contains_key(new_aim):
                new_first_user = self.__users_by_aim.lookup_first(new_aim)

                if old_first_user != new_first_user:
                    if old_first_user != None:
                        self.aim_users._remove(old_first_user)
                    if new_first_user != None:
                        self.aim_users._add(new_first_user)
                    _logger.debug("aim users is now %s", self.aim_users)

        self.__update_user_aim_buddy(user)

    def __add_user(self, user):
        _logger.debug("Adding user %s into global map", user.name)
        
        if self.__users_by_resource_id.contains_key(user.resource_id):
            return

        if self.__local_buddies.contains_key(user.resource_id):
            old_first_user = self.__users_by_resource_id.lookup_first(user.resource_id)
        
        self.__users_by_resource_id.add(user.resource_id, user)
        self.__update_user_local_buddy(user)

        if self.__local_buddies.contains_key(user.resource_id):
            new_first_user = self.__users_by_resource_id.lookup_first(user.resource_id)
        
        user.connect(self.__on_user_aim_changed, 'aim')
        self.__on_user_aim_changed(user)

        if self.__local_buddies.contains_key(user.resource_id):
            if old_first_user != new_first_user:
                if old_first_user != None:
                    self.local_users._remove(old_first_user)
                if new_first_user != None:
                    self.local_users._add(new_first_user)

    def __clear_users(self):
        for user in self.__users_by_resource_id.itervalues():
            user.disconnect(self.__on_user_aim_changed)
            
        self.__users_by_aim.clear()
        self.__users_by_resource_id.clear()

def sort_users(a,b):
    try:
        statusA = a.contactStatus
    except AttributeError:
        statusA = 0
        
    try:
        statusB = b.contactStatus
    except AttributeError:
        statusB = 0

    if statusA == 0:
        statusA = 3
    if statusB == 0:
        statusB = 3

    if statusA != statusB:
        return statusB - statusA

    return bignative.utf8_collate(a.name, b.name)

