#!/usr/bin/python

import os,sys,re,urllib,urllib2,logging,webbrowser,tempfile,shutil
import cookielib
import xml.etree.ElementTree
from StringIO import StringIO

import gobject,gtk,gnomevfs,gtkmozembed

import pyonlinedesktop.mozembed_wrap as mozembed_wrap
from pyonlinedesktop.widget import *

_logger = logging.getLogger("od.GoogleGadget")
  
class Gadget(gtk.VBox):
    def __init__(self, metadata, env):
        super(Gadget, self).__init__()
      
        f = gtk.Frame()
        self.__moz = mozembed_wrap.MozClient()
        self.__metadata = metadata
        (content_type, content) = metadata.content
        if content_type == 'html':
            _logger.debug("using content HTML: %s", content)
            self.__moz.set_data("http://www.google.com/", content)
        elif content_type == 'url':
            _logger.debug("using content url: %s", content)            
            self.__moz.load_url(content)
        else:
            pass
            
        self.__moz.connect("open-uri", self.__on_open_uri)
        self.__moz.connect("location", self.__on_location)   
        self.__moz.connect("new-window", self.__on_new_window)
        f.add(self.__moz)
        self.pack_start(f, expand=True)
        self.__moz.show_all()
        self.__moz.set_size_request(200, int(metadata.height))

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
        if self.__metadata.content[0] == 'url' and self.__metadata.content[1] == uri:
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
        metadata = WidgetParser(url, urllib2.urlopen(url), widget_environ)
        widget = Gadget(metadata, widget_environ) 
        vb.add(widget)
    #win.set_title(widget.get_title())
    win.add(vb)
    win.show_all()
    gtk.main()

if __name__ == '__main__':
    main()
