import httplib2, keyring, libbig, sys, xml, xml.sax, logging, threading, gobject, gtk

class AbstractDocument(libbig.AutoStruct):
    def __init__(self):
        libbig.AutoStruct.__init__(self,
                                   { 'title' : 'Untitled', 'link' : None })

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


class Event(libbig.AutoStruct):
    def __init__(self):
        libbig.AutoStruct.__init__(self,
                                   { 'title' : '', 'start_time' : '', 'end_time' : '' })

class EventsParser(xml.sax.ContentHandler):
    def __init__(self):
        self.__events = []

    def startElement(self, name, attrs):
        #print "<" + name + ">"
        #print attrs.getNames() # .getValue('foo')

        if name == 'entry':
            e = Event()
            self.__events.append(e)
        elif len(self.__events) > 0:
            e = self.__events[-1]
            if name == 'title':
                e.update({'title' : ''}) # FIXME
            elif name == 'gd:when':
                e.update({ 'start_time' : attrs.getValue('startTime'),
                           'end_time' : attrs.getValue('endTime') })

    def endElement(self, name):
        #print "</" + name + ">"
        pass

    def characters(self, content):
        #print content
        pass

    def get_events(self):
        return self.__events

class AsyncHTTPLib2Fetcher:
    """Asynchronously fetch objects over HTTP, invoking
       callbacks using the GLib main loop."""
    def fetch(self, url, username, password, cb, errcb, authcb):
        self.__logger = logging.getLogger("bigboard.AsyncHTTPLib2Fetcher")
        self.__logger.debug('creating async HTTP request thread for %s' % (url,))

        thread = threading.Thread(target=self._do_fetch, name="AsyncHTTPLib2Fetch", args=(url, username, password, cb, errcb, authcb))
        thread.setDaemon(True)
        thread.start()
        
    def _do_fetch(self, url, username, password, cb, errcb, authcb):
        self.__logger.debug("in thread fetch of %s" % (url,))
        try:
            h = httplib2.Http()
            h.add_credentials(username, password)
            h.follow_all_redirects = True

            self.__logger.debug("sending http request")

            headers, data = h.request(url, "GET", headers = {})

            if headers.status == 401:
                self.__logger.error("auth failure for fetch of %s: %s" % (url, headers.status))
                gobject.idle_add(lambda: authcb(url) and False)
            else:
                self.__logger.debug("adding idle after http request")

                gobject.idle_add(lambda: cb(url, data) and False)
        except Exception, e:
            self.__logger.error("caught error for fetch of %s: %s" % (url, e))
            # in my experience sys.exc_info() is some kind of junk here, while "e" is useful
            gobject.idle_add(lambda: errcb(url, sys.exc_info()) and False)


class AuthDialog:
    def __init__(self, username, password):
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
        if username is not None:
            local_entry1.set_text(username)
        table.attach(local_entry1, 1, 2, 0, 1)
        label.set_mnemonic_widget(local_entry1)

        label = gtk.Label("_Password")
        label.set_alignment(0.0, 0.5)
        label.set_use_underline(True)
        table.attach(label, 0, 1, 1, 2)
        local_entry2 = gtk.Entry()
        local_entry2.set_visibility(False)
        local_entry2.set_activates_default(True)
        if password is not None:
            local_entry2.set_text(password)
        table.attach(local_entry2, 1, 2, 1, 2)
        label.set_mnemonic_widget(local_entry2)

        dialog.vbox.show_all()    

        dialog.connect('delete-event', self.__on_delete_event)

        dialog.connect('response', self.__on_response)

        self.__dialog = dialog

        self.__username_entry = local_entry1
        self.__password_entry = local_entry2

        self.__okcb = None
        self.__cancelcb = None

    def __on_delete_event(self, dialog, event):
        self.__dialog.hide()
        return True # prevent destroy

    def __on_response(self, dialog, id):
        if id == gtk.RESPONSE_OK:
            if self.__okcb:
                self.__okcb()
        elif id == gtk.RESPONSE_CANCEL or id == gtk.RESPONSE_DELETE_EVENT:
            if self.__cancelcb:
                self.__cancelcb()
        self.__dialog.hide()

    def present(self):
        self.__dialog.present()

    def get_username(self):
        return self.__username_entry.get_text()

    def get_password(self):
        return self.__password_entry.get_text()
        
    def set_callbacks(self, okcb, cancelcb):
        self.__okcb = okcb
        self.__cancelcb = cancelcb

class Google:

    def __init__(self):
        self.__logger = logging.getLogger("bigboard.Google")
        self.__username = None
        self.__password = None
        self.__fetcher = AsyncHTTPLib2Fetcher()
        self.__auth_dialog = None
        self.__post_auth_hooks = []

        k = keyring.get_keyring()

        username, password = k.get_login("google")

        self.__username = username
        self.__password = password

    def __on_auth_dialog_ok(self):
        self.__username = self.__auth_dialog.get_username()
        self.__password = self.__auth_dialog.get_password()
        keyring.get_keyring().store_login('google', self.__username, self.__password)

        hooks = self.__post_auth_hooks
        self.__post_auth_hooks = []
        for h in hooks:
            h()

    def __on_auth_dialog_cancel(self):
        self.__username = None
        self.__password = None

    def __open_auth_dialog(self):
        if not self.__auth_dialog:
            self.__auth_dialog = AuthDialog(self.__username, self.__password)
            self.__auth_dialog.set_callbacks(self.__on_auth_dialog_ok,
                                             self.__on_auth_dialog_cancel)
            
        self.__auth_dialog.present()

    def __with_login_info(self, func):
        """Call func after we get username and password"""

        if not self.__username or not self.__password:
            self.__post_auth_hooks.append(func)
            self.__open_auth_dialog()
            return

        func()

    def __on_bad_auth(self, func):
        # don't null username, leave it filled in
        self.__password = None
        self.__with_login_info(func)

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
        
if __name__ == '__main__':

    import gtk, gtk.gdk

    gtk.gdk.threads_init()

    libbig.init_logging('DEBUG', ['bigboard.AsyncHTTP2LibFetcher'])

    libbig.set_application_name("BigBoard")

    #AuthDialog('foo', 'bar').present()
    #gtk.main()
    #sys.exit(0)

    #keyring.get_keyring().store_login('google', 'havoc.pennington', 'wrong')

    g = Google()

    def display(x):
        print x
    
    g.fetch_documents(display, display)
    g.fetch_calendar(display, display)

    gtk.main()

    sys.exit(0)
