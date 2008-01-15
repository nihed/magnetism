import logging, os, urlparse

import gobject, cairo

import hippo

import bigboard.globals as globals
import bigboard.libbig as libbig
from bigboard.stock import Stock, AbstractMugshotStock
from bigboard.big_widgets import CanvasURLImage, CanvasVBox, CanvasHBox, CanvasMugshotURLImage, ActionLink

_logger = logging.getLogger('bigboard.stocks.PhotosStock')

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
        image_cache = libbig.imagecache.URLImageCache()
        image_cache.get(url, self.__handle_image_load, self.__handle_image_error)
            
    def __handle_image_load(self, url, surface):
        if url != self.__current_url:
            return
        
        _logger.debug("loaded url=%s", url)
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

        _logger.debug("rendering img width=%s height=%s scale=%s", img_width, img_height, scale)

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
    SLIDE_TIMEOUT_SEC = 1 * 60  # 1 minute
    
    MAX_PREV_IMAGES = 5

    """Cycles between photos from friends in Mugshot network"""
    def __init__(self, *args, **kwargs):
        super(PhotosStock,self).__init__(*args, **kwargs)

        self.__images = None
        self.__images_reverse = []
        self.__images_forward = []
        self.__current_image = None

        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)

        self.__successive_load_failures = 0
        
        self.__photosize = 120

        self.__idle_display_id = 0
        self.__text = hippo.CanvasText(text="No thumbnails found")

        self.__displaymode = "uninitialized" # "none", "photo"
        
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

        self.__controlbox = CanvasHBox()
        prev_link = ActionLink(text=u"\u00ab Prev", xalign=hippo.ALIGNMENT_START)
        prev_link.connect("button-press-event", lambda b,e: self.__do_prev())
        self.__controlbox.append(prev_link)
        next_link = ActionLink(text=u"Next \u00bb", xalign=hippo.ALIGNMENT_END)
        next_link.connect("button-press-event", lambda b,e: self.__do_next())
        self.__controlbox.append(next_link, hippo.PACK_EXPAND)
        self.__displaybox.append(self.__controlbox)        
        
        self.__person_accts_len = {} # <Person,int>
        
        self.__bearbox = CanvasVBox()
        self.__bearphoto = TransitioningURLImage(dimension=self.SIZE_BEAR_CONTENT_PX-6)
        self.__bearphoto.connect("button-press-event", lambda photo, event: self.__visit_photo())        
        self.__bearbox.append(self.__bearphoto)

    def _on_ready(self):
        if self._model.self_resource != None:
            query = self._model.query_resource(self._model.self_resource, "contacts user [+;lovedAccounts [+;thumbnails +]]")
            query.add_handler(self.__on_got_self)
            query.execute()

    def __on_got_self(self, myself):
        self.__reset()

    def get_authed_content(self, size):
        return size == self.SIZE_BULL and self.__box or self.__bearbox
    
    def __visit_photo(self):
        _logger.debug("visiting photo for %s", self.__current_image)
        if not self.__current_image:
            return
        libbig.show_url(self.__current_image[2].get_href())
        
    def __visit_person(self):
        _logger.debug("visiting person for %s", self.__current_image)
        if not self.__current_image:
            return
        libbig.show_url(urlparse.urljoin(globals.get_baseurl(), self.__current_image[0].get_home_url()))
        
    def __thumbnails_generator(self):
        """The infinite photos function.  Cool."""
        while True:
            found_one = False
            # Iterate through all thumbnails for all "loved accounts" for all contacts.
            # We don't handle change notification ... if something changes we'll pick
            # it up next time around. Note the use of temporary copies of lists to avoid
            # problems if a list is mutated by a change notification while we are iterating it.
            if self._model.self_resource:
                for contact in list(getattr(self._model.self_resource, "contacts", [])):
                    user = getattr(contact, "user", None)
                    if user != None:
                        lovedAccounts = getattr(user, "lovedAccounts", None)
                        if lovedAccounts:
                            for externalAccount in lovedAccounts:
                                thumbnails = getattr(externalAccount, "thumbnails", None)
                                if thumbnails:
                                    for thumbnail in thumbnails:
                                        yield (user, externalAccount, thumbnail)

            # If we didn't find any photos, we go into a "no photos" state; we'll keep on trying
            # to restart the iterator in the timeout, so when things appear we'll display them
            if not found_one:
                return
            
    def __next_image(self):
        if self.__current_image:
            self.__images_reverse.append(self.__current_image)
            if len(self.__images_reverse) > self.MAX_PREV_IMAGES:
                self.__images_reverse.pop(0)
        if self.__images_forward:
            return self.__images_forward.pop()
        else:
            return self.__images.next()

    def __prev_image(self):
        if self.__current_image:
            self.__images_forward.append(self.__current_image)
        return self.__images_reverse.pop()
    
    def __set_image(self, imageinfo):
        self.__current_image = imageinfo
        (user, account, thumbnail) = imageinfo
        
        _logger.debug("starting load of url %s" % (thumbnail.src,))
        self.__photo.set_url(thumbnail.src)
        self.__bearphoto.set_url(thumbnail.src)
        
    def __on_image_load(self, success):
        if self.__current_image is None:
            _logger.debug("image load complete, but no current image")       
            return
        if not success:
            self.__successive_load_failures = max(self.__successive_load_failures+1, 17)
            _logger.debug("image load failed, queueing skip to next")                   
            gobject.timeout_add(8000 + (2 ** self.__successive_load_failures) * 1000, self.__do_next)
        else:
            self.__successive_load_failures = 0
        
        _logger.debug("image load success, syncing metadata")          
        (user, account, thumbnail) = self.__current_image

        self.__favicon.set_url(account.iconUrl)
        
        title = getattr(thumbnail, "title", None)
        if not title: title = "(untitled)"
        self.__title.set_property("text", title)
     
        self.__fromname.set_property("text", user.name)
        self.__fromphoto.set_url(user.photoUrl)

    def __idle_display_image(self):
        _logger.debug("in idle, doing next image")          
        self.__idle_display_id = 0
        self.__do_next()
        return False
    
    def __do_direction(self, is_next):
        _logger.debug("skipping to %s" % (is_next and "next" or "prev",))
        try:
            self.__set_image(is_next and self.__next_image() or self.__prev_image())
            if self.__displaymode == 'text':
                self.__box.remove(self.__text)
            if self.__displaymode != 'photo':
                self.__box.append(self.__displaybox)
            self.__displaymode = 'photo'
        except StopIteration:
            _logger.debug("caught StopIteration, displaying no photos text")            
            if self.__displaymode == 'photo':
                self.__box.remove(self.__displaybox)
            if self.__displaymode != 'text':
                self.__box.append(self.__text)        
            self.__displaymode = 'text'
        if self.__idle_display_id > 0:
            gobject.source_remove(self.__idle_display_id)            
        self.__idle_display_id = gobject.timeout_add(self.SLIDE_TIMEOUT_SEC * 1000, self.__idle_display_image)        

    def __do_next(self):
        self.__do_direction(True)
        
    def __do_prev(self):
        self.__do_direction(False)

    def __reset(self):
        _logger.debug("resetting")
        self.__images = self.__thumbnails_generator()
        self.__images_reverse = []
        self.__box.remove_all()        
        self.__displaymode = 'uninitialized'
        self.__do_next()
