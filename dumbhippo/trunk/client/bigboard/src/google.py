import httplib2, keyring, libbig, sys, xml, xml.sax, logging, threading, gobject

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
        print "<" + name + ">"
        print attrs.getNames() # .getValue('foo')

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
        print "</" + name + ">"

    def characters(self, content):
        print content

    def get_events(self):
        return self.__events

class AsyncHTTPLib2Fetcher:
    """Asynchronously fetch objects over HTTP, invoking
       callbacks using the GLib main loop."""
    def fetch(self, url, username, password, cb, errcb):
        self.__logger = logging.getLogger("bigboard.AsyncHTTPLib2Fetcher")
        self.__logger.debug('creating async HTTP request thread for %s' % (url,))

        thread = threading.Thread(target=self._do_fetch, name="AsyncHTTPLib2Fetch", args=(url, username, password, cb, errcb))
        thread.setDaemon(True)
        thread.start()
        
    def _do_fetch(self, url, username, password, cb, errcb):
        self.__logger.debug("in thread fetch of %s" % (url,))
        try:
            h = httplib2.Http()
            h.add_credentials(username, password)
            h.follow_all_redirects = True

            self.__logger.debug("sending http request")

            headers, data = h.request(url, "GET", headers = {})

            self.__logger.debug("adding idle after http request")

            gobject.idle_add(lambda: cb(url, data) and False)
        except Exception, e:
            self.__logger.error("caught error for fetch of %s: %s" % (url, e))
            # in my experience sys.exc_info() is some kind of junk here, while "e" is useful
            gobject.idle_add(lambda: errcb(url, sys.exc_info()) and False)


class Google:

    def __init__(self):
        k = keyring.get_keyring()

        username, password = k.get_login("google")

        self.__username = username
        self.__password = password
        self.__fetcher = AsyncHTTPLib2Fetcher()

    def __on_calendar_load(self, url, data, cb, errcb):
        try:
            p = EventsParser()
            xml.sax.parseString(data, p)
            cb(p.get_events())
        except xml.sax.SAXException, e:
            errcb(sys.exc_info())


    def __on_calendar_error(self, url, exc_info, errcb):
        errcb(exc_info)

    def fetch_calendar(self, cb, errcb):

        uri = 'http://www.google.com/calendar/feeds/' + self.__username + '@gmail.com/private/full'

        self.__fetcher.fetch(uri, self.__username, self.__password,
                             lambda url, data: self.__on_calendar_load(url, data, cb, errcb),
                             lambda url, exc_info: self.__on_calendar_error(url, exc_info, errcb))

    def __on_documents_load(self, url, data, cb, errcb):
        try:
            p = DocumentsParser()
            xml.sax.parseString(data, p)
            cb(p.get_documents())
        except xml.sax.SAXException, e:
            errcb(sys.exc_info())

    def __on_documents_error(self, url, exc_info, errcb):
        errcb(exc_info)

    def fetch_documents(self, cb, errcb):

        uri = 'http://spreadsheets.google.com/feeds/spreadsheets/private/full'

        self.__fetcher.fetch(uri, self.__username, self.__password,
                             lambda url, data: self.__on_documents_load(url, data, cb, errcb),
                             lambda url, exc_info: self.__on_documents_error(url, exc_info, errcb))

if __name__ == '__main__':

    import gtk, gtk.gdk

    gtk.gdk.threads_init()

    libbig.init_logging('DEBUG', ['bigboard.AsyncHTTP2LibFetcher'])

    libbig.set_application_name("BigBoard")

    #keyring.get_keyring().store_login('google', 'havoc.pennington', '')

    g = Google()

    def display(x):
        print x
    
    g.fetch_documents(display, display)
    g.fetch_calendar(display, display)

    loop = gobject.MainLoop()

    print "running mainloop"
    loop.run()

    sys.exit(0)
