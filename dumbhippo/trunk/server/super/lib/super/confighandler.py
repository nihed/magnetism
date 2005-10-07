import os
import re
import string
import sys
import xml.sax
import xml.sax.handler

from super.service import Service
from super.merge import Merge

OUTSIDE = 0
SUPERCONF = 1
PARAMETER = 2
SERVICE = 3
SERVICE_PARAMETER = 4
SERVICE_MERGE = 5
SERVICE_DIRECTORY = 6
SERVICE_REQUIREDSERVICE = 6
    
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
                self.param_value = ""
                self.state = PARAMETER
                return
            elif (name == 'service'):
                (name, cls) = self.parse_attributes(name, attrs, 'name', True, 'class', False)
                if (self.config.has_service(name)):
                    self.service = self.config.get_service(name)
                else:
                    self.service = Service(name, self.config)
                    self.config.add_service(self.service)
                self.state = SERVICE
                return
        elif (self.state == SERVICE):
            if (name == 'parameter'):
                (self.param_name,) = self.parse_attributes(name, attrs, 'name', True)
                self.param_value = ""
                self.state = SERVICE_PARAMETER
                return
            elif (name == 'merge'):
                (src, dest, exclude, expand, symlink, hot) = \
                     self.parse_attributes(name, attrs, 'src', True,
                                                        'dest', False,
                                                        'exclude', False,
                                                        'expand', False,
                                                        'symlink', False,
                                                        'hot', False)
                def do_bool(attr, val):
                    if (val == None):
                        return False
                    elif (val == 'yes'):
                        return True
                    elif (val == 'no'):
                        return False
                    else:
                        self.report("'%s' must be either 'yes' or 'no'", attr)

                expand = do_bool('expand', expand)
                symlink = do_bool('symlink', symlink)
                hot = do_bool('hot', hot)
                
                merge = Merge(self.service,
                              src, dest, exclude, expand, symlink, hot)
                self.service.add_merge(merge)

                self.state = SERVICE_MERGE
                return
            elif (name == 'requiredService'):
                (service_name,) = self.parse_attributes(name, attrs, 'service', True)
                self.service.add_required_service(service_name)
                self.state = SERVICE_REQUIREDSERVICE
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
            self.config.set_parameter(self.param_name, self.param_value.strip())
            self.state = SUPERCONF
        elif (self.state == SERVICE):
            self.service = None
            self.state = SUPERCONF
        elif (self.state == SERVICE_PARAMETER):
            self.service.set_parameter(self.param_name, self.param_value.strip())
            self.state = SERVICE
        elif (self.state == SERVICE_MERGE):
            self.state = SERVICE
        elif (self.state == SERVICE_REQUIREDSERVICE):
            self.state = SERVICE
        elif (self.state == SERVICE_DIRECTORY):
            self.state = SERVICE

    def characters(self, content):
        if (self.state == PARAMETER or
            self.state == SERVICE_PARAMETER):
            self.param_value = self.param_value + content
            return

        stripped = content.strip()
        if (stripped != ""):
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
