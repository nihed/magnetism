import logging, os, subprocess, urlparse, urllib
import xml.dom, xml.dom.minidom

import gobject, gtk, pango
import gconf, gnomevfs
import gnome.ui
import dbus, dbus.glib
import hippo

import bigboard.libbig as libbig
from bigboard.libbig.logutil import log_except
from bigboard.workboard import WorkBoard
from bigboard.stock import Stock, AbstractMugshotStock
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasVBox, CanvasHBox, ActionLink, Separator
from bigboard.libbig.xmlquery import query as xml_query, get_attrs as xml_get_attrs

_logger = logging.getLogger('bigboard.stocks.FilesStock')

class FilesStock(Stock):
    """Shows recent files."""
    def __init__(self, *args, **kwargs):
        super(FilesStock,self).__init__(*args, **kwargs)

        self.__display_limit = 3

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)

        gobject.idle_add(self.__redisplay)
        
    def get_content(self, size):
        return self._box        
        
    @log_except(logger=_logger)
    def __redisplay(self):
        recentf_path = os.path.expanduser('~/.recently-used')
        if not os.path.isfile(recentf_path):
            _logger.debug("no recent files")
            return
        f = open(recentf_path, 'r')
        doc = xml.dom.minidom.parseString(f.read())
        self._box.remove_all()
        for i,child in enumerate(xml_query(doc.documentElement, 'RecentItem*')):         
            if i >= self.__display_limit: break
            url = xml_query(child, 'URI#')
            _logger.debug("using recent url %s", url)            
            bn = os.path.basename(url)
            link = hippo.CanvasLink(text=urllib.unquote(bn))
            link.connect("activated", lambda l: self.__on_recentitem(url))
            self._box.append(link)
        
    def __on_recentitem(self, url):
        subprocess.Popen(['gnome-open', url])
        