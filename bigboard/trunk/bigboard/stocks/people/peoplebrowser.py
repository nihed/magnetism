import logging, time, urlparse, urllib

import gobject, gtk
import hippo

from mugshot import DataModel

import bigboard.globals
import bigboard.libbig as libbig
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, ActionLink, PrelightingCanvasBox
from bigboard.overview_table import OverviewTable
from bigboard.people_tracker import PeopleTracker
try:
    import bigboard.bignative as bignative
except:
    import bignative

from peoplewidgets import PersonItem, ProfileItem

_logger = logging.getLogger("bigboard.PeopleBrowser")

LOCAL_PEOPLE = 0
CONTACTS = 1

SECTIONS = {
    LOCAL_PEOPLE : "Local People",
    CONTACTS : "Contacts"
}

class PeopleList(OverviewTable):
    __gsignals__ = {
        "selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
    }

    def __init__(self):
        super(PeopleList, self).__init__(padding=6)
        
        self.__items = {}
        self.__section_counts = {}
        self.__section_headers = {}
        for section in SECTIONS:
            self.__items[section] = {}
            self.__section_counts[section] = 0
            self.__section_headers[section] = self.add_section_head(section, SECTIONS[section])
            self.__section_headers[section].set_visible(False)

        self.__selected_item = None
        self.__search = None

    def add_user(self, user, section):
        if user.resource_id in self.__items[section]:
            return
        
        item = PersonItem(user)
        item.connect("button-press-event", self.__on_item_click)
                
        self.add_column_item(section, item, lambda a,b: bignative.utf8_collate(a.resource.name, b.resource.name))
        self.__section_counts[section] += 1
        if self.__section_counts[section] == 1:
            self.__section_headers[section].set_visible(True)
                    
        self.__update_visibility(section, item)

        self.__items[section][user.resource_id] = item

    def remove_user(self, user, section):
        try:
            item = self.__items[section][user.resource_id]
        except KeyError:
            return
        
        item.destroy()
        del self.__items[section][user.resource_id]

    def __update_visibility(self, section, item):
        was_visible = item.get_visible()
        
        visible = self.__search == None or item.resource.name.lower().find(self.__search) >= 0

        if visible != was_visible:
            if visible:
                self.__section_counts[section] += 1
                if self.__section_counts[section] == 1:
                    self.__section_headers[section].set_visible(True)
            else:
                self.__section_counts[section] -= 1
                if self.__section_counts[section] == 0:
                    self.__section_headers[section].set_visible(False)
        
            item.set_visible(visible)


    def set_search(self, search):
        if search.strip() == "":
            self.__search = None
        else:
            self.__search = search.lower()

        for section in SECTIONS:
            section_items = self.__items[section]
            for id in section_items:
                self.__update_visibility(section, section_items[id])

    def __select_item(self, item):
        if item == self.__selected_item:
            return
        
        if self.__selected_item:
            self.__selected_item.set_force_prelight(False)
            
        self.__selected_item = item
        self.__selected_item.set_force_prelight(True)
        self.emit("selected", item.resource)

    def __on_item_click(self, item, event):
         if event.count == 1:
             self.__select_item(item)

    def select_single_visible_user(self):
        visible_item = None
        
        for section in SECTIONS:
            section_items = self.__items[section]
            for id in section_items:
                item = section_items[id]
                if item.get_visible():
                    if visible_item == None:
                        visible_item = item
                    elif visible_item.resource != item.resource:
                        return # Two visible
                    
        if visible_item != None:
            self.__select_item(visible_item)
             
class PeopleBrowser(hippo.CanvasWindow):
    def __init__(self, stock):
        super(PeopleBrowser, self).__init__(gtk.WINDOW_TOPLEVEL)
        
        self.__stock = stock
        
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        

        self.set_title('People')
    
        self.__box = CanvasHBox()
    
        self.__left_box = CanvasVBox(spacing=6, padding=6, box_width=250)
        self.__left_box.set_property('background-color', 0xEEEEEEFF)
        self.__box.append(self.__left_box)
        
        self.__search_text = hippo.CanvasText(text="Search People:", font="Bold 12px",
                                              color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START)
        self.__left_box.append(self.__search_text)
        self.__search_input = hippo.CanvasEntry()
        #self.__search_input.set_property('border-color', 0xAAAAAAFF)
        self.__search_input.connect("notify::text", self.__on_search_changed)
        self.__search_input.connect("key-press-event", self.__on_search_keypress)
        self.__idle_search_id = 0
        self.__idle_search_mugshot_id = 0
        self.__left_box.append(self.__search_input)

        self.__profile_box = CanvasVBox(border=1, border_color=0x999999FF, background_color=0xFFFFFFFF)
        self.__left_box.append(self.__profile_box)
        self.__set_profile_user(None)
    
        self.__right_scroll = hippo.CanvasScrollbars()
        self.__right_scroll.set_policy(hippo.ORIENTATION_HORIZONTAL,
                                       hippo.SCROLLBAR_NEVER)
        self.__right_box = CanvasVBox(border=0, background_color=0xFFFFFFFF)
        self.__box.append(self.__right_scroll, hippo.PACK_EXPAND)
        
        self.__people_list = PeopleList()
        self.__right_box.append(self.__people_list, hippo.PACK_EXPAND)

        self.__people_list.connect("selected", self.__on_user_selected)
        
        self.__right_scroll.set_root(self.__right_box)        
        
        self.set_default_size(750, 600)
        self.connect("delete-event", lambda *args: self.__hide_reset() or True)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))
               
        self.set_root(self.__box)

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

    def __set_profile_user(self, user):
        self.__profile_box.clear()
        if user == None:
            self.__profile_box.set_property("box-height", 300)
        else:
            self.__profile_box.set_property("box_height", -1)
            self.__profile_box.append(ProfileItem(user))

    def __reset(self):
        self.__search_input.set_property('text', '')

    def __hide_reset(self):
        self.__reset()
        self.hide()

    def __idle_do_search(self):
        self.__people_list.set_search(self.__search_input.get_property("text"))
        self.__idle_search_id = 0
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.__hide_reset()

    def __on_search_changed(self, input, text):
        if self.__idle_search_id > 0:
            return
        self.__idle_search_id = gobject.timeout_add(500, self.__idle_do_search)
        
    def __on_search_keypress(self, entry, event):
        if event.key == hippo.KEY_RETURN:
            self.__people_list.select_single_visible_user()

    def __on_user_selected(self, list, user):
         self.__set_profile_user(user)
         
    def __on_contact_added(self, list, contact):
        self.__people_list.add_user(contact, CONTACTS)
        
    def __on_contact_removed(self, list, contact):
        self.__people_list.remove_user(contact, CONTACTS)
        
    def __on_local_user_added(self, list, user):
        if user.resource_id == self.__model.self_id:
            return
        
        self.__people_list.add_user(user, LOCAL_PEOPLE)
        
    def __on_local_user_removed(self, list, user):
        self.__remove_user(user, LOCAL_PEOPLE)
