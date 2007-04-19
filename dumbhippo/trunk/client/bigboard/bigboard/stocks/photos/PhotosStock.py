import logging, os

import gobject, cairo

import hippo

import bigboard.mugshot as mugshot
import bigboard.libbig as libbig
from bigboard.stock import Stock, AbstractMugshotStock
from bigboard.big_widgets import CanvasURLImage, CanvasVBox, CanvasHBox, CanvasMugshotURLImage, ActionLink

class TransitioningURLImage(hippo.CanvasBox, hippo.CanvasItem):
    __gtype_name__ = 'TransitioningURLImage' 
    
    __gsignals__ = {
        "loaded" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_BOOLEAN,))
    }
    
    __gproperties__ = {
        'dimension': (gobject.TYPE_UINT, 'Dimension', 'Scale to this size', 0, 1000, 0, gobject.PARAM_READWRITE)
    }    
    
    TRANSITION_STEPS = 10
    
    def __init__(self, **kwargs):
        hippo.CanvasBox.__init__(self, **kwargs)
        self.set_clickable(True)
        self.__current_url = None
        self.__prev_url = None
        self.__surface = None
        self.__prev_surface = None
        self.__transition_count = 0
        self.__transition_idle_id = 0
        
    def set_url(self, url):
        self.__prev_url = self.__current_url
        self.__current_url = url
        image_cache = libbig.URLImageCache.getInstance()
        image_cache.get(url, self.__handle_image_load, self.__handle_image_error)
            
    def __handle_image_load(self, url, surface):
        if url != self.__current_url:
            return
        
        self.emit("loaded", True)
        
        req_changed = False
        if self.__surface:
            old_width = self.__surface.get_width()
            old_height = self.__surface.get_height()
            if old_width != surface.get_width() or old_height != surface.get_height():
                req_changed = True
        else:
            req_changed = True
        
        self.__transition_count = 0
        if self.__transition_idle_id > 0:
            gobject.source_remove(self.__transition_idle_id)
        self.__transition_idle_id = gobject.timeout_add(100, self.__idle_step_transition)
        self.__prev_surface = self.__surface
        self.__surface = surface            
            
        if req_changed:
            self.emit_request_changed()
        
        self.emit_paint_needed(0, 0, -1, -1)
            
    def __handle_image_error(self, url, exc):
        self.emit("loaded", False) 
    
    def __idle_step_transition(self):
        self.__transition_count += 1
        self.emit_paint_needed(0, 0, -1, -1)
        
        if self.__transition_count >= self.TRANSITION_STEPS:
            self.__transition_idle_id = 0
            return False
        return True
    
    # override
    def do_paint_below_children(self, cr, box):    
        if not self.__surface:
            return

        img_width = self.__surface.get_width()
        img_height = self.__surface.get_height()        
        
        if self.__dimension == 0 or img_width == 0 or img_height == 0:       
            return
        
        xdelta = abs(img_width - self.__dimension)
        ydelta = abs(img_height - self.__dimension)
        if xdelta > ydelta:
            scale = (1.0*self.__dimension) / img_width
        else:
            scale = (1.0*self.__dimension) / img_height       
        
        (x,y,w,h) = self.align(int(img_width*scale), int(img_height*scale))
                
        cr.rectangle(x, y, w, h)
        cr.clip()
        cr.scale(scale, scale)
        cr.translate(x, y)
        if self.__prev_surface:
            cr.set_source_surface(self.__prev_surface, 0, 0)
            #cr.paint_with_alpha((1.0*self.TRANSITION_STEPS-self.__transition_count)/self.TRANSITION_STEPS)
            cr.paint()
        cr.set_source_surface(self.__surface)
        cr.paint_with_alpha((1.0*self.__transition_count)/self.TRANSITION_STEPS)
        
    # override
    def do_get_content_width_request(self):
        (children_min, children_natural) = hippo.CanvasBox.do_get_content_width_request(self)
        dim = self.__dimension or 0
        return (max(dim, children_min), max(dim, children_natural))
    
    # override
    def do_get_content_height_request(self, for_width):
        (children_min, children_natural) = hippo.CanvasBox.do_get_content_height_request(self, for_width)
        dim = self.__dimension or 0        
        return (max(dim, children_min), max(dim, children_natural))
    
    # override
    def do_set_property(self, pspec, value):
        if pspec.name == 'dimension':
            self.__dimension = value
            self.emit_request_changed()            
        else:
            raise AttributeError, 'unknown property %s' % pspec.name
        
    # override
    def do_get_property(self, pspec):
        if pspec.name == 'dimension':
            return self.__dimension
        else:
            raise AttributeError, 'unknown property %s' % pspec.name        

class PhotosStock(AbstractMugshotStock):
    """Cycles between photos from friends in Mugshot network"""
    def __init__(self, *args, **kwargs):
        super(PhotosStock,self).__init__(*args, **kwargs)

        self.__images = None
        self.__current_image = None

        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)
        
        self.__photosize = 120

        self.__idle_display_id = 0
        self.__text = hippo.CanvasText(text="No thumbnails found")
        
        self.__displaybox = CanvasVBox(spacing=4)
        
        self.__photo_header = CanvasHBox(spacing=4)
        self.__favicon = CanvasMugshotURLImage()
        self.__title = ActionLink(size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__title.connect("button-press-event", lambda photo, event: self.__visit_photo())        
        self.__photo_header.append(self.__favicon)
        self.__photo_header.append(self.__title)
        
        self.__displaybox.append(self.__photo_header)
        
        self.__photobox = CanvasHBox(spacing=6)
        self.__photo = TransitioningURLImage(dimension=self.__photosize)
        self.__photo.connect("loaded", lambda photo, loaded: self.__on_image_load(loaded))
        self.__photo.connect("button-press-event", lambda photo, event: self.__visit_photo())
        self.__metabox = CanvasVBox()
        self.__metabox.append(hippo.CanvasText(text="from"))
        self.__fromphoto = CanvasMugshotURLImage()
        self.__fromphoto.set_clickable(True)
        self.__fromphoto.connect("button-press-event", lambda photo, event: self.__visit_person())           
        self.__metabox.append(self.__fromphoto)
        self.__fromname = ActionLink(size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__fromname.connect("button-press-event", lambda photo, event: self.__visit_person())              
        self.__metabox.append(self.__fromname)
        self.__photobox.append(self.__photo)
        self.__photobox.append(self.__metabox)
        
        self.__displaybox.append(self.__photobox)        
        
        self.__person_accts_len = {} # <Person,int>
        
        self.__bearbox = CanvasVBox()
        self.__bearphoto = TransitioningURLImage(dimension=self.SIZE_BEAR_CONTENT_PX-6)
        self.__bearphoto.connect("button-press-event", lambda photo, event: self.__visit_photo())        
        self.__bearbox.append(self.__bearphoto)
        
        self._mugshot.connect("network-changed", lambda mugshot: self.__handle_network_change())  
        
    def _on_mugshot_ready(self):
        super(PhotosStock, self)._on_mugshot_ready()       
        self._mugshot.get_network()
        
    def get_authed_content(self, size):
        return size == self.SIZE_BULL and self.__box or self.__bearbox
    
    def __visit_photo(self):
        self._logger.debug("visiting photo for %s", self.__current_image)
        if not self.__current_image:
            return
        libbig.show_url(self.__current_image[2].get_href())
        
    def __visit_person(self):
        self._logger.debug("visiting person for %s", self.__current_image)
        if not self.__current_image:
            return
        libbig.show_url(mugshot.get_mugshot().get_baseurl() + self.__current_image[0].get_home_url())    
    
    def __thumbnails_generator(self):
        """The infinite photos function.  Cool."""
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
        self.__current_image = imageinfo
        (entity, acct, thumbnail) = imageinfo
        
        self._logger.debug("starting load of url %s" % (thumbnail.get_src(),))
        self.__photo.set_url(thumbnail.get_src())
        self.__bearphoto.set_url(thumbnail.get_src())
        
    def __on_image_load(self, success):
        if self.__current_image is None:
            self._logger.debug("image load complete, but no current image")       
            return
        if not success:
            self._logger.debug("image load failed, skipping to next")                   
            self.__do_next()
        
        self._logger.debug("image load success, syncing metadata")          
        (entity, acct, thumbnail) = self.__current_image

        self.__favicon.set_url(acct.get_icon())
        self.__title.set_property("text", thumbnail.get_title() or "(untitled)")
     
        self.__fromname.set_property("text", entity.get_name())
        self.__fromphoto.set_url(entity.get_photo_url())        
    
    def __idle_display_image(self):
        self._logger.debug("in idle, doing next image")          
        imageinfo = self.__images.next()
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
        not_in_network = []
        for person in self.__person_accts_len.iterkeys():
            if not person in self._mugshot.get_network():
                not_in_network.append(person)
        for person in not_in_network:
            self._logger.debug("removing not-in-network person %s", person.get_guid())
            del self.__person_accts_len[person]
        self.__reset()

    def __do_next(self):
        self._logger.debug("skipping to next")                  
        try:
            self.__set_image(self.__images.next())
            self.__box.append(self.__displaybox)
        except StopIteration:
            self._logger.debug("caught StopIteration, displaying no photos text")            
            self.__box.append(self.__text)        
        if self.__idle_display_id > 0:
            gobject.source_remove(self.__idle_display_id)            
        self.__idle_display_id = gobject.timeout_add(10000, self.__idle_display_image)        
        
    def __reset(self):
        self._logger.debug("resetting")        
        self.__images = self.__thumbnails_generator()
        self.__box.remove_all()        
        self.__do_next()
