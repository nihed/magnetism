import logging, time

import gmenu, gobject, pango, gnomedesktop
import hippo
import bigboard.globals as globals

from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasHBox, CanvasVBox, ActionLink

import apps_directory

class AppLocation:   
    (STOCK, APP_BROWSER, DESCRIPTION_HEADER) = range(3)

class AppDisplay(PhotoContentItem):
    __gsignals__ = {
        "title-clicked" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
    }
    def __init__(self, app_location, app=None, **kwargs):
        PhotoContentItem.__init__(self, border_right=6, **kwargs)
        self.__app = None 
 
        self.__description_mode = False
            
        self._logger = logging.getLogger('bigboard.AppDisplay')                
                
        self.__photo = CanvasMugshotURLImage(scale_width=30, scale_height=30)
        self.set_photo(self.__photo)
        self.__box = CanvasVBox(spacing=2, border_right=4)
        sub_kwargs = {}
        if kwargs.has_key('color'): 
            sub_kwargs['color'] = kwargs['color']

        self.__title = ActionLink(font="14px",xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END, **sub_kwargs)
        self.__title.connect("activated", lambda t: self.emit("title-clicked"))
        self.__subtitle = hippo.CanvasText(font="10px",xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)

        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        self.__subtitle.set_property("attributes", attrs)        
        self.__box.append(self.__title)
        self.__box.append(self.__subtitle)        
        self.set_child(self.__box)

        self.__description = hippo.CanvasText(size_mode=hippo.CANVAS_SIZE_WRAP_WORD, **sub_kwargs)
        self.__box.append(self.__description)
        
        self.__photo.set_clickable(True)
        self.__box.set_clickable(True)
        self.__app_location = app_location         
        if app:
            self.set_app(app)

    def set_description_mode(self, mode):
        self.__description_mode = mode
        self.__app_display_sync()
        
    def get_app(self):
        return self.__app
        
    def set_app(self, app):
        self.__app = app
        self.__app_display_sync()
    
    def __get_name(self):
        if self.__app is None:
            return "unknown"
        return self.__app.get_name()
    
    def __str__(self):
        return '<AppDisplay name="%s">' % (self.__get_name())
    
    def __app_display_sync(self):
        if not self.__app:
            return

        self.__box.set_child_visible(self.__subtitle, not self.__description_mode)
        self.__box.set_child_visible(self.__description, self.__description_mode)
 
        self.__title.set_property("text", self.__app.get_name())
        if self.__app.is_installed() or self.__app_location == AppLocation.DESCRIPTION_HEADER:
            self.__subtitle.set_property("text", self.__app.get_generic_name() or self.__app.get_tooltip() or self.__app.get_comment())
        ## for now, install won't work if not connected
        elif self.__app_location == AppLocation.STOCK and globals.get_data_model().ready and globals.get_data_model().global_resource.online:
            self.__subtitle.set_property('text', "(Click to Install)")
        else:
            self.__subtitle.set_property('text', "(Not Installed)")
 
        self.__description.set_property("text", self.__app.get_description())

        if self.__app.get_icon_url():
            self.__photo.set_url(self.__app.get_icon_url())
        else:
            pixbuf = self.__app.get_local_pixbuf()
            if pixbuf:
                self.__photo.set_property("image", hippo.cairo_surface_from_gdk_pixbuf(pixbuf))
        
    def launch(self):
        self._logger.debug("launching app %s", self)
        self.__app.launch()

