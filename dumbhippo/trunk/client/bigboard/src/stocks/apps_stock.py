import logging

import gmenu, gobject, pango, gnomedesktop
import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage, PhotoContentItem

class AppDirectory(gobject.GObject):
    def __init__(self):
        gobject.GObject.__init__(self)
        self._tree = gmenu.lookup_tree('applications.menu')
        self._apps = {} 
        self._tree.add_monitor(lambda tree: self._on_apps_changed)        
        self._on_apps_changed()
        
    def _append_directory(self, directory):
        for child in directory.contents:
            if isinstance(child, gmenu.Directory):
                self._append_directory(child)
                continue
            
            if not isinstance(child, gmenu.Entry):
                continue
            
            self._apps[child.desktop_file_id] = child
            
    def _on_apps_changed(self):
        logging.debug("installed apps changed")
        self._apps = {} 
        self._append_directory(self._tree.root)
        logging.debug("app read complete (%d apps)", len(self._apps.keys()))
        
    def lookup(self, desktop_name):
        if not (desktop_name[-8:] == '.desktop'):
            desktop_name += '.desktop'
        return self._apps[desktop_name]
    
_app_directory = None
def get_app_directory():
    global _app_directory
    if _app_directory is None:
        _app_directory = AppDirectory()
    return _app_directory

class AppDisplay(PhotoContentItem):
    def __init__(self, app):
        PhotoContentItem.__init__(self, border_right=6)
        self.__app = None
                
        self.__photo = CanvasMugshotURLImage(scale_width=30,
                                             scale_height=30)
        self.set_photo(self.__photo)
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=2, 
                                     border_right=4)
        self.__title = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__description = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        self.__description.set_property("attributes", attrs)        
        self.__box.append(self.__title)
        self.__box.append(self.__description)        
        self.set_child(self.__box)
    
        self.connect("button-press-event", lambda self, event: self.__on_button_press(event))
        
        self.set_app(app)
        
    def set_app(self, app):
        self.__app = app
        self.__app.connect("changed", lambda app: self.__app_display_sync())
        self.__app_display_sync()
    
    def __get_name(self):
        if self.__app is None:
            return "unknown"
        return self.__app.get_name()
    
    def __str__(self):
        return '<AppDisplay name="%s">' % (self.__get_name())
    
    def __set_app_installed(self, installed):
        attrs = pango.AttrList()
        if installed:
            attrs.insert(pango.AttrForeground(0x0, 0x0, 0xFFFF, 0, 0xFFFF))
        else:
            logging.debug("app %s is not installed", self)
        self.__title.set_property("attributes", attrs)
        
    # override
    def do_prelight(self):
        return not self.__desktop_entry is None
    
    def __app_display_sync(self):
        self.__title.set_property("text", self.__app.get_name())
        self.__description.set_property("text", self.__app.get_description())
        self.__photo.set_url(self.__app.get_icon_url())
        self.__desktop_entry = None
        names = self.__app.get_desktop_names()
        for name in names.split(';'):
            ad = get_app_directory()            
            menuitem = None
            try:
                menuitem = ad.lookup(name)
            except KeyError, e:
                continue
            entry_path = menuitem.get_desktop_file_path()
            logging.debug("loading desktop file %s", entry_path) 
            desktop = gnomedesktop.item_new_from_file(entry_path, 0)
            self.__desktop_entry = desktop
            break
        self.__set_app_installed(not self.__desktop_entry is None)
        
    def __on_button_press(self, event):
        logging.debug("activated app %s", self)
        if self.__desktop_entry is None:
            logging.error("couldn't find installed app %s, ignoring activate")
            return
        self.__desktop_entry.launch(())

class AppsStock(bigboard.AbstractMugshotStock):
    def __init__(self):
        super(AppsStock, self).__init__("Applications")
        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self._apps = {}

    def _on_mugshot_initialized(self):
        super(AppsStock, self)._on_mugshot_initialized()
        self._mugshot.connect("my-top-apps-changed", self._handle_my_top_apps_changed)
        self._mugshot.get_my_top_apps()

    def get_content(self, size):
        return self._box
            
    def _set_item_size(self, item, size):
        if size == bigboard.Stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        item.set_size(size)            
            
    def set_size(self, size):
        super(AppsStock, self).set_size(size)
        for child in self._box.get_children():
            self._set_item_size(child, size)        
            
    def _handle_my_top_apps_changed(self, mugshot, apps):
        logging.debug("my apps changed")
        self._box.remove_all()
        for app in apps:
            display = AppDisplay(app)
            self._box.append(display)
