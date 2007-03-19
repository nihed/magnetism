import logging, time

import gobject, gtk
import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, ActionLink, CanvasEntry, PrelightingCanvasBox

import apps_widgets

_logger = logging.getLogger("bigboard.AppBrowser")

class AppOverview(PrelightingCanvasBox):
    def __init__(self, app=None):
        super(AppOverview, self).__init__(box_width=200)
        
        self.__header = apps_widgets.AppDisplay()
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
        
    def launch(self):
        return self.__header.launch()
        
    def get_app(self):
        return self.__app
        
class MultiVTable(CanvasHBox):
    """Implements an multi-column aligned table.  May have inner section headings."""
    def __init__(self, columns=2, horiz_spacing=30, vert_spacing=5, item_height=None):
        super(MultiVTable, self).__init__()    
        
        if item_height is None:
            raise NotImplementedError("Must specify an item_height right now")
        
        self.__horiz_spacing = horiz_spacing
        self.__vert_spacing = vert_spacing
        self.__column_count = columns
        self.__item_height = item_height

    def __setup(self):
        if self.__column_count == len(self.get_children()):
            return
        for i in range(self.__column_count):
            self.append(CanvasVBox(spacing=self.__vert_spacing))
        
        self.__append_index = 0
        
    def append_section_head(self, text):
        self.__setup()
        # Fill out empty spaces as necessary
        if self.__append_index != 0:
            (x,y) = self.get_children()[self.__append_index-1].get_allocation()            
            for i,v in enumerate(self.get_children()[self.__append_index:]):
                empty = CanvasVBox(box_height=(y>0 and y or self.__item_height))
                v.append(empty)
                
        self.__append_index = 0
        
        for i,v in enumerate(self.get_children()):
            subbox = CanvasVBox(color=0x666666FF, border_bottom=1, border_color=0x666666FF)
            text_item = hippo.CanvasText(font="Bold 14px", xalign=hippo.ALIGNMENT_START)
            if i == 0:
                text_item.set_property("text", text)
            else:
                text_item.set_property("text", " ")
            subbox.append(text_item)
            v.append(subbox)
    
    def append_column_item(self, child):
        self.__setup()
        for i,v in enumerate(self.get_children()):
            if i == self.__append_index:
                kwargs = {'box_height': self.__item_height}
                if i != self.__column_count-1:
                    kwargs['padding_right'] = self.__horiz_spacing
                wrapper = CanvasVBox(**kwargs)
                wrapper.append(child)
                v.append(wrapper)
                break
        self.__append_index = (self.__append_index + 1) % self.__column_count
        
class AppList(MultiVTable):
    __gsignals__ = {
        "selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "launch" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())      
    }
    
    def __init__(self):
        super(AppList, self).__init__(columns=3,item_height=40)
        
        self.__mugshot = mugshot.get_mugshot()
        self.__mugshot.connect("initialized", self.__on_mugshot_initialized)
        self.__mugshot.connect("global-top-apps-changed", 
                               lambda mugshot, apps: self.__on_top_apps_changed(apps))          
        
        self.__search = None
        self.__all_apps = set()
        
    def __on_mugshot_initialized(self, mugshot):
        self.__on_top_apps_changed(self.__mugshot.get_global_top_apps())
        
    def __on_top_apps_changed(self, apps):
        if not apps:
            return
        
        _logger.debug("handling top apps changed")
        
        self.__all_apps = set(apps)
        self.__sync_display()
        
    def __sync_display(self):
        
        self.remove_all()
                
        categories = {} 
        
        # sort into categories
        for app in filter(self.__filter_app, self.__all_apps):
            cat = app.get_category()
            if not categories.has_key(cat):
                categories[cat] = set()
            categories[cat].add(app)
             
        cat_keys = categories.keys()
        cat_keys.sort()
        for catname in cat_keys:
            self.append_section_head(catname)
            for app in categories[catname]:
                overview = apps_widgets.AppDisplay(app)
                overview.connect("button-press-event", self.__on_overview_click)             
                self.append_column_item(overview)
                
    def __filter_app(self, app):
        if not self.__search:
            return True
        search = self.__search.lower()
        keys = (app.get_name(), app.get_description())
        for key in keys:
            if key.lower().find(search) >= 0:
                return True
        return False
                
    def set_search(self, search):
        self.__search = (search == "" and None) or search
        self.__sync_display()
             
    def __on_overview_click(self, overview, event):
         _logger.debug("pressed %s %d", overview, event.count)
         
         if event.count == 1:
             self.emit("selected", overview.get_app())
         else:
             self.emit("launch")
             
class AppBrowser(hippo.CanvasWindow):
    def __init__(self):
        super(AppBrowser, self).__init__(gtk.WINDOW_TOPLEVEL)
        
        self.set_keep_above(1)
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        
    
        self.__box = CanvasHBox()
    
        self.__left_box = CanvasVBox()
        self.__box.append(self.__left_box)
        
        self.__search_text = hippo.CanvasText(text="Search", font="Bold 12px")
        self.__left_box.append(self.__search_text)
        self.__search_input = CanvasEntry()
        self.__search_input.connect("notify::text", self.__on_search_changed)
        self.__idle_search_id = 0
        self.__left_box.append(self.__search_input)        
    
        self.__overview = AppOverview()
        self.__left_box.append(self.__overview)
        
        self.__right_box = CanvasVBox()
        self.__box.append(self.__right_box)
        
        self.__app_list = AppList()
        self.__right_box.append(self.__app_list)
        self.__app_list.connect("selected", lambda list, app: self.__on_app_selected(app))
        self.__app_list.connect("launch", lambda list: self.__on_app_launch()) 
        
        self.set_default_size(600, 400)
        self.connect("delete-event", gtk.Widget.hide_on_delete)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))
        
        self.set_root(self.__box)
        
    def __on_app_selected(self, app):
        self.__overview.set_app(app)
        
    def __on_app_launch(self):
        self.__overview.launch()
        self.hide()
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.hide()
            
    def __on_search_changed(self, input, text):
        if self.__idle_search_id > 0:
            return
        self.__idle_search_id = gobject.timeout_add(500, self.__idle_do_search)
        
    def __idle_do_search(self):
        self.__app_list.set_search(self.__search_input.get_property("text"))
        self.__idle_search_id = 0
