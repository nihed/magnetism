import logging, time, urlparse, urllib, time

import gobject, gtk
import hippo, gconf

import bigboard.globals as globals
import bigboard.global_mugshot as global_mugshot
import bigboard.libbig as libbig
from bigboard.libbig.gutil import *
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, CanvasTable, \
             ActionLink, PrelightingCanvasBox, CanvasSpinner, CanvasCheckbox
from bigboard.overview_table import OverviewTable

import apps_widgets, apps_directory

_logger = logging.getLogger("bigboard.AppBrowser")

GCONF_KEY_APP_SIZE = '/apps/bigboard/application_list_size'

class AppOverview(CanvasVBox):
    __gsignals__ = {
        "more-info" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "toggle-pinned" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT, gobject.TYPE_BOOLEAN,)),
        "move-up" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "move-down" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
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

        self.__check_showing = CanvasCheckbox("Show in sidebar")
        self.__check_showing.checkbox.connect('toggled', self.__on_show_in_sidebar_toggled)

        self.__up_button = hippo.CanvasLink(text="Up", border_right=10)
        self.__down_button = hippo.CanvasLink(text="Down")
        self.__up_down_controls = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.__up_down_controls.append(self.__up_button)
        self.__up_down_controls.append(self.__down_button)

        self.__up_button.connect("activated", lambda ci: self.emit("move-up", self.__app))
        self.__down_button.connect("activated", lambda ci: self.emit("move-down", self.__app))
        
        if app:
            self.set_app(app)


    def __on_show_in_sidebar_toggled(self, checkbox):
        active = checkbox.get_active()
        _logger.debug("Checkbox toggled, active = " + str(active))
        if self.__app:
            pinned = self.__app.get_pinned()
            _logger.debug("App is currently pinned = " + str(pinned))
            if active != pinned:
                self.emit("toggle-pinned", self.__app, active)

    def sync_pinned_checkbox(self):
        _logger.debug("Syncing pinned checkbox")
        if self.__app:
            _logger.debug("  app " + self.__app.get_id() + " pinned: " + str(self.__app.get_pinned()))
            self.__check_showing.checkbox.set_active(self.__app.get_pinned())

    def set_app(self, app):
        if self.__unselected:
            self.__unselected = False
            self.remove(self.__app_unselected_text)
            self.append(self.__header)
            self.append(self.__description, hippo.PACK_CLEAR_RIGHT)
            self.append(self.__moreinfo)
            self.append(self.__check_showing)
            self.append(self.__up_down_controls)

        self.__app = app
        self.__header.set_app(app)
        self.__description.set_property("text", app.get_description())

        self.sync_pinned_checkbox()
        
        self.set_child_visible(self.__moreinfo, not not self.__app.get_mugshot_app())
        
    def launch(self):
        return self.__header.launch()
        
    def get_app(self):
        return self.__app

    def get_header(self):
        return self.__header

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
        
class Bar(hippo.CanvasBox):
    _height = 12
    _width = 100
    _min_color = (0xeb, 0xdc, 0xf3); 
    _max_color = (0xa4, 0x5a, 0xc6);        
        
    def __init__(self, factor, **kwargs):
        self.__factor = factor
        self.__color = map(lambda (min,max): (min * (1.0-factor) + max*factor) / 255., zip(Bar._min_color, Bar._max_color))
        kwargs['xalign'] = hippo.ALIGNMENT_START
        kwargs['yalign'] = hippo.ALIGNMENT_CENTER

        hippo.CanvasBox.__init__(self, **kwargs)

    def do_paint_below_children(self, cr, damaged):
        (width, height) = self.get_allocation()

        # border/padding should be subtracted out from width
        x,y,width,height = self.align(int(width * self.__factor), Bar._height)

        cr.set_source_rgb(*self.__color)
        cr.rectangle(x,y,width,height)
        cr.fill()

    def do_get_content_width_request(self):
        return (0, Bar._width)
        
    def do_get_content_height_request(self, for_width):
        return (Bar._height, Bar._height)

gobject.type_register(Bar)

class AppCategoryUsage(CanvasTable):
    def __init__(self):
        super(AppCategoryUsage, self).__init__(column_spacing=10, row_spacing=3)
        self.set_column_expand(1, True)
        
        self.__apps = set()

        self.set_property('background-color', 0xFFFFFFFF)
        self.set_property('padding', 5)
                
    def set_apps(self, apps, used_apps):
        self.remove_all()

        self.add(hippo.CanvasText(text="Category", font="Bold 12px", color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START), left=0, top=0)
        self.add(hippo.CanvasText(text="Your Usage", font="Bold 12px", color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START), left=1, top=0)
        
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
        row = 1
        for category in cat_keys_sorted:
            self.add(hippo.CanvasText(text=category, 
                                      yalign=hippo.ALIGNMENT_CENTER,
                                      xalign=hippo.ALIGNMENT_START, color=0x3F3F3FFF),
                     left=0, top=row)
            factor = (cat_usage[category] * 1.0) / max_usage_count[1]
            
            self.add(Bar(factor), left=1, top=row)
            
            row += 1
            

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

        global_mugshot.get_mugshot().connect("apps-search-changed", self.__handle_mugshot_results)
        global_mugshot.get_mugshot().connect("category-top-apps-changed", self.__handle_category_top_apps_changed)        

    def have_apps(self):
        return not not self.__found_app_count

    def __get_apps(self):
        if self.__catname:
            _logger.debug("getting top apps for category %s", self.__catname)
            return global_mugshot.get_mugshot().get_category_top_apps(self.__catname)
        else:
            return global_mugshot.get_mugshot().get_global_top_apps()          

    def set_catname(self, catname, search):
        self.__catname = catname
        mugshot_apps = self.__get_apps()
        if mugshot_apps is None:
            self.set_top_apps(None, search)
        else:
            self.set_top_apps(map(self.__stock.get_app, mugshot_apps), search)

    def __handle_mugshot_results(self, mugshot, search, apps):
        if search != self.__search:
            return
        self.__mugshot_search_hits = not not apps
        self.__sync()
        
    def __handle_category_top_apps_changed(self, *args):
        self.__apps = map(self.__stock.get_app, self.__get_apps())
        self.__sync()

    def __on_more_popular(self, w):
        subquery = (self.__search and ("?q=" + urllib.quote(self.__search))) \
                    or self.__catname and ("?category=" + urllib.quote(self.__catname)) \
                    or ''
        libbig.show_url(urlparse.urljoin(globals.get_baseurl(),
                                         "applications%s" % (subquery,)))
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

        self.__app_pair.clear()
        self.__app_pair2.clear()
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

        self.__table = OverviewTable(min_column_width=200, max_column_width=250)
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

        categories = categorize(filter(self.__filter_app_and_installed_or_used, self.__all_apps)) 

        self.__categorized = categories
             
        cat_keys = categories.keys()
        cat_keys.sort()

        self.__extras_section.set_catname(self.__selected_cat, self.__search)

        section_key = 0
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
            self.__table.add_section_head(section_key, catname, left_control=left_link, right_control=right_link)
            if display_only_used:
                appsource = cat_used_apps
            else:
                appsource = categories[catname]
            for app in appsource:
                overview = apps_widgets.AppDisplay(app)
                overview.connect("button-press-event", self.__on_overview_click) 
                self.__table.add_column_item(section_key, overview)
            section_key += 1

    def reset_category(self):
        self.__handle_nocategory(None)

    def on_category_changed(self, cat, apps):
        _logger.debug("got category changed: %s", cat)
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

    def __filter_app_and_installed_or_used(self, app):
        if not app.is_installed() and not app in self.__used_apps:
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
        if search.strip() == "":
            self.__search = None
        else:
            self.__search = search.lower()

        self.__sync_display()
             
    def __on_overview_click(self, overview, event):
         _logger.debug("pressed %s %d", overview, event.count)
         
         if event.count == 1:
             if overview == self.__selected_app:
                 self.emit("launch")
                 return
             
             if self.__selected_app:
                 self.__selected_app.set_force_prelight(False)

             app = overview.get_app()
             self.__selected_app = overview
             self.__selected_app.set_force_prelight(True)
             self.emit("selected", app)
         elif event.count == 2:
             self.emit("launch")
             
class AppBrowser(hippo.CanvasWindow):
    def __init__(self, stock):
        super(AppBrowser, self).__init__(gtk.WINDOW_TOPLEVEL)
        
        self.__stock = stock
        self.__all_apps = []
        
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        

        self.set_title('Applications')
    
        self.__box = CanvasHBox()
    
        self.__left_box = CanvasVBox(spacing=6, padding=6, box_width=250)
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
        self.__overview.connect("toggle-pinned", lambda o, app, active: self.__on_toggle_app_pinned(app, active))
        self.__overview.connect("move-up", lambda o, app: self.__on_move_app(app, True))
        self.__overview.connect("move-down", lambda o, app: self.__on_move_app(app, False))
        self.__left_box.append(self.__overview)
        
        self.__cat_usage = AppCategoryUsage()
        self.__left_box.append(self.__cat_usage)   

        self.__left_box.append(hippo.CanvasText(text="Tools", font="Bold 12px",
                                                color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START))

        browse_link = ActionLink(text="Find New Applications", xalign=hippo.ALIGNMENT_START) 
        browse_link.connect("button-press-event", lambda l,e: self.__on_browse_popular_apps())
        self.__left_box.append(browse_link)
        spinbox = CanvasHBox()
        spinbox.append(hippo.CanvasText(text='Visible applications: '))
        self.__apps_spinner = CanvasSpinner()
        gconf.client_get_default().notify_add(GCONF_KEY_APP_SIZE, self.__on_visible_apps_key_changed)
        self.__apps_spinner.spinner.connect('value-changed', self.__on_visible_apps_spin_changed)
        self.__apps_spinner.spinner.get_adjustment().set_all(gconf.client_get_default().get_int(GCONF_KEY_APP_SIZE),
                                                             1, 20, 1, 1, 1)
        spinbox.append(self.__apps_spinner)
        self.__left_box.append(spinbox)
    
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
        
        self.set_default_size(1000, 600)
        self.connect("delete-event", lambda *args: self.__hide_reset() or True)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))
               
        self.set_root(self.__box)
        
        ad = apps_directory.get_app_directory()
        self.__last_local_app_change = 0
        self.__last_local_app_idle_id = 0
        ad.connect('changed', self.__on_local_apps_changed)

        self.__mugshot = global_mugshot.get_mugshot()
        self.__mugshot.connect("initialized", lambda mugshot: self.__sync())
        self.__mugshot.connect("my-top-apps-changed", 
                               lambda mugshot, apps: self.__sync())          
        self.__mugshot.connect("global-top-apps-changed", 
                               self.__handle_global_changed)          
        self.__mugshot.connect("apps-search-changed", 
                               lambda *args: self.__sync())          
        self.__mugshot.connect("category-top-apps-changed", 
                               self.__handle_category_changed)
        self.__mugshot.connect("pinned-apps-changed", self.__on_pinned_apps_changed)
        self.__stock.connect("all-apps-loaded",
                             lambda as: self.__sync())
        
        self.__sync()
        
    @defer_idle_func(logger=_logger)
    def __on_visible_apps_spin_changed(self, *args):
        _logger.debug("spinner changed")
        count = self.__apps_spinner.spinner.get_value()
        curval = gconf.client_get_default().get_int(GCONF_KEY_APP_SIZE) or 4
        if curval != count:        
            gconf.client_get_default().set_int(GCONF_KEY_APP_SIZE, count)
        
    def __on_visible_apps_key_changed(self, *args):
        _logger.debug("apps count key changed")
        curval = gconf.client_get_default().get_int(GCONF_KEY_APP_SIZE) or 4
        if curval != self.__apps_spinner.spinner.get_value():
            self.__apps_spinner.set_value(curval)
        
    def __on_local_apps_changed(self, ad):
        _logger.debug("handling local app change")
        self.__sync()
        
    def __handle_global_changed(self, m, apps):
        self.__handle_category_changed(m, None, apps)

    def __handle_category_changed(self, m, cat, apps):
        _logger.debug("category %s changed: %d apps", cat, len(apps))
        apps = map(self.__stock.get_app, apps)
        self.__app_list.on_category_changed(cat, apps)
                
    def __on_app_selected(self, app):
        self.__overview.set_app(app)
        self.__overview.get_header().connect("button-press-event", lambda l,e: self.__on_app_launch()) 

    def __reset(self):
        self.__search_input.set_property('text', '')
        self.__app_list.reset_category()

    def __hide_reset(self):
        self.__reset()
        self.hide()

    def __on_show_more_info(self, app):
        if app:
            libbig.show_url(urlparse.urljoin(globals.get_baseurl(), "application?id=" + app.get_mugshot_app().get_id()))
        self.__hide_reset()

    def __on_set_pinned_reply(self):
        ## this is incredibly broken and means we don't have change notification from the server
        _logger.debug("got reply to setting pinned apps")
        self.__mugshot.get_pinned_apps(force=True)

    def __on_toggle_app_pinned(self, app, active):
        _logger.debug("Toggle app " + app.get_id() + " pinned = " + str(active))

        pinned_apps = self.__mugshot.get_pinned_apps()
        if pinned_apps is None:
            pinned_apps = []
        pinned_ids = {}
        for p in pinned_apps:
            pinned_ids[p.get_id()] = p

        _logger.debug("  (pinned_ids = " + str(pinned_ids.keys()) + ")")

        if app.get_id() in pinned_ids:
            if active:
                _logger.debug("App is already pinned")
                return
            else:
                _logger.debug("Unpinning app")
                del pinned_ids[app.get_id()]
        else:
            if active:
                _logger.debug("Pinning app")
                pinned_ids[app.get_id()] = app.get_mugshot_app()
            else:
                _logger.debug("App is already not pinned")
                return

        _logger.debug("Setting pinned apps to " + str(pinned_ids.keys()))
        self.__mugshot.set_pinned_apps(pinned_ids.keys(), self.__on_set_pinned_reply)

    def __on_move_app(self, app, up):
        _logger.debug("Move app up= " + str(up))
        if up:
            pass
        else:
            pass        

    def __on_app_launch(self):
        self.__overview.launch()
        self.__hide_reset()
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.__hide_reset()

    def __on_browse_popular_apps(self):
        libbig.show_url(urlparse.urljoin(globals.get_baseurl(), "applications"))
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
            global_mugshot.get_mugshot().request_app_search(searchtext)
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

    def __on_pinned_apps_changed(self, mugshot, pinned_apps):
        _logger.debug("Pinned apps changed: " + str(map(lambda x: x.get_id(), pinned_apps)))
        self.__overview.sync_pinned_checkbox()
