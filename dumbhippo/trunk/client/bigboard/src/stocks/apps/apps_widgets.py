import logging, time

import gmenu, gobject, pango, gnomedesktop
import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasHBox, CanvasVBox, ActionLink

import apps_directory

class AppDisplay(PhotoContentItem):
    def __init__(self, app=None):
        PhotoContentItem.__init__(self, border_right=6)
        self.__app = None 
            
        self._logger = logging.getLogger('bigboard.AppDisplay')                
                
        self.__photo = CanvasMugshotURLImage(scale_width=30, scale_height=30)
        self.set_photo(self.__photo)
        self.__box = CanvasVBox(spacing=2, border_right=4)
        self.__title = ActionLink(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__description = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        self.__description.set_property("attributes", attrs)        
        self.__box.append(self.__title)
        self.__box.append(self.__description)        
        self.set_child(self.__box)
        
        self.__desktop_entry = None        
        
        if app:
            self.set_app(app)
        
    def get_app(self):
        return self.__app
        
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
        pass
#        attrs = pango.AttrList()
#        if installed:
#            attrs.insert(pango.AttrForeground(0x0, 0x0, 0xFFFF, 0, 0xFFFF))
#        else:
#            self._logger.debug("app %s is not installed", self)
#        self.__title.set_property("attributes", attrs)
        
    # override
    def do_prelight(self):
        return not self.__desktop_entry is None
    
    def __app_display_sync(self):
        self.__title.set_property("text", self.__app.get_name())
        self.__description.set_property("text", self.__app.get_description())
        self.__photo.set_url(self.__app.get_icon_url())
        names = self.__app.get_desktop_names()
        for name in names.split(';'):
            ad = apps_directory.get_app_directory()            
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
        
    def launch(self):
        self._logger.debug("launching app %s", self)
        if self.__desktop_entry is None:
            self._logger.error("couldn't find installed app %s, ignoring activate", self)
            return
        self.__desktop_entry.launch(())
