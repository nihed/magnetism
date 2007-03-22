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
        self.set_url(self._acct.get_icon_url())
        
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
        
        self._whereim = {}
        
        self._mugshot.connect("self-changed", self._handle_self_changed)
        self._mugshot.connect('whereim-added', self._handle_whereim_added)         
        
    def _on_mugshot_ready(self):
        super(SelfStock, self)._on_mugshot_ready()       
        self._mugshot.get_self()
        self._mugshot.get_whereim()        
        
    def _on_edit_self(self):
        baseurl = self._mugshot.get_baseurl()
        libbig.show_url(baseurl + "/account")
        
    def get_authed_content(self, size):
        return self._box
    
    def set_size(self, size):
        super(SelfStock, self).set_size(size)
        self._namephoto_box.set_size(size)
    
    def _handle_self_changed(self, mugshot, myself):
        self._logger.debug("self (%s) changed" % (myself.get_guid(),))
        self._photo.set_url(myself.get_photo_url())
        self._name.set_property("text", myself.get_name())
    
    def _handle_whereim_added(self, mugshot, acct):
        name = acct.get_name()        
        if self._whereim.has_key(name):
            return
        self._whereim[name] = ExternalAccountIcon(acct)
        if not acct.get_sentiment() == 'love':
            self._logger.debug("ignoring account %s with sentiment %s", acct, acct.get_sentiment())
            return
        self._logger.debug("appending external account %s" % (name,))
        self._whereim_box.append(self._whereim[name])
