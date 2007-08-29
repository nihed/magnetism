import os, sys, re, logging

import gtk, gobject, gnomeapplet, gconf

_logger = logging.getLogger("bigboard.Deskbar")

import deskbar
try:
	from deskbar.core.CoreImpl import CoreImpl
	_logger.debug("using new deskbar")
	new_deskbar = True
	from bigboard.bigbar.EmbedView import EmbedView
	from bigboard.bigbar.EmbedController import EmbedController
except ImportError, e:
	new_deskbar = False
	_logger.debug("trying old deskbar", exc_info=True)
	try:
		import deskbar.DeskbarApplet as DeskbarApplet
	except ImportError, e2:
		import deskbar.ui.DeskbarApplet as DeskbarApplet

class AbstractDeskbar(gtk.VBox):
    __gsignals__ = {
        "match-selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, [])
    }
    def __init__(self, **kwargs):
        super(AbstractDeskbar, self).__init__(**kwargs)

	def focus(self):
		raise NotImplementedError()

class OldDeskbar(AbstractDeskbar):
    def __init__(self, **kwargs):
        super(OldDeskbar, self).__init__(**kwargs)
        self.__applet = gnomeapplet.Applet()
        self.__applet.get_orient = lambda: gnomeapplet.ORIENT_DOWN
        self.__deskbar = DeskbarApplet.DeskbarApplet(self.__applet)
        self.__deskbar.loader.connect("modules-loaded", self.__override_modules_loaded)
        self.__applet.reparent(self)
        uiname = gconf.Value(gconf.VALUE_STRING)
        uiname.set_string(deskbar.ENTRIAC_UI_NAME)
        self.__deskbar.on_ui_changed(uiname)
        self.__deskbar.ui.connect('match-selected', lambda *args: self.emit('match-selected'))

    def __override_modules_loaded(self, loader):
        _logger.debug("got modules loaded")        
        gobject.idle_add(self.__idle_override_modules_loaded)

    def __idle_override_modules_loaded(self):
        _logger.debug("idle override modules")
        enabled_handlers = gconf.Value(gconf.VALUE_LIST)
        def make_str_val(s):
            v = gconf.Value(gconf.VALUE_STRING)
            v.set_string(s)
            return v
        enabled_handlers.set_list_type(gconf.VALUE_STRING)
        enabled_handlers.set_list(map(make_str_val, ['ProgramsHandler', 'MozillaBookmarksHandler', 'YahooHandler']))
        self.__deskbar.on_config_handlers(enabled_handlers)  
        self._logger.debug("idle override modules complete")

    def focus(self):
        self.__deskbar.on_keybinding_button_press(None, gtk.get_current_event_time())

class NewDeskbar(AbstractDeskbar):
	def __init__(self, **kwargs):
		super(NewDeskbar, self).__init__(**kwargs)

		self.__model = CoreImpl(deskbar.MODULES_DIRS)
		self.__model.run()
		self.__controller = EmbedController(self.__model)
		self.__view = EmbedView(self.__controller, self.__model)

		self.add(self.__view.get_entry())

	def focus(self):
		self.__view.get_entry().grab_focus()
		
if new_deskbar:
	Deskbar = NewDeskbar
else:
	Deskbar = OldDeskbar

