import sys, logging, threading, datetime, time, functools, base64, rfc822
import xml, xml.sax

import hippo, gobject, gtk, dbus, dbus.glib

import bigboard.globals as globals
from bigboard.libbig.singletonmixin import Singleton
from bigboard.libbig.http import AsyncHTTPFetcher
from bigboard import libbig
from bigboard.workboard import WorkBoard
import libbig.logutil
from bigboard.libbig.gutil import *
from libbig.logutil import log_except
from libbig.struct import AutoStruct, AutoSignallingStruct
import libbig.polling
import htmllib
import bigboard.accounts as accounts
import gdata.docs as gdocs

_logger = logging.getLogger("bigboard.Google")

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
            if second.endswith('Z'):
                second = second[:-1]
        else:
            hour, minute, second = (time_str[0:2], time_str[2:4], time_str[4:6])
    else:
        date_str = timestamp
        hour, minute, second = 0, 0, 0

    if date_str.find('-') >= 0:
        year, month, day = date_str.split('-')
    else:
        year, month, day = (date_str[0:4], date_str[4:6], date_str[6:8])

    year = int(year)
    month = int(month)
    day = int(day)
    hour = int(hour)
    minute = int(minute)
    second = int(second)

    # _logger.debug("%d-%d-%d %d:%d:%d  (original '%s')" % (year, month, day, hour, minute, second, timestamp))

    ## google appears to return hour 24 in the gmail feed ... but RFC 3339 and Python are both 0-23.
    ## for now just "fix it" but not sure what it really means. Does it mean 0 on the next day?
    if hour > 23:
        hour = 23
        _logger.debug("*** adjusted hour down to 23???")
    
    return datetime.datetime(year, month, day, hour, minute, second, tzinfo=tz)

def fmt_date_for_feed_request(date):
    return datetime.datetime.utcfromtimestamp(time.mktime(date.timetuple())).strftime("%Y-%m-%dT%H:%M:%S")

#class RemoveTagsParser(xml.sax.ContentHandler):
#    def __init__(self):
#        self.__content = ''
#
#    def characters(self, content):
#        self.__content = self.__content + content
#
#    def get_content(self):
#        return self.__content

def html_to_text(html):
    p = htmllib.HTMLParser(None)
    p.save_bgn()
    p.feed(html)
    text = p.save_end()

    ## HTMLParser helpfully replaces &hellip; with &#8230; in output
    ## that is not supposed to be HTML or XML
    text = text.replace("&#8230;", "...")
    
    return text

class NewMail(AutoStruct):
    def __init__(self):
        super(NewMail, self).__init__({ 'title' : '', 'summary' : '', 'issued' : '',
                                        'link' : '', 'id' : '', 'sender_name' : '',
                                        'sender_email' : '', 'modified' : datetime.datetime(1980, 8, 8)})

class NewMailParser(xml.sax.ContentHandler):
    def __init__(self):
        self.__mails = []
        self.__inside_title = False
        self.__inside_summary = False
        self.__inside_id = False
        self.__inside_author = False
        self.__inside_author_name = False
        self.__inside_author_email = False
        self.__inside_modified = False
        self.__modified_content = ''

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
            elif name == 'modified':
                self.__inside_modified = True                
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
            ## parse the HTML
            if len(self.__mails) > 0:
                d = self.__mails[-1]
                d.update({'title' : html_to_text(d.get_title())})
        elif name == 'summary':
            self.__inside_summary = False
            ## parse the HTML
            if len(self.__mails) > 0:
                d = self.__mails[-1]
                d.update({'summary' : html_to_text(d.get_summary())})            
        elif name == 'id':
            self.__inside_id = False
        elif name == 'name':
            self.__inside_author_name = False
            ## Should the author name be HTML-unescaped?
        elif name == 'email':
            self.__inside_author_email = False            
        elif name == 'author':
            self.__inside_author = False
        elif name == 'modified':            
            self.__inside_modified = False
            if len(self.__mails) > 0:
                d = self.__mails[-1]
                d.update({'modified' : parse_timestamp(self.__modified_content) })
            self.__modified_content = ''

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
            elif self.__inside_modified:
                self.__modified_content = self.__modified_content + content

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
                                      no_store=True,
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

class GooglePollAction(object):
    def __init__(self, google):
        self.__google = google

    def get_google(self):
        return self.__google

    def update(self):
        pass        

class MailPollAction(GooglePollAction):
    def __init__(self, google):
        super(MailPollAction, self).__init__(google)

        self.__ids_seen = {}
        self.__newest_modified_seen = None

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
        elif action == 'inbox-no-icon':
            libbig.show_url(self.__google.get_mail_base_url())
        elif action == 'default':
            # 'dafault' corresponds to the notification being clicked in any place,
            # and we use to go to the site in that case; now clicking on the notification
            # just closes it    
            pass
        else:
            print "unknown action " + action

    def __on_fetched_mail(self, mails):
        not_yet_seen = 0
        for m in mails:

            ## Save latest timestamp and record if we were
            ## the newest mail yet seen
            was_newer = False
            if not self.__newest_modified_seen or \
               m.get_modified() > self.__newest_modified_seen:
                self.__newest_modified_seen = m.get_modified()
                was_newer = True

            ## record this mail ID and if we are EITHER a new ID,
            ## or were just modified, notify. We may have to
            ## tweak this depending on what "modified" turns out
            ## to mean. 
            notify_on_this = False
            if self.__ids_seen.has_key(m.get_id()):
                if was_newer:
                    notify_on_this = True
            else:
                self.__ids_seen[m.get_id()] = True
                notify_on_this = True

            ## Never notify on something that's more than a few days
            ## old, though, since some people keep tons of stuff
            ## unread forever in gmail.                
            if notify_on_this and m.get_modified() < (datetime.datetime.today() - datetime.timedelta(days=3)):
                notify_on_this = False

            if notify_on_this:
                not_yet_seen = not_yet_seen + 1

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
                                                           "Inbox (%d)" % len(mails)], # action array
                                                          {'foo' : 'bar'}, # hints (pydbus barfs if empty)
                                                          10000) # timeout, 10 seconds

            self.__notify_id = notify_id
            self.__latest_mail = first

    def __on_fetch_error(self, exc_info):
        pass

    def update(self):
        self.get_google().fetch_new_mail(self.__on_fetched_mail, self.__on_fetch_error)

class GenericPollAction(GooglePollAction):
    def __init__(self, google, func):
        super(GenericPollAction, self).__init__(google)
        self.__func = func

    def update(self):
        self.__func(self.get_google())

class CheckGoogleTask(libbig.polling.Task):
    def __init__(self, google):
        libbig.polling.Task.__init__(self, 1000 * 120, initial_interval=1000*5)
        self.__google = google
        self.__actions = {} ## hash from id to [add count, GooglePollAction object]

    def add_action(self, id, action_constructor):
        if id not in self.__actions:
            action = action_constructor()
            self.__actions[id] = [1, action]
        else:
            self.__actions[id][0] = self.__actions[id][0] + 1

        _logger.debug("check count bumped to %d for google check action %s" % (self.__actions[id][0], id))

    def remove_action(self, id):
        if id not in self.__actions:
            raise Exception("removing action id that wasn't added")
        self.__actions[id][0] = self.__actions[id][0] - 1
        _logger.debug("check count reduced to %d for google check action %s" % (self.__actions[id][0], id))
        if self.__actions[id][0] == 0:
            del self.__actions[id]            
        
    def do_periodic_task(self):
        for (id, a) in self.__actions.values():
            a.update()

class Google(gobject.GObject):
    __gsignals__ = {
        ## "auth-badness-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_BOOLEAN,))
    }

    def __init__(self, account):
        super(Google, self).__init__()
        self.__logger = logging.getLogger("bigboard.Google")
        self.__last_username_tried = ''
        self.__last_password_tried = ''
        self.__last_auth_attempt_failed = False
        self.__fetcher = AsyncHTTPFetcherWithAuth()
        self.__account = account

        self.__checker = CheckGoogleTask(self)
        self.add_poll_action('mail', lambda: MailPollAction(self))

        self.__account.connect('changed', lambda account: self.__on_account_changed())
        self.__on_account_changed()

    def get_account(self):
        return self.__account

    def destroy(self):
        ## FIXME close down all the polling tasks and remove all signal handlers
        self.__checker.stop()

    def add_poll_action(self, id, action_constructor):
        self.__checker.add_action(id, action_constructor)

    def remove_poll_action(self, id):
        self.__checker.remove_action(id)

    def add_poll_action_func(self, id, func):
        self.__checker.add_action(id, lambda: GenericPollAction(self, func))

    def __consider_checking(self):
        if self.get_current_auth_credentials_known_bad():
            _logger.debug("Disabling google polling since auth credentials known bad")
            self.__checker.stop()
        else:
            _logger.debug("Enabling google polling since auth credentials maybe good")
            self.__checker.start()

    def __auth_needs_retry(self):

        username = self.__account.get_username_as_google_email()
        password = self.__account.get_password()

        _logger.debug("auth retry for google username %s" % (username))

        self.__last_username_tried = username
        self.__last_password_tried = password
        self.__last_auth_attempt_failed = False

        self.__consider_checking()

    def __on_auth_failed(self, failed_username, failed_password, errcb):
        _logger.debug("bad auth for google")

        if failed_username == self.__last_username_tried and \
           failed_password == self.__last_password_tried:
            self.__last_auth_attempt_failed = True
            self.__consider_checking()

        errcb({ 'status' : 401, 'message' : 'Bad or missing username or password' })

    def get_current_auth_credentials_known_bad(self):
        return self.__account.get_username_as_google_email() == '' or \
               self.__account.get_password() == '' or \
             self.__last_auth_attempt_failed

    def __on_account_changed(self):
        auth_changed = False
        if self.__last_username_tried != self.__account.get_username_as_google_email():
            auth_changed = True
        if self.__last_password_tried != self.__account.get_password():
            auth_changed = True

        _logger.debug("google account changed, auth_changed=%d" % (auth_changed))

        if auth_changed:
            self.__auth_needs_retry()

    def __call_if_may_have_auth(self, func, errfunc):
        if self.get_current_auth_credentials_known_bad():
            errfunc({ 'status' : 401, 'message' : 'Bad or missing username or password' })
        else:
            func()

    ### Calendar

    def __have_login_fetch_calendar_list(self, cb, errcb):

        username = self.__account.get_username_as_google_email()
        password = self.__account.get_password()

        # there is a chance that someone might have access to more than 25 calendars, so let's
        # specify 1000 for max-results to make sure we get information about all calendars 
        uri = 'http://www.google.com/calendar/feeds/' + username + '?max-results=1000'

        self.__fetcher.fetch(uri, username, password,
                             lambda url, data: cb(url, data, self),
                             lambda url, resp: errcb(resp),
                             lambda url: self.__on_auth_failed(username, password, errcb))

    def fetch_calendar_list(self, cb, errcb):
        self.__call_if_may_have_auth(lambda: self.__have_login_fetch_calendar_list(cb, errcb), errcb)

    def __have_login_fetch_calendar(self, cb, errcb, calendar_feed_url, event_range_start, event_range_end):

        username = self.__account.get_username_as_google_email()
        password = self.__account.get_password()        

        min_and_max_str = ""
        if event_range_start is not None and event_range_end is not None:
            # just specifying start-min and start-max includes multiple "when" tags in the response for the recurrent events,
            # specifying singlevents=true in addition to that expands recurrent events into separate events, which gives us
            # links to each particular event in the recurrence
            # the default for max-results is 25, we usually use a range of 29 days, so 1000 max-results should be a good "large number" 
            min_and_max_str =  "?start-min=" + fmt_date_for_feed_request(event_range_start) + "&start-max=" + fmt_date_for_feed_request(event_range_end) + "&singleevents=true" + "&max-results=1000"

        if calendar_feed_url is None:
            uri = 'http://www.google.com/calendar/feeds/' + username + '/private/full' + min_and_max_str
        else:
            uri = calendar_feed_url + min_and_max_str

        self.__fetcher.fetch(uri, username, password,
                             lambda url, data: cb(url, data, calendar_feed_url, event_range_start, event_range_end, self),
                             lambda url, resp: errcb(resp),
                             lambda url: self.__on_auth_failed(username, password, errcb))

    def fetch_calendar(self, cb, errcb, calendar_feed_url = None, event_range_start = None, event_range_end = None):
        self.__call_if_may_have_auth(lambda: self.__have_login_fetch_calendar(cb, errcb, calendar_feed_url, event_range_start, event_range_end), errcb)

    ### Recent Documents

    def __on_documents_load(self, url, data, cb, errcb):
        self.__logger.debug("loaded documents from " + url)
        document_list = gdocs.DocumentListFeedFromString(data)   
        cb(document_list.entry)

    def __on_documents_error(self, url, exc_info, errcb):
        self.__logger.debug("error loading documents from " + url)
        errcb(exc_info)

    def __have_login_fetch_documents(self, cb, errcb):
        username = self.__account.get_username_as_google_email()
        password = self.__account.get_password()        
        
        uri = 'http://docs.google.com/feeds/documents/private/full'
        # uri = 'http://spreadsheets.google.com/feeds/spreadsheets/private/full'

        self.__fetcher.fetch(uri, username, password,
                             lambda url, data: self.__on_documents_load(url, data, cb, errcb),
                             lambda url, resp: self.__on_documents_error(url, resp, errcb),
                             lambda url: self.__on_auth_failed(username, password, errcb))

    def fetch_documents(self, cb, errcb):
        self.__call_if_may_have_auth(lambda: self.__have_login_fetch_documents(cb, errcb), errcb)

    ### New Mail

    def get_mail_base_url(self):
        username = self.__account.get_username_as_google_email()
        
        if not username:
            return None
            
        at_sign = username.find('@')

        domain = username[at_sign+1:]

        if not domain.endswith('gmail.com'):
            uri = 'http://mail.google.com/a/' + domain
        else:
            uri = 'http://mail.google.com/mail'

        return uri

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

        username = self.__account.get_username_as_google_email()
        password = self.__account.get_password()        

        uri = self.get_mail_base_url() + '/feed/atom'

        self.__fetcher.fetch(uri, username, password,
                             lambda url, data: self.__on_new_mail_load(url, data, cb, errcb),
                             lambda url, exc_info: self.__on_new_mail_error(url, exc_info, errcb),
                             lambda url: self.__on_auth_failed(username, password, errcb))

    def fetch_new_mail(self, cb, errcb):
        self.__call_if_may_have_auth(lambda: self.__have_login_fetch_new_mail(cb, errcb), errcb)

__googles_by_account = {}
__initialized = False

def get_googles():
    global __googles_by_account
    return __googles_by_account.values()

def get_google_for_account(account):
    global __googles_by_account
    return __googles_by_account[account]

def __refresh_googles(a):
    global __googles_by_account    
    gaccounts = a.get_accounts_with_kind(accounts.KIND_GOOGLE)
    new_googles = {}
    for g in gaccounts:
        if g in __googles_by_account:
            new_googles[g] = __googles_by_account[g]
        else:
            new_googles[g] = Google(g)

    for (g, old) in __googles_by_account.items():
        if g not in new_googles:
            old.destroy()
            
    __googles_by_account = new_googles

def __on_account_added(a, account):
    __refresh_googles(a)

def __on_account_removed(a, account):
    __refresh_googles(a)

def init():
    global __initialized
    
    if __initialized:
        return
    __initialized = True
    
    a = accounts.get_accounts()
    __refresh_googles(a)
    a.connect('account-added', __on_account_added)
    a.connect('account-removed', __on_account_removed)
        
if __name__ == '__main__':

    import gtk, gtk.gdk

    import bigboard.libbig
    try:
        import bigboard.bignative as bignative
    except:
        import bignative

    gtk.gdk.threads_init()

    libbig.logutil.init('DEBUG', ['AsyncHTTP2LibFetcher', 'bigboard.Keyring', 'bigboard.Google'])

    bignative.set_application_name("BigBoard")
    bignative.set_program_name("bigboard")

    # to test, you have to change this temporarily to store your correct
    # password, then comment it out again.
    # keyring.get_keyring().store_login('google', 'hp@redhat.com', 'wrong')

    #AuthDialog().present('foo', 'bar')
    #gtk.main()
    #sys.exit(0)

    g = get_google_at_personal()

    def display(x):
        print x
        for nm in x:
            print "Title: " + nm.get_title()
            print "Summary: " + nm.get_summary()
            print "Sender: " + nm.get_sender_name()
            print "Link: " + nm.get_link()
            print "Modified: " + str(nm.get_modified())
    
    #g.fetch_documents(display, display)
    #g.fetch_calendar(display, display)
    g.fetch_new_mail(display, display)

    gtk.main()

    sys.exit(0)
