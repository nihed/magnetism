import logging

import hippo

import bigboard, mugshot
from big_widgets import CanvasURLImage

class EntityItem(hippo.CanvasBox):
    def __init__(self):
        hippo.CanvasBox.__init__(self)
        self._entity = None

        self._photo = CanvasURLImage()
        self.append(self._photo)

        self._name = hippo.CanvasText()
        self.append(self._name)
        
    def set_entity(self, entity):
        if self._entity == entity:
            return
        self._entity = entity
        self._update()

    def _update(self):
        if not self._entity:
            return
        self._name.set_property("text", self._entity.get_name())
        if self._entity.get_photo_url():
            self._photo.set_url(self._entity.get_photo_url())

class PeopleStock(bigboard.Stock):
    def __init__(self):
        super(PeopleStock, self).__init__("People")
        
        self._mugshot = mugshot.get_mugshot()

        self._mugshot.connect("entity-added", self._handle_entity_added)
        self._mugshot.connect("self-changed", self._handle_self_changed)
        
        self._mugshot.get_self()
        self._mugshot.get_network()
        
        self._box = hippo.CanvasBox()
        self._box.set_property("orientation", hippo.ORIENTATION_VERTICAL)        
        
    def get_content(self, size):
        return self._box
    
    def _handle_self_changed(self, mugshot, myself):
        logging.debug("self (%s) changed" % (myself.get_guid(),))
    
    def _handle_entity_added(self, mugshot, entity):
        logging.debug("entity added to people stock %s" % (entity.get_name()))
        item = EntityItem()
        item.set_entity(entity)
        self._box.append(item)
