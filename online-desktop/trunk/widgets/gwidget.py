#!/usr/bin/python

import os,sys,re,urllib,urllib2,logging,webbrowser,tempfile,shutil
import cookielib
import xml.etree.ElementTree
from StringIO import StringIO

import gobject,gtk,gnomevfs,gtkmozembed

sys.path.insert(0, '.')
import mozembed_wrap

_logger = logging.getLogger("od.WidgetSystem")

def iterdir(path):
  for fname in os.listdir(path):
    yield os.path.join(path, fname)

class VfsMonitor(object):
  """Avoid some locking oddities in gnomevfs monitoring"""
  def __init__(self, path, montype, cb):
    self.__path = path
    self.__cb = cb
    self.__idle_id = 0
    self.__monid = gnomevfs.monitor_add(path, montype, self.__on_vfsmon)
  
  def __idle_emit(self):
    self.__idle_id = 0
    self.__cb()

  def __on_vfsmon(self, *args):
    if not self.__monid:
      return
    if self.__idle_id == 0:
      self.__idle_id = gobject.timeout_add(300, self.__idle_emit)

  def cancel(self):
    if self.__idle_id:
      gobject.source_remove(self.__idle_id)
      self.__idle_id = 0
    if self.__monid:
      gnomevfs.monitor_cancel(self.__monid)
      self.__monid = Nones

class FirefoxProfile(object):
  def __init__(self):
    self.__ffpath = os.path.expanduser('~/.mozilla/firefox')
    # todo integrate with monitoring from web-login-driver
    self.__profilepath = self.__get_profile_path()

  def __get_profile_path(self):
    if not os.path.isdir(self.__ffpath):
      os.makedirs(self.__ffpath)
    for p in iterdir(self.__ffpath):
      if not p.endswith('.default'): continue
      return p
    raise KeyError("Couldn't find mozilla profile")

  def path_join(self, *args):
    return os.path.join(self.__profilepath, *args)

class FirefoxHTTP(object):
  def __init__(self):
    self.__profile = FirefoxProfile()
    self.__cookies = cookielib.MozillaCookieJar(policy=cookielib.DefaultCookiePolicy(rfc2965=True))
    cookiepath = self.__profile.path_join('cookies.txt')
    _logger.debug("reading cookies from %s", cookiepath)
    self.__cookies.load(cookiepath)
    self.opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(self.__cookies))

  def open(self, url):
    req = urllib2.Request(url, headers={'User-Agent': 'GNOME Online Desktop Widget System 0.1',})
    return self.opener.open(req)

class WidgetEnvironment(dict):
  def replace_string(self, s):
    for k,v in self.iteritems():
      s = s.replace('__ENV_' + k + '__', v)
    return s

class WidgetError(Exception):
  def __init__(self, msg):
    super(WidgetError, self).__init__(msg)

class GoogleWidget(gtk.VBox):
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

    def __init__(self, url, env):
        super(GoogleWidget, self).__init__()
      
        f = gtk.Frame()
        self.__moz = mozembed_wrap.MozClient()
        _logger.debug("Reading module url %s", url)
        
        self.__doc = doc = xml.etree.ElementTree.ElementTree()
        doc.parse(urllib2.urlopen(url))
        module_prefs = doc.find('ModulePrefs')
        self.__title = module_prefs.attrib['title']
        self.__prefs = {}
        for prefnode in self.__doc.findall('UserPref'):
          try:
            self.__prefs[prefnode.attrib['name']] = (prefnode.attrib['default_value'],)
          except KeyError, e:
            _logger.debug("parse failed for pref", exc_info=True)

        content_node = doc.find('Content')
        self.__content_uri = None
        if content_node.attrib['type'] == 'html':
          content = content_node.text
          _logger.debug("got content of %d chars", len(content))
 
          if module_prefs.attrib.get('render_inline', '') == 'required':
            content = env.replace_string(content)
            content = content.replace('__MODULE_ID__', '0')
            htmlcontent = '''<html><head><title>Widget</title><script type="text/javascript">'''
            htmlcontent += self.__default_prefs_js()
            htmlcontent += self.IG_JS
            htmlcontent += '''</script></head><body>''' + content + '''</body></html>'''
            self.__moz.set_data("http://www.google.com/", htmlcontent)
          else:
            gmodule_url = 'http://gmodules.com/ig/ifr?url=' + urllib.quote(url)
            self.__content_uri = gmodule_url
            self.__moz.load_url(gmodule_url)
        elif content_node.attrib['type'] == 'url':
          href = content_node.attrib['href']
          href = env.replace_string(href)
          self.__content_uri = href
          _logger.debug("Reading content url %s", href)
          self.__moz.load_url(href)
        else:
          raise WidgetError("Unknown content type")
        self.__moz.connect("open-uri", self.__on_open_uri)
        f.add(self.__moz)
        self.pack_start(f, expand=True)
        self.__moz.show_all()
        height = module_prefs.attrib.get('height', '200')
        self.__moz.set_size_request(200, int(height))
   
    def __default_prefs_js(self):
      result = StringIO()
      result.write('''__IG_PREFS_DEFAULTS = {''')
      def js_quotestr(s):
        return '"' + s + '"'
      for k,v in self.__prefs.iteritems():
        result.write(k)
        result.write(': ')
        result.write(js_quotestr(v[0]))
        result.write(',\n')
      result.write('''}\n''')
      return result.getvalue()

    def __on_open_uri(self, m, uri):
      if uri == self.__content_uri:
        return False
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
  ffp = FirefoxProfile()
  gtkmozembed.set_profile_path(oddir, 'widgets')
  shutil.copy(ffp.path_join('cookies.txt'), odwidgets)

  widget_environ = WidgetEnvironment()
  widget_environ['google_apps_auth_path'] = ''

  win = gtk.Window()
  vb = gtk.VBox()
  win.set_deletable(False)
  for url in sys.argv[1:]:
    widget = GoogleWidget(url, widget_environ) 
    vb.add(widget)
  #win.set_title(widget.get_title())
  win.add(vb)
  win.show_all()
  gtk.main()

if __name__ == '__main__':
  main()
