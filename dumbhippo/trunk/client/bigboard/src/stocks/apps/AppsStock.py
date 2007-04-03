import logging, time

import gmenu, gobject, pango, gnomedesktop
import hippo

import bigboard, mugshot, libbig
from big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasHBox, CanvasVBox, ActionLink

import appbrowser, apps_widgets, apps_directory

class Application(gobject.GObject):
    __gsignals__ = {
        "changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
    }    
    def __init__(self, app):
        super(Application, self).__init__()
        self.__app = app
        self.__install_checked = False
        self.__desktop_entry = None
        ad = apps_directory.get_app_directory()            
        ad.connect("changed", lambda ad: self.__recheck_installed())        
        
    def get_id(self):
        return self.__app.get_id()
        
    def get_mugshot_app(self):
        return self.__app
        
    def __lookup_desktop(self):
        names = self.__app.get_desktop_names()        
        for name in names.split(';'):
            ad = apps_directory.get_app_directory()            
            menuitem = None
            try:
                menuitem = ad.lookup(name)
            except KeyError, e:
                continue
            entry_path = menuitem.get_desktop_file_path()
            desktop = gnomedesktop.item_new_from_file(entry_path, 0)
            if desktop:
                return desktop
        return None
        
    def is_installed(self):
        if not self.__install_checked:
            self.__recheck_installed()            
        self.__install_checked = True               
        return self.__desktop_entry is not None      
   
    def __recheck_installed(self):
        old_installed = self.__desktop_entry is not None
        self.__desktop_entry = self.__lookup_desktop()
        if (self.__desktop_entry is not None) != old_installed:
            self.emit("changed")
            
    def launch(self):
        if self.__desktop_entry:
            self.__desktop_entry.launch()
            
class AppDisplayLauncher(apps_widgets.AppDisplay):
    def __init__(self):
        super(AppDisplayLauncher, self).__init__()
  

class AppsStock(bigboard.AbstractMugshotStock):
    STATIC_SET_SIZE = 7
    DYNAMIC_SET_SIZE = 7    
    STATIFICATION_TIME_SEC = 60 * 60 #* 24 * 3; # 3 days
    def __init__(self, *args, **kwargs):
        super(AppsStock, self).__init__(*args, **kwargs)        
        
        self.__box = CanvasVBox(spacing=3)
        self.__message = hippo.CanvasText()
        self.__message_link = ActionLink()
        self.__message_link.connect("button-press-event", lambda link, event: self.__on_message_link())
        self.__message_link_url = None
        self.__subtitle = hippo.CanvasText(font="Bold 12px")
        self.__static_set = CanvasVBox()
        self.__dynamic_set = CanvasVBox()
        
        self.__box.append(self.__message)
        self.__box.append(self.__message_link)        
        self.__box.append(self.__subtitle)        
        
        self.__box.append(self.__static_set)
        self.__box.append(self.__dynamic_set)        
        self.__box.set_child_visible(self.__dynamic_set, False)
        
        self.__app_browser = None
        self._add_more_link(self.__on_more_link)

        self._mugshot.connect("global-top-apps-changed", lambda mugshot, apps: self.__sync())  
        self._mugshot.connect("my-top-apps-changed", lambda mugshot, apps: self.__sync())      
        self._mugshot.connect("pinned-apps-changed", lambda mugshot, apps: self.__sync())        
        self._mugshot.connect("pref-changed", lambda mugshot, key, value: self.__handle_pref_change(key, value))          
        
        self.__usage_enabled = False
        
        self.__static_set_ids = {}
        self.__set_message('Loading...')
        
        self.__apps = {} # mugshot app -> app
        
    def __on_more_link(self):
        self._logger.debug("more!")
        if self.__app_browser is None:
            self.__app_browser = appbrowser.AppBrowser(self)            
        self.__app_browser.present()
        
    def __on_message_link(self):
        libbig.show_url(self.__message_link_url)
        
    def __set_message(self, text, link=None):
        self.__box.set_child_visible(self.__message, (not text is None) and link is None)
        self.__box.set_child_visible(self.__message_link, not (text is None or link is None))        
        if text:
            self.__message.set_property("text", text)
            self.__message_link.set_property("text", text)
        if link:
            self.__message_link_url = link
            
    def __set_subtitle(self, text):
        self.__box.set_child_visible(self.__subtitle, not text is None)
        if text:
            self.__subtitle.set_property("text", text)        

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

    def __handle_pref_change(self, key, value):          
        if key != 'applicationUsageEnabled':
            return
        self._logger.debug("handling %s pref change: %s", key, value)              
        self.__sync()
            
    def __on_pinned_apps_success(self, pinned_ids):
        self._logger.debug("app pin set succeeded")       
        self._mugshot.get_pinned_apps(force=True)
            
    def __set_dynamic_set(self, mugshot_apps):
        self.__dynamic_set.remove_all()        
        i = 0
        for mugshot_app in mugshot_apps or []:
            app = self.get_app(mugshot_app)
            if self.__static_set_ids.has_key(app.get_id()):
                continue
            if i > self.DYNAMIC_SET_SIZE:
                break
            self._logger.debug("setting dynamic app: %s", app)            
            i += 1
            display = apps_widgets.AppDisplay(app)
            display.connect("button-press-event", lambda display, event: display.launch())             
            self.__dynamic_set.append(display)
        if i > 0:
            self.__box.set_child_visible(self.__dynamic_set,True)
                        
    def get_app(self, mugshot_app):
        if not self.__apps.has_key(mugshot_app.get_id()):
            self.__apps[mugshot_app.get_id()] = Application(mugshot_app)
        return self.__apps[mugshot_app.get_id()]
        
    def __sync(self):
        self._logger.debug("doing sync")
        
        self.__set_message(None)        
             
        self.__box.set_child_visible(self.__dynamic_set, False)
        self.__static_set.remove_all()
        self.__static_set_ids = {}
        for mugshot_app in self._mugshot.get_pinned_apps() or []:
            app = self.get_app(mugshot_app)
            display = apps_widgets.AppDisplay(app)
            display.connect("button-press-event", lambda display, event: display.launch()) 
            self._logger.debug("setting pinned app: %s", app)
            self.__static_set.append(display)
            self.__static_set_ids[app.get_id()] = True   
        self._mugshot.set_my_apps_poll_frequency(30 * 60) # 30 minutes
        usage = self._mugshot.get_pref('applicationUsageEnabled')
        self._logger.debug("usage: %s", usage)
        if usage is False:
            self.__set_message("Enable application tracking", 
                               self._mugshot.get_baseurl() + "/account")
        
        if not self._mugshot.get_pinned_apps() and usage:          
            self.__set_message("Finding your application list...")
            self._mugshot.set_my_apps_poll_frequency(3 * 60) # 3 minutes                
            if len(self.__static_set.get_children()) == 0 and self._mugshot.get_my_app_usage_start():
                self._logger.debug("no static set")           
                app_stalking_duration = (time.mktime(time.gmtime())) - (int(self._mugshot.get_my_app_usage_start())/1000) 
                self._logger.debug("comparing stalking duration %s to statification time %s", app_stalking_duration, self.STATIFICATION_TIME_SEC)                  
                if app_stalking_duration > self.STATIFICATION_TIME_SEC and self._mugshot.get_my_top_apps(): 
                    # We don't have a static set yet, time to make it
                    pinned_ids = []
                    for app in self._mugshot.get_my_top_apps():
                        if len(pinned_ids) >= self.STATIC_SET_SIZE:
                            break
                        pinned_ids.append(app.get_id())
                    # FIXME - we need to retry if this fails for some reason                    
                    # this is generally true of any state setting
                    self.__set_message("Saving your applications...")
                    self._logger.debug("creating initial pin set: %s", pinned_ids)
                    self._mugshot.set_pinned_apps(pinned_ids, lambda: self.__on_pinned_apps_success(pinned_ids))       
                    
        i = 0
        self.__set_dynamic_set(self._mugshot.get_my_top_apps() or self._mugshot.get_global_top_apps())
        self.__set_subtitle(None)        
        if (not self._mugshot.get_my_top_apps()) and self._mugshot.get_global_top_apps():
            self.__set_subtitle("Popular Applications")     
    