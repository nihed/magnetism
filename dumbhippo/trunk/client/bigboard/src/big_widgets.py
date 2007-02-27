import os, code, sys, traceback, logging, StringIO, threading

import cairo, gtk, gobject

import hippo

from libgimmie import DockWindow
from libbig import URLImageCache
import libbig      

class CanvasURLImage(hippo.CanvasImage):
    def __init__(self, url=None):
        hippo.CanvasImage.__init__(self)
        self.set_property("xalign", hippo.ALIGNMENT_START)
        self.set_property("yalign", hippo.ALIGNMENT_START)        
        if not url is None:
            self.set_url(url)
        
    def set_url(self, url):
        image_cache = URLImageCache.getInstance()
        image_cache.get(url, self._handle_image_load, self._handle_image_error)
        
    def _handle_image_load(self, url, image):
        self.image = image
        
    def _handle_image_error(self, url, exc):
        logging.exception("failed to load image for '%s'", exc)  #FIXME queue retry
        

class Sidebar(DockWindow):
    __gsignals__ = {
        'size-request' : 'override',
        'size-allocate' : 'override'
        }

    def __init__(self, is_left):
        gravity = gtk.gdk.GRAVITY_WEST
        if not is_left:
            gravity = gtk.gdk.GRAVITY_EAST
        DockWindow.__init__(self, gravity)
        self.set_keep_above(True)
        self.set_wm_strut(True)
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
    def __init__(self, locals={}):
        gtk.Window.__init__(self, type=gtk.WINDOW_TOPLEVEL)
        
        self._locals = locals
        
        self._history_path = libbig.get_bigboard_config_file('cmdshell_history')
        self._save_text_id = 0        
        
        box = gtk.VBox()
        paned = gtk.VPaned()
        self.output = gtk.TextBuffer()
        self.output_view = gtk.TextView(self.output)
        self.output_view.set_property("editable", False)
        scroll = gtk.ScrolledWindow()
        scroll.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)
        scroll.add(self.output_view)
        paned.pack1(scroll, True, True)

        self.input = gtk.TextBuffer()
        self.input_view = gtk.TextView(self.input)
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
