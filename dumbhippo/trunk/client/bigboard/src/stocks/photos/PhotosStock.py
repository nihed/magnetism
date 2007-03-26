import logging, os

import gobject, cairo

import hippo

import mugshot,libbig
from bigboard import Stock, AbstractMugshotStock
from big_widgets import CanvasURLImage,CanvasVBox,CanvasHBox,CanvasMugshotURLImage

class TransitioningURLImage(hippo.CanvasBox, hippo.CanvasItem):
    __gtype_name__ = 'TransitioningURLImage' 
    
    __gproperties__ = {
        'dimension': (gobject.TYPE_UINT, 'Dimension', 'Scale to this size', 0, 2**32-1, 0, gobject.PARAM_READWRITE)
    }    
    
    def __init__(self, **kwargs):
        hippo.CanvasBox.__init__(self, **kwargs)
        self.__current_url = None
        self.__surface = None
        
        #self.connect("paint", self.__handle_paint)
        
    # override
    def set_url(self, url):
        image_cache = libbig.URLImageCache.getInstance()
        image_cache.get(url, self.__handle_image_load, self.__handle_image_error)
            
    def __handle_image_load(self, url, surface):
        print "img loaded"
        req_changed = False
        if self.__surface:
            old_width = self.__surface.get_width()
            old_height = self.__surface.get_height()
            if old_width != surface.get_width() or old_height != surface.get_height():
                req_changed = True
        else:
            req_changed = True
            
        self.__surface = surface            
            
        if req_changed:
            print "request changed"
            self.emit_request_changed()
        
        self.emit_paint_needed(0, 0, -1, -1)
            
    def __handle_image_error(self, url, exc):
        pass        
    
    def do_paint_below_children(self, cr, box):    
        print "painting"
        
        if not self.__surface:
            print "no surface"
            return

        img_width = self.__surface.get_width()
        img_height = self.__surface.get_height()        
        
        if self.__dimension == 0 or img_width == 0 or img_height == 0:
            print "no dimension (%s %s %s)" % (self.__dimension, img_width, img_height)            
            return
        
        xdelta = abs(img_width - self.__dimension)
        ydelta = abs(img_height - self.__dimension)
        if xdelta > ydelta:
            scale = (1.0*self.__dimension) / img_width
        else:
            scale = (1.0*self.__dimension) / img_height
            
        print "painting scale=%s" % (scale,)            
        
        (x,y,w,h) = self.align(int(img_width*scale), int(img_height*scale))
                
        cr.rectangle(x, y, w, h)
        cr.clip()
        cr.scale(scale, scale)
        cr.translate(x, y)
        cr.set_source_surface(self.__surface, 0, 0)
        cr.paint()
        
    def do_get_content_width_request(self):
        (children_min, children_natural) = hippo.CanvasBox.do_get_content_width_request(self)
        print "width %s %s %s" % (self.__dimension, children_min, children_natural)
        dim = self.__dimension or 0
        return (max(dim, children_min), max(dim, children_natural))
    
    def do_get_content_height_request(self, for_width):
        (children_min, children_natural) = hippo.CanvasBox.do_get_content_height_request(self, for_width)
        print "height %s %s %s" % (self.__dimension, children_min, children_natural)
        dim = self.__dimension or 0        
        return (max(dim, children_min), max(dim, children_natural))
    
    def do_set_property(self, pspec, value):
        if pspec.name == 'dimension':
            self.__dimension = value
            self.emit_request_changed()            
        else:
            raise AttributeError, 'unknown property %s' % pspec.name
        
    def do_get_property(self, pspec):
        if pspec.name == 'dimension':
            return self.__dimension
        else:
            raise AttributeError, 'unknown property %s' % pspec.name        

class PhotosStock(AbstractMugshotStock):
    """Cycles between photos from friends in Mugshot network"""
    def __init__(self, *args, **kwargs):
        super(PhotosStock,self).__init__(*args, **kwargs)

        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)
        
        self.__photosize = 120

        self.__idle_display_id = 0
        self.__text = hippo.CanvasText(text="No thumbnails found")
        
        self.__displaybox = CanvasVBox()
        
        self.__photo_header = CanvasHBox()
        self.__favicon = CanvasMugshotURLImage()
        self.__title = hippo.CanvasText(size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__photo_header.append(self.__favicon)
        self.__photo_header.append(self.__title)
        
        self.__displaybox.append(self.__photo_header)
        
        self.__photobox = CanvasHBox(spacing=6)
        self.__photo = TransitioningURLImage(dimension=self.__photosize)
        self.__metabox = CanvasVBox()
        self.__metabox.append(hippo.CanvasText(text="from"))
        self.__fromphoto = CanvasMugshotURLImage()
        self.__metabox.append(self.__fromphoto)
        self.__fromname = hippo.CanvasText(text="somebody", size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__photobox.append(self.__photo)
        self.__photobox.append(self.__metabox)
        
        self.__displaybox.append(self.__photobox)        
        
        self.__person_accts_len = {} # <Person,int>
        
        self._mugshot.connect("network-changed", lambda mugshot: self.__handle_network_change())  
        
    def _on_mugshot_ready(self):
        super(PhotosStock, self)._on_mugshot_ready()       
        self._mugshot.get_network()
        
    def get_authed_content(self, size):
        return self.__box
    
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
        (entity, acct, thumbnail) = imageinfo
        self._logger.debug("switching to %s %s %s" % (entity,acct,thumbnail))
        self.__favicon.set_url(acct.get_icon())
        self.__title.set_property("text", thumbnail.get_title())
        self.__photo.set_url(thumbnail.get_src())        
        self.__fromname.set_property("text", entity.get_name())
        self.__fromphoto.set_url(entity.get_photo_url())
    
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
            self.__box.append(self.__displaybox)
        except StopIteration:
            self.__box.append(self.__text)
        self.__idle_display_id = gobject.timeout_add(10000, self.__idle_display_image, 
                                                     images)

            