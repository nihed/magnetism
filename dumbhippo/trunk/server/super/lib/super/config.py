import os
import re
import string
import sys
import xml.sax
import xml.sax.handler

import super.service;
import super.merge;

OUTSIDE = 0
SUPERCONF = 1
PARAMETER = 2
SERVICE = 3
SERVICE_PARAMETER = 4
SERVICE_MERGE = 5
SERVICE_DIRECTORY = 6
    
class ConfigHandler (xml.sax.ContentHandler):
    def __init__(self, config):
        self.config = config
        self.state = OUTSIDE
        self.locator = None
        self.param_name = None

    def setDocumentLocator(self, locator):
        self.locator = locator

    def startElement(self, name, attrs):
        if (self.state == OUTSIDE):
            if (name == 'superconf'):
                self.state = SUPERCONF
                return
            else:
                self.report("Root element must be 'superconf'")
        elif (self.state == SUPERCONF):
            if (name == 'parameter'):
                (self.param_name,) = self.parse_attributes(name, attrs, 'name', True)
                self.state = PARAMETER
                return
            elif (name == 'service'):
                (name, cls) = self.parse_attributes(name, attrs, 'name', True, 'class', False)
                if (self.config.has_service(name)):
                    self.service = self.config.get_service(name)
                else:
                    self.service = super.service.Service(name, self.config)
                    self.config.add_service(self.service)
                self.state = SERVICE
                return
        elif (self.state == SERVICE):
            if (name == 'parameter'):
                (self.param_name,) = self.parse_attributes(name, attrs, 'name', True)
                self.state = SERVICE_PARAMETER
                return
            elif (name == 'merge'):
                (src, target, exclude, expand, hot) = \
                     self.parse_attributes(name, attrs, 'src', True,
                                                        'target', True,
                                                        'exclude', False,
                                                        'expand', False,
                                                        'hot', False)
                if expand == None:
                    expand = False
                if hot == None:
                    hot = False

                merge = super.merge.Merge(src, target, exclude, expand, hot)
                self.service.add_merge(merge)

                self.state = SERVICE_MERGE
                return
            elif (name == 'directory'):
                self.state = SERVICE_DIRECTORY
                return

        self.report("Invalid element <%s/>" % name)

    def endElement(self, name):
        if (self.state == OUTSIDE):
            pass
        elif (self.state == SUPERCONF):
            self.state = OUTSIDE
        elif (self.state == PARAMETER):
            self.param_name = None
            self.state = SUPERCONF
        elif (self.state == SERVICE):
            self.service = None
            self.state = SUPERCONF
        elif (self.state == SERVICE_PARAMETER):
            self.state = SERVICE
        elif (self.state == SERVICE_MERGE):
            self.state = SERVICE
        elif (self.state == SERVICE_DIRECTORY):
            self.state = SERVICE

    def characters(self, content):
        content = content.strip()
        if (content == ""):
            return
        
        if (self.state == PARAMETER):
            self.config.set_parameter(self.param_name, content)
            return
        elif (self.state == SERVICE_PARAMETER):
            self.service.set_parameter(self.param_name, content)
            return

        self.report("Unexpected characters: '%s'" % content)

    def report(self, message):
        raise xml.sax.SAXParseException(message, None, self.locator)

    def parse_attributes(self, element, attrs, *specs):
        result = []
        listed = {}
        for i in range(0, len(specs)-1, 2):
            name = specs[i]
            required = specs[i+1]
            listed[name] = 1
            if attrs.has_key(name):
                result.append(attrs[name])
            else:
                if required:
                    self.report("Attribute '%s' is required for <%s/>" % (name, element))
                else:
                    result.append(None)
            
        for key in attrs.keys():
            if not listed.has_key(key):
                self.report("Unknown attribute '%s' for <%s/>" % (key, element))

        return result

class Config:
    def __init__(self, superdir, conffile):
        self.params={}
        self.services={}

        self.load(os.path.join(superdir, 'conf', 'base.conf'), True)
        if conffile != None:
            self.load(conffile, True)
        elif (os.environ['HOME']):
            self.load(os.path.join(os.environ['HOME'], '.super.conf'), False)

        print self.services['jboss'].expand("$stopCommand")

    def get_services(self):
        return ['jboss', 'mysql', 'jive']
    def run_action(self, action, services):
        pass

    def load(self, filename, must_exist):
        handler = ConfigHandler(self)
        f = None
        try: 
            f = open(filename)
        except IOError:
            if must_exist:
                print >>sys.stderr, "Cannot open config file '%s'" % filename
                sys.exit(1)
        if f:
            try:
                xml.sax.parse(f, handler)
            except xml.sax.SAXParseException, e:
                print >>sys.stderr, e
                sys.exit(1)
            f.close()

    def add_service(self, service):
        self.services[service.get_name()] = service
        
    def has_service(self, name):
        return self.services.has_key(name)

    def get_service(self, name):
        self.services[name]

    def set_parameter(self, name, value):
        self.params[name] = value

    def get_parameter(self, name):
        return self.params[name]

    def has_parameter(self, name):
        return self.params.has_key(name)

    def expand_parameter(self, name, scope=None):
        if scope == None:
            scope = self

        return self.expand(scope.get_parameter(name), scope)

    def expand(self, str, scope=None):
        if scope == None:
            scope = self

        ident = '[a-zA-Z_][a-zA-Z_0-9]*'
        arithmetic = '\$\(\(\s*(%s)\s*\+\s*([0-9]+)\s*\)\)' % ident
        
        def repl(m):
            match_str = m.group(0)
            arithmetic_m = re.match(arithmetic, match_str)
            if arithmetic_m:
                param_name = arithmetic_m.group(1)
                increment = arithmetic_m.group(2)

                param = self.expand_parameter(param_name, scope)

                return "%d" % (string.atoi(param) + string.atoi(increment))
            else:
                param_name = match_str[1:]
                
                return self.expand_parameter(param_name, scope)

        ident = "[a-zA-Z_][a-zA-Z_0-9]*"
        return re.sub('%s|\$%s' % (arithmetic, ident), repl, str)
