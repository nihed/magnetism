import logging, time, re, os, sys

from ddm import DataModel

import gmenu, gobject, pango, gnomedesktop, gtk
import gconf, hippo

import bigboard.globals as globals
import bigboard.global_mugshot as global_mugshot
import bigboard.libbig as libbig
from bigboard.libbig.gutil import *
import bigboard.logins
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasHBox, CanvasVBox, ActionLink
import bigboard.stock

import apps, appbrowser, apps_widgets, apps_directory

from ddm import DataModel

import bigboard.search as search

_logger = logging.getLogger("bigboard.stocks.AppsStock")

GCONF_KEY_APP_SIZE = '/apps/bigboard/application_list_size'
            
class AppDisplayLauncher(apps_widgets.AppDisplay):
    def __init__(self):
        super(AppDisplayLauncher, self).__init__()
  

class AppsStock(bigboard.stock.AbstractMugshotStock):
    STATIC_SET_SIZE = 4
    DYNAMIC_SET_SIZE = 0
    STATIFICATION_TIME_SEC = 60 * 60 #* 24 * 3; # 3 days
    __gsignals__ = {

    }    
    def __init__(self, *args, **kwargs):
        super(AppsStock, self).__init__(*args, **kwargs)

        search.enable_search_provider('apps')

        self.__model = bigboard.globals.get_data_model()

        self.__model.add_connected_handler(self.__on_connected)
        if self.__model.connected:
            self.__on_connected()
        
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
        self._add_more_button(self.__on_more_button)
        
        self.__static_set_ids = {}

        gconf.client_get_default().notify_add(GCONF_KEY_APP_SIZE, self.__on_app_size_changed)
         
        self.__set_message('Loading...')

        bigboard.logins.get_logins().connect('changed', lambda *args: self.__sync())

        self.__repo = apps.get_apps_repo()

        self.__repo.connect('enabled-changed', self.__on_usage_enabled_changed)
        self.__repo.connect('all-apps-loaded', self.__on_all_apps_loaded)
        self.__repo.connect('my-pinned-apps-changed', self.__on_my_pinned_apps_changed)
        self.__repo.connect('my-top-apps-changed', self.__on_my_top_apps_changed)
        self.__repo.connect('global-top-apps-changed', self.__on_global_top_apps_changed)

        self.__sync()

    def __on_connected(self):
        # When we disconnect from the server we freeze existing content, then on reconnect
        # we clear everything and start over.
        _logger.debug("Connected to data model")

    def __on_query_error(self, where, error_code, message):
        _logger.warn("Query '" + where + "' failed, code " + str(error_code) + " message: " + str(message))

    def __on_usage_enabled_changed(self, repo):
        _logger.debug("usage enabled changed")
        self.__sync()

    def __on_all_apps_loaded(self, repo):
        _logger.debug("all apps are loaded")
        self.__sync()

    def __on_my_pinned_apps_changed(self, repo, pinned_apps):
        _logger.debug("Pinned apps changed from apps repo: " + str(pinned_apps))
        self.__sync()

    def __on_my_top_apps_changed(self, repo, my_top_apps):
        _logger.debug("My top apps changed from apps repo: " + str(my_top_apps))
        self.__sync()

    def __on_global_top_apps_changed(self, repo, global_top_apps):
        _logger.debug("Global top apps changed from apps repo: " + str(global_top_apps))
        self.__sync()
        
    def __on_app_size_changed(self, *args):
        _logger.debug("app size changed")  
        self.__sync()

    def __on_more_button(self):
        _logger.debug("more!")
        if self.__app_browser is None:
            self.__app_browser = appbrowser.AppBrowser()  
        if self.__app_browser.get_property('is-active'):
            self.__app_browser.hide()
        else:
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

    def __fill_static_set(self):
        self.__static_set.remove_all()
        self.__static_set_ids = {}

        usage = self.__repo.get_app_usage_enabled()
        pinned_apps = self.__repo.get_pinned_apps()
        global_top_apps = self.__repo.get_global_top_apps()
        static_size = gconf.client_get_default().get_int(GCONF_KEY_APP_SIZE) or 7

        self.__set_subtitle(None)
        apps_in_set = []
        if usage:
            apps_in_set = pinned_apps
        if len(apps_in_set) == 0:
            apps_in_set = global_top_apps
            self.__set_subtitle("Popular Applications")

        ## note the "-" in front of the cmp to sort descending
        apps_in_set.sort(lambda a, b: - cmp(a.get_usage_count(), b.get_usage_count()))
   
        for i, app in enumerate(apps_in_set):
            if i >= static_size:
                break

            # don't display apps that are not installed if the user is not logged in;
            # because the user should be able to see the same list regardless of whether
            # they are connected, we don't check self.__model.connected here
            if not self.__model.self_id and not app.is_installed():
                continue

            display = apps_widgets.AppDisplay(apps_widgets.AppLocation.STOCK, app)
            display.connect("button-press-event", lambda display, event: display.launch()) 
            #_logger.debug("setting static set app: %s", app)
            self.__static_set.append(display)
            self.__static_set_ids[app.get_id()] = True

    @defer_idle_func(logger=_logger)
    def __sync(self):
        #_logger.debug("doing sync")
        
        self.__set_message(None)        
             
        self.__box.set_child_visible(self.__dynamic_set, False)

        usage = self.__repo.get_app_usage_enabled()

        #_logger.debug("usage: %s", usage)

        if usage is False and self.__model.connected:
            self.__set_message("Enable application tracking", 
                               globals.get_baseurl() + "/account")        

        self.__fill_static_set()

        self.__repo.pin_stuff_if_we_have_none()
