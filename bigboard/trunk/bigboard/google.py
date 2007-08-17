import sys, logging, threading, datetime, time, functools
import xml, xml.sax

import hippo, gobject, gtk, dbus, dbus.glib

from bigboard.libbig.singletonmixin import Singleton
from bigboard.libbig.http import AsyncHTTPFetcher
from bigboard import libbig
from bigboard.workboard import WorkBoard
import bigboard.keyring as keyring
import libbig.logutil
from libbig.struct import AutoStruct, AutoSignallingStruct
import libbig.polling

_logger = logging.getLogger("bigboard.Google")


def fmt_date_for_feed_request(date):
    return datetime.datetime.utcfromtimestamp(time.mktime(date.timetuple())).strftime("%Y-%m-%dT%H:%M:%S")

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

        if notification_id != self.__notify_id:
            return

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
        # this line allows to enter new Google account information on bigboard restarts    
        # k.store_login('google', "", "")
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
        self.__username = username.find('@') >= 0 and username or username + '@gmail.com'
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

    def get_auth(self):
        return (self.__username, self.__password)

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

    def __have_login_fetch_calendar_list(self, cb, errcb):

        # there is a chance that someone might have access to more than 25 calendars, so let's
        # specify 1000 for max-results to make sure we get information about all calendars 
        uri = 'http://www.google.com/calendar/feeds/' + self.__username + '?max-results=1000'

        self.__fetcher.fetch(uri, self.__username, self.__password,
                             lambda url, data: cb(url, data),
                             lambda url, resp: errcb(resp),
                             lambda url: self.__on_bad_auth())

    def fetch_calendar_list(self, cb, errcb):
        self.__with_login_info(lambda: self.__have_login_fetch_calendar_list(cb, errcb))

    def __have_login_fetch_calendar(self, cb, errcb, calendar_feed_url, event_range_start, event_range_end):

        min_and_max_str = ""
        if event_range_start is not None and event_range_end is not None:
            # just specifying start-min and start-max includes multiple "when" tags in the response for the recurrent events,
            # specifying singlevents=true in addition to that expands recurrent events into separate events, which gives us
            # links to each particular event in the recurrence
            # the default for max-results is 25, we usually use a range of 29 days, so 1000 max-results should be a good "large number" 
            min_and_max_str =  "?start-min=" + fmt_date_for_feed_request(event_range_start) + "&start-max=" + fmt_date_for_feed_request(event_range_end) + "&singleevents=true" + "&max-results=1000"

        if calendar_feed_url is None:
            uri = 'http://www.google.com/calendar/feeds/' + self.__username + '/private/full' + min_and_max_str
        else:
            uri = calendar_feed_url + min_and_max_str

        self.__fetcher.fetch(uri, self.__username, self.__password,
                             lambda url, data: cb(url, data, calendar_feed_url, event_range_start, event_range_end),
                             lambda url, resp: errcb(resp),
                             lambda url: self.__on_bad_auth())

    def fetch_calendar(self, cb, errcb, calendar_feed_url = None, event_range_start = None, event_range_end = None):
        self.__with_login_info(lambda: self.__have_login_fetch_calendar(cb, errcb, calendar_feed_url, event_range_start, event_range_end))

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
