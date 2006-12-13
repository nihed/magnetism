import os
import re
import string
import sys
import xml.sax
import xml.sax.handler

from super.service import Service
from super.merge import Merge

# Parsing states; they correspond to permissable nestings. So,
# for example, SERVICE_PARAMETER means that we are in
# <superconf><service><parameter>...</parameter></service></superconf>
OUTSIDE = 0
SUPERCONF = 1
INCLUDE = 2
PARAMETER = 3
SERVICE = 4
SERVICE_PARAMETER = 5
SERVICE_MERGE = 6
SERVICE_DIRECTORY = 7
SERVICE_REQUIREDSERVICE = 8
SERVICE_TARGETATTRIBUTES = 9
    
class ConfigHandler (xml.sax.ContentHandler):

    """A ContentHandler that does the bulk of the work for parsing
    our configuration files.
    """
    
    def __init__(self, config):
        """Create a new ConfigHandler which stores results in config"""
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
                self._report("Root element must be 'superconf'")
        elif (self.state == SUPERCONF):
            if (name == 'include'):
                (filename,) = self._parse_attributes(name, attrs, 'file', True)
                self.config.load_config(os.path.expanduser(filename), True)
                self.state = INCLUDE
                return
            elif (name == 'parameter'):
                (self.param_name,) = self._parse_attributes(name, attrs, 'name', True)
                self.param_value = ""
                self.state = PARAMETER
                return
            elif (name == 'service'):
                (name, cls, enabled) = self._parse_attributes(name, attrs, 'name', True,
                                                                           'class', False,
                                                                           'enabled', False)
                if (self.config.has_service(name)):
                    self.service = self.config.get_service(name)
                else:
                    if (cls):
                        module = "super." + cls.lower()
                        exec "import %s; self.service = %s.%s(name, self.config)" % (module, module, cls)
                    else:
                        self.service = Service(name, self.config)
                    self.config.add_service(self.service)

                if not enabled is None:
                    self.service.set_enabled(enabled)
                    
                self.state = SERVICE
                return
        elif (self.state == SERVICE):
            if (name == 'parameter'):
                (self.param_name,) = self._parse_attributes(name, attrs, 'name', True)
                self.param_value = ""
                self.state = SERVICE_PARAMETER
                return
            elif (name == 'merge'):
                (src, dest, exclude, expand, symlink, hot) = \
                     self._parse_attributes(name, attrs, 'src', True,
                                                         'dest', False,
                                                         'exclude', False,
                                                         'expand', False,
                                                         'symlink', False,
                                                         'hot', False)
                
                merge = Merge(self.service,
                              src, dest,
                              exclude,
                              self._parse_bool('expand', expand),
                              self._parse_bool('symlink', symlink),
                              self._parse_bool('hot', hot))
                                               
                self.service.add_merge(merge)

                self.state = SERVICE_MERGE
                return
            elif (name == 'requiredService'):
                (service_name,) = self._parse_attributes(name, attrs,
                                                         'service', True)
                self.service.add_required_service(service_name)
                self.state = SERVICE_REQUIREDSERVICE
                return
            elif (name == 'targetAttributes'):
                (pattern, ignore, preserve, fuzzy, hot_update_last) = \
                    self._parse_attributes(name, attrs, 
                                           'pattern',  True,
                                           'ignore',   False,
                                           'preserve', False,
                                           'fuzzy',    False,
                                           'hot_update_last', False)
                
                self.service.add_target_attributes(pattern,
                                                   self._parse_bool('ignore', ignore),
                                                   self._parse_bool('preserve', preserve),
                                                   self._parse_bool('fuzzy', fuzzy),
                                                   self._parse_bool('hot_update_last', hot_update_last))
                self.state = SERVICE_TARGETATTRIBUTES
                return
            elif (name == 'directory'):
                self.state = SERVICE_DIRECTORY
                return

        self._report("Invalid element <%s/>" % name)

    def endElement(self, name):
        if (self.state == OUTSIDE):
            pass
        elif (self.state == SUPERCONF):
            self.state = OUTSIDE
        elif (self.state == INCLUDE):
            self.state = SUPERCONF
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
        elif (self.state == SERVICE_TARGETATTRIBUTES):
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
            self._report("Unexpected characters: '%s'" % content)

    def _report(self, message):
        """Raise a properly located parse exception"""
        raise xml.sax.SAXParseException(message, None, self.locator)

    def _parse_attributes(self, element, attrs, *specs):
        """Take the attributes object from startElement, and check
        that it has all elements we require, and no elements that
        we don't understand.

        specs -- a list of attribute_name, required pairs

        returns: list of attributes, in the same order as specs,
        attributes that aren't present are returned as None
        """
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
                    self._report("Attribute '%s' is required for <%s/>" % (name, element))
                else:
                    result.append(None)
            
        for key in attrs.keys():
            if not listed.has_key(key):
                self._report("Unknown attribute '%s' for <%s/>" % (key, element))

        return result

    def _parse_bool(self, attr, val):
        """Parse a boolean attribute value. Default is False."""
        if (val is None):
            return False
        else:
            try:
                return self.config.is_true(val)
            except ValueError, e:
                self._report("%s: %s" % (val, e))

    
