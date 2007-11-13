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

class Person(gobject.GObject):
    """Wrapper around buddy and user resources

    A Person wraps a buddy resource or user resource and provides convenience APIs for
    things that are shared between the two resource types.
    """
    __gsignals__ = {
        "display-name-changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "icon-url-changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "aim-changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "aim-buddy-changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "xmpp-changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "xmpp-buddy-changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "local-buddy-changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
    }
    
    def __init__(self, resource):
        gobject.GObject.__init__(self)
        self.resource = resource
        self.is_user = self.resource.class_id == "http://mugshot.org/p/o/user"

        if self.is_user:
            self.resource.connect(self.__user_name_changed, "name")
            self.resource.connect(self.__user_photo_url_changed, "photoUrl")
            self.resource.connect(self.__user_aim_changed, "aim")
            self.resource.connect(self.__user_xmpp_changed, "xmpp")
            self.resource.connect(self.__user_aim_buddy_changed, "aimBuddy")
            self.resource.connect(self.__user_xmpp_buddy_changed, "xmppBuddy")
            self.resource.connect(self.__user_local_buddy_changed, "mugshotLocalBuddy")

            self.__user_name_changed(resource)
            self.__user_photo_url_changed(resource)
            self.__user_aim_changed(resource)
            self.__user_xmpp_changed(resource)
            self.__user_aim_buddy_changed(resource)
            self.__user_xmpp_buddy_changed(resource)
        else:
            if resource.protocol == 'aim':
                self.aim = resource.name
                self.aim_buddy = resource
            else:
                self.aim = None
                self.aim_buddy = None

            if resource.protocol == 'xmpp':
                self.xmpp = resource.name
                self.xmpp_buddy = resource
            else:
                self.xmpp = None
                self.xmpp_buddy = None

            if resource.protocol == 'mugshot-local':
                self.local_buddy = resource
            else:
                self.local_buddy = None

            self.resource.connect(self.__buddy_alias_changed, "alias")
            self.resource.connect(self.__buddy_icon_changed, "icon")

            self.__buddy_alias_changed(resource)
            self.__buddy_icon_changed(resource)

    def __user_name_changed(self, resource):
        try:
            self.display_name = resource.name
        except AttributeError:
            # FIXME: why does this happen
            self.display_name = "NO_NAME"
            
        self.emit("display-name-changed")

    def __user_photo_url_changed(self, resource):
        try:
            self.icon_url = resource.photoUrl
        except AttributeError:
            self.icon_url = None
            
        self.emit("icon-url-changed")

    def __user_aim_changed(self, resource):
        try:
            self.aim = resource.aim
        except AttributeError:
            self.aim = None

        self.emit("aim-changed")

    def __user_xmpp_changed(self, resource):
        try:
            self.xmpp = resource.xmpp
        except AttributeError:
            self.xmpp = None

        self.emit("xmpp-changed")

    def __user_aim_buddy_changed(self, resource):
        try:
            self.aim_buddy = resource.aimBuddy
        except AttributeError:
            self.aim_buddy = None

        self.emit("aim-buddy-changed")

    def __user_xmpp_buddy_changed(self, resource):
        try:
            self.xmpp_buddy = resource.xmppBuddy
        except AttributeError:
            self.xmpp_buddy = None

        self.emit("xmpp-buddy-changed")

    def __user_local_buddy_changed(self, resource):
        try:
            self.local_buddy = resource.mugshotLocalBuddy
        except AttributeError:
            self.local_buddy = None

        self.emit("local-buddy-changed")

    def __buddy_alias_changed(self, resource):
        try:
            self.display_name = resource.alias
        except AttributeError:
            self.display_name = None

        if self.display_name == None:
            self.display_name = resource.name

        self.emit("display-name-changed")

    def __buddy_icon_changed(self, resource):
        try:
            self.icon_url = resource.icon
        except AttributeError:
            self.icon_url = None
        self.emit("icon-url-changed")

    def __hash__(self):
        return hash(self.resource)

    def __eq__(self, other):
        if isinstance(other, Person):
            return self.resource == other.resource
        else:
            return self.resource == other

class PersonSet(gobject.GObject):
    """A list of Person objects with change notification

    The PersonSet object represents an (unordered) set of person objects with change notification
    when person objects are added and removed from it via the 'added' and 'removed' GObject signals.
    The object also supports the standard Python iteration protocol.

    """
    __gsignals__ = {
        "added": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "removed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
    }

class SinglePersonSet(PersonSet):
    def __init__(self):
        PersonSet.__init__(self)

        self.__resources = set()
        self.__resolved = set()

    def _update(self, resources):
        for resource in self.__resources:
            if resource.class_id == "online-desktop:/p/o/buddy" and resource not in resources:
                resource.disconnect(self.__buddy_user_changed)

        for resource in resources:
            if resource.class_id == "online-desktop:/p/o/buddy" and resource not in self.__resources:
                    resource.connect(self.__buddy_user_changed, "user")

        self.__resources = resources
        self.__update_resolved()

    def __update_resolved(self):
        resolved = set()

        for resource in self.__resources:
            if resource.class_id == "online-desktop:/p/o/buddy":
                try:
                    user = resource.user
                except:
                    user = None
                    
                if user != None:
                    resource = user

            try:
                person = resource.__person
            except AttributeError:
                person = resource.__person = Person(resource)
                
            resolved.add(person)

        old_resolved = self.__resolved
        self.__resolved = resolved
        
        for resource in old_resolved:
            if resource not in self.__resolved:
                self.emit('removed', resource)
                
        for resource in self.__resolved:
            if resource not in old_resolved:
                self.emit('added', resource)

    def __buddy_user_changed(self, resource):
        self.__update_resolved()

    def __str__(self):
        return self.__resolved.__str__()

    def __iter__(self):
        return self.__resolved.__iter__()

class UnionPersonSet(PersonSet):
    def __init__(self, *args):
        PersonSet.__init__(self)
        self.__items = {}
        for s in args:
            s.connect('added', self.__on_added)
            s.connect('removed', self.__on_removed)

            for item in s:
                self.__on_added(self, s, item)

    def __on_added(self, s, item):
        if item in self.__items:
            self.__items[item] += 1
        else:
            self.__items[item] = 1
            self.emit('added', item)

    def __on_removed(self, s, item):
        self.__items[item] -= 1
        if self.__items[item] == 0:
            del self.__items[item]
            self.emit('removed', item)

    def __str__(self):
        return self.__items.values().__str__()

    def __iter__(self):
        return self.__items.itervalues()

class PeopleTracker(Singleton):
    """Singleton object for tracking available users and contacts

    The PeopleTracker is responsible for doing bookkeeping associated with figuring
    out who is around.

    Lists of different types of users are available as the contacts, aim_users and local_users
    attributes of the singleton; these are UserList objects, and have signals for tracking
    changes as well as allowing iteration through the users.
    """
    
    def __init__(self):
        self.__model = DataModel(bigboard.globals.server_name)
        self.__model.add_ready_handler(self.__on_ready)

        self.contacts = SinglePersonSet()
        self.aim_people = SinglePersonSet()
        self.xmpp_people = SinglePersonSet()
        self.local_people = SinglePersonSet()
        
        self.people = UnionPersonSet(self.contacts, self.aim_people, self.xmpp_people, self.local_people)

        if self.__model.ready:
            self.__on_ready()
        
    def __on_ready(self):

        # When we disconnect from the server we freeze existing content, then on reconnect
        # we clear everything and start over.

        query = self.__model.query_resource(self.__model.self_resource, "contacts [+;aim;aimBuddy +;mugshotLocalBuddy +;xmpp;xmppBuddy +;email;contactStatus]")
        query.add_handler(self.__on_got_self)
        query.execute()
        
        query = self.__model.query_resource(self.__model.global_resource, "aimBuddies [+;user [+;aim;aimBuddy +;mugshotLocalBuddy +;xmpp;xmppBuddy +;email;contactStatus]];xmppBuddies [+;user [+;aim;aimBuddy +;mugshotLocalBuddy +;xmpp;xmppBuddy +;email;contactStatus]];mugshotLocalBuddies [+;user [+;aim;aimBuddy +;mugshotLocalBuddy +;xmpp;xmppBuddy +;email;contactStatus]]")
        query.add_handler(self.__on_got_global)
        query.execute()

    def __ensure_list_property(self, resource, property):
        # Workaround for lack of schema support currently; if the property doesn't
        # exist, make it empty
        try:
            resource.get(property)
        except AttributeError:
            resource._update_property(property,
                                      ddm.Resource.UPDATE_CLEAR, ddm.Resource.CARDINALITY_N,
                                      None)
        
    def __on_got_self(self, self_resource):
        self.__ensure_list_property(self_resource, ("http://mugshot.org/p/o/user", "contacts"))
        self_resource.connect(self.__on_contacts_changed, "contacts")
        self.__on_contacts_changed(self_resource)

    def __on_contacts_changed(self, self_resource):
        new_contacts = set()
        for user in self_resource.contacts:
            new_contacts.add(user)

        self.contacts._update(new_contacts)
        
    def __on_got_global(self, global_resource):
        self.__ensure_list_property(global_resource, ("online-desktop:/p/o/global", "aimBuddies"))
        self.__ensure_list_property(global_resource, ("online-desktop:/p/o/global", "xmppBuddies"))
        self.__ensure_list_property(global_resource, ("online-desktop:/p/o/global", "mugshotLocalBuddies"))
        global_resource.connect(self.__on_aim_buddies_changed, "aimBuddies")
        global_resource.connect(self.__on_local_buddies_changed, "mugshotLocalBuddies")
        global_resource.connect(self.__on_xmpp_buddies_changed, "xmppBuddies")
        
        self.__on_aim_buddies_changed(global_resource)
        self.__on_local_buddies_changed(global_resource)
        self.__on_xmpp_buddies_changed(global_resource)

    def __on_aim_buddies_changed(self, global_resource):
        self.aim_people._update(global_resource.aimBuddies)

    def __on_local_buddies_changed(self, global_resource):
        self.local_people._update(global_resource.mugshotLocalBuddies)

    def __on_xmpp_buddies_changed(self, global_resource):
        self.xmpp_people._update(global_resource.xmppBuddies)

def sort_people(a,b):
    if a.is_user:
        try:
            statusA = a.resource.contactStatus
        except AttributeError:
            statusA = 0
    else:
        statusA = 0

    if b.is_user:
        try:
            statusB = b.resource.contactStatus
        except AttributeError:
            statusB = 0
    else:
        statusB = 0

    if statusA == 0:
        statusA = 3
    if statusB == 0:
        statusB = 3

    if statusA != statusB:
        return statusB - statusA

    return bignative.utf8_collate(a.display_name, b.display_name)

