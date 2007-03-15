import logging, time

import gobject, gtk
import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, ActionLink, CanvasEntry, PrelightingCanvasBox

_logger = logging.getLogger("bigboard.AppBrowser")

class AppHeader(CanvasHBox):
    def __init__(self, app=None):
        super(AppHeader, self).__init__()

        self.__icon = CanvasMugshotURLImage(scale_width=30, scale_height=30)
        self.append(self.__icon)
        
        self.__header_text_items_box = CanvasVBox()
        self.__name = hippo.CanvasText(text="App", font="Bold 12px")
        self.__header_text_items_box.append(self.__name)
        self.__category = ActionLink(text="Cat")
        self.__header_text_items_box.append(self.__category)        
        self.__subcategory = hippo.CanvasText(text="Kitty") 
        self.__header_text_items_box.append(self.__subcategory)
        self.append(self.__header_text_items_box)
        
        if app:
            self.set_app(app)
        
    def set_app(self, app):
        self.__app = app
        self.__icon.set_url(app.get_icon_url())
        self.__name.set_property("text", app.get_name())
        
    def get_app(self):
        return self.__app

class AppOverview(PrelightingCanvasBox):
    def __init__(self, app=None):
        super(AppOverview, self).__init__()
        
        self.__header = AppHeader()
        self.append(self.__header)
        
        self.__description = hippo.CanvasText(size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END,
                                              text="""Lorem ipsum dolor sit amet, consectetuer adipiscing elit.""")
        self.append(self.__description)
        
        self.__updated = hippo.CanvasText(text="Last updated")
        self.append(self.__updated)
        self.__homelink = ActionLink(text="Developer's home page")
        self.append(self.__homelink)
        self.__bugreport = ActionLink(text="Submit a bug report")
        self.append(self.__bugreport)
        
        if app:
            self.set_app(app)

    def set_app(self, app):
        self.__app = app
        self.__header.set_app(app)
        self.__description.set_property("text", app.get_description())
        
    def get_app(self):
        return self.__app
        
class AppList(CanvasHBox):
    __gsignals__ = {
        "selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
    }
    
    def __init__(self):
        super(AppList, self).__init__()
        
        self.__mugshot = mugshot.get_mugshot()
        self.__mugshot.connect("initialized", self.__on_mugshot_initialized)
        self.__mugshot.connect("global-top-apps-changed", 
                               lambda mugshot, apps: self.__on_top_apps_changed(apps))          
        
        self.__column_one = CanvasVBox(padding_right=30)
        self.append(self.__column_one)
        self.__column_two = CanvasVBox()
        self.append(self.__column_two)
        
    def __on_mugshot_initialized(self, mugshot):
        self.__on_top_apps_changed(self.__mugshot.get_global_top_apps())
        
    def __on_top_apps_changed(self, apps):
        if not apps:
            return
        
        _logger.debug("handling top apps changed")
        
        self.__column_one.remove_all()
        self.__column_two.remove_all()
        
        idx = 0
        box = None
        for app in apps:
             overview = AppHeader(app)
             overview.connect("button-press-event", self.__on_overview_click)
             if idx == 0:
                 box = self.__column_one
             elif idx == 1:
                 box = self.__column_two
             idx = (idx+1) % 2
             box.append(overview)
             
    def __on_overview_click(self, overview, event):
         _logger.debug("pressed %s", overview)
         
         self.emit("selected", overview.get_app())
             
class AppBrowser(hippo.CanvasWindow):
    def __init__(self):
        super(AppBrowser, self).__init__(gtk.WINDOW_TOPLEVEL)
        
        self.set_resizable(False)
        self.set_keep_above(1)
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        
    
        self.__box = CanvasHBox()
    
        self.__left_box = CanvasVBox()
        self.__box.append(self.__left_box)
        
        self.__search_text = hippo.CanvasText(text="Search", font="Bold 12px")
        self.__left_box.append(self.__search_text)
        self.__search_input = CanvasEntry()
        self.__left_box.append(self.__search_input)        
    
        self.__overview = AppOverview()
        self.__left_box.append(self.__overview)
        
        self.__right_box = CanvasVBox()
        self.__box.append(self.__right_box)
        
        self.__app_list = AppList()
        self.__right_box.append(self.__app_list)
        self.__app_list.connect("selected", lambda list, app: self.__on_app_selected(app))
        
        self.connect("delete-event", gtk.Widget.hide_on_delete)
        
        self.set_root(self.__box)
        
    def __on_app_selected(self, app):
        self.__overview.set_app(app)
