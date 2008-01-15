import copy
import gobject
import logging
import re

from ddm import DataModel
from ddm.NotificationSet import NotificationSet
import ddm.Resource
import bigboard.globals
from libbig.singletonmixin import Singleton
import libbig.gutil as gutil

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
        "online-changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
    }
    
    def __init__(self, resource):
        gobject.GObject.__init__(self)
        self.resource = resource
        self.is_contact = self.resource.class_id == "http://online.gnome.org/p/o/contact"

        #self._debug_rank = -100

        self.icon_url = None
        self.online = False
        self.aim_buddy = None
        self.xmpp_buddy = None

        if self.is_contact:
            self.resource.connect(self.__contact_name_changed, "name")
            self.resource.connect(self.__contact_aims_changed, "aims")
            self.resource.connect(self.__contact_xmpps_changed, "xmpps")
            self.resource.connect(self.__contact_aim_buddies_changed, "aimBuddies")
            self.resource.connect(self.__contact_xmpp_buddies_changed, "xmppBuddies")

            self.resource.connect(self.__contact_user_changed, "user")

            self.local_buddy = None

            self.__contact_user_changed(resource)
            self.__contact_name_changed(resource)
            self.__contact_aims_changed(resource)
            self.__contact_xmpps_changed(resource)
            self.__contact_aim_buddies_changed(resource)
            self.__contact_xmpp_buddies_changed(resource)

        else:
            if resource.class_id != 'online-desktop:/p/o/buddy':
                raise Exception("unknown class ID %s for resource constructing Person" % resource.class_id)

            if resource.protocol == 'aim':
                self.aim = resource.name
                self.aim_buddy = resource
                self.aims = [self.aim]
                self.aim_buddies = [self.aim_buddy]
            else:
                self.aim = None
                self.aim_buddy = None
                self.aims = []
                self.aim_buddies = []

            if resource.protocol == 'xmpp':
                self.xmpp = resource.name
                self.xmpp_buddy = resource
                self.xmpps = [self.xmpp]
                self.xmpp_buddies = [self.xmpp_buddy]
            else:
                self.xmpp = None
                self.xmpp_buddy = None
                self.xmpps = []
                self.xmpp_buddies = []

            if resource.protocol == 'mugshot-local':
                self.local_buddy = resource

                ## The merge code in the data model won't know about the user 
                ## corresponding to the local buddy, so the merge rule won't 
                ## trigger to create local_buddy.user, unless we do this
                _logger.debug("Ensuring we load user %s" % (self.local_buddy.name))
                query = bigboard.globals.get_data_model().query_resource(self.local_buddy.name, "+")
                query.execute()
            else:
                self.local_buddy = None

            self.resource.connect(self.__buddy_alias_changed, "alias")
            self.resource.connect(self.__buddy_icon_changed, "icon")
            self.resource.connect(self.__buddy_online_changed, "isOnline")

            self.__buddy_alias_changed(resource)
            self.__buddy_icon_changed(resource)
            self.__buddy_online_changed(resource)

    def __contact_recompute_online(self):
        isOnline = False
        
        try:
            isOnline = self.aim_buddy.isOnline
        except AttributeError:
            pass

        if not isOnline:
            try:
                isOnline = self.xmpp_buddy.isOnline
            except AttributeError:
                pass
        
        if isOnline != self.online:
            self.online = isOnline
            self.emit('online-changed')
            _logger.debug('%s is now %d' % (self.display_name, self.online))

    def __contact_name_changed(self, resource):
        try:
            self.display_name = resource.name
        except AttributeError:
            # FIXME: why does this happen
            self.display_name = "NO_NAME_"
            
        self.emit("display-name-changed")

    def __contact_aims_changed(self, resource):
        try:
            self.aims = resource.aims
        except AttributeError:
            self.aims = []

        ## FIXME don't just pick one arbitrarily
        if len(self.aims) > 0:
            self.aim = self.aims[0]
        else:
            self.aim = None

        self.emit("aim-changed")
        self.__contact_recompute_online()

    def __contact_xmpps_changed(self, resource):
        try:
            self.xmpps = resource.xmpps
        except AttributeError:
            self.xmpps = []
        
        ## FIXME don't just pick one arbitrarily
        if len(self.xmpps) > 0:
            self.xmpp = self.xmpps[0]
        else:
            self.xmpp = None

        self.emit("xmpp-changed")
        self.__contact_recompute_online()    

    def __contact_buddy_online_changed(self, *args):
        self.__contact_recompute_online()

    def __contact_aim_buddies_changed(self, resource):
        try:
            self.aim_buddies = resource.aimBuddies
        except AttributeError:
            self.aim_buddies = []

        old_aim_buddy = self.aim_buddy

        ## FIXME don't just pick one arbitrarily
        if len(self.aim_buddies) > 0:
            self.aim_buddy = self.aim_buddies[0]
        else:
            self.aim_buddy = None

        self.__refresh_icon_url() # in case we were using the AIM buddy icon

        if old_aim_buddy != self.aim_buddy:
            if old_aim_buddy:
                old_aim_buddy.disconnect(self.__contact_buddy_online_changed)
            if self.aim_buddy:
                self.aim_buddy.connect(self.__contact_buddy_online_changed, 'isOnline')
            self.emit("aim-buddy-changed")
            self.__contact_recompute_online()

    def __contact_xmpp_buddies_changed(self, resource):
        try:
            self.xmpp_buddies = resource.xmppBuddies
        except AttributeError:
            self.xmpp_buddies = []

        old_xmpp_buddy = self.xmpp_buddy

        ## FIXME don't just pick one arbitrarily
        if len(self.xmpp_buddies) > 0:
            self.xmpp_buddy = self.xmpp_buddies[0]
        else:
            self.xmpp_buddy = None

        self.__refresh_icon_url() # in case we were using the XMPP buddy icon

        if old_xmpp_buddy != self.xmpp_buddy:
            if old_xmpp_buddy:
                old_xmpp_buddy.disconnect(self.__contact_buddy_online_changed)
            if self.xmpp_buddy:
                self.xmpp_buddy.connect(self.__contact_buddy_online_changed, 'isOnline')
            self.emit("xmpp-buddy-changed")
            self.__contact_recompute_online()

    def __contact_user_changed(self, resource):
        try:
            user_resource = resource.user
        except AttributeError:
            user_resource = None

        _logger.debug("user changed to %s" % str(user_resource))

        if user_resource:
            user_resource.connect(self.__user_photo_url_changed, "photoUrl")
            user_resource.connect(self.__user_local_buddy_changed, "mugshotLocalBuddy")            

        self.__user_local_buddy_changed(user_resource)
        self.__user_photo_url_changed(user_resource)

    def __user_local_buddy_changed(self, user_resource):
        new_buddy = None
        if user_resource:
            try:
                new_buddy = user_resource.mugshotLocalBuddy
            except:
                pass
        
        if new_buddy != self.local_buddy:
            self.local_buddy = new_buddy
            self.emit("local-buddy-changed")
            self.__contact_recompute_online()

    def __set_icon_url(self, new_icon_url):
        if not new_icon_url:
            try:
                new_icon_url = self.resource.model.global_resource.fallbackUserPhotoUrl
            except AttributeError:
                pass

        #_logger.debug("photo url now %s" % str(new_icon_url))

        if new_icon_url != self.icon_url:
            self.icon_url = new_icon_url
            self.emit("icon-url-changed")

    def __refresh_icon_url(self):
        new_icon_url = None
        no_photo_url = None
        if self.is_contact:
            try:
                new_icon_url = self.resource.user.photoUrl
            except AttributeError:
                pass

            ## see if we can get an icon from one of our buddies 
            try:
                no_photo_url = self.resource.model.global_resource.fallbackUserPhotoUrl
            except AttributeError, e:
                pass

            if not new_icon_url or new_icon_url == no_photo_url:
                try:
                    new_icon_url = self.xmpp_buddy.icon
                except AttributeError:
                    pass

            if not new_icon_url or new_icon_url == no_photo_url:
                try:
                    new_icon_url = self.aim_buddy.icon
                except AttributeError:
                    pass

        else:
            try:
                new_icon_url = self.resource.icon
            except AttributeError:
                pass

        self.__set_icon_url(new_icon_url)
            
    def __user_photo_url_changed(self, user_resource):
        self.__refresh_icon_url()

    def __buddy_alias_changed(self, resource):
        try:
            self.display_name = resource.alias
        except AttributeError:
            self.display_name = None

        if self.display_name == None:
            if resource.protocol == 'mugshot-local':
                ## resource.name for this would be a data model URI thing
                self.display_name = 'NO_NAME'
            else:
                ## resource.name for xmpp/aim should be the xmpp/aim address
                self.display_name = resource.name

        self.emit("display-name-changed")

    def __buddy_icon_changed(self, resource):
        self.__refresh_icon_url()

    def __buddy_online_changed(self, resource):
        if resource.isOnline != self.online:
            self.online = resource.isOnline
            self.emit('online-changed')

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

## PersonSet that wraps a list of Resource objects (each resource is wrapped in a Person object)
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
                except AttributeError:
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
        
        for person in old_resolved:
            if person not in self.__resolved:
                self.emit('removed', person)
                
        for person in self.__resolved:
            if person not in old_resolved:
                self.emit('added', person)

    def __buddy_user_changed(self, resource):
        self.__update_resolved()

    def __str__(self):
        return self.__resolved.__str__()

    def __iter__(self):
        return self.__resolved.__iter__()

## used to merge multiple PersonSet into one
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

## used to allow some resources in a person set to hide others
class RemoveDuplicatesPersonSet(PersonSet):
    def __init__(self, source):
        PersonSet.__init__(self)
        self.__source = source
        self.__source.connect('added', self.__on_added)
        self.__source.connect('removed', self.__on_removed)

        self.__not_included = set()
        self.__included = set()

        for item in self.__source:
            self.__on_added(self, self.__source, item)

        self.__connections = gutil.DisconnectSet()

    def __on_added(self, source, item):

        ## monitor properties that affect what hides what
        self.__connections.add(item, item.connect('xmpp-buddy-changed', self.__on_changed))
        self.__connections.add(item, item.connect('aim-buddy-changed', self.__on_changed))

        ## if we are hidden by anything included, then 
        ## we are not included
        is_hidden = False
        for included in self.__included:
            if self.__is_hidden_by(item, included):
                is_hidden = True
                break

        if is_hidden:
            self.__not_included.add(item)
        else:
            ## if we are not hidden, then see if we hide something 
            ## else and add ourselves to the included set

            items_we_hide = []
            for included in self.__included:
                if self.__is_hidden_by(included, item):
                    items_we_hide.append(included)

            self.__included.add(item)
            self.emit('added', item)

            self.__consider_including_items(items_we_hide)

    def __on_removed(self, source, item):

        self.__connections.disconnect_object(item)

        ## if we weren't included anyhow, nothing to do
        if item in self.__not_included:
            self.__not_included.remove(item)
            return

        if item in self.__included:
            self.__included.remove(item)
            self.emit('removed', item)
            
            ## if we were included, we might have been hiding
            ## something else, so we need to see if anything in 
            ## not_included is now included
            self.__consider_including_items(self.__not_included)

    def __on_changed(self, item):
        ## if we changed, we may no longer be hiding another item, 
        ## or may no longer be hidden by another item, or we may 
        ## now hide another item, or may now be hidden by another 
        ## item

        # this should result in un-hiding either other items or 
        # the changed item, if appropriate
        self.__consider_including_items(self.__not_included)
        
        # this should result in hiding either other items or 
        # the changed item, if appropriate
        self.__consider_including_items(self.__included)        

    def __consider_including_items(self, maybe_now_included):
        ## FIXME this is messed up since self.__included is
        ## used to compute what should be in self.__included ...

        # since maybe_now_included may be self.__included or self.__not_included, 
        # we can't remove or add while iterating over it
        to_add = set()
        to_remove = set()

        for maybe_included in maybe_now_included:
            include = True
            for may_hide in self.__included:
                if self.__is_hidden_by(maybe_included, may_hide):
                    include = False
                    break

            if include:
                to_add.add(maybe_included)
            else:
                to_remove.add(maybe_included)

        for person in to_add:
            if person not in self.__included:
                self.__not_included.remove(person)
                self.__included.add(person)
                self.emit('added', person)
            
        for person in to_remove:
            if person in self.__included:
                self.__included.remove(person)
                self.__not_included.add(person)
                self.emit('removed', person)

    def __is_hidden_by(self, hidden, hider):
        ## the idea is that contacts hide IM buddies they correspond
        ## to (buddies are merged into contacts)

        if hidden == hider:
            return False

        if hider.is_contact and \
                (hidden.resource == hider.aim_buddy or \
                     hidden.resource == hider.xmpp_buddy or \
                     hidden.resource == hider.local_buddy):
            return True
        else:
            return False

    def __str__(self):
        return str(self.__included)

    def __iter__(self):
        return self.__included.__iter__()


class PeopleTracker(Singleton):
    """Singleton object for tracking available users and contacts

    The PeopleTracker is responsible for doing bookkeeping associated with figuring
    out who is around.

    Lists of different types of users are available as the contacts, aim_users and local_users
    attributes of the singleton; these are UserList objects, and have signals for tracking
    changes as well as allowing iteration through the users.
    """
    
    def __init__(self):
        self.__model = bigboard.globals.get_data_model()
        self.__model.add_ready_handler(self.__on_ready)

        self.contacts = SinglePersonSet()
        self.aim_people = SinglePersonSet()
        self.xmpp_people = SinglePersonSet()
        self.local_people = SinglePersonSet()
        
        self.people = RemoveDuplicatesPersonSet(UnionPersonSet(self.contacts, self.aim_people, self.xmpp_people, self.local_people))

        if self.__model.ready:
            self.__on_ready()
        
    def __on_ready(self):

        # When we disconnect from the server we freeze existing content, then on reconnect
        # we clear everything and start over.

        contact_props = '[+;name;user [+;photoUrl;mugshotLocalBuddy];aims;aimBuddies [+;icon;statusMessage];mugshotLocalBuddies [+;icon;user];xmpps;xmppBuddies [+;icon;statusMessage];emails;status]'
        
        if self.__model.self_resource != None:
            query = self.__model.query_resource(self.__model.self_resource, "contacts %s" % contact_props)
            query.add_handler(self.__on_got_self)
            query.execute()

        query = self.__model.query_resource(self.__model.global_resource,
                                            "aimBuddies [+;icon;statusMessage;contact %s]; xmppBuddies [+;icon;statusMessage;contact %s]; mugshotLocalBuddies [+;icon;user;contact %s]" % (contact_props, contact_props, contact_props))

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
        for contact in self_resource.contacts:
            new_contacts.add(contact)

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

## status on a contact is:
## 0 - not a contact
## 1 - blocked
## 2 - cold (never in sidebar)
## 3 - medium (auto-choose sidebar)
## 4 - hot (always in sidebar)

STATUS_NOT_A_CONTACT = 0
STATUS_BLOCKED = 1
STATUS_COLD = 2
STATUS_MEDIUM = 3
STATUS_HOT = 4

## for sorting, we classify with the following 
## ranks (higher is higher in the sidebar).
## The ranks have a gap since not being a user counts 
## as -1 for contacts
RANK_NO_DISPLAY = -2
RANK_COLD = 0
RANK_BUDDY_OFFLINE = 2
RANK_MEDIUM_OFFLINE = 4
RANK_BUDDY_ONLINE = 6
RANK_MEDIUM_ONLINE = 8
RANK_HOT_OFFLINE = 10
RANK_HOT_ONLINE = 12

def __get_raw_contact_rank(contact):
    try:
        status = contact.resource.status
    except AttributeError:
        status = STATUS_NOT_A_CONTACT

    if status == STATUS_NOT_A_CONTACT:
        return RANK_COLD
    elif status == STATUS_BLOCKED:
        return RANK_NO_DISPLAY

    if contact.online:
        if status == STATUS_MEDIUM:
            return RANK_MEDIUM_ONLINE
        elif status == STATUS_HOT:
            return RANK_HOT_ONLINE
    else:
        if status == STATUS_MEDIUM:
            return RANK_MEDIUM_OFFLINE
        elif status == STATUS_HOT:
            return RANK_HOT_OFFLINE

    ## I believe this is not reached
    return RANK_COLD

def __get_contact_rank(contact):
    ## subtract 1 if not a user
    rank = __get_raw_contact_rank(contact)
    try:
        user = contact.resource.user
    except AttributeError:
        rank = rank - 1

    return rank
        
def __get_buddy_rank(buddy):
    if buddy.online:
        return RANK_BUDDY_ONLINE
    else:
        return RANK_BUDDY_OFFLINE

# import libbig.gutil
# rank_changed_list = []
# def rank_changed_idle():
#     global rank_changed_list
#     for person in rank_changed_list:
#         person.emit("display-name-changed")
#     rank_changed_list = []

# def debug_change_rank(person, rank):
#     global rank_changed_list
#     if rank != person._debug_rank:
#         person._debug_rank = rank
#         rank_changed_list.append(person)
#         libbig.gutil.call_idle_once(rank_changed_idle)

def sort_people(a, b):

    rankA = 0
    rankB = 0

    if a.is_contact:
        rankA = __get_contact_rank(a)
    else:
        rankA = __get_buddy_rank(a)

    if b.is_contact:
        rankB = __get_contact_rank(b)
    else:
        rankB = __get_buddy_rank(b)

    #    debug_change_rank(a, rankA)
    #    debug_change_rank(b, rankB)

    if rankA != rankB:
        return rankB - rankA

    return bignative.utf8_collate(a.display_name, b.display_name)

