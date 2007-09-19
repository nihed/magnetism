import logging, os, subprocess, urlparse, urllib
import xml.dom, xml.dom.minidom

import gobject, gtk, pango
import gconf, gnomevfs
import gnome.ui
import dbus, dbus.glib
import hippo

import gdata.docs as gdocs
import bigboard.libbig as libbig
from bigboard.libbig.logutil import log_except
from bigboard.workboard import WorkBoard
from bigboard.stock import Stock
import bigboard.stocks.google_stock as google_stock  
from bigboard.big_widgets import PhotoContentItem, CanvasVBox, CanvasHBox, ActionLink, Separator
from bigboard.libbig.xmlquery import query as xml_query, get_attrs as xml_get_attrs

_logger = logging.getLogger('bigboard.stocks.FilesStock')

def reverse(data):
    for index in range(len(data)-1, -1, -1):
        yield data[index]

class IconLink(CanvasHBox):
    def __init__(self, text, **kwargs):
        kwargs['spacing'] = 4
        super(IconLink, self).__init__(**kwargs)
        self.img = hippo.CanvasImage(scale_width=20, scale_height=20)
        self.append(self.img)
        self.link = hippo.CanvasLink(text=text, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END,)
        self.append(self.link)

class FilesStock(Stock, google_stock.GoogleStock):
    """Shows recent files."""
    def __init__(self, *args, **kwargs):
        Stock.__init__(self, *args, **kwargs)
        google_stock.GoogleStock.__init__(self, *args, **kwargs)

        self.__display_limit = 3
        self.__thumbnails = gnome.ui.ThumbnailFactory(gnome.ui.THUMBNAIL_SIZE_NORMAL)
        self.__itheme = gtk.icon_theme_get_default() 
        self.__recentf_path = os.path.expanduser('~/.recently-used.xbel')        

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)
        self._recentbox = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)
        self._box.append(self._recentbox)

        self._add_more_button(self.__on_more_button)

        self.__monitor = gnomevfs.monitor_add('file://' + self.__recentf_path, gnomevfs.MONITOR_FILE, self.__redisplay)
        gobject.idle_add(self.__redisplay)
        
    def update_google_data(self, google_key = None):
        if google_key is not None:
            self._googles[google_key].fetch_documents(self.__on_documents_load, self.__on_failed_load)
        else:            
            for gobj in self._googles.values():
                gobj.fetch_documents(self.__on_documents_load, self.__on_failed_load)    

    def remove_google_data(self, google_key):
        pass

    def __on_documents_load(self, url, data, gobj):
        document_list = gdocs.DocumentListFeedFromString(data)
        for document_entry in document_list.entry:
            _logger.debug("document entry: %s", document_entry)

    def __on_failed_load(self, response):
        pass

    def __on_more_button(self):
        _logger.debug("more!")
        subprocess.Popen(['nautilus', '--browser', os.path.expanduser('~/Desktop')]) 
        
    def get_content(self, size):
        return self._box
        
    @log_except(logger=_logger)
    def __redisplay(self, *args):
        _logger.debug("doing redisplay")
        if not os.path.isfile(self.__recentf_path):
            _logger.debug("no recent files")
            self._recentbox.append(hippo.CanvasText(text="No recent files"))            
            return
        f = open(self.__recentf_path, 'r')
        doc = xml.dom.minidom.parseString(f.read())
        self._recentbox.remove_all()
        i = 0
        for child in reverse(xml_query(doc.documentElement, 'bookmark*')):         
            if i >= self.__display_limit: break
            attrs = xml_get_attrs(child, ['href'])
            url = attrs['href']
            _logger.debug("using recent url %s", url)
            try:            
                vfsstat = gnomevfs.get_file_info(url.encode('utf-8'), gnomevfs.FILE_INFO_GET_MIME_TYPE | gnomevfs.FILE_INFO_FOLLOW_LINKS)
            except gnomevfs.NotFoundError, e:
                _logger.debug("Failed to get file info for target of '%s'", url, exc_info=True)
                continue
            try:
                (result, flags) = gnome.ui.icon_lookup(self.__itheme, self.__thumbnails, url, file_info=vfsstat, mime_type=vfsstat.mime_type)
            except gnomevfs.NotFoundError, e:
                _logger.debug("Failed to get icon info for '%s'", url, exc_info=True)
                continue
                         
            text = urllib.unquote(os.path.basename(url))
            link = IconLink(text)
            link.img.set_property('image-name', result)
            link.link.connect("activated", self.__on_recentitem, url)
            self._recentbox.append(link)
            i += 1
            
    def __on_recentitem(self, canvas_item, url):
        subprocess.Popen(['gnome-open', url])
        
