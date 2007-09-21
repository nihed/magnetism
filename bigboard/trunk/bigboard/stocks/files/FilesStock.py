import logging, os, subprocess, urlparse, urllib, time
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
import bigboard.google as google
import bigboard.google_stock as google_stock  
from bigboard.big_widgets import PhotoContentItem, CanvasVBox, CanvasHBox, ActionLink, Separator
from bigboard.libbig.xmlquery import query as xml_query, get_attrs as xml_get_attrs

_logger = logging.getLogger('bigboard.stocks.FilesStock')

def reverse(data):
    for index in range(len(data)-1, -1, -1):
        yield data[index]

thumbnails = gnome.ui.ThumbnailFactory(gnome.ui.THUMBNAIL_SIZE_NORMAL)
itheme = gtk.icon_theme_get_default() 
local_file_source_key = -1

def create_account_url(account):
    account = urllib.unquote(account)
    domain = account[account.find("@") + 1:]
    if domain == "gmail.com":
        return "http://docs.google.com"
    else:
        return "https://docs.google.com/a/" + domain

class File:
    def __init__(self):
        self._is_valid = True
        self._url = None
        self._name = None 
        self._image_name = None
        self._access_time = None
        self._source_key = None

    def is_valid(self):
        return self._is_valid

    def get_url(self):
        return self._url

    def get_name(self):
        return self._name

    def get_image_name(self):
        return self._image_name

    def get_access_time(self):
        return self._access_time

    def get_source_key(self):
        return self._source_key

class LocalFile(File):
    def __init__(self, bookmark_child):
        File.__init__(self)
        attrs = xml_get_attrs(bookmark_child, ['href', 'modified', 'visited'])
        self._url = attrs['href']
        # google.parse_timestamp() just parses an RFC 3339 format timestamp,
        # which 'modified' and 'visited' timestamps here use as well.
        # We'll need to move that function to some more generic file. 
        modified = google.parse_timestamp(attrs['modified'])
        visited = google.parse_timestamp(attrs['visited'])
        self._access_time = max(modified, visited)        
        try:            
            vfsstat = gnomevfs.get_file_info(self._url.encode('utf-8'), gnomevfs.FILE_INFO_GET_MIME_TYPE | gnomevfs.FILE_INFO_FOLLOW_LINKS)
        except gnomevfs.NotFoundError, e:
            _logger.debug("Failed to get file info for target of '%s'", self._url, exc_info=True)
            self._is_valid = False
            return
        try:
            (self._image_name, flags) = gnome.ui.icon_lookup(itheme, thumbnails, self._url, file_info=vfsstat, mime_type=vfsstat.mime_type)
        except gnomevfs.NotFoundError, e:
            _logger.debug("Failed to get icon info for '%s'", self._url, exc_info=True)
            self._is_valid = False
            return
        self._name = urllib.unquote(os.path.basename(self._url))
        self._source_key = local_file_source_key

class GoogleFile(File):
    def __init__(self, google_key, doc_entry):
        File.__init__(self)
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

    def get_doc_entry(self):
        return self.__doc_entry

 
def compare_by_date(file_a, file_b):
    # access time on all File types is currently UTC
    return cmp(file_b.get_access_time(), file_a.get_access_time())

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

        # files in this list are either LocalFile or GoogleFile 
        self.__files = []
        self.__display_limit = 5
        self.__recentf_path = os.path.expanduser('~/.recently-used.xbel') 

        try:
            self.__desktop_path = subprocess.Popen(['xdg-user-dir', 'DESKTOP'], stdout=subprocess.PIPE).communicate()[0].strip()    
        except OSError, e:
            self.__desktop_path = os.path.expanduser("~/Desktop")

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)
        self._recentbox = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)
        self._box.append(self._recentbox)

        self._add_more_button(self.__on_more_button)

        self.__monitor = gnomevfs.monitor_add('file://' + self.__recentf_path, gnomevfs.MONITOR_FILE, self.__update_local_files)
        gobject.idle_add(self.__update_local_files)
        
    def update_google_data(self, google_key = None):
        if google_key is not None:
            self._googles[google_key].fetch_documents(self.__on_documents_load, self.__on_failed_load)
        else:            
            for gobj in self._googles.values():
                gobj.fetch_documents(self.__on_documents_load, self.__on_failed_load)    

    def __remove_files_for_key(self, source_key):
        files_to_keep = []
        for a_file in self.__files:
            if a_file.get_source_key() != source_key:
                files_to_keep.append(a_file)
        self.__files = files_to_keep

    def remove_google_data(self, google_key):
        self.__remove_files_for_key(google_key)

    def __on_documents_load(self, url, data, gobj):
        document_list = gdocs.DocumentListFeedFromString(data)   
        google_key = self.get_google_key(gobj)
        self.__remove_files_for_key(google_key) 
        for document_entry in document_list.entry:
            google_file = GoogleFile(google_key, document_entry)
            self.__files.append(google_file)
        self.__files.sort(compare_by_date)
        self.__refresh_files() 

    def __on_failed_load(self, response):
        pass

    def __on_more_button(self):
        subprocess.Popen(['nautilus', '--browser', self.__desktop_path]) 
        done_with_sleep_state = 0
        for google_account in self._googles.itervalues():
            if done_with_sleep_state == 1:
                # in case the browser is just starting, we should wait a bit, otherwise
                # Firefox produces this for the second link:  
                # "Firefox is already running, but is not responding. To open a new window, 
                #  you must first close the existing Firefox process, or restart your system."
                time.sleep(2)
                done_with_sleep_state = 2  
            libbig.show_url(create_account_url(google_account.get_auth()[0]))
            if done_with_sleep_state == 0:
                done_with_sleep_state = 1
        
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

        self.__remove_files_for_key(local_file_source_key) 
        # we sort the list of files after we add them, so reversing doesn't
        # really matter anymore
        for child in reverse(xml_query(doc.documentElement, 'bookmark*')):         
            local_file = LocalFile(child) 
            self.__files.append(local_file)
        self.__files.sort(compare_by_date)
        self.__refresh_files()

    def __refresh_files(self):
        self._recentbox.remove_all()
        i = 0
        for a_file in self.__files:         
            if i >= self.__display_limit: break
            if a_file.is_valid():                          
                link = IconLink(a_file.get_name())
                link.img.set_property('image-name', a_file.get_image_name())
                link.link.connect("activated", self.__on_item_clicked, a_file.get_url())
                self._recentbox.append(link)
                i += 1 

    def __on_item_clicked(self, canvas_item, url):
        subprocess.Popen(['gnome-open', url])
        
