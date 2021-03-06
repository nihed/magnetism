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
        super(AppOverview, self).__init__(box_width=200, box_height=260,
                                          border=1, border_color=0xAAAAAAFF, background_color=0xFFFFFFFF, padding=5)

        self.__unselected = True
        self.__app_unselected_text = hippo.CanvasText(text="Click an application to see its description here.\n\nDouble-click to launch.",
                                                      size_mode=hippo.CANVAS_SIZE_WRAP_WORD,
                                                      xalign=hippo.ALIGNMENT_CENTER,
                                                      yalign=hippo.ALIGNMENT_CENTER,
                                                      color=0x3F3F3FFF)
        self.append(self.__app_unselected_text, hippo.PACK_CLEAR_RIGHT)
        
        self.__header = apps_widgets.AppDisplay()
        
        self.__description = hippo.CanvasText(font="12px",size_mode=hippo.CANVAS_SIZE_WRAP_WORD)
        
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
        self.set_child_visible(self.__moreinfo, not not self.__app.get_mugshot_app())
        
    def launch(self):
        return self.__header.launch()
        
    def get_app(self):
        return self.__app

def categorize(apps):    
    """Given a set of applications, returns a map <string,set<Application>> based on category name."""
    categories = {}
    local_categories = {}
    for app in apps:
        cat = app.get_category()
        local_cat = app.get_local_category()
        if not categories.has_key(cat):
            categories[cat] = set()
        categories[cat].add(app)  
        if not local_categories.has_key(local_cat):
            local_categories[local_cat] = set()
        local_categories[local_cat].add(app)
    # heuristic for detecting when we don't have enough data from Mugshot
    if len(categories) <= 2:
        return local_categories
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
            subbox = CanvasVBox(color=0xAAAAAAFF, border_bottom=1, border_color=0xAAAAAAFF)
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

        self.set_property('background-color', 0xFFFFFFFF)
        self.set_property('padding', 5)
                
    def set_apps(self, apps, used_apps):
        self.remove_all()

        self.append_column_item(hippo.CanvasText(text="Category", font="Bold 12px", color=0x3F3F3FFF))
        self.append_column_item(hippo.CanvasText(text="Your Usage", font="Bold 12px", color=0x3F3F3FFF))
        
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

        cat_keys_sorted = cat_usage.keys()
        cat_keys_sorted.sort()
        for category in cat_keys_sorted:
            self.append_column_item(hippo.CanvasText(text=category, 
                                                     yalign=hippo.ALIGNMENT_CENTER,
                                                     xalign=hippo.ALIGNMENT_START, color=0x3F3F3FFF))
            factor = (cat_usage[category] * 1.0) / max_usage_count[1]
            box = CanvasHBox()
            (r, g, b) = map(lambda (min,max): int(min * (1.0-factor)) + int(max*factor), zip(self.__bar_min_color, self.__bar_max_color))          

            box.append(CanvasVBox(box_height=self.__bar_height,
                                  box_width=(int(self.__bar_width * factor)),
                                  yalign=hippo.ALIGNMENT_CENTER,
                                  background_color=(r << 24) + (g << 16) + (b << 8) + (0xFF << 0)))

            self.append_column_item(box)
            

class AppExtras(CanvasVBox):
    __gsignals__ = {
        "have-apps" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_BOOLEAN,)),
        "more-info" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
    }
    def __init__(self, stock, filterfunc, **args):
        super(AppExtras, self).__init__(background_color=0x888888FF,
                                        color=0xFFFFFFFF,
                                        padding=4,
                                        **args)

        self.__search = False
        self.__search_filter = filterfunc
        self.__stock = stock
        self.__catname = None
        self.__apps = None

        self.__headerbox = CanvasHBox()
        self.append(self.__headerbox)

        self.__left_title = hippo.CanvasText(text="New Popular %s" % (self.__catname,),
                                             font="12px Bold",
                                             xalign=hippo.ALIGNMENT_START)
        self.__headerbox.append(self.__left_title)

        self.__right_title = ActionLink(font="12px",
                                        color=0xFFFFFFFF,
                                        xalign=hippo.ALIGNMENT_END)
        self.__right_title.connect("activated", self.__on_more_popular)
        self.__headerbox.append(self.__right_title, hippo.PACK_EXPAND)

        self.__app_pair = CanvasHBox(box_height=120)
        self.__app_pair2 = CanvasHBox(box_height=120)
        self.append(self.__app_pair)
        self.append(self.__app_pair2)

        mugshot.get_mugshot().connect("apps-search-changed", self.__handle_mugshot_results)

    def have_apps(self):
        return not not self.__found_app_count

    def set_catname(self, catname, search):
        self.__catname = catname
        if catname:
            mugshot_apps = mugshot.get_mugshot().get_category_top_apps(catname)
        else:
            mugshot_apps = mugshot.get_mugshot().get_global_top_apps()  
        if mugshot_apps is None:
            self.set_top_apps(None, search)
        else:
            self.set_top_apps(map(self.__stock.get_app, mugshot_apps), search)

    def __handle_mugshot_results(self, mugshot, search, apps):
        if search != self.__search:
            return
        self.__mugshot_search_hits = not not apps
        self.__sync()

    def __on_more_popular(self, w):
        libbig.show_url(urlparse.urljoin(mugshot.get_mugshot().get_baseurl(),
                                         "applications%s" % ((self.__search and ("?q=" + urllib.quote(self.__search)))
                                                             or self.__catname and ("?category=" + urllib.quote(self.__catname)))))
        # more-info with None just means hide window
        self.emit("more-info", None)

    def set_top_apps(self, apps, search):
        self.__apps = apps
        self.__search = search
        self.__mugshot_search_hits = None
        self.__sync()
        self.emit("have-apps", self.have_apps())

    def __sync(self):
        thing = self.__catname or 'apps'
        if self.__apps:
            if self.__search:
                if self.__mugshot_search_hits is None:
                    self.__left_title.set_property('markup', "Searching for <b>%s</b>" % (gobject.markup_escape_text(self.__search),))

                else:
                    self.__left_title.set_property('markup', "No results for <b>%s</b>" % (gobject.markup_escape_text(self.__search),))
            else:
                self.__left_title.set_property('text', "New Popular %s" % (thing,))
            self.__right_title.set_property('text', u"More Popular %s \u00BB" % (thing,))
        elif self.__apps is None:
            self.__left_title.set_property('text', '')
            self.__right_title.set_property("text", "Loading popular %s..." % (thing,))
        else:
            self.__left_title.set_property('text', '')
            self.__right_title.set_property("text", "No popular %s found" % (thing,))

        self.__app_pair.remove_all()
        self.set_child_visible(self.__app_pair, not not self.__apps)
        found = 0
        for i,app in enumerate(self.__apps or []):
            if app.is_installed():
                continue
            if self.__search_filter and (not self.__search_filter(app)):
                continue
            app_view = apps_widgets.AppDisplay(app, color=0xFFFFFFFF)
            app_view.connect("title-clicked", self.__on_app_clicked)
            app_view.set_description_mode(True)
            if found > 1:
                self.__app_pair2.append(app_view, hippo.PACK_EXPAND)
            else:
                self.__app_pair.append(app_view, hippo.PACK_EXPAND)
            found += 1
            if found > 3:
                break
        self.set_child_visible(self.__app_pair, found > 0)
        self.set_child_visible(self.__app_pair2, found > 2 and (not not self.__search))
        self.__found_app_count = found

    def __on_app_clicked(self, a):
        self.emit("more-info", a.get_app())
        
        
class AppList(CanvasVBox):
    __gsignals__ = {
        "category-selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_STRING,)),
        "selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "launch" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "more-info" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))      
    }
    
    def __init__(self, stock):
        super(AppList, self).__init__(padding=6)

        self.__table = MultiVTable(columns=3,item_height=40)
        self.append(self.__table, hippo.PACK_EXPAND)

        self.__extras_section = AppExtras(stock, self._filter_app, yalign=hippo.ALIGNMENT_END)
        self.__extras_section.connect("more-info", lambda e, app: self.emit("more-info", app))
        self.append(self.__extras_section)

        self.__stock = stock

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

    def __sync_display(self):
        
        self.__table.remove_all()

        categories = categorize(filter(self.__filter_app_and_installed, self.__all_apps))
        self.__categorized = categories
             
        cat_keys = categories.keys()
        cat_keys.sort()

        self.__extras_section.set_catname(self.__selected_cat, self.__search)

        display_only_used = (self.__used_apps) and (not self.__selected_cat) and (not self.__search) 
        for catname in (self.__selected_cat and not self.__search) and [self.__selected_cat] or cat_keys:
            cat_used_apps = filter(lambda app: app in self.__used_apps, categories[catname])
            cat_used_apps_count = len(cat_used_apps)
            if display_only_used:
                right_link = ActionLink(text=u"More (%d) \u00BB" % (len(categories[catname]) - cat_used_apps_count,))
                right_link.connect("activated", self.__handle_category_more, catname)
                left_link = None
            else:
                right_link = None
                if self.__selected_cat:
                    left_link = ActionLink(text=u"All Applications /")
                    left_link.connect("activated", self.__handle_nocategory)
                else:
                    left_link = None
            self.__table.append_section_head(catname, left_control=left_link, right_control=right_link)
            if display_only_used:
                appsource = cat_used_apps
            else:
                appsource = categories[catname]
            for app in appsource:
                overview = apps_widgets.AppDisplay(app)
                overview.connect("button-press-event", self.__on_overview_click)             
                self.__table.append_column_item(overview)

    def reset_category(self):
        self.__handle_nocategory(None)

    def on_category_changed(self, cat, apps):
        if cat != self.__selected_cat:
            return
        self.__extras_section.set_top_apps(apps, self.__search)

    def __handle_nocategory(self, l):
        _logger.debug("no category selected")
        self.__selected_cat = None
        self.__sync_display()

    def __handle_category_more(self, l, catname):
        _logger.debug("category %s selected", catname)
        self.__selected_cat = catname
        self.__sync_display()

    def __filter_app_and_installed(self, app):
        if not app.is_installed():
            return False
        return self._filter_app(app)
                
    def _filter_app(self, app):
        if not self.__search:
            return True
        search = self.__search.lower()
        keys = (app.get_name(), app.get_description(), app.get_generic_name(), app.get_tooltip())
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
        
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        

        self.set_title('Applications')
    
        self.__box = CanvasHBox()
    
        self.__left_box = CanvasVBox(spacing=6, padding=6)
        self.__left_box.set_property('background-color', 0xEEEEEEFF)                
        self.__box.append(self.__left_box)
        
        self.__search_text = hippo.CanvasText(text="Search All Applications:", font="Bold 12px",
                                              color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START)
        self.__left_box.append(self.__search_text)
        self.__search_input = hippo.CanvasEntry()
        #self.__search_input.set_property('border-color', 0xAAAAAAFF)
        self.__search_input.connect("notify::text", self.__on_search_changed)
        self.__idle_search_id = 0
        self.__idle_search_mugshot_id = 0
        self.__left_box.append(self.__search_input)        
    
        self.__overview = AppOverview()
        self.__overview.connect("more-info", lambda o, app: self.__on_show_more_info(app))
        self.__left_box.append(self.__overview)
        
        self.__cat_usage = AppCategoryUsage()
        self.__left_box.append(self.__cat_usage)   

        self.__left_box.append(hippo.CanvasText(text="Tools", font="Bold 12px",
                                                color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START))

        browse_link = ActionLink(text="Find New Applications", xalign=hippo.ALIGNMENT_START) 
        browse_link.connect("button-press-event", lambda l,e: self.__on_browse_popular_apps())
        self.__left_box.append(browse_link)
    
        self.__right_scroll = hippo.CanvasScrollbars()
        self.__right_scroll.set_policy(hippo.ORIENTATION_HORIZONTAL,
                                       hippo.SCROLLBAR_NEVER)
        self.__right_box = CanvasVBox(border=0, background_color=0xFFFFFFFF)
        self.__box.append(self.__right_scroll, hippo.PACK_EXPAND)
        
        self.__app_list = AppList(self.__stock)
        self.__right_box.append(self.__app_list, hippo.PACK_EXPAND)
        self.__app_list.connect("category-selected", lambda list, cat: self.__on_category_selected(cat))
        self.__app_list.connect("selected", lambda list, app: self.__on_app_selected(app))
        self.__app_list.connect("launch", lambda list: self.__on_app_launch()) 
        self.__app_list.connect("more-info", lambda list, app: self.__on_show_more_info(app)) 
        
        self.__right_scroll.set_root(self.__right_box)        
        
        self.set_default_size(750, 600)
        self.connect("delete-event", lambda *args: self.__hide_reset() or True)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))
               
        self.set_root(self.__box)

        self.__mugshot = mugshot.get_mugshot()
        self.__mugshot.connect("initialized", lambda mugshot: self.__sync())
        self.__mugshot.connect("my-top-apps-changed", 
                               lambda mugshot, apps: self.__sync())          
        self.__mugshot.connect("global-top-apps-changed", 
                               self.__handle_global_changed)          
        self.__mugshot.connect("apps-search-changed", 
                               lambda *args: self.__sync())          
        self.__mugshot.connect("category-top-apps-changed", 
                               self.__handle_category_changed)          
        self.__stock.connect("all-apps-loaded",
                             lambda as: self.__sync())
        self.__sync()

    def __handle_global_changed(self, m, apps):
        self.__handle_category_changed(m, None, apps)

    def __handle_category_changed(self, m, cat, apps):
        _logger.debug("category %s changed: %d apps", cat, len(apps))
        apps = map(self.__stock.get_app, apps)
        self.__app_list.on_category_changed(cat, apps)
                
    def __on_app_selected(self, app):
        self.__overview.set_app(app)

    def __reset(self):
        self.__search_input.set_property('text', '')
        self.__app_list.reset_category()

    def __hide_reset(self):
        self.__reset()
        self.hide()

    def __on_show_more_info(self, app):
        if app:
            libbig.show_url(urlparse.urljoin(mugshot.get_mugshot().get_baseurl(), "application?id=" + app.get_mugshot_app().get_id()))
        self.__hide_reset()

    def __on_app_launch(self):
        self.__overview.launch()
        self.__hide_reset()
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.__hide_reset()

    def __on_browse_popular_apps(self):
        libbig.show_url(urlparse.urljoin(mugshot.get_mugshot().get_baseurl(), "applications"))
        self.__hide_reset()
            
    def __on_search_changed(self, input, text):
        if self.__idle_search_id > 0:
            return
        self.__idle_search_id = gobject.timeout_add(500, self.__idle_do_search)
        if self.__idle_search_mugshot_id > 0:
            return
        self.__idle_search_mugshot_id = gobject.timeout_add(2000, self.__idle_do_mugshot_search)
        
    def __idle_do_search(self):
        self.__app_list.set_search(self.__search_input.get_property("text"))
        self.__idle_search_id = 0
        
    def __idle_do_mugshot_search(self):
        searchtext = self.__search_input.get_property("text")
        if searchtext:
            mugshot.get_mugshot().request_app_search(searchtext)
        self.__idle_mugshot_search_id = 0

    def __on_mugshot_initialized(self, mugshot):
        self.__sync()
            
    def __sync(self):
        local_apps = self.__stock.get_local_apps() 
        mugshot_apps = self.__stock.get_all_apps()
                
        self.__all_apps = set(local_apps).union(set(mugshot_apps))
        self.__used_apps = map(self.__stock.get_app, self.__mugshot.get_my_top_apps() or [])
        self.__app_list.set_apps(self.__all_apps, self.__used_apps)
        self.__cat_usage.set_apps(self.__all_apps, self.__used_apps) 
