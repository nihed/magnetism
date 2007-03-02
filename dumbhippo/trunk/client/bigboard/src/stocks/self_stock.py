import logging, os

import hippo

import identity_spider, mugshot
import libbig
from bigboard import AbstractMugshotStock
from big_widgets import CanvasMugshotURLImage

class ExternalAccountIcon(CanvasMugshotURLImage):
    def __init__(self, acct):
        CanvasMugshotURLImage.__init__(self)
        self.set_acct(acct)
        
    def set_acct(self, acct):
        self._acct = acct
        self._acct.connect("changed", self._sync)
        self._sync()
        
    def _sync(self):
        self.set_url(self._acct.get_icon_url())

class SelfStock(AbstractMugshotStock):
    """Shows a user's Mugshot personal information."""
    def __init__(self):
        super(SelfStock,self).__init__("Self", ticker="")
        
        spider = identity_spider.IdentitySpider()
        self._mugshot = mugshot.get_mugshot()
        
        self._mugshot.connect("self-changed", self._handle_self_changed)
        
        self._mugshot.get_self()
        
        self._mugshot.connect('whereim-added', self._handle_whereim_added)
        
        self._box = hippo.CanvasBox()    
        
        self._namephoto_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        
        self._photo = CanvasMugshotURLImage()
        self.connect_mugshot_handler(self._photo, "button-press-event", lambda button, event: self._on_edit_self())           
        #self._photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
            
        self._namephoto_box.append(self._photo)
        
        self._name = hippo.CanvasText(text=spider.get_self_name())
        self.connect_mugshot_handler(self._name, "button-press-event", lambda button, event: self._on_edit_self())        
        self.append_bull(self._namephoto_box, self._name)        
        
        self._box.append(self._namephoto_box)
        
        self._whereim_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL, spacing=2)
        self._whereim_box.set_property("padding-top", 4)
        
        self._box.append(self._whereim_box)
        
        self._whereim = {}
        
        self._mugshot.get_whereim()
        
    def _on_edit_self(self):
        baseurl = self._mugshot.get_baseurl()
        libbig.show_url(baseurl + "/account")
        
    def get_content(self, size):
        return self._box
    
    def set_size(self, size):
        super(SelfStock, self).set_size(size)
    
    def _handle_self_changed(self, mugshot, myself):
        logging.debug("self (%s) changed" % (myself.get_guid(),))
        self._photo.set_url(myself.get_photo_url())
        self._name.set_property("text", myself.get_name())
    
    def _handle_whereim_added(self, mugshot, acct):
        name = acct.get_name()
        self._whereim[name] = ExternalAccountIcon(acct)
        logging.debug("appending external account %s" % (name,))
        self._whereim_box.append(self._whereim[name])

