import logging, os

import gobject

import hippo

import mugshot,libbig
from bigboard import Stock, AbstractMugshotStock
from big_widgets import CanvasURLImage

class PhotosStock(AbstractMugshotStock):
    """Cycles between photos from friends in Mugshot network"""
    def __init__(self, *args, **kwargs):
        super(PhotosStock,self).__init__(*args, **kwargs)

        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)

        self.__idle_display_id = 0
        self.__text = hippo.CanvasText(text="No thumbnails found")
        self.__photo = CanvasURLImage(scale_width=150, scale_height=150)
        
        self.__person_accts_len = {} # <Person,int>
        
        self._mugshot.connect("network-changed", lambda mugshot: self.__handle_network_change())  
        
    def _on_mugshot_ready(self):
        super(PhotosStock, self)._on_mugshot_ready()       
        self._mugshot.get_network()
        
    def get_authed_content(self, size):
        return self.__box
    
    def __thumbnails_generator(self):
        found_one = False
        while True:
            for entity in self._mugshot.get_network():
                accts = entity.get_external_accounts()
                if not accts:
                    continue
                for acct in accts:
                    if acct.get_thumbnails():
                        for thumbnail in acct.get_thumbnails():
                            found_one = True
                            yield (entity, acct, thumbnail)
            if not found_one:
                return
    
    def __set_image(self, imageinfo):
        (entity, acct, thumbnail) = imageinfo
        self._logger.debug("switching to %s %s %s" % (entity,acct,thumbnail))        
        self.__photo.set_url(thumbnail.get_src())        
    
    def __idle_display_image(self, images):
        imageinfo = images.next()
        self.__set_image(imageinfo)
        return True
    
    def __handle_person_change(self, person):
        need_reset = False
        
        accts = person.get_external_accounts()
        if not self.__person_accts_len.has_key(person):
            need_reset = True
            self.__person_accts_len[person] = -1
        elif accts and self.__person_accts_len[person] != len(accts):
            self.__person_accts_len[person] = len(accts)
            need_reset = True
    
        if need_reset:
            self.__reset()
    
    def __handle_network_change(self):
        self._logger.debug("handling network change")
        for person in self._mugshot.get_network():
            if not self.__person_accts_len.has_key(person):
                person.connect("changed", self.__handle_person_change)
            accts = person.get_external_accounts()
            self.__person_accts_len[person] = accts and len(accts) or 0
        for person in self.__person_accts_len.iterkeys():
            if not person.get_guid() in self._mugshot.get_network():
                print "delete this"
        self.__reset()
        
    def __reset(self):
        self._logger.debug("resetting")        
        if self.__idle_display_id > 0:
            gobject.source_remove(self.__idle_display_id)
        images = self.__thumbnails_generator()
        self.__box.remove_all()        
        try:
            self.__set_image(images.next())
            self.__box.append(self.__photo)
        except StopIteration:
            self.__box.append(self.__text)
        self.__idle_display_id = gobject.timeout_add(10000, self.__idle_display_image, 
                                                     images)

            