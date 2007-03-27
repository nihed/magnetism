import logging, time

import gmenu, gobject, pango, gnomedesktop
import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasHBox, CanvasVBox, ActionLink

import appbrowser, apps_widgets

class HideableBox(hippo.CanvasBox):
    def __init__(self, hidetext, showtext, **kwargs):
        if not kwargs.has_key('spacing'):
            kwargs['spacing'] = 4
        hippo.CanvasBox.__init__(self, **kwargs)
        
        self.__shown = True
        self.__hidetext = hidetext
        self.__showtext = showtext
        self.__text = hippo.CanvasLink(xalign=hippo.ALIGNMENT_CENTER)
        self.__text.connect("button-press-event", lambda text, event: self.__toggle_show())
        
        self.append(self.__text)
        
        self.__content = None
        
        self.__sync_show()
        
    def set_hidetext(self, text):
        self.__hidetext = text
        self.__sync_show()
        
    def set_showtext(self, text):
        self.__showtext = text
        self.__sync_show()
                
    def set_content(self, content):
        assert(self.__content is None)
        self.__content = content
        self.append(self.__content)
        
    def set_shown(self, shown):
        shown = not not shown
        if shown != self.__shown:
            self.__toggle_show()
        
    def __toggle_show(self):
        assert(not self.__content is None)        
        self.__shown = not self.__shown
        self.__sync_show()
        
    def __sync_show(self):
        self.__text.set_property("text", (self.__shown and self.__hidetext) or self.__showtext)
        if self.__content:
            self.set_child_visible(self.__content, self.__shown)
            
class AppDisplayLauncher(apps_widgets.AppDisplay):
    def __init__(self):
        super(AppDisplayLauncher, self).__init__()
  

class AppsStock(bigboard.AbstractMugshotStock):
    STATIC_SET_SIZE = 7
    DYNAMIC_SET_SIZE = 7    
    STATIFICATION_TIME_SEC = 60 * 60 * 24 * 3; # 3 days
    def __init__(self, *args, **kwargs):
        super(AppsStock, self).__init__(*args, **kwargs) 
                
        self.__initialized = False           
        
        self.__box = CanvasVBox(spacing=3)
        self.__message = hippo.CanvasText()
        self.__static_set = CanvasVBox()
        self.__dynamic_set = CanvasVBox()
        
        self.__box.append(self.__message)
        self.__box.append(self.__static_set)
        
        self.__app_browser = None
        self._add_more_link(self.__on_more_link)
        
        self._mugshot.connect("my-top-apps-changed", self.__handle_my_top_apps_changed)      
        self._mugshot.connect("pinned-apps-changed", self.__handle_pinned_apps_changed)        
        
        self.__static_set_ids = {}
        self.__set_message('Loading...')
        
    def __on_more_link(self):
        self._logger.debug("more!")
        if self.__app_browser is None:
            self.__app_browser = appbrowser.AppBrowser()            
        self.__app_browser.present()
        
    def __set_message(self, text):
        self.__box.set_child_visible(self.__message, not text is None)
        if text:
            self.__message.set_property("text", text)

    def _on_mugshot_ready(self):
        super(AppsStock, self)._on_mugshot_ready()
        self._mugshot.get_pinned_apps()        
        self._mugshot.get_my_top_apps()

    def get_authed_content(self, size):
        return self.__box
            
    def __set_item_size(self, item, size):
        if size == bigboard.Stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        item.set_size(size)            
            
    def set_size(self, size):
        super(AppsStock, self).set_size(size)
        for child in self.__static_set.get_children() + self.__dynamic_set.get_children():
            self.__set_item_size(child, size)        
            
    def __handle_pinned_apps_changed(self, mugshot, apps):
        self._logger.debug("pinned apps changed: %s", apps)
        self.__static_set.remove_all()
        self.__static_set_ids = {}
        for app in apps:
            display = apps_widgets.AppDisplay(app)
            display.connect("button-press-event", lambda display, event: display.launch()) 
            self.__static_set.append(display)           
            self.__static_set_ids[app.get_id()] = True
            
        if not self.__initialized:
            self.__initialized = True
            
    def __on_pinned_apps_success(self, pinned_ids):
        self._logger.debug("app pin set succeeded")       
        pinned = []
        for id in pinned_ids:
            pinned.append(self._mugshot.get_app(id))
        self.__handle_pinned_apps_changed(None, pinned)
            
    def __handle_my_top_apps_changed(self, mugshot, apps):
        self._logger.debug("my apps changed")
        
        if self._mugshot.get_my_app_usage_start():
            app_stalking_duration = (time.mktime(time.gmtime())) - (int(self._mugshot.get_my_app_usage_start())/1000) 
            self._logger.debug("comparing stalking duration %s to statification time %s", app_stalking_duration, self.STATIFICATION_TIME_SEC)            
            if app_stalking_duration <= self.STATIFICATION_TIME_SEC:
                self.__set_message("Building application list...")
            else:
                self.__set_message(None) 
            if len(self.__static_set.get_children()) == 0:
                self._logger.debug("no static set")
                if app_stalking_duration > self.STATIFICATION_TIME_SEC:                    
                    # We don't have a static set yet, time to make it
                    pinned_ids = []
                    for app in apps:
                        if len(pinned_ids) >= self.STATIC_SET_SIZE:
                            break
                        pinned_ids.append(app.get_id())
                    # FIXME - we need to retry if this fails for some reason                    
                    # this is generally true of any state setting
                    self._logger.debug("creating initial pin set: %s", pinned_ids)
                    self._mugshot.set_pinned_apps(pinned_ids, self.__on_pinned_apps_success(pinned_ids))
        
        self.__dynamic_set.remove_all()
        i = 0
        for app in apps:
            if self.__static_set_ids.has_key(app.get_id()):
                continue
            if i > self.DYNAMIC_SET_SIZE:
                break
            i += 1
            display = apps_widgets.AppDisplay(app)
            display.connect("button-press-event", lambda display, event: display.launch())             
            self.__dynamic_set.append(display)
            
