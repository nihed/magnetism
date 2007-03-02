import logging

import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage

class AppDisplay(CanvasMugshotURLImage):
    def __init__(self, app):
        CanvasMugshotURLImage.__init__(self, 
                                       scale_width=30,
                                       scale_height=30)
        self.set_app(app)
        
    def set_app(self, app):
        self._app = app
        self._app.connect("changed", lambda app: self._app_display_sync())
        self._app_display_sync()
        
    def _app_display_sync(self):
        self.set_url(self._app.get_icon_url())

class AppsStock(bigboard.AbstractMugshotStock):
    def __init__(self):
        super(AppsStock, self).__init__("Applications")
        
        self._mugshot = mugshot.get_mugshot()

        self._mugshot.connect("top-apps-changed", self._handle_top_apps_changed)
        
        self._mugshot.get_applications()
        
        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)

        self._apps = {}

    def get_content(self, size):
        return self._box
            
    def _handle_top_apps_changed(self, mugshot, apps):
        logging.debug("apps changed")
        self._box.remove_all()
        for app in apps:
            display = AppDisplay(app)
            self._box.append(display)
