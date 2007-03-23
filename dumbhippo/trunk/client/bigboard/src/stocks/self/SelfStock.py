import logging, os

import hippo

import mugshot
import libbig
from bigboard import Stock, AbstractMugshotStock
from big_widgets import CanvasMugshotURLImage, PhotoContentItem

class ExternalAccountIcon(CanvasMugshotURLImage):
    def __init__(self, acct):
        CanvasMugshotURLImage.__init__(self)
        self.connect("button-press-event", lambda img,event: self.__launch_browser())
        self.set_acct(acct)
        
    def set_acct(self, acct):
        self._acct = acct
        self._acct.connect("changed", self._sync)
        self._sync()
        
    def _sync(self):
        self.set_url(self._acct.get_icon())
        
    def __launch_browser(self):
        libbig.show_url(self._acct.get_link())

class SelfStock(AbstractMugshotStock):
    """Shows a user's Mugshot personal information."""
    def __init__(self, *args, **kwargs):
        super(SelfStock,self).__init__(*args, **kwargs)

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)

        self._namephoto_box = PhotoContentItem()
        
        self._photo = CanvasMugshotURLImage(scale_width=48, scale_height=48)
        self.connect_mugshot_handler(self._photo, "button-press-event", lambda button, event: self._on_edit_self())           
        #self._photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
            
        self._namephoto_box.set_photo(self._photo)
        
        self._name = hippo.CanvasText()
        self.connect_mugshot_handler(self._name, "button-press-event", lambda button, event: self._on_edit_self())        
        self._namephoto_box.set_child(self._name)        
        
        self._box.append(self._namephoto_box)
        
        self._whereim_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL, spacing=2)
        self._whereim_box.set_property("padding-top", 4)
        
        self._box.append(self._whereim_box)
        
    def _on_mugshot_ready(self):
        super(SelfStock, self)._on_mugshot_ready()       
        if self._mugshot.get_self():
            self.__init_self()
        self._mugshot.connect("self-known", lambda mugshot: self.__init_self())
            
    def __init_self(self):
        myself = self._mugshot.get_self()
        myself.connect("changed", lambda myself: self.__handle_self_changed())        
        self.__handle_self_changed()        
        
    def _on_edit_self(self):
        baseurl = self._mugshot.get_baseurl()
        libbig.show_url(baseurl + "/account")
        
    def get_authed_content(self, size):
        return self._box
    
    def set_size(self, size):
        super(SelfStock, self).set_size(size)
        self._namephoto_box.set_size(size)
    
    def __handle_self_changed(self):
        myself = self._mugshot.get_self()
        self._logger.debug("self (%s) changed" % (myself.get_guid(),))
        self._photo.set_url(myself.get_photo_url())
        self._name.set_property("text", myself.get_name())
        
        self._whereim_box.remove_all()
        accts = myself.get_external_accounts()        
        if not accts:  # don't have them yet, will get async
            self._logger.debug("no accounts known yet")
            return
        for acct in accts:
            name = acct.get_type()             
            if not acct.get_sentiment() == 'love':
                self._logger.debug("ignoring account %s with sentiment %s", name, acct.get_sentiment())
                continue
            icon = ExternalAccountIcon(acct)
            self._logger.debug("appending external account %s" % (name,))
            self._whereim_box.append(icon)
