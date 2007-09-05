import logging, os, datetime, re, string, copy
import xml, xml.sax, xml.sax.saxutils

import gobject, pango, dbus, dbus.glib
import hippo

import gdata.calendar as gcalendar
import bigboard.libbig as libbig
import bigboard.global_mugshot as global_mugshot
import bigboard.stock as stock
import bigboard.google as google
import bigboard.slideout as slideout
from bigboard.stock import AbstractMugshotStock
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasVBox, CanvasHBox, ActionLink, Button, PrelightingCanvasBox
from bigboard.libbig.struct import AutoStruct
import bigboard.libbig.polling as polling

_logger = logging.getLogger("bigboard.stocks.CalendarStock")

_events_polling_periodicity_seconds = 120

# we prime the calendar 14 days back and 14 days forward
# the maximum reminder time Google UI allows to specify is 1 week, so we 
# should not miss any reminders if we always get events for 2 weeks ahead 
_default_events_range = 14
_prepare_events_days = 10

def is_midnight(dt):
    return dt.time().hour == 0 and dt.time().minute == 0 and dt.time().second == 0

def fmt_datetime(dt):
    today = datetime.date.today()
    if today == dt.date():
        date_str = "Today"
    elif today + datetime.timedelta(1) == dt.date():
        date_str = "Tomorrow"
    else: 
        date_str = str(dt.date())
     
    if is_midnight(dt):
        return date_str
    return date_str + " " + fmt_time(dt, False)

def fmt_datetime_interval(dt_start, dt_end):
    first_part = fmt_datetime(dt_start)
    if is_midnight(dt_start) and not is_midnight(dt_end):
        first_part = first_part + " " + fmt_time(dt_start, False)

    if dt_start.date() == dt_end.date():
        second_part = " - " + fmt_time(dt_end, False)
    elif dt_start.date() + datetime.timedelta(1) == dt_end.date() and is_midnight(dt_start) and is_midnight(dt_end):
        second_part = " all day"
    elif is_midnight(dt_start) and is_midnight(dt_end):
        second_part = " - " + fmt_datetime(dt_end - datetime.timedelta(1))
    else:
        second_part = " - " + fmt_datetime(dt_end)
    
    if is_midnight(dt_end) and not is_midnight(dt_start):
        second_part = second_part + " " + fmt_time(dt_end, False)

    return first_part + second_part
   
def fmt_time(dt, right_justify = True):
    if right_justify:
        extra_space = "  "
    else:
        extra_space = ""    
    time = dt.time().strftime("%I:%M%p")
    return time.find("0") == 0 and (time[1:].lower() + extra_space) or time.lower()

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

def fmt_canvas_text(canvas_text, is_today, is_over, color=None):
    # stuff that is over is grey 
    if is_over:
        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        canvas_text.set_property("attributes", attrs)               
    elif color is not None:
        attrs = pango.AttrList()
        # the color is in the format '#FFFFFF' 
        attrs.insert(pango.AttrForeground(int(color[1:3], 16) * 0x101, int(color[3:5], 16) * 0x101, int(color[5:7], 16) * 0x101, 0, 0xFFFF))
        canvas_text.set_property("attributes", attrs)   
    # stuff for today is bold
    if is_today:
        canvas_text.set_property("font", "13px Bold")

def compare_by_date(event_a, event_b):
    return cmp(event_a.get_start_time(), event_b.get_start_time())

# we use calendar feed urls as calendar ids, such urls show up
# as ids for individual calendars when we get events feed, e.g.
# in calendar list feed id would be 
# http://www.google.com/calendar/feeds/example%40gmail.com/example%40gmail.com
# while id for the same calendar in the calendar events feed woud be
# http://www.google.com/calendar/feeds/example%40gmail.com/private/full
# this function converts from the first format to the second one, which
# is then used as a calendar feed url and a calendar id in the code
# This would still work fine if our access level to a certain calendar 
# changed, because we would drop events for the old calendar id, and add
# events for the new calendar id.
def create_calendar_feed_url(calendar_entry):
    calendar_id = calendar_entry.id.text
    projection = 'full'
    # we currently filter out all calendars with 'freebusy' access level, but if we included them,
    # we would have to specify the 'free-busy' projection for the feed
    if calendar_entry.access_level.value == 'freebusy':
        projection = 'free-busy'
    calendar_id_feeds_index = calendar_id.find("/feeds/")
    calendar_id_slash_index = calendar_id.find("/", calendar_id_feeds_index + len(str("/feeds/")) + 1)
    return calendar_id[:calendar_id_feeds_index + len(str("/feeds"))] + calendar_id[calendar_id_slash_index:] + "/private/" + projection

# for some reason python library doesn't provide a value
# for gCal:selected element, so we need to fish it out from
# extension elements list; it should be a simple change to add 
# getting gCal:selected in our local copy of the library 
def get_selected_value(extension_elements):
    for extension in extension_elements:
        _logger.debug("tag %s value %s", extension.tag, extension.attributes['value'])  
        if extension.tag == 'selected' and extension.namespace == 'http://schemas.google.com/gCal/2005':
            return extension.attributes['value'].find('true') >= 0 and 'true' or 'false'
    # return false since that way we are more likely to notice something is
    # wrong; though true would be a meaningful default, gCal:selected should always be there 
    return 'false'
    
# hidden calendars can still be selected, we should check both flags and only
# include the ones that are selected and not hidden
# we don't include calendars to which you only have 'freebusy' access level, because
# busy 'events' and notifications about them don't seem useful
def include_calendar(calendar):
    return get_selected_value(calendar.extension_elements) == 'true' and calendar.hidden.value == 'false' and ['owner', 'contributor', 'read'].count(calendar.access_level.value) == 1

class Event(AutoStruct):
    def __init__(self):
        super(Event, self).__init__({ 'calendar_title' : '', 'calendar_link' : '', 'color' : '', 'title' : '', 'start_time' : '', 'end_time' : '', 'link' : '', 'is_all_day' : False })
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
        calendar_link = calendar.id.text
        for entry in calendar.entry:
            original_events_length = len(self.__events) 
            entry_title = entry.title.text and entry.title.text or "<No Title>"   
            for when in entry.when:
                # we get recurrent events expanded into separate entries, however if we only specified 
                # start-min and start-max, we would get back entries with multiple "when" tags,
                # in this case we would want to create a new Event object for each instance of a recurrent 
                # event represented by a "when" tag (the disadvantage of that is that we don't get links
                # to the individual events in the recurrence) 
                e = Event()
                self.__events.append(e)
                
                if len(entry.when) == 1:
                    entry_copy = entry
                else:      
                    entry_copy = copy.deepcopy(entry) 
                    entry_copy.when = []
                    entry_copy.when.append(when)
  
                e.set_event_entry(entry_copy)
                e.update({ 'calendar_title' : calendar_title,
                           'calendar_link' : calendar_link, 
                           'title' : entry_title, 
                           'link' : entry.GetHtmlLink().href })  
                # _logger.debug("start time %s\n" % (google.parse_timestamp(when.start_time),))
                # _logger.debug("end time %s\n" % (google.parse_timestamp(when.end_time),))     
                e.update({ 'start_time' : google.parse_timestamp(when.start_time),
                           'end_time' : google.parse_timestamp(when.end_time) })
                if e.get_start_time().time() == datetime.time(0) and e.get_end_time().time() == datetime.time(0) and e.get_start_time() < e.get_end_time():
                    e.update({ 'is_all_day' : True})
                # for reminder in when.reminder:
                    # _logger.debug('%s %s\n '% (reminder.minutes, reminder.extension_attributes['method']))
        
            # if this is a recurring event, use the first time interval it occurres for the start and end time
            # TODO: double check that this also applies for all-day events that are saved as one-time recurrences
            if len(entry.when) == 0 and entry.recurrence is not None:
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
                    e = Event()
                    self.__events.append(e) 
                    e.set_event_entry(entry)
                    e.update({ 'calendar_title' : calendar_title, 
                               'title' : entry_title, 
                               'link' : entry.GetHtmlLink().href }) 
                    # _logger.debug("recurrence start time %s\n" % (google.parse_timestamp(dt_start),))
                    # _logger.debug("recurrence end time %s\n" % (google.parse_timestamp(dt_end),))   
                    e.update({ 'start_time' : google.parse_timestamp(dt_start),
                               'end_time' : google.parse_timestamp(dt_end) })

            if original_events_length == len(self.__events):
                _logger.warn("could not parse event %s\n" % (entry,))

    def get_events(self, sort = False):
        if sort and not self.__events_sorted:
            self.__events.sort(compare_by_date)
            self.__events_sorted = True
        return self.__events

class EventDisplay(PrelightingCanvasBox):
    def __init__(self, event, day_displayed):
        PrelightingCanvasBox.__init__(self,
                                      orientation=hippo.ORIENTATION_VERTICAL,
                                      padding_top=1, padding_bottom=1,
                                      border_right=2)
        self.__event = None
        self.__day_displayed = day_displayed   

        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        self.__title = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__box.append(self.__title)
        
        self.append(self.__box)
    
        # self.connect("button-press-event", lambda self, event: self.__on_button_press(event))
        
        self.set_event(event)
        
    def set_event(self, event):
        self.__event = event
        #self.__event.connect("changed", lambda event: self.__event_display_sync())
        self.__event_display_sync()
    
    def get_event(self):
        return self.__event

    def __get_title(self):
        if self.__event is None:
            return "unknown"
        return self.__event.get_title()
    
    def __str__(self):
        return '<EventDisplay name="%s">' % (self.__get_title())
    
    def __event_display_sync(self):
        if self.__event.get_is_all_day() : 
            time_portion = "all day    "
        else : 
            time_portion = fmt_time(self.__event.get_start_time())
        self.__title.set_property("text", time_portion + "  " + self.__event.get_title())
        today = datetime.date.today()
        is_today = self.__event.get_start_time().date() <= today and self.__event.get_end_time().date() >= today and self.__day_displayed == today   
        is_over = self.__event.get_end_time() < datetime.datetime.now()
        fmt_canvas_text(self.__title, is_today, is_over, self.__event.get_color())
        
    def __on_button_press(self, event):
        if event.button != 1:
            return False
        
        _logger.debug("activated event %s", self)
    
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', self.__event.get_link())

    def get_screen_coords(self):
        return self.get_context().translate_to_screen(self)

class EventDetailsDisplay(hippo.CanvasBox):
    __gsignals__ = {
        "close": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
       }
       
    def __init__(self, event, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        kwargs['border'] = 1
        kwargs['border-color'] = 0x000000ff
        hippo.CanvasBox.__init__(self, **kwargs)
        self.__event = event
        
        color = event.get_color()
        end_color=0xc8c8c8ff
        if color is not None:
            # the color is in the format '#FFFFFF' 
            end_color = int(color[1:7], 16) * 0x100 + 0xff

        self.__header = hippo.CanvasGradient(orientation=hippo.ORIENTATION_HORIZONTAL,
                                             start_color=0xf2f2f2f2,
                                             end_color=end_color)

        self.append(self.__header)
        event_link = ActionLink(text=self.__get_title(), font="14px", padding=4)
        self.__header.append(event_link)
        event_link.connect("activated", self.__on_activate_web)

        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0xFFFF, 0xFFFF, 0xFFFF, 0, 0xFFFF))
        event_link.set_property("attributes", attrs)   

        self.__header.append(event_link)

        self.__top_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, border_top=4, border_bottom=4)
        self.append(self.__top_box)

        event_time = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, padding_left=4, padding_right=4, text="for: " + fmt_datetime_interval(event.get_start_time(), event.get_end_time()))
        self.__top_box.append(event_time)

        if len(event.get_event_entry().where) > 0:
            where_string = event.get_event_entry().where[0].value_string
            if where_string is not None and len(where_string.strip()) > 0:
                event_where_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL, padding_left=4, padding_right=4)
                event_where = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, text="where: " + where_string + "   (")

                event_map_link = ActionLink(text="map")
                event_map_link.connect("activated", self.__on_activated_event_map_link)

                event_map_link_parenthesis = hippo.CanvasText(text=")")
 
                event_where_box.append(event_where)
                event_where_box.append(event_map_link)
                event_where_box.append(event_map_link_parenthesis) 
                self.__top_box.append(event_where_box)

        event_calendar = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, padding_left=4, padding_right=4, text="from: " + xml.sax.saxutils.escape(event.get_calendar_title()))
        self.__top_box.append(event_calendar)

        if event.get_event_entry().content.text is not None:
            # TODO: description might have urls which it would be nice to detect and display 
            event_description = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, padding_left=4, padding_right=4, size_mode=hippo.CANVAS_SIZE_WRAP_WORD, text=xml.sax.saxutils.escape(google.html_to_text(event.get_event_entry().content.text)))
            self.__top_box.append(event_description)

    def __on_activate_web(self, canvas_item):
        self.emit("close")
        _logger.debug("activated event %s", self)
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', self.__event.get_link())

    def __on_activated_event_map_link(self, canvas_item):
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', "http://maps.google.com/maps?q=" + self.__event.get_event_entry().where[0].value_string)

    def __get_title(self):
        if self.__event is None:
            return "unknown"
        return self.__event.get_title()

    def __str__(self):
        return '<EventDetailsDisplay name="%s">' % (self.__get_title())

class CalendarStock(AbstractMugshotStock, polling.Task):
    def __init__(self, *args, **kwargs):
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        # A dictionary of authenticated google accounts, with keys that are used
        # to identify those accounts within CalendarStock.
        self.__googles = {}
        self.__google_key = 0; 
        # We keep calendars in a dictionary, referenced by calendar feed links,
        # so that we can get calendar names without updating events that are
        # currently not in range. We also use it to get calendar color for events. 
        # The values in this dictionary are dictionaries themselves, with keys that
        # identify google accounts and values that are Calendar objects received
        # for those accounts. This structure allows to keep track of calendars that
        # are shared across multiple accounts and make sure we don't display duplicate
        # events if they are from the same calendar. 
        self.__calendars = {}
        self.__events = []
        self.__events_for_day_displayed = None
        self.__day_displayed = datetime.date.today()
        self.__top_event_displayed = None
        self.__move_up = False
        self.__move_down = False

        self.__slideout = None
        self.__slideout_event = None

        self.__event_alerts = {}
        self.__event_notify_ids = {}
         
        self.__event_range_start = datetime.date.today() - datetime.timedelta(_default_events_range)
        self.__event_range_end = datetime.date.today() + datetime.timedelta(_default_events_range + 1)  
        self.__min_event_range_start = self.__event_range_start
        self.__max_event_range_end = self.__event_range_end

        # these are at the end since they have the side effect of calling on_mugshot_ready it seems?
        AbstractMugshotStock.__init__(self, *args, **kwargs)
        polling.Task.__init__(self, _events_polling_periodicity_seconds * 1000)
        
        bus = dbus.SessionBus()
        o = bus.get_object('org.freedesktop.Notifications', '/org/freedesktop/Notifications')
        self.__notifications_proxy = dbus.Interface(o, 'org.freedesktop.Notifications')
        self.__notifications_proxy.connect_to_signal('ActionInvoked', self.__on_action)
        
        gobj_list = google.get_googles()
        for gobj in gobj_list:
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
        if self.__day_displayed == datetime.date.today():
            self.__on_today_button()
            return
 
        self.__change_day()
        # we update the range when the user is two clicks away from the day we don't have info for
        # we update both start and end, so as to not have a range that produces more than max-results  
        # which we set to a 1000 in google.py 
        if self.__day_displayed + datetime.timedelta(_prepare_events_days + 1) > self.__event_range_end:   
            self.__event_range_end = self.__event_range_end + datetime.timedelta(_default_events_range)
            # + 1 allows to include Today when the user goes outside of the original range the first time,
            # the user is always one click away from Today, but it's ok to not have info for that day since 
            # we want to ensure the range we request events for is always reasonably small  
            self.__event_range_start = self.__event_range_end - datetime.timedelta(_default_events_range * 2 + 1)
            self.__update_events()       
        
    def __do_prev(self):
        self.__day_displayed = self.__day_displayed - datetime.timedelta(1)
        if self.__day_displayed == datetime.date.today():
            self.__on_today_button()
            return

        self.__change_day()
        # see comments in __do_next()
        if self.__day_displayed - datetime.timedelta(_prepare_events_days) < self.__event_range_start:
            self.__event_range_start = self.__event_range_start - datetime.timedelta(_default_events_range)
            self.__event_range_end = self.__event_range_start + datetime.timedelta(_default_events_range * 2 + 1)
            self.__update_events()

    def __on_today_button(self):
        self.__day_displayed = datetime.date.today()

        need_to_update_events = False
        if self.__event_range_start > datetime.date.today() - datetime.timedelta(_default_events_range) or self.__event_range_end < datetime.date.today() + datetime.timedelta(_default_events_range + 1):
            need_to_update_events = True 

        self.__event_range_start = datetime.date.today() - datetime.timedelta(_default_events_range)
        self.__event_range_end = datetime.date.today() + datetime.timedelta(_default_events_range + 1) 

        if need_to_update_events:         
            self.__update_events()

        self.__change_day()

    def __on_up_button(self):
        self.__move_up = True
        self.__refresh_events()
        #__refresh_events() resets it too, but we do it here just in case
        self.__move_up = False
        
    def __on_down_button(self):
        self.__move_down = True
        self.__refresh_events()
        #__refresh_events() resets it too, but we do it here just in case
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
        # TODO: need to change this somehow
        libbig.show_url('http://calendar.google.com')

    def _on_mugshot_ready(self):
        super(CalendarStock, self)._on_mugshot_ready()
        self.__update_calendar_list_and_events()

    def __get_google_key(self, gobj):
        for google_item in self.__googles.items():
            if google_item[1] == gobj:
                return google_item[0]
        return None 

    def __on_google_auth(self, gobj, have_auth):
        _logger.debug("google auth state: %s", have_auth)
        if have_auth:           
            if self.__googles.values().count(gobj) == 0:
                self.__googles[self.__google_key] = gobj   
            self.__update_calendar_list_and_events(self.__google_key)
            self.__google_key = self.__google_key + 1
            if not self.is_running():
                self.start()
        else:
            key = self.__get_google_key(gobj)
            if key is not None:
                if len(self.__googles) == 1: 
                    self.stop()
                self.__remove_calendar_list_and_events(key)
                del self.__googles[key]                   
            
            # Possibly do this if we want to completely clear the box
            # self.__box.remove_all()

    def do_periodic_task(self):
        self.__update_calendar_list_and_events()   

    def get_authed_content(self, size):
        return size == self.SIZE_BULL and self.__box or None
            
    def set_size(self, size):
        super(CalendarStock, self).set_size(size)

    def __remove_calendar_list_and_events(self, google_key):
        removed_calendar_dictionary = {}
        affected_calendar_ids = [] 
        for calendar_item in self.__calendars.items():
            if calendar_item[1].has_key(google_key):
                if len(calendar_item[1]) == 1:
                    removed_calendar_dictionary[calendar_item[0]] = calendar_item[1][google_key]     
                else:
                    affected_calendar_ids.append(calendar_item[0])    
        
        for removed_calendar_item in removed_calendar_dictionary.items():
            del self.__calendars[removed_calendar_item[0]]

        for affected_calendar_id in affected_calendar_ids:
            del self.__calendars[affected_calendar_id][google_key]        
         
        events = copy.copy(self.__events)
        for calendar_link in removed_calendar_dictionary.keys():  
            for event in events:
                if event.get_calendar_link() == calendar_link:
                    self.__events.remove(event)    

        self.__refresh_events()

    def __on_calendar_list_load(self, url, data, gobj):
        google_key = self.__get_google_key(gobj)
        if google_key is None:
            _logger.warn("didn't find google_key for %s", gobj)
            return 
        # parse calendar list feed into a list of Calendar objects
        calendar_list = gcalendar.CalendarListFeedFromString(data)
        updated_calendar_dictionary = {}
        for calendar_entry in calendar_list.entry:
            calendar_entry_id = create_calendar_feed_url(calendar_entry)
            _logger.debug("calendar feed id: %s", calendar_entry_id)
 
            updated_calendar_dictionary[calendar_entry_id] = calendar_entry
            # we delete entries from the old dictionary if they were not deselected
            if self.__calendars.has_key(calendar_entry_id) and self.__calendars[calendar_entry_id].has_key(google_key) and (include_calendar(calendar_entry) or not include_calendar(self.__calendars[calendar_entry_id][google_key])):
                if len(self.__calendars[calendar_entry_id]) == 1:
                    del self.__calendars[calendar_entry_id]
                else:
                    del self.__calendars[calendar_entry_id][google_key]

        events = copy.copy(self.__events)
        calendars_to_remove = []
        # remove items from calendars for this google object that are no longer returned 
        # or are no longer selected; this should still keep events list sorted by event time
        for calendar_item in self.__calendars.items():  
            if calendar_item[1].has_key(google_key):
                calendar_link = calendar_item[0]
                if len(calendar_item[1]) == 1:
                    for event in events:
                        if event.get_calendar_link() == calendar_link:
                            self.__events.remove(event)             
                    calendars_to_remove.append(calendar_link)
                else:
                    del self.__calendars[calendar_link][google_key]

        for calendar_to_remove in calendars_to_remove:
             del self.__calendars[calendar_to_remove]

        for updated_calendar in updated_calendar_dictionary.items():
            if self.__calendars.has_key(updated_calendar[0]):
                self.__calendars[updated_calendar[0]][google_key] = updated_calendar[1]
            else:
                self.__calendars[updated_calendar[0]] = {google_key: updated_calendar[1]}
     
        self.__update_events(google_key)
 
    def __on_calendar_load(self, url, data, calendar_feed_url, event_range_start, event_range_end, gobj):
        _logger.debug("loaded calendar from " + url)
        google_key = self.__get_google_key(gobj)
        if google_key is None:
            _logger.warn("didn't find google_key for %s", gobj)
            return 
        try:
            p = EventsParser(data)
            color = self.__calendars[calendar_feed_url][google_key].color.value 
            for event in p.get_events(): 
                event.update({ 'color' : color})
            self.__on_load_events(p.get_events(), calendar_feed_url, event_range_start, event_range_end)
        except xml.sax.SAXException, e:
            __on_failed_load(sys.exc_info())

    # we could use the calendar off each event, assuming they are all from the same calendar
    # but it's better to make it work for loading events from multiple calendars too
    def __on_load_events(self, events, calendar_feed_url, event_range_start, event_range_end):
        _logger.debug("loading events %s", events)
        events_to_keep = []
        for event in self.__events:
            # keep events from other date ranges and calendars, as we are only updating
            # events from a particular calendar and date range (this should work too if we are
            # updating events from all the calendars if calendar_feed_url is None) 
            if datetime.datetime.combine(event_range_start, datetime.time(0)) > event.get_end_time() or datetime.datetime.combine(event_range_end, datetime.time(0)) <= event.get_start_time() or calendar_feed_url is not None and calendar_feed_url != event.get_calendar_link():
                events_to_keep.append(event)
        self.__events = events_to_keep
        _logger.debug("events_to_keep length %s", len(self.__events))
        self.__events.extend(list(events)) 
        _logger.debug("extended events length %s", len(self.__events))
        self.__events.sort(compare_by_date)
        self.__min_event_range_start = min(event_range_start, self.__min_event_range_start) 
        self.__max_event_range_end = max(event_range_end, self.__max_event_range_end) 
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

        if not self.is_running():
            return

        title = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        title.set_property("text", fmt_date(self.__day_displayed))
        title.set_property("font", "13px Bold")
        self.__box.append(title) 

        content_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        content_box.set_property("box-height", 95)

        arrows_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        arrows_box.set_property("box-height", 95)
        arrows_box.set_property("padding-top", 4)
        arrows_box.set_property("padding-bottom", 1)
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

        events_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, yalign=hippo.ALIGNMENT_START, spacing=1)
        events_box.set_property("box-height", 95)

        events_available = self.__min_event_range_start <= self.__day_displayed and self.__max_event_range_end > self.__day_displayed
 
        if self.__events_for_day_displayed is None:
            self.__events_for_day_displayed = []
            if events_available:   
                for event in self.__events:
                    if event.get_start_time().date() == self.__day_displayed or event.get_is_all_day() and event.get_start_time().date() < self.__day_displayed and event.get_end_time().date() > self.__day_displayed: 
                        self.__events_for_day_displayed.append(event)
                    elif event.get_start_time().date() > self.__day_displayed:
                        # we can break here because events should be ordered by start time
                        break;  
                     
        events_to_display = 5
        index = 0
        today = datetime.date.today()
        now = datetime.datetime.now()
        # we expect the events to be ordered by start time
        for event in self.__events_for_day_displayed:   
            if len(self.__events_for_day_displayed) <= events_to_display:
                break
            # by default, start with the event that is still happenning if displaying today's agenda,
            # start with the first event for any other day
            # this should have an effect of re-centering the calendar on data reloads
            elif self.__top_event_displayed is not None:
                # we need to handle scrolling through multiple events with the same start time, so we use
                # the links to compare events; however, on refresh, an event with a given link might be gone,
                # so we should include the next one after it timewise; it would be ideal to include all other
                # events we previously included that have the same time as the removed event, but that would
                # require a bit more work
                finalize_index = event.get_link() == self.__top_event_displayed.get_link() or event.get_start_time() > self.__top_event_displayed.get_start_time()                 
            elif self.__day_displayed == today:
                finalize_index = event.get_end_time() >= now
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
                        continue

                # skip the first page of events if the user wanted to move down
                # events_to_display should be greater than 0, but let's include the
                # last check just in case
                if self.__move_down and index < len(self.__events_for_day_displayed) - events_to_display and index < len(self.__events_for_day_displayed) - 1:                   
                    index = index + events_to_display 

                break

            index = index + 1   

        end_index = 0  
        if len(self.__events_for_day_displayed) > 0:
            end_index = min(index + events_to_display, len(self.__events_for_day_displayed))
        
            for event in self.__events_for_day_displayed[index:end_index]:
                # we can update the event color here by using
                # self.__calendars[event.get_calendar_link()].color.value
                # if we want to make sure we always use the updated color
                display = EventDisplay(event, self.__day_displayed)
                display.connect('button_press_event', self.__handle_event_pressed)
                events_box.append(display)

            if self.__move_up or self.__move_down or self.__top_event_displayed is not None:
                self.__top_event_displayed = self.__events_for_day_displayed[index]    
        else: 
            if events_available:
                text = "No events scheduled"
            else:
                text = "Loading events..."

            no_events_text = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
            no_events_text.set_property("text", text)
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

        left_button = hippo.CanvasImage()
        left_button.set_property('image-name', 'bigboard-left-button.png') 
        left_button.set_clickable(True)
        left_button.connect("button-press-event", lambda text, event: self.__do_prev())
        left_button.set_property("padding-right", 4)
        control_box.append(left_button)

        today_button = hippo.CanvasImage()
        if self.__day_displayed == datetime.date.today():
            today_button.set_property('image-name', 'bigboard-today-disabled.png') 
            today_button.set_clickable(False)
        else:
            today_button.set_property('image-name', 'bigboard-today-enabled.png') 
            today_button.set_clickable(True)
            today_button.connect("button-press-event", lambda text, event: self.__on_today_button())
        control_box.append(today_button)

        right_button = hippo.CanvasImage()
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
            body = body + "\n\n" + xml.sax.saxutils.escape(google.html_to_text(event.get_event_entry().content.text))

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

    def __update_calendar_list_and_events(self, google_key = None):
        _logger.debug("retrieving calendar list")
        # we update events in __on_calendar_list_load() 
        if google_key is not None:
            self.__googles[google_key].fetch_calendar_list(self.__on_calendar_list_load, self.__on_failed_load)
        else:            
            for gobj in self.__googles.values():
                gobj.fetch_calendar_list(self.__on_calendar_list_load, self.__on_failed_load)      

    def __update_events(self, google_key = None):
        _logger.debug("retrieving events")
        for google_calendar_dict in self.__calendars.values():  
            if google_key is None or google_calendar_dict.has_key(google_key):
                local_google_key = google_key
                if google_key is None:
                    local_google_key = google_calendar_dict.keys()[0]
                calendar = google_calendar_dict[local_google_key]  
                if include_calendar(calendar):
                    calendar_feed_url =  create_calendar_feed_url(calendar)
                    self.__googles[local_google_key].fetch_calendar(self.__on_calendar_load, self.__on_failed_load, calendar_feed_url, self.__event_range_start, self.__event_range_end)

    def __close_slideout(self, *args):
        if self.__slideout:
            self.__slideout.destroy()
            self.__slideout = None
            self.__slideout_event = None
                
    def __handle_event_pressed(self, event, *args):
        same_event = self.__slideout_event == event
        self.__close_slideout()
        if same_event:
            return True

        self.__slideout = slideout.Slideout()
        self.__slideout_event = event
        coords = event.get_screen_coords()
        _logger.debug("coords are %s %s; allocation alone %s", self.__box.get_context().translate_to_screen(self.__box)[0] + self.__box.get_allocation()[0] + 4, coords[1], event.get_allocation())
        self.__slideout.slideout_from(self.__box.get_context().translate_to_screen(self.__box)[0] + self.__box.get_allocation()[0] + 4, coords[1])

        p = EventDetailsDisplay(event.get_event())

        self.__slideout.get_root().append(p)
        p.connect("close", self.__close_slideout)

        return True

