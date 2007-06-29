import logging, os
import xml.dom.minidom

import gobject

import bigboard.libbig as libbig
from bigboard.libbig import BiMap
from bigboard.libbig.singletonmixin import Singleton

class State(Singleton):
    """Extremely simple state saving mechanism.  Intended to be
    used for stocks to store simple options, as well as non-preference
    state.  For now, machine-local semantics may be assumed.  
    
    Only unicode strings, numbers, and booleans may be stored."""
    def __init__(self):
        self._logger = logging.getLogger("bigboard.State")
        self.__path = libbig.get_bigboard_config_file("state.xml")
        self.__defaults = {}
        self.__state = {}
        self.__idle_save_id = 0
        self.__type_name = BiMap('pytype', 'name', 
                                 {unicode: "string", 
                                  int: "number",
                                  long: "number",
                                  bool: "boolean"})
        self.__type_read = {bool: lambda val: val.lower() == 'true'}
        self.__type_write = {}
        self.__read()
        
    def __validate_key(self, key):
        if isinstance(key, str):
            key = unicode(key)
        if not isinstance(key, unicode):
            raise ValueError("Keys must be unicode")
        return key        
        
    def set_default(self, key, value):
        key = self.__validate_key(key)
        self._logger.debug("set default of %s: %s", key, value)
        self.__defaults[key] = value
        
    def __getitem__(self, key):
        key = self.__validate_key(key)
        try:
            val = self.__state[key]
        except KeyError:
            val = self.__defaults[key]
        self._logger.debug("lookup of %s: %s", key, val)
        return val 

    def __setitem__(self, key, value):
        self._logger.debug("set %s: %s", key, value)
        key = self.__validate_key(key)
        if not key.startswith("/"):
            raise ValueError("Keys must begin with /")
        if not self.__type_name['pytype'].has_key(type(value)):
            raise ValueError("Can't store value of type '%s'", type(value))
        self.__state[key] = value
        self.__queue_save()
        
    def __read(self):
        self._logger.info("reading state file %s", self.__path)
        if not os.access(self.__path, os.R_OK):
            self._logger.info("no state file %s", self.__path)            
            return
        try:
            f = file(self.__path, 'r')
            doc = xml.dom.minidom.parse(f)
            for child in doc.documentElement.childNodes:
                if not (child.nodeType == xml.dom.Node.ELEMENT_NODE and child.tagName == 'key'):
                    continue
                name = child.getAttribute("name")
                type_str = child.getAttribute("type")
                type_val = self.__type_name['name'][type_str]
                valnode = child.firstChild
                if not valnode.nodeType == xml.dom.Node.TEXT_NODE:
                    continue
                value = valnode.nodeValue
                if self.__type_read.has_key(type_val):
                    parsed_val = self.__type_read[type_val](value)
                else:
                    parsed_val = unicode(value)
                self._logger.debug("read value %s %s '%s'", name, type_val, parsed_val)
                self.__state[unicode(name)] = parsed_val
        except:
            self._logger.exception("Failed to read state")
        
    def __queue_save(self):
        if self.__idle_save_id == 0:
            self.__idle_save_id = gobject.timeout_add(3000, self.__idle_save)
        
    def __idle_save(self):
        # TODO: if we're feeling ambitious, do this save in a separate thread to not block
        # ui.  Note you must handle locking on self.__state, maybe just make a copy
        self._logger.info("in idle state save")
        doc = xml.dom.minidom.getDOMImplementation().createDocument(None, "bigboard-state", None)
        doc.documentElement.setAttribute("version", "0")
        for k,v in self.__state.iteritems():
            elt = doc.createElement("key")
            elt.setAttribute("name", k)
            typename = self.__type_name['pytype'][type(v)]
            elt.setAttribute("type", typename)
            self._logger.debug("saving value %s %s '%s'", k, typename, v)
            if self.__type_write.has_key(type(v)):
                serialized_val = self.__type_write(type(v))[v]
            else:
                serialized_val = unicode(v)
            elt.appendChild(doc.createTextNode(serialized_val))
            doc.documentElement.appendChild(elt)
        tmpf_path = self.__path + ".tmp"
        tmpf = file(tmpf_path, 'w')
        doc.writexml(tmpf)
        tmpf.close()
        os.rename(tmpf_path, self.__path)
        self._logger.info("idle state save complete")
        self.__idle_save_id = 0
        
class PrefixedState(object):
    def __init__(self, prefix):
        self.__prefix = prefix
        self.__state = State.getInstance()

    def set_default(self, key, value):
        return self.__state.set_default(self.__prefix + key, value)
        
    def __getitem__(self, key):
        return self.__state[self.__prefix + key]

    def __setitem__(self, key, value):
        self.__state[self.__prefix + key] = value
