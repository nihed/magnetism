import os, sys, re, logging

import gtk, gobject, gnomeapplet, gconf

import deskbar, deskbar.DeskbarApplet

class Deskbar(gtk.VBox):
    def __init__(self, **kwargs):
        super(Deskbar, self).__init__(**kwargs)

        self._logger = logging.getLogger("bigboard.Deskbar")

        self.__applet = gnomeapplet.Applet()
        self.__applet.get_orient = lambda: gnomeapplet.ORIENT_DOWN
        self.__deskbar = deskbar.DeskbarApplet.DeskbarApplet(self.__applet)
        self.__deskbar.loader.connect("modules-loaded", self.__override_modules_loaded)
        self.__applet.reparent(self)
        uiname = gconf.Value(gconf.VALUE_STRING)
        uiname.set_string(deskbar.ENTRIAC_UI_NAME)
        self.__deskbar.on_ui_changed(uiname)

    def __override_modules_loaded(self, loader):
        self._logger.debug("got modules loaded")        
        gobject.idle_add(self.__idle_override_modules_loaded)

    def __idle_override_modules_loaded(self):
        self._logger.debug("idle override modules")
        enabled_handlers = gconf.Value(gconf.VALUE_LIST)
        def make_str_val(s):
            v = gconf.Value(gconf.VALUE_STRING)
            v.set_string(s)
            return v
        enabled_handlers.set_list_type(gconf.VALUE_STRING)
        enabled_handlers.set_list(map(make_str_val, ['ProgramsHandler', 'MozillaBookmarksHandler', 'YahooHandler']))
        self.__deskbar.on_config_handlers(enabled_handlers)  
        self._logger.debug("idle override modules complete")

    def get_deskbar(self):
        return self.__deskbar

    def focus(self):
        self.__deskbar.on_keybinding_button_press(None, gtk.get_current_event_time())
        
       
