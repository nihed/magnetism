import logging

import gmenu, gobject, pango, gnomedesktop
import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage

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

class AppDisplay(hippo.CanvasBox):
    def __init__(self, app):
        hippo.CanvasBox.__init__(self, 
                                 orientation=hippo.ORIENTATION_HORIZONTAL,
                                 spacing=4)
        self._app = None
                
        self._photo = CanvasMugshotURLImage(scale_width=30,
                                            scale_height=30)
        self._title = hippo.CanvasText()
        
        self.append(self._photo)
        self.append(self._title)
    
        self.connect("button-press-event", lambda self, event: self._on_button_press(event))
        
        self.set_app(app)
        
    def set_app(self, app):
        self._app = app
        self._app.connect("changed", lambda app: self._app_display_sync())
        self._app_display_sync()
    
    def _get_name(self):
        if self._app is None:
            return "unknown"
        return self._app.get_name()
    
    def __str__(self):
        return '<AppDisplay name="%s">' % (self._get_name())
    
    def _set_app_installed(self, installed):
        attrs = pango.AttrList()
        if not installed:
            attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
            logging.debug("app %s is not installed", self)
        self._title.set_property("attributes", attrs)
    
    def _app_display_sync(self):
        self._title.set_property("text", self._app.get_name())
        self._photo.set_url(self._app.get_icon_url())
        self._desktop_entry = None
        names = self._app.get_desktop_names()
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
            self._desktop_entry = desktop
            break
        self._set_app_installed(not self._desktop_entry is None)
        
    def _on_button_press(self, event):
        logging.debug("activated app %s", self)
        if self._desktop_entry is None:
            logging.error("couldn't find installed app %s, ignoring activate")
            return
        self._desktop_entry.launch(())

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
            
    def _handle_my_top_apps_changed(self, mugshot, apps):
        logging.debug("my apps changed")
        self._box.remove_all()
        for app in apps:
            display = AppDisplay(app)
            self._box.append(display)
