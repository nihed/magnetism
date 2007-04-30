import logging, os, datetime

import gobject, pango 
import hippo

import bigboard.mugshot as mugshot
import bigboard.stock as stock
import bigboard.google as google
from bigboard.stock import AbstractMugshotStock
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasVBox
import bigboard.libbig.polling as polling

class EventDisplay(CanvasVBox):
    def __init__(self, event):
        super(EventDisplay, self).__init__(border_right=6)
        self.__event = None
                
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=2, 
                                     border_right=4)
        self.__title = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__description = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
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

    def _fmt_time(self, dt):
        date_str = str(dt.date())
        if dt.time().hour == 0 and dt.time().minute == 0 and  dt.time().second == 0:
            return date_str
        return date_str + " " + str(dt.time())
    
    def __event_display_sync(self):
        self.__title.set_property("text", self.__event.get_title())
        self.__description.set_property("text", "  " + (self._fmt_time(self.__event.get_start_time())))

        now = datetime.datetime.now()
        if self.__event.get_end_time() < now:
            attrs = pango.AttrList()
            attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
            self.__title.set_property("attributes", attrs)        
            self.__description.set_property("attributes", attrs)        
        # stuff for today is bold
        if self.__event.get_start_time() < now:
            delta = now - self.__event.get_start_time()
        else:
            delta = self.__event.get_start_time() - now
        if delta.days == 0:
            self.__title.set_property("font", "12px Bold")
        
    def __on_button_press(self, event):
        if event.button != 1:
            return False
        
        logging.debug("activated event %s", self)

        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', self.__event.get_link())

class CalendarStock(AbstractMugshotStock, polling.Task):
    def __init__(self, *args, **kwargs):
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self.__events = {}

        self.__auth_ui = google.AuthCanvasItem()
        self.__box.append(self.__auth_ui)
        google.get_google().add_auth_ui(self.__auth_ui)

        # these are at the end since they have the side effect of calling on_mugshot_ready it seems?
        AbstractMugshotStock.__init__(self, *args, **kwargs)
        polling.Task.__init__(self, 1000 * 120)

    def _on_mugshot_ready(self):
        super(CalendarStock, self)._on_mugshot_ready()
        self.__update_events()

    def do_periodic_task(self):
        self.__update_events()

    def get_authed_content(self, size):
        return size == self.SIZE_BULL and self.__box or None
            
    def set_size(self, size):
        super(CalendarStock, self).set_size(size)

    def __on_load_events(self, events):
        auth_was_visible = self.__auth_ui.get_visible()
        self.__box.remove_all()
        self.__box.append(self.__auth_ui) # put this back, kind of a hack
        self.__auth_ui.set_visible(auth_was_visible)
        events = list(events)
        events.reverse()
        for event in events:
            now = datetime.datetime.now()            
            if event.get_end_time() < now:
                delta = now - event.get_end_time()
                # don't show stuff older than a week
                if delta.days >= 7:
                    continue
            display = EventDisplay(event)
            self.__box.append(display)

    def __on_failed_load(self, exc_info):
        pass
            
    def __update_events(self):
        logging.debug("retrieving events")
        google.get_google().fetch_calendar(self.__on_load_events, self.__on_failed_load)
