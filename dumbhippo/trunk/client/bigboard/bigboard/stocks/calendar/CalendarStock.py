import logging, os

import gobject, pango 
import hippo

import mugshot, stock, google
from bigboard.stock import AbstractMugshotStock
from big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasVBox

class EventDisplay(CanvasVBox):
    def __init__(self, event):
        super(EventDisplay, self).__init__(border_right=6)
        self.__event = None
                
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=2, 
                                     border_right=4)
        self.__title = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__description = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        self.__description.set_property("attributes", attrs)        
        self.__box.append(self.__title)
        self.__box.append(self.__description)        
        
        self.append(self.__box)
    
        self.connect("button-press-event", lambda self, event: self.__on_button_press(event))
        
        self.set_event(event)
        
    def set_event(self, event):
        self.__event = event
        #self.__event.connect("changed", lambda event: self.__event_display_sync())
        self.__event_display_sync()
    
    def __get_title(self):
        if self.__event is None:
            return "unknown"
        return self.__event.get_title()
    
    def __str__(self):
        return '<EventDisplay name="%s">' % (self.__get_title())
    
    def __event_display_sync(self):
        self.__title.set_property("text", self.__event.get_title())
        self.__description.set_property("text", str(self.__event.get_start_time()))
        
    def __on_button_press(self, event):
        if event.button != 1:
            return False
        
        logging.debug("activated event %s", self)

        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', self.__event.get_link())

class CalendarStock(AbstractMugshotStock):
    def __init__(self, *args, **kwargs):
        super(CalendarStock, self).__init__(*args, **kwargs)
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self.__events = {}

    def _on_mugshot_ready(self):
        super(CalendarStock, self)._on_mugshot_ready()
        self.__update_events()

    def get_authed_content(self, size):
        return self.__box
            
    def _set_item_size(self, item, size):
        if size == bigboard.Stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        item.set_size(size)       
            
    def set_size(self, size):
        super(CalendarStock, self).set_size(size)
        for child in self.__box.get_children():
            self._set_item_size(child, size)        

    def __on_load_events(self, events):
        self.__box.remove_all()
        for event in events:
            display = EventDisplay(event)
            self.__box.append(display)

    def __on_failed_load(self, exc_info):
        pass
            
    def __update_events(self):
        logging.debug("retrieving events")
        google.Google().fetch_calendar(self.__on_load_events, self.__on_failed_load)
