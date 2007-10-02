import logging, os, subprocess, urlparse, urllib, time, threading
import xml.dom, xml.dom.minidom

import gobject, gtk, pango
import gconf, gnomevfs
import gnome.ui
import dbus, dbus.glib
import hippo

import gdata.docs as gdocs
import bigboard.libbig as libbig
from bigboard.libbig.logutil import log_except
from bigboard.libbig.gutil import *
from bigboard.workboard import WorkBoard
from bigboard.stock import Stock
import bigboard.google as google
import bigboard.google_stock as google_stock  
from bigboard.big_widgets import IconLink
from bigboard.libbig.xmlquery import query as xml_query, get_attrs as xml_get_attrs

import filebrowser

_logger = logging.getLogger('bigboard.stocks.FilesStock')

def reverse(data):
    for index in range(len(data)-1, -1, -1):
        yield data[index]

def on_link_clicked(canvas_item, url):
    subprocess.Popen(['gnome-open', url])

thumbnails = gnome.ui.ThumbnailFactory(gnome.ui.THUMBNAIL_SIZE_NORMAL)
itheme = gtk.icon_theme_get_default() 

class File(gobject.GObject):
    __gsignals__ = {
        "changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
    }    
    def __init__(self):
        super(File, self).__init__()
        self._is_valid = True
        self._url = None
        self._name = None 
        self._full_name = None
        self._image_name = None
        self._access_time = None
        self._source_key = None

    def is_valid(self):
        return self._is_valid

    def get_url(self):
        return self._url

    def get_name(self):
        return self._name

    def get_full_name(self):
        return self._full_name

    def get_image_name(self):
        return self._image_name

    def get_access_time(self):
        return self._access_time

    def get_source_key(self):
        return self._source_key

    def create_icon_link(self):
        link = IconLink(self.get_name())
        link.img.set_property('image-name', self.get_image_name())
        link.link.connect("activated", on_link_clicked, self.get_url())
        link.link.set_property("tooltip", self.get_full_name())
        return link   

class LocalFile(File):
    def __init__(self, bookmark_child):
        super(LocalFile, self).__init__()
        attrs = xml_get_attrs(bookmark_child, ['href', 'modified', 'visited'])
        self._url = attrs['href'].encode('utf-8')     
        # google.parse_timestamp() just parses an RFC 3339 format timestamp,
        # which 'modified' and 'visited' timestamps here use as well.
        # We'll need to move that function to some more generic file. 
        modified = google.parse_timestamp(attrs['modified'])
        visited = google.parse_timestamp(attrs['visited'])
        self._access_time = max(modified, visited)
        self._name = urllib.unquote(os.path.basename(self._url))
        self._full_name = self._url
        self._source_key = 'files'
        self._is_valid = True
        self.__update_async(self.__on_async_update)
        
    def __on_async_update(self, results):
        _logger.debug("got async results: %s", results)
        (vfsstat, image_name) = results
        if not vfsstat:
            self._is_valid = False
        self._image_name = image_name
        self.emit("changed")
        
    def __do_update_async(self, url, cb):
        results = (None, None)
        uri = gnomevfs.URI(url)
        try:            
            vfsstat = gnomevfs.get_file_info(uri, gnomevfs.FILE_INFO_GET_MIME_TYPE | gnomevfs.FILE_INFO_FOLLOW_LINKS)
        except gnomevfs.NotFoundError, e:
            _logger.debug("Failed to get file info for target of '%s'", url, exc_info=True)
            gobject.idle_add(cb, results)
            return
        try:
            (image_name, flags) = gnome.ui.icon_lookup(itheme, thumbnails, url, file_info=vfsstat, mime_type=vfsstat.mime_type)
        except gnomevfs.NotFoundError, e:
            _logger.debug("Failed to get icon info for '%s'", self._url, exc_info=True)
            gobject.idle_add(cb, results)
            return
        results = (vfsstat, image_name)
        gobject.idle_add(cb, results)
        
    def __update_async(self, cb):
        t = threading.Thread(target=self.__do_update_async, name="FileImageWorker", args=(self._url, cb))
        t.setDaemon(True)
        t.start()          

class GoogleFile(File):
    def __init__(self, google_key, google_name, doc_entry):
        super(GoogleFile, self).__init__()
        self._source_key = google_key
        self.__doc_entry = doc_entry
        self._access_time = google.parse_timestamp(self.__doc_entry.updated.text)

        if self.__doc_entry.category[0].label == "document":
            self._image_name = 'bigboard-document.png'
        elif self.__doc_entry.category[0].label == "spreadsheet":
            self._image_name = 'bigboard-spreadsheet.png'
        elif self.__doc_entry.category[0].label == "presentation":
            self._image_name = 'bigboard-presentation.png'
        else:
            self._image_name = 'document.png'
            _logger.warn("Unknown Google Docs category %s", self.__doc_entry.category.text)

        self._url = self.__doc_entry.GetAlternateLink().href
        self._name = self.__doc_entry.title.text
        self._full_name = self.__doc_entry.title.text + " from " + google_name + " Docs" 

    def get_doc_entry(self):
        return self.__doc_entry
 
def compare_by_date(file_a, file_b):
    # access time on all File types is currently UTC
    return cmp(file_b.get_access_time(), file_a.get_access_time())

class FilesStock(Stock, google_stock.GoogleStock):
    """Shows recent files."""
    def __init__(self, *args, **kwargs):
        Stock.__init__(self, *args, **kwargs)
        google_stock.GoogleStock.__init__(self, *args, **kwargs)

        # files in this list are either LocalFile or GoogleFile 
        self.__files = []
        self.__display_limit = 5
        self.__recentf_path = os.path.expanduser('~/.recently-used.xbel') 

        try:
            self.desktop_path = subprocess.Popen(['xdg-user-dir', 'DESKTOP'], stdout=subprocess.PIPE).communicate()[0].strip()    
        except OSError, e:
            self.desktop_path = os.path.expanduser("~/Desktop")

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)
        self._recentbox = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)
        self._box.append(self._recentbox)

        self.__file_browser = None
        self._add_more_button(self.__on_more_button)

        self.__monitor = gnomevfs.monitor_add('file://' + self.__recentf_path, gnomevfs.MONITOR_FILE, self.__update_local_files)
        gobject.idle_add(self.__update_local_files)
        
    def update_google_data(self, selected_gobj = None):
        if selected_gobj is not None:
            selected_gobj.fetch_documents(self.__on_documents_load, self.__on_failed_load)
        else:            
            for gobj in self.googles:
                gobj.fetch_documents(self.__on_documents_load, self.__on_failed_load)    

    def __remove_files_for_key(self, source_key):
        files_to_keep = []
        for a_file in self.__files:
            if a_file.get_source_key() != source_key:
                files_to_keep.append(a_file)
        self.__files = files_to_keep

    def remove_google_data(self, gobj):
        self.__remove_files_for_key(gobj)

    def __on_documents_load(self, url, data, gobj):
        document_list = gdocs.DocumentListFeedFromString(data)   
        self.__remove_files_for_key(gobj) 
        for document_entry in document_list.entry:
            google_file = GoogleFile(gobj, gobj.get_auth()[0], document_entry)
            self.__files.append(google_file)
        self.__files.sort(compare_by_date)
        self.__refresh_files() 

    def __on_failed_load(self, response):
        pass

    def __on_more_button(self):
        if self.__file_browser is None:
            self.__file_browser = filebrowser.FileBrowser(self)
        if self.__file_browser.get_property('is-active'):
            self.__file_browser.hide()
        else:
            # we don't refresh files in the file browser when it is hidden
            self.__file_browser.refresh_files()
            self.__file_browser.present()
        
    def get_content(self, size):
        return self._box
        
    @log_except(logger=_logger)
    def __update_local_files(self, *args):
        if not os.path.isfile(self.__recentf_path):
            _logger.debug("no recent files")
            self._recentbox.append(hippo.CanvasText(text="No recent files"))            
            return
        f = open(self.__recentf_path, 'r')
        doc = xml.dom.minidom.parse(f)

        self.__remove_files_for_key('files') 
        # we sort the list of files after we add them, so reversing doesn't
        # really matter anymore
        for child in reverse(xml_query(doc.documentElement, 'bookmark*')):         
            local_file = LocalFile(child)
            local_file.connect("changed", self.__on_local_file_changed)
            self.__files.append(local_file)
        self.__files.sort(compare_by_date)
        self.__refresh_files()
        
    def __on_local_file_changed(self, f):
        call_timeout_once(500, self.__refresh_files)
  
    def __refresh_files(self):
        self._recentbox.remove_all() 
        i = 0
        for a_file in self.__files:         
            if i >= self.__display_limit: break
            if a_file.is_valid():                          
                link = a_file.create_icon_link()
                self._recentbox.append(link)
                i += 1 

        if self.__file_browser is not None and self.__file_browser.get_property('visible'):
            _logger.debug("will refresh files")
            self.__file_browser.refresh_files()
        
    def get_files(self):
        return self.__files

