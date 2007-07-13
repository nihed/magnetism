import logging, os, datetime, re, string
import xml, xml.sax, xml.sax.saxutils

import gobject, pango, dbus, dbus.glib
import hippo

import gdata.calendar as gcalendar
import bigboard.libbig as libbig
import bigboard.global_mugshot as global_mugshot
import bigboard.stock as stock
import bigboard.google as google
from bigboard.stock import AbstractMugshotStock
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasVBox, CanvasHBox, ActionLink, Button
from bigboard.libbig.struct import AutoStruct
import bigboard.libbig.polling as polling

_logger = logging.getLogger("bigboard.stocks.CalendarStock")

_events_polling_periodicity_seconds = 120

## this is from the "Wuja" applet code, GPL v2
def parse_timestamp(timestamp, tz=None):
    """ Convert internet timestamp (RFC 3339) into a Python datetime
    object.
    """
    date_str = None
    hour = None
    minute = None
    second = None
    # Single occurrence all day events return with only a date:
    if timestamp.count('T') > 0:
        date_str, time_str = timestamp.split('T')
        time_str = time_str.split('.')[0]
        if time_str.find(':') >= 0:
            hour, minute, second = time_str.split(':')
        else:
            hour, minute, second = (time_str[0:2], time_str[2:4], time_str[4:6])
    else:
        date_str = timestamp
        hour, minute, second = 0, 0, 0

    if date_str.find('-') >= 0:
        year, month, day = date_str.split('-')
    else:
        year, month, day = (date_str[0:4], date_str[4:6], date_str[6:8])
    return datetime.datetime(int(year), int(month), int(day), int(hour), int(minute),
        int(second), tzinfo=tz)

def fmt_datetime(dt):
    today = datetime.date.today()
    if today == dt.date():
        date_str = "Today"
    elif today + datetime.timedelta(1) == dt.date():
        date_str = "Tomorrow"
    else: 
        date_str = str(dt.date())
     
    if dt.time().hour == 0 and dt.time().minute == 0 and  dt.time().second == 0:
        return date_str
    return date_str + " " + fmt_time(dt)

def fmt_time(dt):     
    time = dt.time().strftime("%I:%M%p")
    return time.find("0") == 0 and (time[1:].lower() + "  ") or time.lower()

def fmt_date(date):
    today = datetime.date.today()
    abbreviated_date_str = date.strftime("%a %b %d")   
    if today == date:
        return "Today - " + abbreviated_date_str
    elif today + datetime.timedelta(1) == date:
        return "Tomorrow - " + abbreviated_date_str
    elif today - datetime.timedelta(1) == date:
        return "Yesterday - " + abbreviated_date_str
    
    return date.strftime("%A %b %d")

def fmt_canvas_text(canvas_text, is_today, is_over):
    # stuf that is over is grey 
    if is_over:
        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        canvas_text.set_property("attributes", attrs)               
    # stuff for today is bold
    if is_today:
        canvas_text.set_property("font", "13px Bold")

class Event(AutoStruct):
    def __init__(self):
        super(Event, self).__init__({ 'calendar_title' : '', 'title' : '', 'start_time' : '', 'end_time' : '', 'link' : '' })
        self.__event_entry = None

    def set_event_entry(self, event_entry):
        self.__event_entry = event_entry

    def get_event_entry(self):
        return self.__event_entry 

class EventsParser:
    def __init__(self, data):
        self.__events = []
        self.__events_sorted = False
        self.__dt_re = re.compile(r'DT[\w;=/_]+:(\d+)(?:T(\d+))\s')
        self.__parseEvents(data)

    def __parseEvents(self, data):
        calendar = gcalendar.CalendarEventFeedFromString(data)
        _logger.debug("number of entries: %s ", len(calendar.entry)) 
        
        calendar_title = calendar.title.text and calendar.title.text or "<No Title>"
        for entry in calendar.entry:  
            e = Event()
            self.__events.append(e)
            e.set_event_entry(entry)
            e.update({ 'calendar_title' : calendar_title, 
                       'title' : entry.title.text and entry.title.text or "<No Title>", 
                       'link' : entry.GetHtmlLink().href })           

            for when in entry.when:
                # _logger.debug("start time %s\n" % (parse_timestamp(when.start_time),))
                # _logger.debug("end time %s\n" % (parse_timestamp(when.end_time),))     
                e.update({ 'start_time' : parse_timestamp(when.start_time),
                           'end_time' : parse_timestamp(when.end_time) })
                # for reminder in when.reminder:
                    # _logger.debug('%s %s\n '% (reminder.minutes, reminder.extension_attributes['method']))
        
            # if this is a recurring event, use the first time interval it occurres for the start and end time
            # TODO: double check that this also applies for all-day events that are saved as one-time recurrences
            if entry.recurrence is not None:
                dt_start = None
                dt_end = None
                # _logger.debug("recurrence text %s" % (entry.recurrence.text,))
                match = self.__dt_re.search(entry.recurrence.text)
                if match:
                    dt_start = match.group(1) 
                    if match.group(2) is not None: 
                        dt_start = dt_start + "T" + match.group(2)
                    match = self.__dt_re.search(entry.recurrence.text, match.end())
                    if match:
                        dt_end = match.group(1)
                        if match.group(2) is not None: 
                            dt_end = dt_end + "T" + match.group(2)
                if dt_start and dt_end:
                    # _logger.debug("recurrence start time %s\n" % (parse_timestamp(dt_start),))
                    # _logger.debug("recurrence end time %s\n" % (parse_timestamp(dt_end),))   
                    e.update({ 'start_time' : parse_timestamp(dt_start),
                               'end_time' : parse_timestamp(dt_end) })

    def __compare_by_date(self, a, b):
        return cmp(a.get_start_time(), b.get_start_time())

    def get_events(self):
        if not self.__events_sorted:
            self.__events.sort(self.__compare_by_date)
            self.__events_sorted = True
        return self.__events

class EventDisplay(CanvasVBox):
    def __init__(self, event):
        super(EventDisplay, self).__init__(border_right=2)
        self.__event = None
                
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        self.__title = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__box.append(self.__title)
        
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
        self.__title.set_property("text", fmt_time(self.__event.get_start_time()) + "  " + self.__event.get_title())
        is_today = self.__event.get_start_time().date() == datetime.date.today()
        is_over = self.__event.get_end_time() < datetime.datetime.now()
        fmt_canvas_text(self.__title, is_today, is_over)
        
    def __on_button_press(self, event):
        if event.button != 1:
            return False
        
        _logger.debug("activated event %s", self)

        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', self.__event.get_link())

class CalendarStock(AbstractMugshotStock, polling.Task):
    def __init__(self, *args, **kwargs):
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        self.__events = []
        self.__events_for_day_displayed = None
        self.__day_displayed = datetime.date.today()
        self.__top_event_displayed = None
        self.__move_up = False
        self.__move_down = False

        self.__event_alerts = {}
        self.__event_notify_ids = {}
         
        # these are at the end since they have the side effect of calling on_mugshot_ready it seems?
        AbstractMugshotStock.__init__(self, *args, **kwargs)
        polling.Task.__init__(self, _events_polling_periodicity_seconds * 1000)
        
        bus = dbus.SessionBus()
        o = bus.get_object('org.freedesktop.Notifications', '/org/freedesktop/Notifications')
        self.__notifications_proxy = dbus.Interface(o, 'org.freedesktop.Notifications')
        self.__notifications_proxy.connect_to_signal('ActionInvoked', self.__on_action)
        
        gobj = google.get_google()
        gobj.connect("auth", self.__on_google_auth)
        if gobj.have_auth():
            self.__on_google_auth(gobj, True) 
        else:
            gobj.request_auth()

        self._add_more_button(self.__on_more_button)

    def __change_day(self):
        self.__events_for_day_displayed = None
        self.__top_event_displayed = None
        self.__refresh_events()

    def __do_next(self):
        self.__day_displayed = self.__day_displayed + datetime.timedelta(1)
        self.__change_day()
        
    def __do_prev(self):
        self.__day_displayed = self.__day_displayed - datetime.timedelta(1)
        self.__change_day()

    def __on_today_button(self):
        self.__day_displayed = datetime.date.today()
        self.__change_day()

    def __on_up_button(self):
        self.__move_up = True
        self.__refresh_events()
        #__refresh_events() restes it too, but we do it here just in case
        self.__move_up = False
        
    def __on_down_button(self):
        self.__move_down = True
        self.__refresh_events()
        #__refresh_events() restes it too, but we do it here just in case
        self.__move_down = False

    # what to do when buttons on the notification are clicked
    def __on_action(self, *args):
        notification_id = args[0]
        action = args[1]

        if notification_id not in self.__event_notify_ids.values():
            return

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

    def __on_more_button(self):
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

    def __on_calendar_load(self, url, data):
        _logger.debug("loaded calendar from " + url)
        try:
            p = EventsParser(data)
            self.__on_load_events(p.get_events())
        except xml.sax.SAXException, e:
            __on_failed_load(sys.exc_info())

    def __on_load_events(self, events):
        _logger.debug("loading events %s", events)
        self.__events = list(events)
        self.__events_for_day_displayed = None
        for event in self.__events:
            now = datetime.datetime.now()            
            if event.get_end_time() >= now:
                delta = event.get_start_time() - now
                delta_seconds = (delta.days * 3600 * 24) + delta.seconds 
                # notify about an event if it is currently happening or coming up in the next 24 hours,
                # and we have not yet notified about it
                # TODO: use alert preferences from the event
                # this won't create an additional notification if the event time changes,
                # so will need to check if the time is the same
                # should also see what happens for the recurring events
                # TODO: create only a single alert in the same cycle 
                entry = event.get_event_entry() 
                for when in entry.when:
                    for reminder in when.reminder:
                        reminder_seconds = int(reminder.minutes) * 60  
                        # _logger.debug('delta days %s delta seconds %s reminder seconds %s %s\n '% (delta.days, delta_seconds, reminder_seconds, reminder.extension_attributes['method']))
                        # schedule notifications for alerts that need to happen before the next time we poll events
                        if reminder.extension_attributes['method'] == 'alert' and (delta_seconds - _events_polling_periodicity_seconds) < reminder_seconds and not self.__event_alerts.has_key(event.get_link() + reminder.minutes):   
                           
                           self.__event_alerts[event.get_link() + reminder.minutes] = event
                           if delta_seconds > reminder_seconds:
                               gobject.timeout_add((delta_seconds - reminder_seconds) * 1000, self.__create_notification, event)
                           else:
                               self.__create_notification(event)

        self.__refresh_events()


    def __refresh_events(self):      
        self.__box.remove_all()

        title = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        title.set_property("text", fmt_date(self.__day_displayed))
        title.set_property("font", "13px Bold")
        self.__box.append(title) 

        content_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        content_box.set_property("box-height", 95)

        arrows_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        arrows_box.set_property("box-height", 95)
        arrows_box.set_property("padding-top", 3)
        arrows_box.set_property("padding-bottom", 2)
        arrows_box.set_property("padding-right", 2)
 
        # we set the button images below, because we want to have them enabled or disabled 
        # depending on whether there are more events to scroll to
        up_button = hippo.CanvasImage(yalign=hippo.ALIGNMENT_START)
        up_button.set_clickable(True)
        up_button.connect("button-press-event", lambda text, event: self.__on_up_button())
        arrows_box.append(up_button)

        down_button = hippo.CanvasImage(yalign=hippo.ALIGNMENT_END)
        down_button.set_clickable(True)
        down_button.connect("button-press-event", lambda text, event: self.__on_down_button())
        arrows_box.append(down_button, hippo.PACK_EXPAND)

        content_box.append(arrows_box) 

        events_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, yalign=hippo.ALIGNMENT_START, spacing=3)
        events_box.set_property("box-height", 95)
        if self.__events_for_day_displayed is None:
            self.__events_for_day_displayed = []
            for event in self.__events:
                # TODO we can break from this loop once we pass the day displayed, since the 
                # events are sorted by start time
                if event.get_start_time().date() == self.__day_displayed: 
                    self.__events_for_day_displayed.append(event) 
                     
        events_to_display = 5
        index = 0
        today = datetime.date.today()
        now = datetime.datetime.now()
        # we expect the events to be ordered by start time
        for event in self.__events_for_day_displayed:    
            # by default, start with the event that is still happenning if displaying today's agenda,
            # start with the first event for any other day
            # this should have an effect of re-centering the calendar on data reloads
            if self.__top_event_displayed is not None:
                # we need to handle scrolling through multiple events with the same start time, so we use
                # the links to compare events; however, on refresh, an event with a given link might be gone,
                # so we should include the next one after it timewise; it would be ideal to include all other
                # events we previously included that have the same time as the removed event, but that would
                # require a bit more work
                finalize_index = event.get_link() == self.__top_event_displayed.get_link() or event.get_start_time() > self.__top_event_displayed.get_start_time()                 
            elif self.__day_displayed == today:
                finalize_index = event.get_end_time() >= now
                _logger.debug("finalize_index %s", finalize_index)
            else:
                finalize_index = True   

            if finalize_index:

                if self.__day_displayed == today and self.__top_event_displayed is not None and self.__top_event_displayed.get_end_time() > now and self.__move_up:
                    new_index = max(index - events_to_display, 0)
                    if self.__events_for_day_displayed[new_index].get_end_time() < now:
                        # we need to search for the index to start with that should be between new_index and index
                        while self.__events_for_day_displayed[new_index].get_end_time() < now and new_index < index:  
                            new_index = new_index + 1
                        if index == new_index:
                            # if __top_event_displayed is the first one that is current, we want to process
                            # that below
                            self.__top_event_displayed = None
                        else: 
                            # we just want to re-center in this case, so return to default
                            self.__move_up = False
                            self.__top_event_displayed = None
                            index = new_index
                            break

                if self.__day_displayed == today and self.__top_event_displayed is None and self.__move_up and index % events_to_display != 0:
                    # we are centered, but we should change into the mode where pages we display are 
                    # consistent when we are scrolling up to past events
                    index = max(index - (index % events_to_display), 0)                     
                elif self.__move_up:
                    # display the previous page of events if the user wanted to move up
                    index = max(index - events_to_display, 0) 
                  
                if self.__day_displayed == today and self.__top_event_displayed is not None and self.__top_event_displayed.get_end_time() < now and self.__move_down:
                    new_index = min(index + events_to_display, len(self.__events_for_day_displayed) - 1)
                    if self.__events_for_day_displayed[new_index].get_end_time() >= now:
                        # we just want to re-center in this case, so don't break and return to default
                        self.__move_down = False
                        self.__top_event_displayed = None
                        index = index + 1  
                        _logger.debug("we decide to continue")
                        continue

                # skip the first page of events if the user wanted to move down
                # events_to_display should be greater than 0, but let's include the
                # last check just in case
                if self.__move_down and index < len(self.__events_for_day_displayed) - events_to_display and index < len(self.__events_for_day_displayed) - 1:                   
                    index = index + events_to_display 

                break
            _logger.debug("index + 1 %d", index)
            index = index + 1   

        end_index = 0  
        if len(self.__events_for_day_displayed) > 0:
            end_index = min(index + events_to_display, len(self.__events_for_day_displayed))
        
            for event in self.__events_for_day_displayed[index:end_index]:
                display = EventDisplay(event)
                events_box.append(display)

            if self.__move_up or self.__move_down or self.__top_event_displayed is not None:
                self.__top_event_displayed = self.__events_for_day_displayed[index]    
        else:
            no_events_text = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
            no_events_text.set_property("text", "No events scheduled")
            today = datetime.date.today()
            # always pass in false for is_today because we don't want to make it bold 
            is_today = False # self.__day_displayed == today
            is_over = self.__day_displayed < today
            fmt_canvas_text(no_events_text, is_today, is_over)
            events_box.append(no_events_text)  

        if index > 0:
            up_button.set_property('image-name', 'bigboard-up-arrow-enabled.png') 
        else:
            up_button.set_property('image-name', 'bigboard-up-arrow-disabled.png')         

        if end_index < len(self.__events_for_day_displayed):
            down_button.set_property('image-name', 'bigboard-down-arrow-enabled.png') 
        else:
            down_button.set_property('image-name', 'bigboard-down-arrow-disabled.png') 

        self.__move_up = False
        self.__move_down = False 
   
        content_box.append(events_box) 
        self.__box.append(content_box)
       
        control_box = CanvasHBox(xalign=hippo.ALIGNMENT_CENTER)
        control_box.set_property("padding-top", 5) 

        left_button = hippo.CanvasImage(xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_CENTER)
        left_button.set_property('image-name', 'bigboard-left-button.png') 
        left_button.set_clickable(True)
        left_button.connect("button-press-event", lambda text, event: self.__do_prev())
        left_button.set_property("padding-right", 4)
        control_box.append(left_button)

        today_button = Button()      
        today_button.set_property("text", "Today")      
        today_button.get_label().modify_font(pango.FontDescription("normal 8"))
        today_button.get_label().set_padding(4, 0)         
        today_button.connect("activated", lambda l: self.__on_today_button())
        if self.__day_displayed == datetime.date.today():
            today_button.get_button().set_sensitive(False)
        control_box.append(today_button)

        right_button = hippo.CanvasImage(xalign=hippo.ALIGNMENT_END, yalign=hippo.ALIGNMENT_CENTER)
        right_button.set_property('image-name', 'bigboard-right-button.png') 
        right_button.set_clickable(True)
        right_button.connect("button-press-event", lambda text, event: self.__do_next())
        right_button.set_property("padding-left", 4)
        control_box.append(right_button, hippo.PACK_EXPAND)

        self.__box.append(control_box)  

    def __create_notification(self, event):
        # We don't want multiple alerts for the same event to be showing at the same time, 
        # so make sure we use a single notification for all alert times for the same event
        # (we otherwise would have displayed multiple alerts for the same event
        # when bigboard is started and some alert times for upcoming events were missed).  
        # When notify_id = 0 we will not replace any existing notification.
        notify_id = 0
        if self.__event_notify_ids.has_key(event.get_link()):      
            notify_id = self.__event_notify_ids[event.get_link()]

        body = "<i>for: " + fmt_datetime(event.get_start_time()) + \
               "\nfrom: " + xml.sax.saxutils.escape(event.get_calendar_title()) + "</i>"

        if event.get_event_entry().content.text is not None:
            body = body + "\n\n" + xml.sax.saxutils.escape(event.get_event_entry().content.text)

        self.__event_notify_ids[event.get_link()] = self.__notifications_proxy.Notify(
                                                        "BigBoard",
                                                        notify_id, # "id"
                                                        "stock_timer", # icon name
                                                        event.get_title(),   # summary
                                                        body, # body
                                                        ['view_event' + event.get_link(),
                                                         "View Event",
                                                         'calendar',
                                                         "View Calendar"], # action array
                                                        {'foo' : 'bar'}, # hints (pydbus barfs if empty)
                                                        10000) # timeout, 10 seconds                  
        return False       
     
    def __on_failed_load(self, response):
        pass
            
    def __update_events(self):
        _logger.debug("retrieving events")
        google.get_google().fetch_calendar(self.__on_calendar_load, self.__on_failed_load)
