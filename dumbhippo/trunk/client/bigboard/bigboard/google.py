import httplib2, sys, logging, threading, datetime, re
import xml, xml.sax

import hippo, gobject, gtk, dbus, dbus.glib

from bigboard import libbig
import bigboard.keyring as keyring
import libbig.logutil
from libbig.struct import AutoStruct, AutoSignallingStruct
import libbig.polling

class AbstractDocument(AutoStruct):
    def __init__(self):
        super(AbstractDocument, self).__init__({ 'title' : 'Untitled', 'link' : None })

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
        super(Event, self).__init__({ 'title' : '', 'start_time' : '', 'end_time' : '', 'link' : '' })

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
        hour, minute, second = time_str.split(':')
    else:
        date_str = timestamp
        hour, minute, second = 0, 0, 0

    if date_str.find('-') >= 0:
        year, month, day = date_str.split('-')
    else:
        year, month, day = (date_str[0:4], date_str[4:6], date_str[6:8])
    return datetime.datetime(int(year), int(month), int(day), int(hour), int(minute),
        int(second), tzinfo=tz)

class EventsParser(xml.sax.ContentHandler):
    def __init__(self):
        self.__events = []
        self.__events_sorted = False
        self.__inside_title = False
        self.__inside_recurrence = False
        self.__content = ""
        self.__dt_re = re.compile(r'DT\w+;VALUE=DATE:(\d+)\s')

    def startElement(self, name, attrs):
        #print "<" + name + ">"
        #print attrs.getNames() # .getValue('foo')
        self.__content = ""

        if name == 'entry':
            e = Event()
            self.__events.append(e)
        elif len(self.__events) > 0:
            e = self.__events[-1]
            if name == 'title':
                self.__inside_title = True
            elif name == 'gd:when':
                e.update({ 'start_time' : parse_timestamp(attrs.getValue('startTime')),
                           'end_time' : parse_timestamp(attrs.getValue('endTime')) })
            elif name == 'gd:recurrence':
                self.__inside_recurrence = True
            elif name == 'link':
                rel = attrs.getValue('rel')
                href = attrs.getValue('href')
                type = attrs.getValue('type')
                #print str((rel, href, type))
                if rel == 'alternate' and type == 'text/html':
                    e.update({'link' : href})
                
    def endElement(self, name):
        #print "</" + name + ">"

        if len(self.__events) > 0:
            e = self.__events[-1]
        if name == 'title' and self.__inside_title:
            e.update({'title' : self.__content})
            self.__inside_title = False
        elif name == 'gd:recurrence' and self.__inside_recurrence:
            self.__inside_recurrence = False
            dt_start = None
            dt_end = None
            match = self.__dt_re.search(self.__content)
            if match:
                dt_start = match.group(1)
                match = self.__dt_re.search(self.__content, match.end())
                if match:
                    dt_end = match.group(1)
            if dt_start and dt_end:
                e.update({ 'start_time' : parse_timestamp(dt_start),
                           'end_time' : parse_timestamp(dt_end) })

    def characters(self, content):
        self.__content += content
        
    def __compare_by_date(self, a, b):
        return cmp(a.get_start_time(), b.get_start_time())

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

class AsyncHTTPLib2Fetcher:
    """Asynchronously fetch objects over HTTP, invoking
       callbacks using the GLib main loop."""
    def fetch(self, url, username, password, cb, errcb, authcb):
        self.__logger = logging.getLogger("bigboard.AsyncHTTPLib2Fetcher")
        #self.__logger.debug('creating async HTTP request thread for %s' % (url,))

        thread = threading.Thread(target=self._do_fetch, name="AsyncHTTPLib2Fetch", args=(url, username, password, cb, errcb, authcb))
        thread.setDaemon(True)
        thread.start()
        
    def _do_fetch(self, url, username, password, cb, errcb, authcb):
        #self.__logger.debug("in thread fetch of %s" % (url,))
        try:
            h = httplib2.Http()
            h.add_credentials(username, password)
            h.follow_all_redirects = True

            #self.__logger.debug("sending http request")

            headers, data = h.request(url, "GET", headers = {})

            if headers.status == 401:
                self.__logger.error("auth failure for fetch of %s: %s" % (url, headers.status))
                gobject.idle_add(lambda: authcb(url) and False)
            else:
                #self.__logger.debug("adding idle after http request")
                pass

                gobject.idle_add(lambda: cb(url, data) and False)
        except Exception, e:
            self.__logger.error("caught error for fetch of %s: %s" % (url, e))
            # in my experience sys.exc_info() is some kind of junk here, while "e" is useful
            gobject.idle_add(lambda: errcb(url, sys.exc_info()) and False)

## interface to be implemented by an authentication UI
class AuthUI:
    def __init__(self):
        self.__okcb = None
        self.__cancelcb = None

    # virtual
    def present(self, username, password):
        pass

    # virtual
    def hide(self):
        pass

    # final
    def set_callbacks(self, okcb, cancelcb):
        self.__okcb = okcb
        self.__cancelcb = cancelcb

    # protected
    def run_ok_callback(self, username, password):
        if self.__okcb:
            self.__okcb(username, password)

    # protected
    def run_cancel_callback(self):
        if self.__cancelcb:
            self.__cancelcb()

class AuthCanvasItem (hippo.CanvasBox, AuthUI):
    def __init__(self):
        hippo.CanvasBox.__init__(self, orientation=hippo.ORIENTATION_VERTICAL, spacing=3)        
        AuthUI.__init__(self)

        # I can't get Pango to take just "Bold" without a size; not sure what the problem is here
        self.append(hippo.CanvasText(text="Google Login", xalign=hippo.ALIGNMENT_START, font="Bold 12px"))

        box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.append(box)

        box.append(hippo.CanvasText(text="Username:", xalign=hippo.ALIGNMENT_START, border_right=3))
        self.__username_entry = hippo.CanvasEntry()
        self.__username_entry.set_property("xalign", hippo.ALIGNMENT_FILL)
        box.append(self.__username_entry, hippo.PACK_EXPAND)

        box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.append(box)

        box.append(hippo.CanvasText(text="Password:", xalign=hippo.ALIGNMENT_START, border_right=3))
        self.__password_entry = hippo.CanvasEntry()
        self.__password_entry.set_property("xalign", hippo.ALIGNMENT_FILL)
        box.append(self.__password_entry, hippo.PACK_EXPAND)

        self.__password_entry.set_property("password-mode", True)
        
        self.__ok_button = hippo.CanvasButton()
        # why don't keywords work on CanvasButton constructor?
        self.__ok_button.set_property("text", "Login")
        self.__ok_button.set_property("xalign", hippo.ALIGNMENT_END)
        self.append(self.__ok_button)

        self.__ok_button.connect("activated", self.__on_login_activated)

    def __on_login_activated(self, somearg):
        self.run_ok_callback(self.__username_entry.get_property("text"),
                             self.__password_entry.get_property("text"))

    def present(self, username, password):
        if username:
            self.__username_entry.set_property("text", username)
        if password:
            self.__password_entry.set_property("text", password)
        self.set_visible(True)

    def hide(self):
        self.set_visible(False)

class AuthDialog (AuthUI):
    def __init__(self):
        AuthUI.__init__(self)
        
        ## dialog code based on gnome-python-desktop example
        
        dialog = gtk.Dialog("Google Login", None, 0,
                            (gtk.STOCK_CANCEL, gtk.RESPONSE_CANCEL,
                             gtk.STOCK_OK, gtk.RESPONSE_OK))
        dialog.props.has_separator = False
        dialog.set_default_response(gtk.RESPONSE_OK)

        label = gtk.Label("<b>Please log in to Google so BigBoard can show your Google stuff</b>")
        label.set_use_markup(True)
        dialog.vbox.pack_start(label, False, False, 0)

        hbox = gtk.HBox(False, 8)
        hbox.set_border_width(8)
        dialog.vbox.pack_start(hbox, False, False, 0)

        stock = gtk.image_new_from_stock(gtk.STOCK_DIALOG_AUTHENTICATION,
                                         gtk.ICON_SIZE_DIALOG)
        hbox.pack_start(stock, False, False, 0)

        table = gtk.Table(2, 2)
        table.set_row_spacings(4)
        table.set_col_spacings(4)
        hbox.pack_start(table, True, True, 0)

        label = gtk.Label("Google _Username")
        label.set_alignment(0.0, 0.5)
        label.set_use_underline(True)
        table.attach(label, 0, 1, 0, 1)
        local_entry1 = gtk.Entry()
        local_entry1.set_activates_default(True)
        table.attach(local_entry1, 1, 2, 0, 1)
        label.set_mnemonic_widget(local_entry1)

        label = gtk.Label("_Password")
        label.set_alignment(0.0, 0.5)
        label.set_use_underline(True)
        table.attach(label, 0, 1, 1, 2)
        local_entry2 = gtk.Entry()
        local_entry2.set_visibility(False)
        local_entry2.set_activates_default(True)
        table.attach(local_entry2, 1, 2, 1, 2)
        label.set_mnemonic_widget(local_entry2)

        dialog.vbox.show_all()    

        dialog.connect('delete-event', self.__on_delete_event)

        dialog.connect('response', self.__on_response)

        self.__dialog = dialog

        self.__username_entry = local_entry1
        self.__password_entry = local_entry2

    def __on_delete_event(self, dialog, event):
        self.hide()
        return True # prevent destroy

    def __on_response(self, dialog, id):
        if id == gtk.RESPONSE_OK:
            self.run_ok_callback(self.__get_username(), self.__get_password())
        elif id == gtk.RESPONSE_CANCEL or id == gtk.RESPONSE_DELETE_EVENT:
            self.run_cancel_callback()
        self.__dialog.hide()

    def hide(self):
        self.__dialog.hide()

    def present(self, username, password):
        if username:
            self.__username_entry.set_text(username)
        if password:
            self.__password_entry.set_text(password)
        self.__dialog.present()

    def __get_username(self):
        return self.__username_entry.get_text()

    def __get_password(self):
        return self.__password_entry.get_text()

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
                                                          5000) # timeout

            self.__notify_id = notify_id
            self.__latest_mail = first

    def __on_fetch_error(self, exc_info):
        pass

    def do_periodic_task(self):
        self.__google.fetch_new_mail(self.__on_fetched_mail, self.__on_fetch_error)

class Google:

    def __init__(self):
        self.__logger = logging.getLogger("bigboard.Google")
        self.__username = None
        self.__password = None
        self.__fetcher = AsyncHTTPLib2Fetcher()
        self.__auth_dialog = None
        self.__auth_uis = []
        self.__post_auth_hooks = []

        k = keyring.get_keyring()

        try:
            username, password = k.get_login("google")
            self.__username = username
            self.__password = password            
        except TypeError:
            self.__username = None
            self.__password = None

        self.__mail_checker = None
        self.__consider_checking_mail()

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
        keyring.get_keyring().store_login('google', self.__username, self.__password)

        for ui in self.__auth_uis:
            ui.hide()

        if self.__auth_dialog:
            self.__auth_dialog.hide()

        hooks = self.__post_auth_hooks
        self.__post_auth_hooks = []
        for h in hooks:
            h()

        self.__consider_checking_mail()

    def __on_auth_cancel(self):
        self.__username = None
        self.__password = None
        self.__consider_checking_mail()

    def __open_auth_uis(self):
        # we use the registered "auth uis" if available, else
        # the auth dialog. The "auth uis" are inline login boxes
        # in the stocks
        if len(self.__auth_uis) == 0:
            if not self.__auth_dialog:
                self.__auth_dialog = AuthDialog()
                self.__auth_dialog.set_callbacks(self.__on_auth_ok,
                                                 self.__on_auth_cancel)
            
            self.__auth_dialog.present(self.__username, self.__password)
        else:
            for ui in self.__auth_uis:
                ui.set_callbacks(self.__on_auth_ok,
                                 self.__on_auth_cancel)
                ui.present(self.__username, self.__password)

    def __with_login_info(self, func):
        """Call func after we get username and password"""

        if not self.__username or not self.__password:
            self.__post_auth_hooks.append(func)
            self.__open_auth_uis()
            return

        func()

    def __on_bad_auth(self, func):
        # don't null username, leave it filled in
        self.__password = None
        self.__with_login_info(func)

    ### Authentication

    def add_auth_ui(self, ui):
        ui.hide()
        self.__auth_uis.append(ui)

    def remove_auth_ui(self, ui):
        self.__auth_uis.remove(ui)

    ### Calendar

    def __on_calendar_load(self, url, data, cb, errcb):
        self.__logger.debug("loaded calendar from " + url)
        try:
            p = EventsParser()
            xml.sax.parseString(data, p)
            cb(p.get_events())
        except xml.sax.SAXException, e:
            errcb(sys.exc_info())

    def __on_calendar_error(self, url, exc_info, errcb):
        self.__logger.debug("error loading calendar from " + url)
        errcb(exc_info)

    def __have_login_fetch_calendar(self, cb, errcb):

        uri = 'http://www.google.com/calendar/feeds/' + self.__username + '@gmail.com/private/full'

        self.__fetcher.fetch(uri, self.__username, self.__password,
                             lambda url, data: self.__on_calendar_load(url, data, cb, errcb),
                             lambda url, exc_info: self.__on_calendar_error(url, exc_info, errcb),
                             lambda url: self.__on_bad_auth(lambda: self.__have_login_fetch_calendar(cb, errcb)))

    def fetch_calendar(self, cb, errcb):
        self.__with_login_info(lambda: self.__have_login_fetch_calendar(cb, errcb))


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
                             lambda url: self.__on_bad_auth(lambda: self.__have_login_fetch_documents(cb, errcb)))

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
                             lambda url: self.__on_bad_auth(lambda: self.__have_login_fetch_new_mail(cb, errcb)))

    def fetch_new_mail(self, cb, errcb):
        self.__with_login_info(lambda: self.__have_login_fetch_new_mail(cb, errcb))

__the_google = Google()

def get_google():
    return __the_google
        
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
