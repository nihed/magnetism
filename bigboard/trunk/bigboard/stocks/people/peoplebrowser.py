import logging, time, urlparse, urllib

import gobject, gtk
import hippo

import bigboard.mugshot as mugshot
import bigboard.libbig as libbig
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, \
             ActionLink, PrelightingCanvasBox
from bigboard.overview_table import OverviewTable

_logger = logging.getLogger("bigboard.PeopleBrowser")

class PeopleList(OverviewTable):
    __gsignals__ = {
        "selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
    }

    def __init__(self):
        pass

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
        self.__idle_search_id = 0
        self.__idle_search_mugshot_id = 0
        self.__left_box.append(self.__search_input)        
    
        self.__right_scroll = hippo.CanvasScrollbars()
        self.__right_scroll.set_policy(hippo.ORIENTATION_HORIZONTAL,
                                       hippo.SCROLLBAR_NEVER)
        self.__right_box = CanvasVBox(border=0, background_color=0xFFFFFFFF)
        self.__box.append(self.__right_scroll, hippo.PACK_EXPAND)
        
        self.__people_list = PeopleList()
        self.__right_box.append(self.__people_list, hippo.PACK_EXPAND)
#        self.__app_list.connect("selected", lambda list, app: self.__on_app_selected(app))
        
        self.__right_scroll.set_root(self.__right_box)        
        
        self.set_default_size(750, 600)
        self.connect("delete-event", lambda *args: self.__hide_reset() or True)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))
               
        self.set_root(self.__box)

    def __reset(self):
        self.__search_input.set_property('text', '')

    def __hide_reset(self):
        self.__reset()
        self.hide()

    def __idle_do_search(self):
        self.__app_list.set_search(self.__search_input.get_property("text"))
        self.__idle_search_id = 0
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.__hide_reset()

    def __on_search_changed(self, input, text):
        if self.__idle_search_id > 0:
            return
        self.__idle_search_id = gobject.timeout_add(500, self.__idle_do_search)
        
