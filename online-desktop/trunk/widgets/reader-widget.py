#!/usr/bin/python

import os,sys,re,urllib2,logging,webbrowser
import cookielib
import xml.etree.ElementTree

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
    self.__cookies = cookielib.MozillaCookieJar()
    cookiepath = self.__profile.path_join('cookies.txt')
    _logger.debug("reading cookies from %s", cookiepath)
    self.__cookies.load(cookiepath)
    self.opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(self.__cookies))

  def open(self, url):
    req = urllib2.Request(url, headers={'User-Agent': 'GNOME Online Desktop Widget System 0.1'})
    return self.opener.open(req)

class WidgetError(Exception):
  def __init__(self, msg):
    super(WidgetError, self).__init__(msg)

class GoogleWidget(gtk.VBox):
    def __init__(self, url):
        super(GoogleWidget, self).__init__()
      
        f = gtk.Frame()
        self.__moz = mozembed_wrap.MozClient()
        self.__moz.connect("open-uri", lambda m, uri: webbrowser.open(uri))
        doc = xml.etree.ElementTree.ElementTree()
        _logger.debug("Reading module url %s", url)
        doc.parse(urllib2.urlopen(url))
        self.__title = doc.find('ModulePrefs').attrib['title']
        content_node = doc.find('Content')
        if content_node.attrib['type'] == 'html':
          content = content_node.text
        elif content_node.attrib['type'] == 'url':
          href = content_node.attrib['href']
          ffhttp = FirefoxHTTP()
          _logger.debug("Reading content url %s", href)
          content = ffhttp.open(href).read()
        else:
          raise WidgetError("Unknown content type")
        _logger.debug("setting content to %d chars", len(content))
        self.__moz.set_data("http://www.google.com/", content)
        f.add(self.__moz)
        self.pack_start(f, expand=True)
        self.__moz.show_all()
        self.__moz.set_size_request(480, 640)

    def get_title(self):
      return self.__title

def main():
  logging.basicConfig(level=logging.DEBUG)
  gtkmozembed.set_profile_path('/home/walters/tmp', 'widgets')
  win = gtk.Window()
  widget = GoogleWidget(sys.argv[1]) 
  win.add(widget)
  win.set_title(widget.get_title())
  win.show_all()
  gtk.main()

if __name__ == '__main__':
  main()
