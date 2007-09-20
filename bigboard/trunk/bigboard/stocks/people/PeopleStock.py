import logging

import hippo
from ddm import DataModel

import bigboard
from bigboard.people_tracker import PeopleTracker, sort_users
from bigboard.stock import AbstractMugshotStock
import bigboard.globals
import bigboard.slideout
import bigboard.profile

import peoplebrowser
from peoplewidgets import PersonItem, ProfileItem

class PeopleStock(AbstractMugshotStock):
    def __init__(self, *args, **kwargs):
        super(PeopleStock, self).__init__(*args, **kwargs)

        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        
        self.__local_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self.__box.append(self.__local_box)

        self.__separator = hippo.CanvasBox(box_height=1, xalign=hippo.ALIGNMENT_FILL, background_color=0xccccccff)
        self.__box.append(self.__separator)
        
        self.__contact_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self.__box.append(self.__contact_box)

        self.__local_items = {}
        self.__contact_items = {}

        self.__slideout = None
        self.__slideout_item = None

        self.__people_browser = None
        self._add_more_button(self.__on_more_button)        

        self.__update_separators()
        
        self.__tracker = PeopleTracker()
        self.__tracker.contacts.connect("added", self.__on_contact_added)
        self.__tracker.contacts.connect("removed", self.__on_contact_removed)
        self.__tracker.local_users.connect("added", self.__on_local_user_added)
        self.__tracker.local_users.connect("removed", self.__on_local_user_removed)

        self.__model = DataModel(bigboard.globals.server_name)

        for user in self.__tracker.contacts:
            self.__on_contact_added(self.__tracker.contacts, user)
            
        for user in self.__tracker.local_users:
            self.__on_local_user_added(self.__tracker.local_users, user)

    def get_authed_content(self, size):
        return self.__box

    def __set_item_size(self, item, size):
        if size == bigboard.stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        
        item.set_size(size)

    def set_size(self, size):
        super(PeopleStock, self).set_size(size)
        for i in self.__local_items.values():
            self.__set_item_size(i, size)
        for i in self.__contact_items.values():
            self.__set_item_size(i, size)

    def __update_separators(self):
        show_separator = len(self.__local_items) != 0 and len(self.__contact_items) != 0
        self.__box.set_child_visible(self.__separator, show_separator)

    def __add_user(self, user, box, map):
        self._logger.debug("user added to people stock %s" % (user.name))
        if map.has_key(user.resource_id):
            return
        
        item = PersonItem(user)
        box.insert_sorted(item, hippo.PACK_IF_FITS, lambda a,b: sort_users(a.resource, b.resource))

        def resort(resource):
            box.remove(item)
            box.insert_sorted(item, hippo.PACK_IF_FITS, lambda a,b: sort_users(a.resource, b.resource))
        
        user.connect(resort, 'contactStatus')
        user.connect(resort, 'name')
        
        map[user.resource_id] = item
        self.__set_item_size(item, self.get_size())
        item.connect('activated', self.__handle_item_pressed)

        self.__update_separators()

    def __remove_user(self, user, box, map):
        try:
            item = map[user.resource_id]
        except KeyError:
            return
        
        item.destroy()
        del map[user.resource_id]
        
        self.__update_separators()

    def __on_contact_added(self, list, contact):
        self.__add_user(contact, self.__contact_box, self.__contact_items)
        
    def __on_contact_removed(self, list, contact):
        self.__remove_user(contact, self.__contact_box, self.__contact_items)
        
    def __on_local_user_added(self, list, user):
        if user.resource_id == self.__model.self_id:
            return
        
        self.__add_user(user, self.__local_box, self.__local_items)
        
    def __on_local_user_removed(self, list, user):
        self.__remove_user(user, self.__local_box, self.__local_items)
        
    def __close_slideout(self, *args):
        if self.__slideout:
            self.__slideout.destroy()
            self.__slideout = None
            self.__slideout_item = None
                
    def __handle_item_pressed(self, item):
        same_item = self.__slideout_item == item
        self.__close_slideout()
        if same_item:
            return True

        self.__slideout = bigboard.slideout.Slideout()
        self.__slideout_item = item
        coords = item.get_screen_coords()
        self.__slideout.slideout_from(coords[0] + item.get_allocation()[0] + 4, coords[1])

        p = ProfileItem(item.get_user(),
                        border=1,
                        border_color = 0x0000000ff)

        self.__slideout.get_root().append(p)
        p.connect("close", self.__close_slideout)

        return True

    def __on_more_button(self):
        if self.__people_browser is None:
            self.__people_browser = peoplebrowser.PeopleBrowser(self)
        if self.__people_browser.get_property('is-active'):
            self.__people_browser.hide()
        else:
            self.__people_browser.present()
