import os, code, sys, traceback, logging, StringIO, threading, urlparse

import cairo, gtk, gobject

import hippo

from libgimmie import DockWindow
from libbig.imagecache import URLImageCache
import libbig, stock, mugshot, bigboard

class CanvasVBox(hippo.CanvasBox):
    def __init__(self, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        hippo.CanvasBox.__init__(self, **kwargs)

class CanvasHBox(hippo.CanvasBox):
    def __init__(self, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_HORIZONTAL
        hippo.CanvasBox.__init__(self, **kwargs)
        
class ActionLink(hippo.CanvasLink):
    def __init__(self, **kwargs):
        hippo.CanvasLink.__init__(self, **kwargs)
        self.set_property("color", 0x0066DDFF)
        
class CanvasURLImageMixin:
    """A wrapper for CanvasImage which has a set_url method to retrieve
       images from a URL."""
    def __init__(self, url=None):
        if url:
            self.set_url(url)
        self.__is_button = False
        
    def set_url(self, url):
        if url:
            #print "fetching %s" % url
            image_cache = URLImageCache.getInstance()
            image_cache.get(url, self.__handle_image_load, self.__handle_image_error)

    def __handle_image_load(self, url, image):
        #print "got %s: %s" % (url, str(image))
        if self.__is_button:
            self.set_property("normal-image", image)
            self.set_property("prelight-image", image)
        else:
            self.set_property("image", image)
        
    def __handle_image_error(self, url, exc):
        # note exception is automatically added to log
        logging.exception("failed to load image for '%s'", url)  #FIXME queue retry

    def _set_is_button(self, is_button):
        self.__is_button = is_button
    
class CanvasMugshotURLImageMixin:
    """A canvas image that takes a Mugshot-relative image URL."""
    def __init__(self, url=None):
        self.__rel_url = None
        if url:
            self.set_url(url)
        
    def set_url(self, url):
        if url:
            self.__rel_url = url
            self.__sync()
        
    def __sync(self):
        baseurl = mugshot.get_mugshot().get_baseurl()
        if not (baseurl is None or self.__rel_url is None):
            CanvasURLImageMixin.set_url(self, urlparse.urljoin(baseurl, self.__rel_url))

class CanvasURLImage(hippo.CanvasImage, CanvasURLImageMixin):
    """A wrapper for CanvasImage which has a set_url method to retrieve
       images from a URL."""
    def __init__(self, url=None, **kwargs):
        hippo.CanvasImage.__init__(self, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START, **kwargs)
        CanvasURLImageMixin.__init__(self, url)
        
class CanvasMugshotURLImage(CanvasMugshotURLImageMixin, CanvasURLImage):
    """A canvas image that takes a Mugshot-relative image URL."""
    def __init__(self, url=None, **kwargs):
        CanvasURLImage.__init__(self, **kwargs)
        CanvasMugshotURLImageMixin.__init__(self, url)

class CanvasURLImageButton(hippo.CanvasImageButton, CanvasURLImageMixin):
    """A wrapper for CanvasImageButton which has a set_url method to retrieve
       images from a URL."""
    def __init__(self, url=None, **kwargs):
        hippo.CanvasImageButton.__init__(self, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START, **kwargs)
        CanvasURLImageMixin.__init__(self, url)
        self._set_is_button(True)
        
class CanvasMugshotURLImageButton(CanvasMugshotURLImageMixin, CanvasURLImageButton):
    """A canvas image button that takes a Mugshot-relative image URL."""
    def __init__(self, url=None, **kwargs):
        CanvasURLImageButton.__init__(self, **kwargs)
        CanvasMugshotURLImageMixin.__init__(self, url)


class PrelightingCanvasBox(hippo.CanvasBox):
    """A box with a background that changes color on mouse hover."""
    def __init__(self, **kwargs):
        hippo.CanvasBox.__init__(self, **kwargs)
        self.__hovered = False
        self.__force_prelight = False
        self.connect('motion-notify-event', lambda self, event: self.__handle_motion(event))
        
    def __handle_motion(self, event):
        if event.detail == hippo.MOTION_DETAIL_ENTER:
            self.__hovered = True
        elif event.detail == hippo.MOTION_DETAIL_LEAVE:
            self.__hovered = False

        self.sync_prelight_color()

    def set_force_prelight(self, force):
        self.__force_prelight = force
        self.sync_prelight_color()
        
    # protected
    def sync_prelight_color(self): 
        if self.__force_prelight or (self.__hovered and self.do_prelight()):
            self.set_property('background-color', 0xE2E2E2FF)
        else:
            self.set_property('background-color', 0x00000000)           
            
    # protected
    def do_prelight(self):
        return True
    
class PhotoContentItem(PrelightingCanvasBox):
    """A specialized container that has a photo and some
    corresponding content.  Handles size changes via 
    set_size."""
    def __init__(self, **kwargs):
        PrelightingCanvasBox.__init__(self,
                                      orientation=hippo.ORIENTATION_HORIZONTAL,
                                      spacing=4, **kwargs)
        self.__photo = None
        self.__photo_native_width = None
        self.__photo_native_height = None
        self.__child = None
        
    def set_photo(self, photo):
        assert(self.__photo is None)
        self.__photo = photo
        self.__photo_native_width = photo.get_property("scale-width")
        self.__photo_native_height = photo.get_property("scale-height")
        self.append(self.__photo)       
        
    def set_child(self, child):
        assert(self.__child is None)
        self.__child = child
        self.append(self.__child)         
        
    def set_size(self, size):
        assert(not None in (self.__photo, self.__child, self.__photo_native_width, self.__photo_native_height))
        if size == stock.Stock.SIZE_BULL:
            self.set_child_visible(self.__child, True)
            if self.__photo:
                self.__photo.set_property('xalign', hippo.ALIGNMENT_START)
                self.__photo.set_property('yalign', hippo.ALIGNMENT_START)
                self.__photo.set_property("scale-width", self.__photo_native_width)
                self.__photo.set_property("scale-height", self.__photo_native_height)   
        else:
            self.set_child_visible(self.__child, False)
            if self.__photo:
                self.__photo.set_property('xalign', hippo.ALIGNMENT_CENTER)
                self.__photo.set_property('yalign', hippo.ALIGNMENT_CENTER)        
                self.__photo.set_property("scale-width", 30)
                self.__photo.set_property("scale-height", 30)            

class Sidebar(DockWindow):
    __gsignals__ = {
        'size-request' : 'override'
        }

    def __init__(self, is_left):
        gravity = gtk.gdk.GRAVITY_WEST
        if not is_left:
            gravity = gtk.gdk.GRAVITY_EAST
        DockWindow.__init__(self, gravity)
        self.is_left = is_left

    def do_size_request(self, req):
        ret = DockWindow.do_size_request(self, req)
        # Give some whitespace
        geom = self.get_screen().get_monitor_geometry(0)
        
        screen = gtk.gdk.screen_get_default()
        rootw = screen.get_root_window()
        prop = rootw.property_get("_NET_WORKAREA")
        logging.debug("got _NET_WORKAREA: %s" % (prop,))
        (_, _, workarea) = prop
        work_height = workarea[3]
        req.height = work_height 
        # Never take more than available size
        req.width = min(geom.width, req.width)
        return ret

class CommandShell(gtk.Window):
    """Every application needs a development shell."""
    def __init__(self, locals={}):
        gtk.Window.__init__(self, type=gtk.WINDOW_TOPLEVEL)
        
        self._locals = locals
        
        self._history_path = libbig.get_bigboard_config_file('cmdshell_history')
        self._save_text_id = 0        
        
        box = gtk.VBox()
        paned = gtk.VPaned()
        self.output = gtk.TextBuffer()
        self.output_view = gtk.TextView(self.output)
        self.output_view.set_wrap_mode(gtk.WRAP_WORD)
        self.output_view.set_property("editable", False)
        scroll = gtk.ScrolledWindow()
        scroll.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)
        scroll.add(self.output_view)
        paned.pack1(scroll, True, True)

        self.input = gtk.TextBuffer()
        self.input_view = gtk.TextView(self.input)
        self.input_view.set_wrap_mode(gtk.WRAP_WORD)        
        self.input.connect("changed", self._handle_text_changed)
        scroll = gtk.ScrolledWindow()
        scroll.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)        
        scroll.add(self.input_view)        
        paned.pack2(scroll, True, True)
        
        box.pack_start(paned, True, True)
        
        eval_button = gtk.Button("Eval")
        eval_button.connect("clicked", self.do_eval)
        box.pack_start(eval_button, False)
        self.add(box)

        try:
            history = file(self._history_path).read()
            self.input.set_property("text", history)
        except IOError, e:
            pass

        self.set_size_request(400, 600)
        self.set_focus(self.input_view)
    
    def _idle_save_text(self):
        history_file = file(self._history_path, 'w+')
        text = self.input.get_property("text")
        history_file.write(text)
        history_file.close()
        self._save_text_id = 0
        return False
    
    def _handle_text_changed(self, text):
        if self._save_text_id == 0:
            self._save_text_id = gobject.timeout_add(3000, self._idle_save_text)
    
    def do_eval(self, entry):
        try:
            output_stream = StringIO.StringIO()
            text = self.input.get_property("text")
            code_obj = compile(text, '<input>', 'exec')
            locals = {}
            for k, v in self._locals.items():
                locals[k] = v
            locals['output'] = output_stream
            exec code_obj in locals
            logging.debug("execution complete with %d output characters" % (len(output_stream.getvalue())),)
            self.output.set_property("text", output_stream.getvalue())
        except:
            logging.debug("caught exception executing")
            self.output.set_property("text", traceback.format_exc())
