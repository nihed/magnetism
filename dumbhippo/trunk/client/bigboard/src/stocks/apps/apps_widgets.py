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
        
        if app:
            self.set_app(app)
        
    def get_app(self):
        return self.__app
        
    def set_app(self, app):
        self.__app = app
        self.__app.get_mugshot_app().connect("changed", lambda app: self.__app_display_sync())
        self.__app.connect("changed", lambda app: self.__app_display_sync())
        self.__app_display_sync()
    
    def __get_name(self):
        if self.__app is None:
            return "unknown"
        return self.__app.get_name()
    
    def __str__(self):
        return '<AppDisplay name="%s">' % (self.__get_name())
        
    # override
    def do_prelight(self):
        return self.__app.is_installed()
    
    def __app_display_sync(self):
        if not self.__app:
            return
        self.__photo.set_clickable(self.__app.is_installed())
        self.__box.set_clickable(self.__app.is_installed())  
        app = self.__app.get_mugshot_app()
        self.__title.set_property("text", app.get_name())
        self.__description.set_property("text", app.get_description())
        self.__photo.set_url(app.get_icon_url())
        
    def launch(self):
        self._logger.debug("launching app %s", self)
        self.__app.launch()
