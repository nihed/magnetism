import logging, time, urlparse, urllib

import gobject, gtk, gconf
import hippo

from ddm import DataModel

import bigboard.globals
import bigboard.libbig as libbig
from bigboard.libbig.logutil import log_except
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, CanvasCombo
from bigboard.big_widgets import ActionLink, PrelightingCanvasBox, Button, CanvasCheckbox, CanvasURLImage
from bigboard.overview_table import OverviewTable

_logger = logging.getLogger("bigboard.PortfolioManager")

LISTED = 0
UNLISTED = 1
SECTIONS = {
    LISTED : "Installed Widgets",
    UNLISTED : "Available Widgets"
}

GCONF_KEY_VISIBLE = '/apps/bigboard/visible'
GCONF_KEY_THEME = '/apps/bigboard/theme'

class StockPreview(CanvasVBox):
    __gsignals__ = {
        "add-remove" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),
        "move" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),     
    }
        
    def __init__(self, metainfo, listed):
        super(StockPreview, self).__init__()
        self.metainfo = metainfo
        self.listed = listed
        
        self.append(hippo.CanvasText(text=self.metainfo.title, font='Bold 12px'))
        if self.metainfo.thumbnail:
            self.append(CanvasURLImage(self.metainfo.thumbnail))        
        self.append(hippo.CanvasText(text=self.metainfo.description, font="12px", size_mode=hippo.CANVAS_SIZE_WRAP_WORD))
        self.__button = Button(label=(self.listed and 'Remove from Sidebar' or 'Add to Sidebar'))
        self.__button.connect('activated', lambda *args: self.emit('add-remove'))
        self.append(self.__button)
        
        if self.listed:
            self.__up_button = hippo.CanvasLink(text="Up", border_right=10)
            self.__down_button = hippo.CanvasLink(text="Down")
            self.__up_down_controls = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
            self.__up_down_controls.append(self.__up_button)
            self.__up_down_controls.append(self.__down_button)

            self.__up_button.connect("activated", lambda ci: self.emit("move", True))
            self.__down_button.connect("activated", lambda ci: self.emit("move", False))

            self.append(self.__up_down_controls)
        
class StockItem(CanvasVBox):
    def __init__(self, metainfo, listed):
        super(StockItem, self).__init__()
        
        self.metainfo = metainfo
        self.listed = listed
        
        self.set_clickable(True)
        
        self.append(hippo.CanvasText(text=metainfo.title, font='Bold 12px'))
        if metainfo.thumbnail:
            box = CanvasVBox()
            box.append(CanvasURLImage(metainfo.thumbnail, border=1, border_color=0xAAAAAAFF, xalign=hippo.ALIGNMENT_CENTER, yalign=hippo.ALIGNMENT_CENTER))
            self.append(box)

class StockList(OverviewTable):
    __gsignals__ = {
        "selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
    }

    def __init__(self, panel=None):
        super(StockList, self).__init__(padding=6)
        
        self.__panel = panel
        self.__items = {}
        self.__section_counts = {}
        self.__section_headers = {}
        for section in SECTIONS:
            self.__items[section] = {}
            self.__section_counts[section] = 0
            self.__section_headers[section] = self.add_section_head(section, SECTIONS[section])
            self.__section_headers[section].set_visible(False)

        self.__selected_item = None
        self.__search = None
        self.__mgr = self.__panel.get_stock_manager()
        self.__mgr.connect('listings-changed', lambda *args: self.__sync())
        gobject.idle_add(self.__sync)

    def add_stock(self, metainfo, section):
        if metainfo.srcurl in self.__items[section]:
            return
        
        item = StockItem(metainfo, section == LISTED)
        item.connect("button-press-event", self.__on_item_click)
                
        self.add_column_item(section, item, lambda a,b: cmp(a.metainfo.title,b.metainfo.title))
        self.__section_counts[section] += 1
        if self.__section_counts[section] == 1:
            self.__section_headers[section].set_visible(True)
        
        self.__update_visibility(section, item)

        self.__items[section][metainfo.srcurl] = item

    def remove_stock(self, srcurl, section):
        try:
            item = self.__items[section][srcurl]
        except KeyError:
            return
        
        item.destroy()
        del self.__items[section][srcurl]

    def __update_visibility(self, section, item):
        was_visible = item.get_visible()
        
        if self.__search is None:
            visible = True
        else:
            visible = False
            lcsearch = self.__search.lower()
            for str in [item.metainfo.title, item.metainfo.description]:
                lcstr = str.lower()
                if lcstr.find(lcsearch) >= 0:
                    visible = True
                    break

        if visible != was_visible:
            if visible:
                self.__section_counts[section] += 1
                if self.__section_counts[section] == 1:
                    self.__section_headers[section].set_visible(True)
            else:
                self.__section_counts[section] -= 1
                if self.__section_counts[section] == 0:
                    self.__section_headers[section].set_visible(False)
        
            item.set_visible(visible)

    def set_search(self, search):
        if search.strip() == "":
            self.__search = None
        else:
            self.__search = search.lower()

        for section in SECTIONS:
            section_items = self.__items[section]
            for id in section_items:
                self.__update_visibility(section, section_items[id])

    def __select_item(self, item):
        if item == self.__selected_item:
            return
        
        #if self.__selected_item:
        #    self.__selected_item.set_force_prelight(False)
            
        self.__selected_item = item
        #self.__selected_item.set_force_prelight(True)
        self.emit("selected", item.metainfo.srcurl)

    def __on_item_click(self, item, event):
         if event.count == 1:
             self.__select_item(item)
             
    @log_except(_logger)
    def __sync(self):
        all = list(self.__mgr.get_all_builtin_metadata())
        listed = list(self.__mgr.get_listed())
        itemsections = []
        for section in self.__items:
            for srcurl in self.__items[section]:
                itemsections.append((section, srcurl))
        for (section, srcurl) in itemsections:
            self.remove_stock(srcurl, section)
        for metainfo in listed:
            _logger.debug("displaying LISTED stock %s", metainfo.srcurl)
            self.add_stock(metainfo, LISTED)
        for metainfo in all:
            if metainfo not in listed:
                _logger.debug("displaying UNLISTED stock %s", metainfo.srcurl)                
                self.add_stock(metainfo, UNLISTED)
             
class PortfolioManager(hippo.CanvasWindow):
    def __init__(self, panel):
        super(PortfolioManager, self).__init__(gtk.WINDOW_TOPLEVEL)
        
        self.__panel = panel
        self.__mgr = self.__panel.get_stock_manager()        
        self.__mgr.connect('listings-changed', lambda *args: self.__on_listings_change())        
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        

        self.set_title('Widgets')
    
        self.__box = CanvasHBox()
    
        self.__left_box = CanvasVBox(spacing=6, padding=6, box_width=250)
        self.__left_box.set_property('background-color', 0xEEEEEEFF)
        self.__box.append(self.__left_box)
        
        self.__search_text = hippo.CanvasText(text="Search All Widgets:", font="Bold 12px",
                                              color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START)
        self.__left_box.append(self.__search_text)
        self.__search_input = hippo.CanvasEntry()
        #self.__search_input.set_property('border-color', 0xAAAAAAFF)
        self.__search_input.connect("notify::text", self.__on_search_changed)
        self.__search_input.connect("key-press-event", self.__on_search_keypress)
        self.__idle_search_id = 0
        self.__left_box.append(self.__search_input)
        
        self.__profile_box = CanvasVBox(border=1, border_color=0x999999FF, background_color=0xFFFFFFFF)
        self.__left_box.append(self.__profile_box)
        self.__set_profile_stock(None)
        
        self.__left_box.append(hippo.CanvasText(text="Tools", font="Bold 12px",
                                                color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START))
        minimized_box = CanvasHBox()
        self.__minimized_check = CanvasCheckbox("Minimized Sidebar Mode")
        self.__minimized_check.checkbox.connect('toggled', self.__on_minimize_toggled)
        gconf.client_get_default().notify_add(GCONF_KEY_VISIBLE, self.__on_minimize_key_changed)
        self.__on_minimize_key_changed()
        minimized_box.append(self.__minimized_check)
        self.__left_box.append(minimized_box)
        
        theme_box = CanvasHBox()
        theme_box.append(hippo.CanvasText(text='Theme: ', font='12px'))
        model = gtk.ListStore(gobject.TYPE_STRING)
        model.append(['Milky'])
        model.append(['Fedora'])
        self.__theme_combo = CanvasCombo(model)
        theme_box.append(self.__theme_combo, hippo.PACK_EXPAND)
        textrender = gtk.CellRendererText()
        self.__theme_combo.combo.pack_start(textrender, True)
        self.__theme_combo.combo.add_attribute(textrender, 'text', 0)
        self.__theme_combo.combo.connect('notify::active', self.__on_active_theme_changed)
        gconf.client_get_default().notify_add(GCONF_KEY_THEME, self.__on_theme_key_changed)
        self.__on_theme_key_changed()        
        
        self.__left_box.append(theme_box)        
        
        gadget_box = CanvasHBox()
        gadget_box.append(hippo.CanvasText(text='Widget Link: ', font="12px"))
        self.__google_gadget_entry = hippo.CanvasEntry()
        gadget_box.append(self.__google_gadget_entry, hippo.PACK_EXPAND)
        self.__google_gadget_add_button = Button(label='Add')
        gadget_box.append(self.__google_gadget_add_button)
        self.__google_gadget_add_button.connect('activated', self.__on_google_gadget_add)
        self.__left_box.append(gadget_box)
    
        self.__right_scroll = hippo.CanvasScrollbars()
        self.__right_scroll.set_policy(hippo.ORIENTATION_HORIZONTAL,
                                       hippo.SCROLLBAR_NEVER)
        self.__right_box = CanvasVBox(border=0, background_color=0xFFFFFFFF)
        self.__box.append(self.__right_scroll, hippo.PACK_EXPAND)
        
        self.__stock_list = StockList(panel=panel)
        self.__right_box.append(self.__stock_list, hippo.PACK_EXPAND)

        self.__stock_list.connect("selected", self.__on_stock_selected)
        
        self.__right_scroll.set_root(self.__right_box)
        
        self.__preview = None
        self.__set_profile_stock(None) 
        
        self.set_default_size(750, 600)
        self.connect("delete-event", lambda *args: self.__hide_reset() or True)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))
               
        self.set_root(self.__box)
        
    def __on_google_gadget_add(self, but):
        _logger.debug("got add for ggadget")
        url = self.__google_gadget_entry.get_property('text')
        if not url:
            return
        try:
            urlparse.urlparse(url)
        except:
            _logger.debug("invalid URL: %s", url)
            return
        self.__mgr.set_listed(url, True)        

    def __set_profile_stock(self, url):
        self.__profile_box.clear()
        if url is None:
            self.__profile_box.set_property("box-height", 300)
        else:
            self.__profile_box.set_property("box_height", -1)
            metainfo = self.__mgr.load_metainfo(url)
            listed = url in self.__mgr.get_listed_urls()
            pv = StockPreview(metainfo, listed)
            self.__preview = pv
            self.__profile_box.append(pv)
            pv.connect('add-remove', self.__on_item_add_remove)
            pv.connect('move', self.__on_item_move)                        
            
    def __on_item_add_remove(self, item):
        _logger.debug("got addremove for item %s", item.metainfo.srcurl)
        self.__mgr.set_listed(item.metainfo.srcurl, not item.listed)
        
    def __on_item_move(self, item, isup):
        _logger.debug("got move for item %s up: %s", item.metainfo.srcurl, isup)
        self.__mgr.move_listing(item.metainfo.srcurl, isup)        

    def __reset(self):
        self.__search_input.set_property('text', '')

    def __hide_reset(self):
        self.__reset()
        self.hide()

    @log_except(_logger)
    def __idle_do_search(self):
        self.__stock_list.set_search(self.__search_input.get_property("text"))
        self.__idle_search_id = 0
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.__hide_reset()

    def __on_search_changed(self, input, text):
        if self.__idle_search_id > 0:
            return
        self.__idle_search_id = gobject.timeout_add(500, self.__idle_do_search)
        
    def __on_search_keypress(self, entry, event):
        if event.key == hippo.KEY_RETURN:
            #self.__stock_list.select_single_visible_user()
            pass

    def __on_stock_selected(self, list, stock):
         self.__set_profile_stock(stock)

    @log_except(_logger)
    def __on_minimize_key_changed(self, *args):
        _logger.debug("minimize key changed")
        self.__minimized_check.checkbox.set_active(not gconf.client_get_default().get_bool(GCONF_KEY_VISIBLE))  
       
    @log_except(_logger)        
    def __on_minimize_toggled(self, *args):
        _logger.debug("minimize toggled")        
        new_visible = not self.__minimized_check.checkbox.get_active()
        old_visible = gconf.client_get_default().get_bool(GCONF_KEY_VISIBLE)
        if old_visible == new_visible:
            return
        gconf.client_get_default().set_bool(GCONF_KEY_VISIBLE, new_visible)
            
    def __findobj(self, model, obj, colidx=0):
        iter = model.get_iter_first()
        while iter:
            val = model.get_value(iter, colidx)
            if val == obj:
                return iter
            iter = model.iter_next(iter)            
            
    @log_except(_logger)
    def __on_theme_key_changed(self, *args):
        _logger.debug("theme key changed")
        newtheme = gconf.client_get_default().get_string(GCONF_KEY_THEME)        
        iter = self.__findobj(self.__theme_combo.combo.get_property('model'), newtheme)
        if not iter:
            return
        self.__theme_combo.combo.set_active_iter(iter)
        
    def __on_active_theme_changed(self, *args):
        iter = self.__theme_combo.combo.get_active_iter()
        _logger.debug("theme iter changed to %r", iter)
        prevtheme = gconf.client_get_default().get_string(GCONF_KEY_THEME)
        if iter:
            themename = self.__theme_combo.combo.get_property('model').get_value(iter, 0)
        else:
            themename = ''
        if prevtheme == themename:
            return
        gconf.client_get_default().set_string(GCONF_KEY_THEME, themename)
       
    @log_except(_logger)
    def __on_minimize_toggled(self, *args):
        _logger.debug("minimize toggled")        
        new_visible = not self.__minimized_check.checkbox.get_active()
        old_visible = gconf.client_get_default().get_bool(GCONF_KEY_VISIBLE)
        if old_visible != new_visible:
            gconf.client_get_default().set_bool(GCONF_KEY_VISIBLE, new_visible)            

    @log_except(_logger)
    def __on_listings_change(self, *args):
        _logger.debug("got listings change")
        if self.__preview:
            self.__set_profile_stock(self.__preview.metainfo.srcurl)
