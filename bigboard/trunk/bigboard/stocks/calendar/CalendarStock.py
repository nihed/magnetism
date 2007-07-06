import logging, os, datetime, string

import gobject, pango, dbus, dbus.glib
import hippo

import bigboard.libbig as libbig
import bigboard.global_mugshot as global_mugshot
import bigboard.stock as stock
import bigboard.google as google
from bigboard.stock import AbstractMugshotStock
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasVBox
import bigboard.libbig.polling as polling

_logger = logging.getLogger("bigboard.stocks.CalendarStock")

def fmt_time(dt):
    date_str = str(dt.date())
    if dt.time().hour == 0 and dt.time().minute == 0 and  dt.time().second == 0:
        return date_str
    return date_str + " " + str(dt.time())

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
    
    def __event_display_sync(self):
        self.__title.set_property("text", self.__event.get_title())
        self.__description.set_property("text", "  " + (fmt_time(self.__event.get_start_time())))

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
        
        _logger.debug("activated event %s", self)

        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', self.__event.get_link())

class CalendarStock(AbstractMugshotStock, polling.Task):
    def __init__(self, *args, **kwargs):
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self.__event_alerts = {}

        self.__auth_ui = google.AuthCanvasItem()
        self.__box.append(self.__auth_ui)
        gobj = google.get_google()
        gobj.add_auth_ui(self.__auth_ui)
        gobj.connect("auth", self.__on_google_auth)

        # these are at the end since they have the side effect of calling on_mugshot_ready it seems?
        AbstractMugshotStock.__init__(self, *args, **kwargs)
        polling.Task.__init__(self, 1000 * 120)
        
        bus = dbus.SessionBus()

        o = bus.get_object('org.freedesktop.Notifications', '/org/freedesktop/Notifications')
        self.__notifications_proxy = dbus.Interface(o, 'org.freedesktop.Notifications')

        self.__notifications_proxy.connect_to_signal('ActionInvoked', self.__on_action)

        self.__on_google_auth(gobj, gobj.have_auth())

        self._add_more_link(self.__on_more_link)

    # what to do when buttons on the notification are clicked
    def __on_action(self, *args):
        notification_id = args[0]
        action = args[1]

        if string.find(action, 'view_event') >= 0:
            # the rest of the view_event action is the link to the event,
            # we should use a shorter event id in the future
            _logger.debug("will visit %s", action[10:])
            libbig.show_url(action[10:])
        elif action == 'calendar' or action == 'default':
            _logger.debug("will visit calendar")
            libbig.show_url("http://calendar.google.com")
        else:
            _logger.debug("unknown action: %s", action)   
            print "unknown action " + action

    def __on_more_link(self):
        libbig.show_url('http://calendar.google.com')

    def _on_mugshot_ready(self):
        super(CalendarStock, self)._on_mugshot_ready()
        self.__update_events()

    def __on_google_auth(self, gobj, have_auth):
        _logger.debug("google auth state: %s", have_auth)
        if have_auth:
            self.start()
        else:
            self.stop()

    def do_periodic_task(self):
        self.__update_events()

    def get_authed_content(self, size):
        return size == self.SIZE_BULL and self.__box or None
            
    def set_size(self, size):
        super(CalendarStock, self).set_size(size)

    def __on_load_events(self, events):
        _logger.debug("loading events %s", events)
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
            else: 
                delta = event.get_start_time() - now
                # notify about an event if it is currently happening or coming up in the next 24 hours,
                # and we have not yet notified about it
                # TODO: use alert preferences from the event
                # this won't create an additional notification if the event time changes,
                # so will need to check if the time is the same
                # should also see what happens for the recurring events 
                if delta.days < 1 and not self.__event_alerts.has_key(event.get_link()):
                    self.__event_alerts[event.get_link()] = event
                    self.__notifications_proxy.Notify("BigBoard",
                                                      0, # "id" - 0 to not replace any existing
                                                      "", # icon name
                                                      event.get_title(),   # summary
                                                      fmt_time(event.get_start_time()), # body
                                                      ['view_event' + event.get_link(),
                                                       "View Event",
                                                       'calendar',
                                                       "View Calendar"], # action array
                                                       {'foo' : 'bar'}, # hints (pydbus barfs if empty)
                                                       10000) # timeout, 10 seconds        
            display = EventDisplay(event)
            self.__box.append(display)

    def __on_failed_load(self, exc_info):
        pass
            
    def __update_events(self):
        _logger.debug("retrieving events")
        google.get_google().fetch_calendar(self.__on_load_events, self.__on_failed_load)
