import sys, logging, threading, datetime, re, functools
import xml, xml.sax

import hippo, gobject, gtk, dbus, dbus.glib

import gdata.calendar as gcalendar
from bigboard.libbig.singletonmixin import Singleton
from bigboard.libbig.http import AsyncHTTPFetcher
from bigboard import libbig
from bigboard.workboard import WorkBoard
import bigboard.keyring as keyring
import libbig.logutil
from libbig.struct import AutoStruct, AutoSignallingStruct
import libbig.polling

_logger = logging.getLogger("bigboard.Google")

class AbstractDocument(AutoStruct):
    def __init__(self):
        AutoStruct.__init__(self, { 'title' : 'Untitled', 'link' : None })

class SpreadsheetDocument(AbstractDocument):
    def __init__(self):
        AbstractDocument.__init__(self)

class WordProcessorDocument(AbstractDocument):
    def __init__(self):
        AbstractDocument.__init__(self)

class DocumentsParser(xml.sax.ContentHandler):
    def __init__(self):
        self.__docs = []
        self.__inside_title = False

    def startElement(self, name, attrs):
        #print "<" + name + ">"
        #print attrs.getNames() # .getValue('foo')

        if name == 'entry':
            d = SpreadsheetDocument()
            self.__docs.append(d)
        elif len(self.__docs) > 0:
            d = self.__docs[-1]
            if name == 'title':
                self.__inside_title = True
            elif name == 'link':
                rel = attrs.getValue('rel')
                href = attrs.getValue('href')
                type = attrs.getValue('type')
                #print str((rel, href, type))
                if rel == 'alternate' and type == 'text/html':
                    d.update({'link' : href})

    def endElement(self, name):
        #print "</" + name + ">"
        
        if name == 'title':
            self.__inside_title = False

    def characters(self, content):
        #print content
        if len(self.__docs) > 0:
            d = self.__docs[-1]
            if self.__inside_title:
                d.update({'title' : content})

    def get_documents(self):
        return self.__docs


class Event(AutoStruct):
    def __init__(self):
        super(Event, self).__init__({ 'calendar_title' : '', 'title' : '', 'start_time' : '', 'end_time' : '', 'link' : '' })
        self.__event_entry = None

    def set_event_entry(self, event_entry):
        self.__event_entry = event_entry

    def get_event_entry(self):
        return self.__event_entry 
    
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
        return cmp(b.get_start_time(), a.get_start_time())

    def get_events(self):
        if not self.__events_sorted:
            self.__events.sort(self.__compare_by_date)
            self.__events_sorted = True
        return self.__events

class NewMail(AutoStruct):
    def __init__(self):
        super(NewMail, self).__init__({ 'title' : '', 'summary' : '', 'issued' : '',
                                        'link' : '', 'id' : '', 'sender_name' : '',
                                        'sender_email' : ''})

class NewMailParser(xml.sax.ContentHandler):
    def __init__(self):
        self.__mails = []
        self.__inside_title = False
        self.__inside_summary = False
        self.__inside_id = False
        self.__inside_author = False
        self.__inside_author_name = False
        self.__inside_author_email = False

    def startElement(self, name, attrs):
        #print "<" + name + ">"
        #print attrs.getNames() # .getValue('foo')

        if name == 'entry':
            d = NewMail()
            self.__mails.append(d)
        elif len(self.__mails) > 0:
            d = self.__mails[-1]
            if name == 'title':
                self.__inside_title = True
            elif name == 'summary':
                self.__inside_summary = True                
            elif name == 'id':
                self.__inside_id = True
            elif name == 'author':
                self.__inside_author = True
            elif self.__inside_author and name == 'name':
                self.__inside_author_name = True
            elif self.__inside_author and name == 'email':
                self.__inside_author_email = True                
            elif name == 'link':
                rel = attrs.getValue('rel')
                href = attrs.getValue('href')
                type = attrs.getValue('type')
                #print str((rel, href, type))
                if rel == 'alternate' and type == 'text/html':
                    d.update({'link' : href})

    def endElement(self, name):
        #print "</" + name + ">"
        
        if name == 'title':
            self.__inside_title = False
        elif name == 'summary':
            self.__inside_summary = False
        elif name == 'id':
            self.__inside_id = False
        elif name == 'name':
            self.__inside_author_name = False
        elif name == 'email':
            self.__inside_author_email = False            
        elif name == 'author':
            self.__inside_author = False

    def characters(self, content):
        if len(self.__mails) > 0:
            d = self.__mails[-1]
            if self.__inside_title:
                d.update({'title' : d.get_title() + content})
            elif self.__inside_summary:
                d.update({'summary' : d.get_summary() + content})
            elif self.__inside_id:
                d.update({'id' : d.get_id() + content})
            elif self.__inside_author_name:
                d.update({'sender_name' : d.get_sender_name() + content })
            elif self.__inside_author_email:
                d.update({'sender_email' : d.get_sender_email() + content })

    def get_new_mails(self):
        return self.__mails

class AsyncHTTPFetcherWithAuth(object):
    """Small proxy class which expects username/password combinations for fetch(),
    and supports invoking a callback on 401."""
    def __init__(self):
        super(AsyncHTTPFetcherWithAuth, self).__init__()
        self.__fetcher = AsyncHTTPFetcher()
        
    def fetch(self, url, username, password, cb, errcb, authcb):
        self.__fetcher.fetch_extended(url=url, cb=cb, 
                                      response_errcb=functools.partial(self.__handle_response_error, authcb, errcb),
                                      setupfn=functools.partial(self.__http_setupfn, username, password))
        
    def __http_setupfn(self, username, password, h):
        h.add_credentials(username, password)
        h.follow_all_redirects = True
        
    def __handle_response_error(self, authcb, errcb, url, response, content):
        if response.status == 401:
            _logger.debug("auth failure for fetch of %s; invoking auth callback", url)
            gobject.idle_add(lambda: authcb(url) and False)
        else:
            _logger.error("caught error for fetch of %s (status %s)", url, response.status)
            # in my experience sys.exc_info() is some kind of junk here, while "e" is useful
            gobject.idle_add(lambda: errcb(url, response) and False)

class CheckMailTask(libbig.polling.Task):
    def __init__(self, google):
        libbig.polling.Task.__init__(self, 1000 * 120)
        self.__google = google
        self.__mails = {}

        # we use dbus directly instead of libnotify because
        # older versions of libnotify crashed us when an action
        # was clicked and threads were initialized
        #self.__notify = pynotify.Notification('foo') # empty string causes return_if_fail

        bus = dbus.SessionBus()

        o = bus.get_object('org.freedesktop.Notifications', '/org/freedesktop/Notifications')
        self.__notifications_proxy = dbus.Interface(o, 'org.freedesktop.Notifications')

        self.__notifications_proxy.connect_to_signal('ActionInvoked', self.__on_action)

        self.__latest_mail = None

        self.__notify_id = 0

    def __on_action(self, *args):
        notification_id = args[0]
        action = args[1]

        if action == 'mail':
            if self.__latest_mail:
                libbig.show_url(self.__latest_mail.get_link())
        elif action == 'inbox-no-icon' or action == 'default':
            libbig.show_url("http://mail.google.com/mail")
        else:
            print "unknown action " + action

    def __on_fetched_mail(self, mails):
        currently_new = {}
        not_yet_seen = 0
        for m in mails:
            currently_new[m.get_id()] = m
            if self.__mails.has_key(m.get_id()):
                pass
            else:
                not_yet_seen = not_yet_seen + 1

        self.__mails = currently_new

        if not_yet_seen > 0:
            first = mails[0]

            body = "<i>from " + xml.sax.saxutils.escape(first.get_sender_name()) + \
                   "&lt;" + xml.sax.saxutils.escape(first.get_sender_email()) + "&gt;</i>\n"
            body = body + xml.sax.saxutils.escape(first.get_summary())

            notify_id = self.__notifications_proxy.Notify("BigBoard",
                                                          self.__notify_id, # "id" - 0 to not replace any existing
                                                          "", # icon name
                                                          first.get_title(),   # summary
                                                          body, # body
                                                          ['mail',
                                                           "Open Message",
                                                           'inbox-no-icon',
                                                           "Inbox (%d)" % len(self.__mails)], # action array
                                                          {'foo' : 'bar'}, # hints (pydbus barfs if empty)
                                                          10000) # timeout, 10 seconds

            self.__notify_id = notify_id
            self.__latest_mail = first

    def __on_fetch_error(self, exc_info):
        pass

    def do_periodic_task(self):
        self.__google.fetch_new_mail(self.__on_fetched_mail, self.__on_fetch_error)

class Google(gobject.GObject):
    __gsignals__ = {
        "auth" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_BOOLEAN,))
    }

    def __init__(self):
        super(Google, self).__init__()
        self.__logger = logging.getLogger("bigboard.Google")
        self.__username = None
        self.__password = None
        self.__fetcher = AsyncHTTPFetcherWithAuth()
        self.__auth_requested = False
        self.__post_auth_hooks = []

        k = keyring.get_keyring()
        
        self.__mail_checker = None
        
        try:
            username, password = k.get_login("google")
            self.__username = username
            self.__password = password
            self.__on_auth_ok(username, password)
        except TypeError:
            self.__username = None
            self.__password = None
            self.__on_auth_cancel()

    def __consider_checking_mail(self):
        if self.__username and self.__password:
            if not self.__mail_checker:
                self.__mail_checker = CheckMailTask(self)
            self.__mail_checker.start()
        elif self.__mail_checker:
            self.__mail_checker.stop()

    def __on_auth_ok(self, username, password):
        self.__username = username
        self.__password = password
        self.__auth_requested = False
        keyring.get_keyring().store_login('google', self.__username, self.__password)

        hooks = self.__post_auth_hooks
        self.__post_auth_hooks = []
        for h in hooks:
            h()
        self.emit("auth", True)

        self.__consider_checking_mail()

    def __on_auth_cancel(self):
        self.__username = None
        self.__password = None
        self.emit("auth", False)        
        self.__consider_checking_mail()

    def have_auth(self):
        return (self.__username is not None) and (self.__password is not None)

    def __with_login_info(self, func, reauth=False):
        """Call func after we get username and password"""

        if self.__username and self.__password:
            _logger.debug("auth looks valid")   
            func()
            return
            
        if not self.__auth_requested:
            self.__auth_requested = True
            WorkBoard().append('service.pwauth', 'Google', self.__on_auth_ok, reauth=reauth)
        else:
            _logger.debug("auth request pending; not resending")            
        self.__post_auth_hooks.append(func)

    def __on_bad_auth(self):
        _logger.debug("got bad auth; invoking reauth")
        # don't null username, leave it filled inf
        self.__password = None
        self.__with_login_info(lambda: True, reauth=True)

    ### Calendar

    def __on_calendar_load(self, url, data, cb, errcb):
        self.__logger.debug("loaded calendar from " + url)
        try:
            p = EventsParser(data)
            cb(p.get_events())
        except xml.sax.SAXException, e:
            errcb(sys.exc_info())


    def __have_login_fetch_calendar(self, cb, errcb):

        uri = 'http://www.google.com/calendar/feeds/' + self.__username + '@gmail.com/private/full'

        self.__fetcher.fetch(uri, self.__username, self.__password,
                             lambda url, data: self.__on_calendar_load(url, data, cb, errcb),
                             lambda url, resp: errcb(resp),
                             lambda url: self.__on_bad_auth())

    def fetch_calendar(self, cb, errcb):
        self.__with_login_info(lambda: self.__have_login_fetch_calendar(cb, errcb))

    def request_auth(self):
        self.__with_login_info(lambda: True)

    ### Recent Documents

    def __on_documents_load(self, url, data, cb, errcb):
        self.__logger.debug("loaded documents from " + url)
        try:
            p = DocumentsParser()
            xml.sax.parseString(data, p)
            cb(p.get_documents())
        except xml.sax.SAXException, e:
            errcb(sys.exc_info())

    def __on_documents_error(self, url, exc_info, errcb):
        self.__logger.debug("error loading documents from " + url)
        errcb(exc_info)

    def __have_login_fetch_documents(self, cb, errcb):

        uri = 'http://spreadsheets.google.com/feeds/spreadsheets/private/full'

        self.__fetcher.fetch(uri, self.__username, self.__password,
                             lambda url, data: self.__on_documents_load(url, data, cb, errcb),
                             lambda url, exc_info: self.__on_documents_error(url, exc_info, errcb),
                             lambda url: self.__on_bad_auth())

    def fetch_documents(self, cb, errcb):
        self.__with_login_info(lambda: self.__have_login_fetch_documents(cb, errcb))

    ### New Mail

    def __on_new_mail_load(self, url, data, cb, errcb):
        #self.__logger.debug("loaded new mail from " + url)
        #print data
        try:
            p = NewMailParser()
            xml.sax.parseString(data, p)
            cb(p.get_new_mails())
        except xml.sax.SAXException, e:
            errcb(sys.exc_info())

    def __on_new_mail_error(self, url, exc_info, errcb):
        self.__logger.debug("error loading new mail from " + url)
        errcb(exc_info)

    def __have_login_fetch_new_mail(self, cb, errcb):

        uri = 'http://mail.google.com/mail/feed/atom'

        self.__fetcher.fetch(uri, self.__username, self.__password,
                             lambda url, data: self.__on_new_mail_load(url, data, cb, errcb),
                             lambda url, exc_info: self.__on_new_mail_error(url, exc_info, errcb),
                             lambda url: self.__on_bad_auth())

    def fetch_new_mail(self, cb, errcb):
        self.__with_login_info(lambda: self.__have_login_fetch_new_mail(cb, errcb))

_google_instance = None
def get_google():
    global _google_instance
    if _google_instance is None:
        _google_instance = Google()
    return _google_instance
        
if __name__ == '__main__':

    import gtk, gtk.gdk

    import bigboard.libbig
    try:
        import bigboard.bignative as bignative
    except:
        import bignative

    gtk.gdk.threads_init()

    libbig.logutil.init('DEBUG', ['AsyncHTTP2LibFetcher'])

    bignative.set_application_name("BigBoard")

    keyring.get_keyring().store_login('google', 'havoc.pennington', 'wrong')

    #AuthDialog().present('foo', 'bar')
    #gtk.main()
    sys.exit(0)

    g = get_google()

    def display(x):
        print x
    
    #g.fetch_documents(display, display)
    g.fetch_calendar(display, display)
    #g.fetch_new_mail(display, display)

    gtk.main()

    sys.exit(0)
