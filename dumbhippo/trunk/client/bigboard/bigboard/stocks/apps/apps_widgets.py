import logging, time

import gmenu, gobject, pango, gnomedesktop
import hippo

import bigboard.mugshot as mugshot
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasHBox, CanvasVBox, ActionLink

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
        self.__subtitle = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        self.__subtitle.set_property("attributes", attrs)        
        self.__box.append(self.__title)
        self.__box.append(self.__subtitle)        
        self.set_child(self.__box)
        
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
        
    # override
    def do_prelight(self):
        return self.__app.is_installed()
    
    def __app_display_sync(self):
        if not self.__app:
            return
        self.__photo.set_clickable(self.__app.is_installed())
        self.__box.set_clickable(self.__app.is_installed())  
        self.__title.set_property("text", self.__app.get_name())
        self.__subtitle.set_property("text", self.__app.get_generic_name() or self.__app.get_tooltip())
        if self.__app.get_mugshot_app():
            self.__photo.set_url(self.__app.get_mugshot_app().get_icon_url())
        else:
            pixbuf = self.__app.get_local_pixbuf()
            if pixbuf:
                self.__photo.set_property("image", hippo.cairo_surface_from_gdk_pixbuf(pixbuf))
        
    def launch(self):
        self._logger.debug("launching app %s", self)
        self.__app.launch()
