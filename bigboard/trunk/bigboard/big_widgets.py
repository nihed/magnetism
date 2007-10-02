import os, code, sys, traceback, logging, StringIO, threading, urlparse

import cairo
import gtk
import gobject
import gconf

import hippo

from libgimmie import DockWindow
from libbig.imagecache import URLImageCache
import libbig, stock, globals, bigboard
from table_layout import TableLayout

class CanvasVBox(hippo.CanvasBox):
    def __init__(self, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        hippo.CanvasBox.__init__(self, **kwargs)

class CanvasHBox(hippo.CanvasBox):
    def __init__(self, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_HORIZONTAL
        hippo.CanvasBox.__init__(self, **kwargs)

class CanvasSpinner(hippo.CanvasWidget):
    def __init__(self):
        super(CanvasSpinner, self).__init__()
        self.spinner = gtk.SpinButton()
        self.set_property('widget', self.spinner)

class CanvasCheckbox(hippo.CanvasWidget):
    def __init__(self, label):
        super(CanvasCheckbox, self).__init__()
        self.checkbox = gtk.CheckButton(label)
        self.set_property('widget', self.checkbox)

class CanvasTable(hippo.CanvasBox):
    def __init__(self, column_spacing=0, row_spacing=0, **kwargs):
        hippo.CanvasBox.__init__(self, **kwargs)

        self.__layout = TableLayout(column_spacing=column_spacing, row_spacing=row_spacing)
        self.set_layout(self.__layout)

    def add(self, child, left=None, right=None, top=None, bottom=None, flags=0):
        self.__layout.add(child, left, right, top, bottom, flags)

    def set_column_expand(self, column, expand):
        self.__layout.set_column_expand(column, expand)

    def set_row_expand(self, row, expand):
        self.__layout.set_row_expand(row, expand)        
        
class ActionLink(hippo.CanvasLink):
    def __init__(self, **kwargs):
        if not kwargs.has_key('color'):
            kwargs['color'] = 0x0066DDFF 
        hippo.CanvasLink.__init__(self, **kwargs)

class ButtonLabel(gtk.Label):
    def __init__(self, ypadding=0):
        super(ButtonLabel, self).__init__()
        self.__ypadding = ypadding
    
    def do_size_request(self, req):
        gtk.Label.do_size_request(self, req)
        req.height += (self.__ypadding*2)

gobject.type_register(ButtonLabel)

class Button(hippo.CanvasButton):
    def __init__(self, label_xpadding=0, label_ypadding=0, label=''):
        super(Button, self).__init__()
        self.__label_xpadding = label_xpadding
        self.__label_ypadding = label_ypadding
        button = self.get_property('widget')
        button.set_property('border-width', 0)
        button.unset_flags(gtk.CAN_DEFAULT)

        # this causes the GtkButton inside the CanvasButton
        # to create a label widget
        self.set_property('text', label)        

        # Avoid some padding
        button.set_name('bigboard-nopad-button')
        child = button.get_child() #gtk.Label() #ButtonLabel(ypadding=label_ypadding)
        child.set_alignment(0.5, 0)
        child.set_padding(self.__label_xpadding, self.__label_ypadding)

    def get_button(self):
        return self.get_property('widget')
  
    def get_label(self):
	return self.get_button().child

    def set_label_text(self, label=''):
        self.set_property('text', label)
        child = self.get_property('widget').get_child()
        child.set_padding(self.__label_xpadding, self.__label_ypadding) 

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
            image_cache = URLImageCache()
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
        logging.error("failed to load image for '%s': %s", url, exc)  #FIXME queue retry

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
        baseurl = globals.get_baseurl()
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

class Separator(hippo.CanvasBox):
    def __init__(self):
        hippo.CanvasBox.__init__(self, border_top=1, border_color=0x999999FF, padding_left=6, padding_right=6)

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

class IconLink(PrelightingCanvasBox):
    def __init__(self, text, **kwargs):
        PrelightingCanvasBox.__init__(self,
                                      orientation=hippo.ORIENTATION_HORIZONTAL,
                                      spacing=4, **kwargs)
        self.img = hippo.CanvasImage(scale_width=20, scale_height=20)
        self.append(self.img)
        self.link = hippo.CanvasLink(text=text, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END,)
        self.append(self.link)

class RootWindowWatcher(gtk.Invisible):
    """Class to track properties of the root window.

    The tracking is a distinctly hacky; what we do is set the user data of the root window
    to point to an GtkInvisible and catch the property-notify events there, since we can't use
    gdk_window_add_filter() from Python. If someone else uses the same trick in the
    same process, we'll break.

    """
    
    __gsignals__ = {
        'realize' : 'override',
        'unrealize' : 'override',
        'workarea-changed' : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        }

    @staticmethod
    def get_for_screen(screen):
        watcher = screen.get_data("RootWindowWatcher")
        if watcher == None:
            watcher = RootWindowWatcher(screen)
            screen.set_data("RootWindowWatcher", watcher)

        return watcher

    def __init__(self, screen):
        """Don't call this, call get_for_screen() instead. (Might be possible to make
           a singleton with __new__, but the GObject interaction could be tricky.)"""
        
        super(RootWindowWatcher, self).__init__()
        self.set_screen(screen)

    def do_realize(self):
        super(RootWindowWatcher, self).do_realize(self)
        
        screen = self.get_screen()
        rootw = screen.get_root_window()
        self.__old_events = rootw.get_events()

        rootw.set_events(self.__old_events | gtk.gdk.PROPERTY_CHANGE_MASK)
        rootw.set_user_data(self)

        self.__compute_workarea()

    def do_unrealize(self):
        rootw.set_events(self.__old_events)
        rootw.set_user_data(None)
        
        super(RootWindowWatcher, self).do_unrealize(self)

    def do_property_notify_event(self, event):
        if event.atom == "_NET_WORKAREA":
            old_workarea = self.__workarea
            self.__compute_workarea()
            if (self.__workarea != old_workarea):
                self.emit("workarea-changed")

    def __compute_workarea(self):
        screen = self.get_screen()
        rootw = screen.get_root_window()
        prop = rootw.property_get("_NET_WORKAREA")
        logging.debug("got _NET_WORKAREA: %s" % (prop,))
        (_, _, workarea) = prop
        self.__workarea = (workarea[0], workarea[1], workarea[2], workarea[3])

    def get_workarea(self):
        return self.__workarea
                
class Sidebar(DockWindow):
    __gsignals__ = {
        'screen-changed' : 'override',
        'show' : 'override',
        }

    def __init__(self, is_left, strut_key):
        gravity = gtk.gdk.GRAVITY_WEST
        if not is_left:
            gravity = gtk.gdk.GRAVITY_EAST
        DockWindow.__init__(self, gravity)
        self.is_left = is_left
        self.__watcher = None
        self.__strut_key = strut_key

    def do_show(self):
        self.__update_watcher()
        super(Sidebar, self).do_show(self)

    def do_screen_changed(self, old_screen):
        super(Sidebar, self).do_screen_changed(old_screen)

        self.__update_watcher()

    def __update_watcher(self):
        if self.__watcher != None:
            self.__watcher.disconnect(self.__watcher_handler_id)
        self.__watcher = RootWindowWatcher.get_for_screen(self.get_screen())
        self.__watcher_handler_id = self.__watcher.connect('workarea-changed', self.__on_workarea_changed)
        self.__on_workarea_changed(self.__watcher)

    def __on_workarea_changed(self, watcher):
        (x,y,width,height) = watcher.get_workarea()
        self.set_size_request(-1, height)
        self.move(0, y)
        
    def do_set_wm_strut(self):
        kwargs = {}
        if not gconf.client_get_default().get_bool(self.__strut_key):
            kwargs['remove'] = True
        super(Sidebar, self).do_set_wm_strut(**kwargs)
            
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
