import logging

import hippo
from ddm import DataModel

import bigboard
from bigboard.people_tracker import PeopleTracker, sort_people
from bigboard.stock import AbstractMugshotStock
import bigboard.globals
import bigboard.slideout
import bigboard.profile
import bigboard.search as search
import bigboard.libbig as libbig

import peoplebrowser
from peoplewidgets import PersonItem, ProfileItem

_logger = logging.getLogger("bigboard.stocks.PeopleStock")

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
        self.__tracker.local_people.connect("added", self.__on_local_person_added)
        self.__tracker.local_people.connect("removed", self.__on_local_person_removed)

        self.__model = DataModel(bigboard.globals.server_name)

        for person in self.__tracker.contacts:
            self.__on_contact_added(self.__tracker.contacts, person)
            
        for person in self.__tracker.local_people:
            self.__on_local_person_added(self.__tracker.local_people, person)

        ## add a new search provider (FIXME never gets disabled)
        search.enable_search_provider('people',
                                      lambda: PeopleSearchProvider(self.__tracker))

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

    def __add_person(self, person, box, map):
        self._logger.debug("person added to people stock %s" % (person.display_name))
        if map.has_key(person):
            return
        
        item = PersonItem(person)
        box.insert_sorted(item, hippo.PACK_IF_FITS, lambda a,b: sort_people(a.person, b.person))

        def resort(*args):
            box.remove(item)
            box.insert_sorted(item, hippo.PACK_IF_FITS, lambda a,b: sort_people(a.person, b.person))

        if person.is_user:
            person.resource.connect(resort, 'contactStatus')
        person.connect('display-name-changed', resort)
        
        map[person] = item
        self.__set_item_size(item, self.get_size())
        item.connect('activated', self.__handle_item_pressed)

        self.__update_separators()

    def __remove_person(self, person, box, map):
        try:
            item = map[person]
        except KeyError:
            return
        
        item.destroy()
        del map[person]
        
        self.__update_separators()

    def __on_contact_added(self, list, contact):
        self.__add_person(contact, self.__contact_box, self.__contact_items)
        
    def __on_contact_removed(self, list, contact):
        self.__remove_person(contact, self.__contact_box, self.__contact_items)
        
    def __on_local_person_added(self, list, person):
        if person == self.__model.self_resource:
            return
        
        self.__add_person(person, self.__local_box, self.__local_items)
        
    def __on_local_person_removed(self, list, person):
        self.__remove_person(person, self.__local_box, self.__local_items)
        
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

        p = ProfileItem(item.get_person(),
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


class PeopleSearchResult(search.SearchResult):
    def __init__(self, provider, person):
        super(PeopleSearchResult, self).__init__(provider)
        self.__person = person

    def get_title(self):
        return self.__person.name

    def get_detail(self):
        return self.__person.name

    def get_icon(self):
        """Returns an icon for the result"""
        return None

    def _on_highlighted(self):
        """Action when user has highlighted the result"""
        pass

    def _on_activated(self):
        """Action when user has activated the result"""
        libbig.show_url(self.__person.homeUrl)

class PeopleSearchProvider(search.SearchProvider):    
    def __init__(self, tracker):
        super(PeopleSearchProvider, self).__init__()
        self.__tracker = tracker

    def get_heading(self):
        return "People"
        
    def perform_search(self, query, consumer):
        results = []
        
        for p in self.__tracker.contacts:
            #_logger.debug("contact: " + str(p))

            email = None
            if person.is_user:
                try:
                    email = p.resource.email
                except AttributeError:
                    pass

            aim = p.aim
            
            if query in p.display_name or (email and (query in email)) or (p.aim and (query in p.aim)):
                results.append(PeopleSearchResult(self, p))
                
        for p in self.__tracker.aim_people:
            #_logger.debug("aim: " + str(p))
            pass ## FIXME
        for p in self.__tracker.local_people:
            #_logger.debug("local: " + str(p))
            pass ## FIXME            

        if len(results) > 0:
            consumer.add_results(results)
