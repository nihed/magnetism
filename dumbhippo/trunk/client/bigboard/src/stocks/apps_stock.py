import logging, time

import gmenu, gobject, pango, gnomedesktop
import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasHBox, CanvasVBox

class AppDirectory(gobject.GObject):
    def __init__(self):
        gobject.GObject.__init__(self)
        self._logger = logging.getLogger('bigboard.AppsDirectory')
        self._tree = gmenu.lookup_tree('applications.menu', gmenu.FLAGS_INCLUDE_EXCLUDED)
        self._apps = {}
        # with gnome-menus-2.16.0-2.fc6 omitting the user_data arg crashes the gmenu module
        self._tree.add_monitor(lambda tree: self._on_apps_changed, None)
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
        self._logger.debug("installed apps changed")
        self._apps = {} 
        self._append_directory(self._tree.root)
        self._logger.debug("app read complete (%d apps)", len(self._apps.keys()))
        
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

class HideableBox(hippo.CanvasBox):
    def __init__(self, hidetext, showtext, **kwargs):
        if not kwargs.has_key('spacing'):
            kwargs['spacing'] = 4
        hippo.CanvasBox.__init__(self, **kwargs)
        
        self.__shown = True
        self.__hidetext = hidetext
        self.__showtext = showtext
        self.__text = hippo.CanvasLink(xalign=hippo.ALIGNMENT_CENTER)
        self.__text.connect("button-press-event", lambda text, event: self.__toggle_show())
        
        self.append(self.__text)
        
        self.__content = None
        
        self.__sync_show()
        
    def set_content(self, content):
        assert(self.__content is None)
        self.__content = content
        self.append(self.__content)
        
    def set_shown(self, shown):
        shown = not not shown
        if shown != self.__shown:
            self.__toggle_show()
        
    def __toggle_show(self):
        assert(not self.__content is None)        
        self.__shown = not self.__shown
        self.__sync_show()
        
    def __sync_show(self):
        self.__text.set_property("text", (self.__shown and self.__hidetext) or self.__showtext)
        if self.__content:
            self.set_child_visible(self.__content, self.__shown)

class AppDisplay(PhotoContentItem):
    def __init__(self, app):
        PhotoContentItem.__init__(self, border_right=6)
        self.__app = None 
            
        self._logger = logging.getLogger('bigboard.AppDisplay')                
                
        self.__photo = CanvasMugshotURLImage(scale_width=30, scale_height=30)
        self.set_photo(self.__photo)
        self.__box = CanvasVBox(spacing=2, border_right=4)
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
            self._logger.debug("app %s is not installed", self)
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
            self._logger.debug("loading desktop file %s", entry_path) 
            desktop = gnomedesktop.item_new_from_file(entry_path, 0)
            self.__desktop_entry = desktop
            break
        self.__set_app_installed(not self.__desktop_entry is None)
        
    def __on_button_press(self, event):
        self._logger.debug("activated app %s", self)
        if self.__desktop_entry is None:
            self._logger.error("couldn't find installed app %s, ignoring activate", self)
            return
        self.__desktop_entry.launch(())

class AppsStock(bigboard.AbstractMugshotStock):
    STATIC_SET_SIZE = 5    
    STATIFICATION_TIME_SEC = 60 * 60 * 24 * 3; # 3 days
    def __init__(self):
        super(AppsStock, self).__init__("Applications") 
                
        self.__initialized = False           
        
        self.__box = CanvasVBox(spacing=3)
        self.__message = hippo.CanvasText()
        self.__static_set = CanvasVBox()
        self.__dynamic_set = CanvasVBox()
        self.__dynamic_set_container = HideableBox('Hide Recent', 'Show Recent')
        
        self.__dynamic_set_container.set_content(self.__dynamic_set)
        
        self.__box.append(self.__message)
        self.__box.append(self.__static_set)
        self.__box.append(self.__dynamic_set_container)
        
        self.__static_set_ids = {}
        self.__set_message('Loading...')
        
    def __set_message(self, text):
        self.__message.set_property("text", text)

    def _on_mugshot_initialized(self):
        super(AppsStock, self)._on_mugshot_initialized()
        self._mugshot.connect("my-top-apps-changed", self.__handle_my_top_apps_changed)
        self._mugshot.connect("global-top-apps-changed", self.__handle_global_top_apps_changed)        
        self._mugshot.connect("pinned-apps-changed", self.__handle_pinned_apps_changed)
        self._mugshot.get_pinned_apps()        
        self._mugshot.get_my_top_apps()

    def get_content(self, size):
        return self.__box
            
    def __set_item_size(self, item, size):
        if size == bigboard.Stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        item.set_size(size)            
            
    def set_size(self, size):
        super(AppsStock, self).set_size(size)
        for child in self.__static_set.get_children() + self.__dynamic_set.get_children():
            self.__set_item_size(child, size)        
            
    def __handle_pinned_apps_changed(self, mugshot, apps):
        self._logger.debug("pinned apps changed")
        self.__static_set.remove_all()
        self.__static_set_ids = {}
        for app in apps:
            display = AppDisplay(app)
            self.__static_set.append(display)           
            self.__static_set_ids[app.get_id()] = True
            
        if not self.__initialized:
            self.__initialized = True
            if len(self.__static_set.get_children()) > 0:
                self.__dynamic_set_container.set_shown(False)
            
    def __handle_global_top_apps_changed(self, mugshot, apps):
        self._logger.debug("global apps changed")
            
    def __handle_my_top_apps_changed(self, mugshot, apps):
        self._logger.debug("my apps changed")
        
        if self._mugshot.get_my_app_usage_start():
            app_stalking_duration = (time.mktime(time.gmtime())) - (int(self._mugshot.get_my_app_usage_start())/1000) 
            self._logger.debug("app stalking duration: %s", app_stalking_duration)
            if app_stalking_duration <= self.STATIFICATION_TIME_SEC:
                self.__set_message("Building application list...")
            else:
                self.__set_message("")                
            if len(self.__static_set.get_children()) == 0 and \
               app_stalking_duration > self.STATIFICATION_TIME_SEC:
                # We don't have a static set yet, time to make it
                pinned_ids = []
                for app in apps:
                    if len(pinned_ids) >= self.STATIC_SET_SIZE:
                        break
                    pinned_ids.append(app.get_id())
                self._logger.debug("creating initial pin set: %s", pinned_ids)
                self._mugshot.set_pinned_apps(pinned_ids)
        
        self.__dynamic_set.remove_all()
        for app in apps:
            if self.__static_set_ids.has_key(app.get_id()):
                continue
            display = AppDisplay(app)
            self.__dynamic_set.append(display)
