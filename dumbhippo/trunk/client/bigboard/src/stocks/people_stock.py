import logging

import hippo

import bigboard, identity_spider, mugshot
from big_widgets import CanvasURLImage

class PeopleStock(bigboard.Stock):
    def __init__(self):
        bigboard.Stock.__init__(self, "People")
        
        spider = identity_spider.IdentitySpider()
        self._mugshot = mugshot.get_mugshot()
        
        self._mugshot.connect("self-changed", self._handle_self_changed)
        
        self._mugshot.get_self()
        
        self._box = hippo.CanvasBox()
        self._box.set_property("orientation", hippo.ORIENTATION_VERTICAL)        
        
        self._name = hippo.CanvasText()
        self._name.set_property("text", "Foo")
        self._box.append(self._name)        
        
    def get_content(self):
        return self._box
    
    def _handle_self_changed(self, mugshot, myself):
        logging.debug("self (%s) changed" % (myself.get_guid(),))
        self._name.set_property("text", myself.get_name())
    
