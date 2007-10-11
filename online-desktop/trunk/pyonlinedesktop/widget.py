#!/usr/bin/python

import os,sys,re,urllib,urllib2,logging,webbrowser,tempfile,shutil
import cookielib
import xml.etree.ElementTree
from StringIO import StringIO

import gobject,gtk,gnomevfs,gtkmozembed

import pyonlinedesktop.mozembed_wrap as mozembed_wrap

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
    def __init__(self, f, env):
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
  
class Widget(gtk.VBox):

    def __init__(self, url, env):
        super(Widget, self).__init__()
      
        f = gtk.Frame()
        self.__moz = mozembed_wrap.MozClient()
        _logger.debug("Reading module url %s", url)
        self.__content = content = WidgetParser(urllib2.urlopen(url), env)
        (content_type, content_data) = content.content
        if content_type == 'html':
            self.__moz.set_data("http://www.google.com/", content_data)
        elif content_type == 'url':
            self.__moz.load_url(content_data)
        else:
            pass
            
        self.__moz.connect("open-uri", self.__on_open_uri)
        self.__moz.connect("location", self.__on_location)   
        self.__moz.connect("new-window", self.__on_new_window)
        f.add(self.__moz)
        self.pack_start(f, expand=True)
        self.__moz.show_all()
        self.__moz.set_size_request(200, int(content.height))

    def __on_temp_moz_location(self, tm, *args):
        uri = tm.get_location()        
        _logger.debug("got location in temp moz: %s", uri)        

    def __on_new_window(self, *args):
        _logger.debug("got new window request args: %s", args)
        self.__temp_moz = tempmoz = gtkmozembed.MozEmbed() 
        tempmoz.connect('location', self.__on_temp_moz_location)
        return tempmoz

    def __on_location(self, *args):
        loc = self.__moz.get_location()
        _logger.debug("got location %s", loc)

    def __on_open_uri(self, m, uri):
        _logger.debug("got open uri: %s", uri)
        if self.__content.content[0] == 'url' and self.__content.content[1] == uri:
            return False
        if not uri.startswith("http"):
            return False
        _logger.debug("opening in external browser: %s", uri)
        webbrowser.open(uri)
        return True

    def get_title(self):
        return self.__title

def main():
    logging.basicConfig(level=logging.DEBUG)
    oddir = os.path.expanduser('~/.od/')
    odwidgets = os.path.join(oddir, 'widgets')
    try:
        os.makedirs(odwidgets)
    except:
        pass

    from pyonlinedesktop.firefox import FirefoxProfile
    ffp = FirefoxProfile()
    gtkmozembed.set_profile_path(oddir, 'widgets')
    shutil.copy(ffp.path_join('cookies.txt'), odwidgets)

    widget_environ = WidgetEnvironment()
    widget_environ['google_apps_auth_path'] = ''

    win = gtk.Window()
    vb = gtk.VBox()
    win.set_deletable(False)
    for url in sys.argv[1:]:
        widget = Widget(url, widget_environ) 
        vb.add(widget)
    #win.set_title(widget.get_title())
    win.add(vb)
    win.show_all()
    gtk.main()

if __name__ == '__main__':
    main()
