import logging

import hippo

import bigboard, identity_spider, mugshot
from big_widgets import CanvasURLImage

class ExternalAccountIcon(CanvasURLImage):
    def __init__(self, acct):
        CanvasURLImage.__init__(self)
        self.set_acct(acct)
        
    def set_acct(self, acct):
        self._acct = acct
        self.set_url(self._acct.get_icon_url())

class SelfStock(bigboard.Stock):
    def __init__(self):
        bigboard.Stock.__init__(self, "Self")
        
        spider = identity_spider.IdentitySpider()
        self._mugshot = mugshot.get_mugshot()
        
        self._mugshot.connect("self-changed", self._handle_self_changed)
        
        self._mugshot.get_self()
        
        self._mugshot.connect('whereim-changed', self._handle_whereim_changed)
        
        self._box = hippo.CanvasBox()
        self._box.set_property("orientation", hippo.ORIENTATION_VERTICAL)        
        
        self._namephoto_box = hippo.CanvasBox()
        self._namephoto_box.set_property("orientation", hippo.ORIENTATION_HORIZONTAL)
        
        self._photo = CanvasURLImage()          
        self._photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
        self._photo.set_property("xalign", hippo.ALIGNMENT_START)
        self._photo.set_property("yalign", hippo.ALIGNMENT_START)
            
        self._namephoto_box.append(self._photo)
        
        self._name = hippo.CanvasText()
        self._name.set_property("text", spider.get_self_name())
        self._namephoto_box.append(self._name)        
        
        self._box.append(self._namephoto_box)
        
        self._whereim_box = hippo.CanvasBox()
        self._whereim_box.set_property("orientation", hippo.ORIENTATION_HORIZONTAL)
        
        self._box.append(self._whereim_box)
        
        self._whereim = {}
        
        self._mugshot.get_whereim()
        
    def get_content(self):
        return self._box
    
    def _handle_self_changed(self, mugshot, myself):
        logging.debug("self (%s) changed" % (myself.get_guid(),))
        self._photo.set_url(myself.get_photo_url())
        self._name.set_property("text", myself.get_name())
    
    def _handle_whereim_changed(self, mugshot, acct):
        name = acct.get_name()
        if not self._whereim.has_key(name):
            self._whereim[name] = ExternalAccountIcon(acct)
            logging.debug("appending external account %s" % (name,))
            self._whereim_box.append(self._whereim[name])
        else:
            self._whereim[name].set_acct(acct)
        