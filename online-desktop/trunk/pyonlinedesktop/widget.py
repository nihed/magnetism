#!/usr/bin/python

import os,sys,re,urllib,urllib2,logging,webbrowser,tempfile,shutil
import cookielib,urlparse
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
    def __init__(self, srcurl, f, env, baseurl=None):
        self.srcurl = srcurl
        base_srcurl = baseurl or os.path.dirname(srcurl)
        doc = xml.etree.ElementTree.ElementTree()        
     
        self.__lang = os.environ.get('LANG', 'en_US.UTF-8').split('.', 1)[0]
     
        self.__required_urls = {}
     
        doc.parse(f)
        module_prefs = doc.find('ModulePrefs')
        self.title = module_prefs.attrib.get('title', None)
        self.description = module_prefs.attrib.get('description', None)
        self.thumbnail = module_prefs.attrib.get('thumbnail', '')
        self.screenshot = module_prefs.attrib.get('screenshot', '')
        ig_pfx = '/ig/modules/'
        # Compatibility with builtin Google gadgets
        if self.thumbnail.startswith(ig_pfx) or self.screenshot.startswith(ig_pfx):
            self.__canonicalize_using_baseurl("http://www.google.com/")
        else:
            self.__canonicalize_using_baseurl(base_srcurl)  
        self.height = module_prefs.attrib.get('height', '200')
        self.prefs = {}
        for prefnode in doc.findall('UserPref'):
            try:
                self.prefs[prefnode.attrib['name']] = (prefnode.attrib['default_value'],)
            except KeyError, e:
                _logger.debug("parse failed for pref", exc_info=True)
        
        relurl_re = re.compile('^[A-Za-z/._]+$')
        matching_locales = {}    
        for localenode in module_prefs.findall('Locale'):
            lang = localenode.attrib.get('lang', u'en')
            if not self.__lang.startswith(lang):
                _logger.debug("ignoring lang %s", lang)
                continue
            msgs_url = localenode.attrib.get('messages', None)
            if not msgs_url:
                continue
            matching_locales[lang] = msgs_url
        for lang, msgs_url in matching_locales.iteritems():
            if not relurl_re.match(msgs_url):
                if not msgs_url.startswith(base_srcurl):
                    _logger.debug("failed same-domain URL match (using baseurl %s): %s", base_srcurl, msgs_url)
                    continue
            msgs_url = urlparse.urljoin(srcurl, msgs_url)
            self.__required_urls[msgs_url] = self.__on_received_locale
            
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
                gmodule_url = 'http://gmodules.com/ig/ifr?url=' + urllib.quote(srcurl)
                self.content = ('url', gmodule_url)
        elif content_node.attrib['type'] == 'url':
            href = content_node.attrib['href']
            href = env.replace_string(href)
            self.content = ('url', href)
        elif content_node.attrib['type'] == 'online-desktop-builtin':
            self.content = ('online-desktop-builtin', content_node.text)
            self.__canonicalize_using_baseurl(base_srcurl)
        else:
            raise WidgetError("Unknown content type")
        
    def __canonicalize_using_baseurl(self, baseurl):
        for attr in ('screenshot', 'thumbnail',):
            v = getattr(self, attr)
            if v:
                setattr(self, attr, urlparse.urljoin(baseurl, v))
   
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

    def get_required_urls(self):
        for url in self.__required_urls:
            yield url
    
    def process_urls(self, urldata):
        for url, content in urldata.iteritems():
            func = self.__required_urls[url]
            func(url, content)
            
    def __substitute_locale_msgs(self, value, msgs):
        oldidx = 0
        msgkey = '__MSG_'
        msgkey_len = len(msgkey)
        buf = StringIO()
        while True:
            idx = value.find(msgkey, oldidx)
            if idx < 0:
                break
            buf.write(value[oldidx:idx])
            keystart = idx+msgkey_len
            keyend = value.find('__', keystart)
            if keyend < 0:
                break
            key = value[keystart:keyend]
            val = msgs[key].strip()
            _logger.debug("substituting environment %s => %s", key, val)
            buf.write(val)
            oldidx = keyend+2
        buf.write(value[oldidx:])
        return buf.getvalue()
          
    def __on_received_locale(self, url, content):
        _logger.debug("got url %s => %d chars", url, len(content))
        doc = xml.etree.ElementTree.ElementTree()
        doc.parse(StringIO(content))
        msgs = {}          
        for msg in doc.findall('msg'):
            name = msg.attrib.get('name', None)
            if not name:
                continue
            msgs[name] = msg.text
        _logger.debug("parsed %d msgs", len(msgs))
        for k in ('title', 'description',):
            oldv = getattr(self, k)
            newv = self.__substitute_locale_msgs(oldv, msgs)
            setattr(self, k, newv)
        