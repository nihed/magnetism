import httplib2, keyring, libbig, sys, xml, xml.sax

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

class Google:

    def __init__(self):
        k = keyring.get_keyring()

        username, password = k.get_login("google")

        self.__username = username
        self.__password = password

    def get_calendar(self):
        h = httplib2.Http()
        h.add_credentials(self.__username, self.__password)
        h.follow_all_redirects = True
        uri = 'http://www.google.com/calendar/feeds/' + self.__username + '@gmail.com/private/full'
        
        headers, data = h.request(uri, "GET", headers = {})

        try:
            p = EventsParser()
            xml.sax.parseString(data, p)
            return p.get_events()
        except xml.sax.SAXException, e:
            return None
        
        
if __name__ == '__main__':

    libbig.set_application_name("BigBoard")

    keyring.get_keyring().store_login('google', 'havoc.pennington', '')

    g = Google()

    events = g.get_calendar()

    print events

    
    
