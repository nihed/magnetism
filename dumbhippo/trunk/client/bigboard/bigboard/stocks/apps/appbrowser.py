import logging, time, urlparse, urllib

import gobject, gtk
import hippo

import bigboard.mugshot as mugshot
import bigboard.libbig as libbig
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, \
             ActionLink, PrelightingCanvasBox

import apps_widgets, apps_directory

_logger = logging.getLogger("bigboard.AppBrowser")

class AppOverview(CanvasVBox):
    __gsignals__ = {
        "more-info" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
    }

    def __init__(self, app=None):
        super(AppOverview, self).__init__(box_width=200, box_height=160,
                                          border=1, border_color=0x666666FF, padding=2)

        self.__unselected = True
        self.__app_unselected_text = hippo.CanvasText(text="Click an application to see its description here.\nDouble-click to launch.",
                                                      size_mode=hippo.CANVAS_SIZE_WRAP_WORD,
                                                      xalign=hippo.ALIGNMENT_CENTER,
                                                      yalign=hippo.ALIGNMENT_CENTER)
        self.append(self.__app_unselected_text, hippo.PACK_CLEAR_RIGHT)
        
        self.__header = apps_widgets.AppDisplay()
        
        self.__description = hippo.CanvasText(size_mode=hippo.CANVAS_SIZE_WRAP_WORD)
        
        self.__moreinfo = ActionLink(text="More Info", xalign=hippo.ALIGNMENT_START)
        self.__moreinfo.connect("button-press-event", lambda l,e: self.emit("more-info", self.__app))
        
        if app:
            self.set_app(app)

    def set_app(self, app):
        if self.__unselected:
            self.__unselected = False
            self.remove(self.__app_unselected_text)
            self.append(self.__header)
            self.append(self.__description, hippo.PACK_CLEAR_RIGHT)     
            self.append(self.__moreinfo)
        self.__app = app
        self.__header.set_app(app)
        self.__description.set_property("text", app.get_description())
        
    def launch(self):
        return self.__header.launch()
        
    def get_app(self):
        return self.__app

def categorize(apps):    
    """Given a set of applications, returns a map <string,set<Application>> based on category name."""
    categories = {}
    for app in apps:
        cat = app.get_category()
        if not categories.has_key(cat):
            categories[cat] = set()
        categories[cat].add(app)  
    return categories
        
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
        
    def append_section_head(self, text, left_control=None, right_control=None):
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
            if i == 0:
                target_box = subbox
                if left_control:
                    target_box = CanvasHBox()
                    target_box.append(left_control)
                    left_control.set_property("padding-right", 8)
                item = hippo.CanvasText(text=text, font="Bold 14px", xalign=hippo.ALIGNMENT_START)
                if left_control:
                    target_box.append(item)
                    item = target_box
            elif right_control and i == self.__column_count-1:
                item = right_control
                right_control.set_property("xalign", hippo.ALIGNMENT_END)
            else:
                item = hippo.CanvasText(text="")
            subbox.append(item)
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
        
class AppCategoryUsage(MultiVTable):
    def __init__(self):
        super(AppCategoryUsage, self).__init__(horiz_spacing=10, item_height=20)
        
        self.__bar_height = 10
        self.__bar_width = 80
        self.__bar_min_color = (0xeb, 0xdc, 0xf3); 
        self.__bar_max_color = (0xa4, 0x5a, 0xc6);        
        
        self.__apps = set()
        
    def set_apps(self, apps, used_apps):
        self.remove_all()
        
        self.append_column_item(hippo.CanvasText(text="Category", font="Bold 12px"))
        self.append_column_item(hippo.CanvasText(text="Your Usage", font="Bold 12px"))
        
        categories = categorize(used_apps)
        cat_usage = {}
        
        max_usage_count = ('', 0)
        for category,apps in categories.iteritems():
            cat_usage_count = 0
            for app in apps:
                mugshot_app = app.get_mugshot_app()
                if mugshot_app:
                    cat_usage_count += int(mugshot_app.get_usage_count())
            if cat_usage_count > max_usage_count[1]:
                max_usage_count = (category, cat_usage_count)
            cat_usage[category] = cat_usage_count
        
        if max_usage_count[1] == 0:
            return
        for category, usage in cat_usage.iteritems():
            self.append_column_item(hippo.CanvasText(text=category, 
                                                     yalign=hippo.ALIGNMENT_CENTER))
            factor = (usage * 1.0) / max_usage_count[1]
            box = CanvasHBox()
            (r, g, b) = map(lambda (min,max): int(min * (1.0-factor)) + int(max*factor), zip(self.__bar_min_color, self.__bar_max_color))          
            box.append(CanvasVBox())            
            box.append(CanvasVBox(box_height=self.__bar_height,
                                  box_width=(int(self.__bar_width * factor)),
                                  yalign=hippo.ALIGNMENT_CENTER,
                                  background_color=(r << 24) + (g << 16) + (b << 8) + (0xFF << 0)))
            box.append(CanvasVBox())
            self.append_column_item(box)

class CategoryExtras(CanvasVBox):
    __gsignals__ = {
        "have-apps" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_BOOLEAN,))      
    }
    def __init__(self, **args):
        super(CategoryExtras, self).__init__(background_color=0x888888FF,
                                             color=0xFFFFFFFF,
                                             **args)

        self.__catname = None
        self.__apps = None

        self.__headerbox = CanvasHBox()
        self.append(self.__headerbox)

        self.__left_title = hippo.CanvasText(text="New Popular %s" % (self.__catname,),
                                             font="12px Bold",
                                             xalign=hippo.ALIGNMENT_START)
        self.__headerbox.append(self.__left_title)

        self.__right_title = ActionLink(font="12px",
                                        xalign=hippo.ALIGNMENT_END)
        self.__right_title.connect("activated", self.__on_more_popular)
        self.__headerbox.append(self.__right_title, hippo.PACK_EXPAND)

        self.__app_pair = CanvasHBox(box_height=120)
        self.append(self.__app_pair)

    def have_apps(self):
        return not not self.__found_app_count

    def set_catname(self, catname):
        self.__catname = catname
        if catname:
            self.set_top_apps(mugshot.get_mugshot().get_category_top_apps(catname))
        self.__sync()

    def __on_more_popular(self, w):
        libbig.show_url(urlparse.urljoin(mugshot.get_mugshot().get_baseurl(),
                                         "applications?category=" + urllib.quote(self.__catname)))

    def set_top_apps(self, apps):
        self.__apps = apps
        self.__sync()
        self.emit("have-apps", self.have_apps())

    def __sync(self):
        if self.__catname:
            self.__left_title.set_property('text', "New Popular %s" % (self.__catname,))
            self.__right_title.set_property('text', "More Popular %s" % (self.__catname,))
        self.__app_pair.remove_all()
        found = 0
        for i,app in enumerate(self.__apps or []):
            if app.is_installed():
                continue
            app_view = apps_widgets.AppDisplay(app)
            app_view.set_description_mode(True)
            self.__app_pair.append(app_view, hippo.PACK_EXPAND)
            found += 1
            if found > 1:
                break
        self.__found_app_count = found
        
class AppList(CanvasVBox):
    __gsignals__ = {
        "category-selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_STRING,)),
        "selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "launch" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())      
    }
    
    def __init__(self):
        super(AppList, self).__init__()
        
        self.__table = MultiVTable(columns=3,item_height=40)
        self.append(self.__table, hippo.PACK_EXPAND)

        self.__extras_section = CategoryExtras(yalign=hippo.ALIGNMENT_END)
        self.__extras_section.connect("have-apps", self.__on_have_extras)
        self.append(self.__extras_section)

        self.__search = None
        self.__all_apps = None
        self.__used_apps = None
        self.__categorized = None
        self.__selected_app = None
        self.__selected_cat = None
        
    def set_apps(self, apps, used_apps):
        self.__all_apps = apps
        self.__used_apps = used_apps
        self.__sync_display()

    def __on_have_extras(self, extras, have_extras):
        self.set_child_visible(extras, self.__selected_cat and have_extras)
        
    def __sync_display(self):
        
        self.__table.remove_all()

        categories = categorize(filter(self.__filter_app, self.__all_apps))
        self.__categorized = categories
             
        cat_keys = categories.keys()
        cat_keys.sort()

        if self.__selected_cat:
            self.__extras_section.set_catname(self.__selected_cat)
            self.set_child_visible(self.__extras_section, self.__extras_section.have_apps())
        else:
            self.set_child_visible(self.__extras_section, False)

        display_only_used = (not self.__selected_cat) and (not self.__search) 
        for catname in (self.__selected_cat and not self.__search) and [self.__selected_cat] or cat_keys:
            cat_used_apps = filter(lambda app: app in self.__used_apps, categories[catname])
            cat_used_apps_count = len(cat_used_apps)
            if display_only_used:
                right_link = ActionLink(text=u"More (%d) \u00BB" % (len(categories[catname]) - cat_used_apps_count,))
                right_link.connect("activated", self.__handle_category_more, catname)
                left_link = None
            else:
                right_link = None
                left_link = ActionLink(text=u"All Applications \u00BB")
                left_link.connect("activated", self.__handle_nocategory)
            self.__table.append_section_head(catname, left_control=left_link, right_control=right_link)
            if display_only_used:
                appsource = cat_used_apps
            else:
                appsource = categories[catname]
            for app in appsource:
                overview = apps_widgets.AppDisplay(app)
                overview.connect("button-press-event", self.__on_overview_click)             
                self.__table.append_column_item(overview)

    def on_category_changed(self, cat, apps):
        if cat != self.__selected_cat:
            return
        self.__extras_section.set_top_apps(apps)

    def __handle_nocategory(self, l):
        _logger.debug("no category selected")
        self.__selected_cat = None
        self.__sync_display()

    def __handle_category_more(self, l, catname):
        _logger.debug("category %s selected", catname)
        self.__selected_cat = catname
        self.__sync_display()
                
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
         
         if self.__selected_app:
             self.__selected_app.set_force_prelight(False)

         if event.count == 1:
             app = overview.get_app()
             self.__selected_app = overview
             self.__selected_app.set_force_prelight(True)
             self.emit("selected", app)
         else:
             self.emit("launch")
             
class AppBrowser(hippo.CanvasWindow):
    def __init__(self, stock):
        super(AppBrowser, self).__init__(gtk.WINDOW_TOPLEVEL)
        
        self.__stock = stock
        self.__all_apps = []
        
        self.set_keep_above(1)
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        
    
        self.__box = CanvasHBox(spacing=10)
    
        self.__left_box = CanvasVBox(spacing=4)
        self.__box.append(self.__left_box)
        
        self.__search_text = hippo.CanvasText(text="Search", font="Bold 12px")
        self.__left_box.append(self.__search_text)
        self.__search_input = hippo.CanvasEntry()
        self.__search_input.connect("notify::text", self.__on_search_changed)
        self.__idle_search_id = 0
        self.__left_box.append(self.__search_input)        
    
        self.__overview = AppOverview()
        self.__overview.connect("more-info", lambda o, app: self.__on_show_more_info(app))
        self.__left_box.append(self.__overview)
        
        self.__cat_usage = AppCategoryUsage()
        self.__left_box.append(self.__cat_usage)   

        browse_link = ActionLink(text="Browse popular applications") 
        browse_link.connect("button-press-event", lambda l,e: self.__on_browse_popular_apps())
        self.__left_box.append(browse_link)
    
        self.__right_scroll = hippo.CanvasScrollbars()
        self.__right_scroll.set_policy(hippo.ORIENTATION_HORIZONTAL,
                                       hippo.SCROLLBAR_NEVER)
        self.__right_box = CanvasVBox(border=0, background_color=0xFFFFFFFF)
        self.__box.append(self.__right_scroll, hippo.PACK_EXPAND)
        
        self.__app_list = AppList()
        self.__right_box.append(self.__app_list, hippo.PACK_EXPAND)
        self.__app_list.connect("category-selected", lambda list, cat: self.__on_category_selected(cat))
        self.__app_list.connect("selected", lambda list, app: self.__on_app_selected(app))
        self.__app_list.connect("launch", lambda list: self.__on_app_launch()) 
        
        self.__right_scroll.set_root(self.__right_box)        
        
        self.set_default_size(750, 600)
        self.connect("delete-event", gtk.Widget.hide_on_delete)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))
               
        self.set_root(self.__box)
        
        self.__mugshot = mugshot.get_mugshot()
        self.__mugshot.connect("initialized", lambda mugshot: self.__sync())
        self.__mugshot.connect("my-top-apps-changed", 
                               lambda mugshot, apps: self.__sync())          
        self.__mugshot.connect("category-top-apps-changed", 
                               self.__handle_category_changed)          
        self.__sync()

    def __handle_category_changed(self, m, cat, apps):
        _logger.debug("category %s changed: %d apps", cat, len(apps))
        apps = map(self.__stock.get_app, apps)
        self.__app_list.on_category_changed(cat, apps)
                
    def __on_app_selected(self, app):
        self.__overview.set_app(app)

    def __on_show_more_info(self, app):
        libbig.show_url(urlparse.urljoin(mugshot.get_mugshot().get_baseurl(), "application?id=" + app.get_mugshot_app().get_id()))
        self.hide()
        
    def __on_app_launch(self):
        self.__overview.launch()
        self.hide()
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.hide()

    def __on_browse_popular_apps(self):
        libbig.show_url(urlparse.urljoin(mugshot.get_mugshot().get_baseurl(), "applications"))
        self.hide()
            
    def __on_search_changed(self, input, text):
        if self.__idle_search_id > 0:
            return
        self.__idle_search_id = gobject.timeout_add(500, self.__idle_do_search)
        
    def __idle_do_search(self):
        self.__app_list.set_search(self.__search_input.get_property("text"))
        self.__idle_search_id = 0
        
    def __on_mugshot_initialized(self, mugshot):
        self.__sync()
            
    def __sync(self):
        ad = apps_directory.get_app_directory()
        apps = map(self.__stock.get_local_app, ad.get_apps())
        apps = filter(lambda app: not app.get_is_excluded(), apps)
                
        self.__all_apps = apps
        self.__used_apps = map(self.__stock.get_app, self.__mugshot.get_my_top_apps() or [])
        self.__app_list.set_apps(apps, self.__used_apps)
        self.__cat_usage.set_apps(apps, self.__used_apps) 
