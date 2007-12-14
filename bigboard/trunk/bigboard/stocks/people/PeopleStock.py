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
import bigboard.scroll_ribbon as scroll_ribbon

import peoplebrowser
from peoplewidgets import PersonItem, ProfileItem

_logger = logging.getLogger("bigboard.stocks.PeopleStock")

class PeopleStock(AbstractMugshotStock):
    def __init__(self, *args, **kwargs):
        super(PeopleStock, self).__init__(*args, **kwargs)

        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        
        self.__scroll_box = scroll_ribbon.VerticalScrollArea()
        self.__scroll_box.set_increment(50)
        self.__box.append(self.__scroll_box, hippo.PACK_EXPAND)

        self.__person_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self.__scroll_box.add(self.__person_box)

        self.__person_items = {}

        self.__slideout = None
        self.__slideout_item = None

        self.__people_browser = None
        self._add_more_button(self.__on_more_button)        
        
        self.__tracker = PeopleTracker()
        self.__tracker.people.connect("added", self.__on_person_added)
        self.__tracker.people.connect("removed", self.__on_person_removed)

        self.__model = DataModel(bigboard.globals.server_name)

        for person in self.__tracker.people:
            self.__on_person_added(self.__tracker.people, person)
            
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
        for i in self.__person_items.values():
            self.__set_item_size(i, size)

    def __add_person(self, person, box, map):
        self._logger.debug("person added to people stock %s" % (person.display_name))
        if map.has_key(person):
            return
        
        item = PersonItem(person)
        box.insert_sorted(item, hippo.PACK_IF_FITS, lambda a,b: sort_people(a.person, b.person))

        def resort(*args):
            box.remove(item)
            box.insert_sorted(item, hippo.PACK_IF_FITS, lambda a,b: sort_people(a.person, b.person))

        if person.is_contact:
            person.resource.connect(resort, 'status')

        person.connect('online-changed', resort)
        person.connect('display-name-changed', resort)
        
        map[person] = item
        self.__set_item_size(item, self.get_size())
        item.connect('activated', self.__handle_item_pressed)

    def __remove_person(self, person, box, map):
        try:
            item = map[person]
        except KeyError:
            return
        
        item.destroy()
        del map[person]    

    def __on_person_added(self, list, person):
        self.__add_person(person, self.__person_box, self.__person_items)
        
    def __on_person_removed(self, list, person):
        self.__remove_person(person, self.__person_box, self.__person_items)        
        
    def __close_slideout(self, object=None, action_taken=False):
        if self.__slideout:
            if action_taken:
                self._panel.action_taken()
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
        if not self.__slideout.slideout_from(coords[0] + item.get_allocation()[0] + 4, coords[1]):
            self.__close_slideout()
            return

        p = ProfileItem(item.get_person(),
                        border=1,
                        border_color = 0x0000000ff)

        self.__slideout.get_root().append(p)
        p.connect("close", self.__close_slideout)
        self.__slideout.connect("close", self.__close_slideout)

        return True

    def __on_more_button(self):
        if self.__people_browser is None:
            self.__people_browser = peoplebrowser.PeopleBrowser(self)
        if self.__people_browser.get_property('is-active'):
            self.__people_browser.hide()
        else:
            self.__people_browser.present()

    def on_popped_out_changed(self, popped_out):
        if not popped_out:
            self.__close_slideout()

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
        
        for p in self.__tracker.people:
            #_logger.debug("person: " + str(p))

            matched = False
            if query in p.display_name:
                matched = True

            if p.is_contact and not matched:
                emails = []
                try:
                    emails = person.resource.emails
                except AttributeError:
                    pass
                for email in emails:
                    if query in email.lower():
                        matched = True
                        break
            
            if not matched:
                if p.aim and query in p.aim:
                    matched = True
                    
            if matched:
                results.append(PeopleSearchResult(self, p))

        if len(results) > 0:
            consumer.add_results(results)
