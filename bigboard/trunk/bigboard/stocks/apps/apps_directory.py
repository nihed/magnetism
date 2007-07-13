import logging, time

import gmenu, gobject, pango, gnomedesktop

class AppDirectory(gobject.GObject):
    __gsignals__ = {
        "changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
    }
    def __init__(self):
        gobject.GObject.__init__(self)
        self._logger = logging.getLogger('bigboard.AppsDirectory')
        self._tree = gmenu.lookup_tree('applications.menu', gmenu.FLAGS_INCLUDE_EXCLUDED)
        self._apps = {}
        # with gnome-menus-2.16.0-2.fc6 omitting the user_data arg crashes the gmenu module
        self._tree.add_monitor(self._on_apps_changed, None)
        self.__last_local_app_update = 0.0
        self.__last_local_app_idle_id = 0
        self._on_apps_changed(None, None)
        
    def _append_directory(self, directory):
        for child in directory.contents:
            if isinstance(child, gmenu.Directory):
                self._append_directory(child)
                continue
            
            if not isinstance(child, gmenu.Entry):
                continue
            
            self._apps[child.desktop_file_id] = child
            
    def _on_apps_changed(self, tree, data):
        self._logger.debug("installed apps changed")
        self._apps = {} 
        self._append_directory(self._tree.root)
        self._logger.debug("app read complete (%d apps)", len(self._apps.keys()))
        curtime = time.time()
        if self.__last_local_app_update < curtime-10:
            self.__last_local_app_update = curtime
            self._logger.debug("no changes before in last 10 secs, emitting changed now")            
            self.emit('changed')
        elif self.__last_local_app_idle_id == 0:
            self._logger.debug("queuing idle change emit")                  
            self.__last_local_app_idle_id = gobject.timeout_add(7000, self.__idle_handle_local_apps_changed)
                        
    def __idle_handle_local_apps_changed(self):
        self._logger.debug("doing idle local app change")
        self.__last_local_app_idle_id = 0
        self.__last_local_app_update = time.time()
        self.emit('changed')
        
    def get_apps(self):
        return self._apps.itervalues()
        
    def lookup(self, desktop_name):
        if not (desktop_name[-8:] == '.desktop'):
            desktop_name += '.desktop'
        return self._apps[desktop_name]
    
_app_directory = None
def get_app_directory():
    global _app_directory
    if _app_directory is None:
        _app_directory = AppDirectory()
    return _app_directory
