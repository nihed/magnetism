#!/usr/bin/python

import os,sys,re,urllib,urllib2,logging,webbrowser,tempfile,shutil
import cookielib
import xml.etree.ElementTree
from StringIO import StringIO

import gobject,gtk,gnomevfs

_logger = logging.getLogger("od.WidgetSystem")

class WidgetEnvironment(dict):
    def replace_string(self, s):
        for k,v in self.iteritems():
            s = s.replace('__ENV_' + k + '__', v)
        return s

class WidgetError(Exception):
    def __init__(self, msg):
        super(WidgetError, self).__init__(msg)

class WidgetParser(object):
    IG_JS = '''

_IG_Prefs = function() { }
_IG_Prefs.prototype = {
  getString: function(key) {
    return __IG_PREFS_DEFAULTS[key];
  },
  getInt: function(key) {
    return __IG_PREFS_DEFAULTS[key];    
  },
  getBool: function(key) {
    return __IG_PREFS_DEFAULTS[key];
  },
  set: function(key, value) {
  }
}

_IG_MiniMessage = function(modid, opt_container) {
}
_IG_MiniMessage.prototype = {
  createDismissableMessage: function(msg, opt_callback) { },
  createTimerMessage: function(msg, seconds, opt_callback) {},
  createStaticMessage: function(msg) {},
  dismissMessage: function(msg) {},
}

'''    
    def __init__(self, srcurl, f, env):
        self.srcurl = srcurl
        doc = xml.etree.ElementTree.ElementTree()        
     
        doc.parse(f)
        module_prefs = doc.find('ModulePrefs')
        self.title = module_prefs.attrib['title']
        self.height = module_prefs.attrib.get('height', '200')
        self.prefs = {}
        for prefnode in doc.findall('UserPref'):
            try:
                self.prefs[prefnode.attrib['name']] = (prefnode.attrib['default_value'],)
            except KeyError, e:
                _logger.debug("parse failed for pref", exc_info=True)

        content_node = doc.find('Content')
        self.__content_uri = None
        self.content_type = content_node.attrib['type']
        if self.content_type == 'html':
            content = content_node.text
            _logger.debug("got content of %d chars", len(content))
 
            if module_prefs.attrib.get('render_inline', '') == 'required':
                content = env.replace_string(content)
                content = content.replace('__MODULE_ID__', '0')
                htmlcontent = '''<html><head><title>Widget</title><script type="text/javascript">'''
                htmlcontent += self.__default_prefs_js()
                htmlcontent += self.IG_JS
                htmlcontent += '''</script></head><body>''' + content + '''</body></html>'''
                self.content = ('html', htmlcontent)
            else:
                gmodule_url = 'http://gmodules.com/ig/ifr?url=' + urllib.quote(url)
                self.content = ('url', gmodule_url)
        elif content_node.attrib['type'] == 'url':
            href = content_node.attrib['href']
            href = env.replace_string(href)
            self.content = ('url', href)
        elif content_node.attrib['type'] == 'online-desktop-builtin':
            self.content = ('online-desktop-builtin', content_node.text)
        else:
            raise WidgetError("Unknown content type")           
   
    def __default_prefs_js(self):
        result = StringIO()
        result.write('''__IG_PREFS_DEFAULTS = {''')
        def js_quotestr(s):
            return '"' + s + '"'
        for k,v in self.prefs.iteritems():
            result.write(k)
            result.write(': ')
            result.write(js_quotestr(v[0]))
            result.write(',\n')
        result.write('''}\n''')
        return result.getvalue()
