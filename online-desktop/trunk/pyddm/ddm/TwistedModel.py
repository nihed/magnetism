import os
import re

from twisted.words.protocols.jabber import client, jid, xmlstream
from twisted.words.protocols.jabber.client import IQ
from twisted.words.xish import domish
from twisted.internet import reactor

from ddm.AbstractModel import *
from ddm.Resource import *
from ddm.NotificationSet import *
from ddm.Query import *

class MustLoginException(Exception):
    """Raised if the username and password can't be found"""
    def __init__(self, message):
        Exception.__init__(self, message)

class TwistedModel(AbstractModel):
    """An implementation of the Mugshot data model for testing purposes

    Twisted model uses the networking and main-loop functions of the 'twisted' framework.
    
    """
    
    def __init__(self, web_server="mugshot.org", xmpp_server=None, username=None, password=None):
        """Create a new TwistedModel instance that connects to the specified server.

        Arguments:
        web_server: Web server to connect to. The port may be specified by appending it after
           a colon. If the port is not specified, a default value will be used. (default='mugshot.org')
        xmpp_server: XMPP server to connect to. The port may be specified by appending it after
           a colon. If the port is not specified, a default value will be used.
           If no xmpp server is specified, the web server hostname will be used.
        username: GUID of the user to connect as (if not specified, found from cookies.txt)
        password: Password of the user (if not specified, found from cookies.txt)
        
        """
        AbstractModel.__init__(self)

        colon = web_server.find(":")
        if (colon >= 0):
            self.web_server = web_server[:colon]
            self.web_port = int(web_server[colon + 1:])
        else:
            self.web_server = web_server
            if web_server == "mugshot.org":
                self.web_port = 80
            elif web_server == "dogfood.mugshot.org":
                self.web_port = 9080
            else:
                self.web_port = 8080

        if self.web_port == 80:
            web_port = ""
        else:
            web_port = ":" + `self.web_port`

        self.web_base_url = "http://%s%s" % (self.web_server, web_port)

        self.xmpp_port = None
        if xmpp_server != None:
            colon = xmpp_server.find(":")
            if (colon >= 0):
                self.xmpp_server = xmpp_server[:colon]
                self.xmpp_port = int(xmpp_server[colon + 1:])
            else:
                self.xmpp_server =xmpp_server
        else:
            self.xmpp_server = self.web_server

        if self.xmpp_port == None:
            if self.web_port == 80:
                self.xmpp_port = 5122
            else:
                self.xmpp_port = 21020
        
        if username == None and password == None:
            username, password = _parse_cookies(self.web_server)
        elif username != None or password != None:
            raise MustLoginException("Either both user and password must be specified or neither")

        # FIXME: Use a legal resource
        self.username = username
        user_jid = jid.JID("%s@%s/mugshot-python" % (_guid_to_jabber_id(username), self.web_server))
        self.__factory = client.basicClientFactory(user_jid, password)
        self._xmlstream = None

    def run(self):
        """Connect to the server and start the twisted main loop.

        Note that to use this for anything but testing purposes, you'd want to break
        the 'connect to server' and 'start the main loop' parts, since the caller would
        presumbaly want to start the main loop itself.
        
        """

        global reactor
        self.__factory.addBootstrap(xmlstream.STREAM_AUTHD_EVENT, self.__on_auth_succeeded)
        self.__factory.addBootstrap(client.BasicAuthenticator.AUTH_FAILED_EVENT, self.__on_auth_failed)
 
        reactor.connectTCP(self.xmpp_server, self.xmpp_port, self.__factory)
        reactor.run()
        

    def query(self, method, fetch=None, single_result=False, **kwargs):
        return TwistedQuery(self, method, fetch, single_result, kwargs)
        
    def __on_auth_succeeded(self, xmlstream):
        # Nothing happens until we actually send a presence
        presence = domish.Element(('jabber:client', 'presence'))
        presence.addElement('status').addContent('Online')

        xmlstream.send(presence)

        # add a callback for the messages
        xmlstream.addObserver('/message', self.__on_message)
        
        self._xmlstream = xmlstream

        self._reset()
        
        notifications = NotificationSet(self)        
        self.global_resource._update_property(("online-desktop:/p/o/global", "online"),
                                              UPDATE_REPLACE, CARDINALITY_1, True,
                                              notifications)

        self.global_resource._update_property(("online-desktop:/p/o/global", "webBaseUrl"),
                                              UPDATE_REPLACE, CARDINALITY_1,  self.web_base_url,
                                              notifications)

        self_resource_id = self.web_base_url + "/o/user/" + self.username
        self_resource = self._ensure_resource(self_resource_id,
                                              "http://mugshot.org/p/o/user")
        
        self.global_resource._update_property(("online-desktop:/p/o/global", "self"),
                                              UPDATE_REPLACE, CARDINALITY_01, self_resource,
                                              notifications)

        notifications.send()
        
        self._on_ready()

    def __on_auth_failed(self, xmlstream):
        global reactor
        print 'Auth failed!'
        
        reactor.stop()

    def __get_resource_id(self, resource_element):
        resource_id = resource_element.getAttribute(("http://mugshot.org/p/system", "resourceId"))
        if resource_id == None:
            return None
    
        resource_base = None
        parent = resource_element
        while resource_base == None and parent != None:
            resource_base = parent.getAttribute(("http://mugshot.org/p/system", "resourceBase"))
            parent = parent.parent

        if resource_base != None:
            return resource_base + resource_id
        else:
            return resource_id

    def __property_value(self, element, type_char):
        value = self.__get_resource_id(element)
        if value != None:
            if type_char != 'r':
                raise Exception("Resource-valued property must have type char 'r'")
            try:
                value = self._get_resource(value)
            except KeyError:
                raise Exception("Resource-valued property points to a resource we don't know about")
            
            return value

        value = u""
        for n in element.children:
            if isinstance(n, basestring):
                value += n

        if type_char == 'b':
            return value.lower.equals("true")
        elif type_char == 'i':
            return int(value)
        elif type_char == 'l':
            return long(value)
        elif type_char == 'f':
            return float(type_char)
        elif type_char == 's':
            return value
        elif type_char == 'u':
            return value
        elif type_char == 'r': 
           raise Exception("resource_id attribute missing for resource-valued property")

    def __update_property_from_element(self, resource, property_element, seen_types, notifications):
        type_attr = property_element.getAttribute(("http://mugshot.org/p/system", "type"))
        if (type_attr == None):
            try:
                type_attr = seen_types[(property_element.uri, property_element.name)]
            except KeyError:
                raise Exception("Type attribute missing")
        else:
            seen_types[(property_element.uri, property_element.name)] = type_attr

        m = re.match(r"^(\+?)([bilfsru])([*?]?)$", type_attr)
        if (m == None):
            raise Exception("Unrecognized type string '%s'" % type_attr)

        if (m.group(3) == '*'):
            cardinality = CARDINALITY_N
        elif (m.group(3) == '?'):
            cardinality = CARDINALITY_01
        else:
            cardinality = CARDINALITY_1

        update_attr = property_element.getAttribute(("http://mugshot.org/p/system", "update"))
        if update_attr == None:
            update = UPDATE_REPLACE
        elif update_attr == "add":
            update = UPDATE_ADD
        elif update_attr == "replace":
            update = UPDATE_REPLACE
        elif update_attr == "delete":
            update = UPDATE_DELETE
        elif update_attr == "clear":
            update = UPDATE_CLEAR
        else:
            raise Exception("Bad update value: " + update)

        if update == UPDATE_CLEAR:
            value = None
        else:
            value = self.__property_value(property_element, m.group(2))

        resource._update_property((property_element.uri, property_element.name), update, cardinality, value, notifications=notifications)

    def _update_resource_from_element(self, resource_element, notifications=None):
        resource_id = self.__get_resource_id(resource_element)
        if resource_id == None:
            return

        resource = self._ensure_resource(resource_id, resource_element.uri)
        
        seen_types = {}
        for property_element in resource_element.elements():
            self.__update_property_from_element(resource, property_element, seen_types, notifications=notifications)

        return resource
                
    def __on_message(self, message):
        child = message.firstChildElement()
        if child.uri == "http://mugshot.org/p/system" and child.name == "notify":
            notifications = NotificationSet(self)
            for resource_element in child.elements():
                self._update_resource_from_element(resource_element, notifications=notifications)
            notifications.send()
        
class TwistedQuery(Query):
    """Implementation of Query for TwistedModel"""
    
    def __init__(self, model, method, fetch, single_result, params):
        Query.__init__(self, params, single_result)
        
        self.__model = model
        self.__iq = IQ(model._xmlstream, "get")
        self.__single_result = single_result
        
        element = self.__iq.addElement(method)
    
        for name in self._params:
            param = element.addElement(("http://mugshot.org/p/system", "param"))
            param["name"] = name
            param.addContent(self._params[name])
        
        if fetch != None:
            element[("http://mugshot.org/p/system", "fetch")] = fetch

        self.__iq.addCallback(self.__on_reply)

    def __is_indirect(self, resource_element):
        indirect_attr = resource_element.getAttribute(("http://mugshot.org/p/system", "indirect"))
        return  indirect_attr != None and indirect_attr.lower() == "true"
    
    def __on_reply(self, iq):
        child = iq.firstChildElement()

        result = []
        for resource_element in child.elements():
            resource = self.__model._update_resource_from_element(resource_element)
            if resource != None and not self.__is_indirect(resource_element):
                result.append(resource)
                
        self._on_success(result)

    def execute(self):
        self.__iq.send()

################################## Utility functions #########################################3
        
def _guid_to_jabber_id(guid):
    return re.sub(r"([a-z])", r"\1_", guid)

def _file_exists(f):
    try:
        os.stat(f)
        return True;
    except OSError:
        return False

def _find_cookies_file():
    home = os.environ['HOME']
    cookies_file = None
    try:
        for subdir in os.listdir(os.path.join(home, ".mozilla", "firefox")):
            if (subdir.endswith(".default")):
                possible = os.path.join(home, ".mozilla", "firefox", subdir, "cookies.txt")
                if (_file_exists(possible)):
                    cookies_file = possible
                    break
    except OSError:
        pass

    if not cookies_file:
        raise MustLoginException("Can't find your browser cookies file")

    return cookies_file

_COOKIE_REGEXP = re.compile(r"^(\S+)(?:\s+\S+){4}\s+(\S+)\s+(.*)$")

def _parse_cookies(server):
    cookies_file = _find_cookies_file()
    f = open(cookies_file)
    
    username = None
    password = None
    
    for l in f:
        if re.match(r"^\s*#", l):
            continue
        if re.match(r"^\s*$", l):
            continue
        l = l.rstrip()

        m = _COOKIE_REGEXP.match(l)
        if m:
            host = m.group(1)
            cookie_name = m.group(2)
            value = m.group(3)
        
        if host.endswith(server) and cookie_name == "auth":
            cookie_username = None
            cookie_password = None
            
            fields = value.split("&")
            for field in fields:
                (k,v) = field.split("=")
                if k == "host":
                    cookie_host = v
                elif k == "name":
                    cookie_username = v
                elif k == "password":
                    cookie_password = v
            if cookie_host == server and cookie_username != None and cookie_password != None:
                username = cookie_username
                password = cookie_password
                break
    f.close()
    
    if username == None or password == None:
        raise MustLoginException("Can't find the Mugshot authentication cookie for %s, do you need to log in?" % server)

    return (username, password)

