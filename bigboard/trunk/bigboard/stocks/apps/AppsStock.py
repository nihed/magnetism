import logging, time, re, os, sys

import gmenu, gobject, pango, gnomedesktop, gtk
import hippo

import bigboard.global_mugshot as global_mugshot
import bigboard.libbig as libbig
import bigboard.logins
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasHBox, CanvasVBox, ActionLink
import bigboard.stock

import appbrowser, apps_widgets, apps_directory

class WebApplication(object):
    def __init__(self, url, name, description, iconurl):
        self.url = url
        self.name = name
        self.description = description
        self.iconurl = iconurl
        
    def get_id(self):
        return self.url
    
    def get_name(self):
        return self.name
    
    def get_generic_name(self):
        return self.get_description()
    
    def get_icon_url(self):
        return self.iconurl
    
    def get_local_pixbuf(self):
        return None
    
    def get_description(self):
        return self.description
    
    def get_category(self):
        return "Web"
    
    def is_installed(self):
        return True
    
    def launch(self):
        return libbig.show_url(self.url)

WEB_APPS = {'gmail.com': WebApplication('http://mail.google.com', 'GMail', 'Mail', 'http://mail.google.com/mail/images/favicon.ico')}
        
class Application(object):
    __slots__ = ['__app', '__install_checked', '__desktop_entry', '__menu_entry']
    def __init__(self, mugshot_app=None, menu_entry=None):
        super(Application, self).__init__()
        self.__app = mugshot_app
        self.__install_checked = False
        self.__desktop_entry = None
        self.__menu_entry = menu_entry      
        
    def get_id(self):
        return self.__app.get_id()
        
    def get_mugshot_app(self):
        return self.__app

    def get_menu(self):
        return self.__menu_entry

    def get_desktop(self):
        return self.__desktop_entry
    
    def get_name(self):
        return self.__app and self.__app.get_name() or self.__menu_entry.get_name()
    
    def get_description(self):
        return self.__app and self.__app.get_description() or ""

    def get_tooltip(self):
        return self.__app and self.__app.get_tooltip() or ""
    
    def get_generic_name(self):
        return self.__app and self.__app.get_generic_name() or ""

    def get_category(self):
        return self.__app and self.__app.get_category() or "Other"

    def get_local_category(self):
        return ((self.__menu_entry and self.__menu_entry.parent) and self.__menu_entry.parent.get_name()) or "Other"

    def get_comment(self):
        return self.__desktop_entry and self.__desktop_entry.get_localestring('Comment')

    def get_is_excluded(self):
        return self.__menu_entry and self.__menu_entry.get_is_excluded()
        
    def get_icon_url(self):
        return self.__app and self.__app.get_icon_url()
        
    def get_local_pixbuf(self):
        if self.__desktop_entry:
            # strip off .png
            icon_name = re.sub(r'\.[a-z]+$','', self.__menu_entry.get_icon()) 
            theme = gtk.icon_theme_get_default()
            try:
                pixbuf = theme.load_icon(icon_name, 48, 0)
            except gobject.GError, e:
                return None
            return pixbuf
        return None        

    def set_app(self, app):
        self.__app = app

    def __lookup_desktop(self):
        if self.__menu_entry:
            entry_path = self.__menu_entry.get_desktop_file_path()
            try:
                desktop = gnomedesktop.item_new_from_file(entry_path, 0)
                if desktop:
                    return desktop            
            except gobject.GError, e:
                desktop = None
        names = self.__app.get_desktop_names()        
        for name in names.split(';'):
            ad = apps_directory.get_app_directory()            
            menuitem = None
            try:
                menuitem = ad.lookup(name)
            except KeyError, e:
                pass
                
            if menuitem and not self.__menu_entry:
                self.__menu_entry = menuitem
                entry_path = menuitem.get_desktop_file_path()
            else:
                entry_path = None
                for dir in libbig.get_xdg_datadirs():
                    appdir = os.path.join(dir, 'applications')
                    path = os.path.join(appdir, name + '.desktop') 
                    if os.access(path, os.R_OK):
                        entry_path = path
                        break
                                                   
            if not entry_path:
                continue
            desktop = gnomedesktop.item_new_from_file(entry_path, 0)
            if desktop:
                return desktop
        return None

    def is_installed(self):
        if not self.__install_checked:
            self.recheck_installed()            
        self.__install_checked = True               
        return self.__desktop_entry is not None      
   
    def recheck_installed(self):
        old_installed = self.__desktop_entry is not None
        self.__desktop_entry = self.__lookup_desktop()
            
    def launch(self):
        if self.__desktop_entry:
            self.__desktop_entry.launch([])
        else:
            global_mugshot.get_mugshot().install_application(self.__app.get_id(), self.__app.get_package_names(), self.__app.get_desktop_names())            

    def __str__(self):
        return "App: %s mugshot: %s menu: %s local: %s" % (self.get_name(), self.__app, self.__menu_entry,
                                                           self.__desktop_entry)
            
class AppDisplayLauncher(apps_widgets.AppDisplay):
    def __init__(self):
        super(AppDisplayLauncher, self).__init__()
  

class AppsStock(bigboard.stock.AbstractMugshotStock):
    WEBAPPS_SET_SIZE = 3
    STATIC_SET_SIZE = 7
    DYNAMIC_SET_SIZE = 1
    STATIFICATION_TIME_SEC = 60 * 60 #* 24 * 3; # 3 days
    __gsignals__ = {
        "all-apps-loaded" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
    }    
    def __init__(self, *args, **kwargs):
        super(AppsStock, self).__init__(*args, **kwargs)        
        
        self.__box = CanvasVBox(spacing=3)
        self.__message = hippo.CanvasText()
        self.__message_link = ActionLink()
        self.__message_link.connect("button-press-event", lambda link, event: self.__on_message_link())
        self.__message_link_url = None
        self.__subtitle = hippo.CanvasText(font="Bold 12px")
        self.__web_set = CanvasVBox()
        self.__static_set = CanvasVBox()
        self.__dynamic_set = CanvasVBox()
        
        self.__box.append(self.__message)
        self.__box.append(self.__message_link)        
        self.__box.append(self.__subtitle)        
 
        self.__box.append(self.__web_set)
        self.__box.append(self.__static_set)
        self.__box.append(self.__dynamic_set)        
        self.__box.set_child_visible(self.__dynamic_set, False)
        
        self.__app_browser = None
        self._add_more_button(self.__on_more_button)

        self._mugshot.connect("all-apps-loaded", lambda mugshot: self.__merge_apps())  
        self._mugshot.connect("global-top-apps-changed", lambda mugshot, apps: self.__sync())  
        self._mugshot.connect("my-top-apps-changed", lambda mugshot, apps: self.__sync())      
        self._mugshot.connect("pinned-apps-changed", lambda mugshot, apps: self.__sync())        
        self._mugshot.connect("pref-changed", lambda mugshot, key, value: self.__handle_pref_change(key, value))          
        
        self.__usage_enabled = False
        
        self.__static_set_ids = {}
        self.__set_message('Loading...')
        
        self.__apps = {} # mugshot app -> app
        # apps installed locally and not known in Mugshot
        self.__local_apps = {} # desktop -> app

        bigboard.logins.get_logins().connect('changed', lambda *args: self.__sync())

        ad = apps_directory.get_app_directory()
        ad.connect('changed', self.__on_local_apps_changed)
        for app in ad.get_apps():
            self.get_local_app(app)
        self.__sync()
            
    def __on_local_apps_changed(self, ad):
        for id,app in self.__apps.iteritems():
            app.recheck_installed()
        self.__sync()

    def __on_more_button(self):
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

    def _on_mugshot_initialized(self):
        super(AppsStock, self)._on_mugshot_initialized()
        self._mugshot.get_global_top_apps()        
        self._mugshot.request_all_apps()

    def _on_mugshot_ready(self):
        super(AppsStock, self)._on_mugshot_ready()
        self._mugshot.get_pinned_apps()        
        self._mugshot.get_my_top_apps()

    def get_authed_content(self, size):
        return self.__box

    def get_unauthed_content(self, size):
        return self.__box
            
    def __set_item_size(self, item, size):
        if size == bigboard.stock.Stock.SIZE_BULL:
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
        for i, mugshot_app in enumerate(mugshot_apps or []):
            app = self.get_app(mugshot_app)
            if self.__static_set_ids.has_key(app.get_id()):
                continue
            if app.get_is_excluded():
                continue
            if i >= self.DYNAMIC_SET_SIZE:
                break
            if not app.is_installed():
                continue            
            self._logger.debug("setting dynamic app: %s", app)            
            display = apps_widgets.AppDisplay(app)
            display.connect("button-press-event", lambda display, event: display.launch())             
            self.__dynamic_set.append(display)
        if mugshot_apps:
            self.__box.set_child_visible(self.__dynamic_set,True)
                        
    def get_app(self, mugshot_app):
        if not self.__apps.has_key(mugshot_app.get_id()):
            ad = apps_directory.get_app_directory()
            for desktop_name in mugshot_app.get_desktop_names().split(';'):
                try:
                    target_menuitem = ad.lookup(desktop_name)
                except KeyError, e:
                    continue
                if self.__local_apps.has_key(target_menuitem.get_name()):
                    self._logger.debug("moving app %s from local to apps", target_menuitem.get_name())
                    existing_app = self.__local_apps[target_menuitem.get_name()]
                    del self.__local_apps[target_menuitem.get_name()]
                    existing_app.set_app(mugshot_app)
                    self.__apps[mugshot_app.get_id()] = existing_app
                    return existing_app
            self._logger.debug("creating app %s", mugshot_app.get_id())
            self.__apps[mugshot_app.get_id()] = Application(mugshot_app=mugshot_app)
        return self.__apps[mugshot_app.get_id()]

    def get_all_apps(self):
        return self.__apps.itervalues()

    def get_local_apps(self):
        return self.__local_apps.itervalues()
    
    def get_local_app(self, menu):
        if self.__local_apps.has_key(menu.get_name()):
            return self.__local_apps[menu.get_name()]
        app = None
        for appvalue in self.__apps.itervalues():
            if not appvalue.get_menu():
                continue
            if appvalue.get_menu().get_name() == menu.get_name():
                app = appvalue
                break
        if app is None:
            app = Application(menu_entry=menu)
            self._logger.debug("creating local app %s", menu.get_name())
            self.__local_apps[menu.get_name()] = app
        return app

    def __merge_apps(self):
        for app in self._mugshot.get_all_apps():
            self.get_app(app)
        self.emit("all-apps-loaded")
        
    def __sync(self):
        self._logger.debug("doing sync")
        
        self.__set_message(None)        
             
        self.__box.set_child_visible(self.__dynamic_set, False)
        
        self.__web_set.remove_all()
        for domain,login in bigboard.logins.get_logins().iter_logins():
            if WEB_APPS.has_key(domain):
                display = apps_widgets.AppDisplay(WEB_APPS[domain])
                display.connect("button-press-event", lambda display, event: display.launch())    
                self.__web_set.append(display)
        
        self.__static_set.remove_all()
        self.__static_set_ids = {}
        for i, mugshot_app in enumerate(self._mugshot.get_pinned_apps() or []):
            if i > self.STATIC_SET_SIZE:
                break
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
    
