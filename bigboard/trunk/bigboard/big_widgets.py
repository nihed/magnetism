import os, code, sys, traceback, logging, StringIO, threading, urlparse, weakref

import cairo
import pango
import gtk
import gobject
import gconf

import hippo

from libgimmie import DockWindow
from libbig.imagecache import URLImageCache
import libbig, stock, globals, bigboard
from bigboard.libbig.signalobject import SignalObject
from bigboard.libbig.singletonmixin import Singleton
from table_layout import TableLayout

_logger = logging.getLogger("bigboard.BigWidgets")

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

class ThemeManager(gobject.GObject):
    __gsignals__ = {
                    'theme-changed' : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
                   }
    def __init__(self):
        super(ThemeManager, self).__init__()
        self.__class__.instance = weakref.ref(self)
        self.__theme = None
        gconf.client_get_default().notify_add('/apps/bigboard/theme', self.__sync_theme)
        self.__sync_theme()
        
    @staticmethod
    def getInstance():
        needinst = False
        if not hasattr(ThemeManager, 'instance'):
            instvalue = None
        else:
            instref = getattr(ThemeManager, 'instance')
            instvalue = instref()
            needinst = instvalue is None
        if instvalue is None:
            inst = ThemeManager()
        else:
            inst = instvalue
        return inst
        
    def get_theme(self):
        return self.__theme
        
    def __sync_theme(self, *args):
        _logger.debug("doing theme sync")
        themename = gconf.client_get_default().get_string('/apps/bigboard/theme')
        if themename == 'fedora':
            from bigboard.themes.fedora import FedoraTheme
            self.__theme = FedoraTheme.getInstance()
        else:
            from bigboard.themes.default import DefaultTheme
            self.__theme = DefaultTheme.getInstance()        
        self.emit('theme-changed')            
         
class ThemedWidgetMixin(object):
    def __init__(self, theme_hints=[]):
        super(ThemedWidgetMixin, self).__init__()
        mgr = ThemeManager.getInstance()
        mgr.connect('theme-changed', self.__sync_theme)
        self.__theme_hints = theme_hints        
        self.__sync_theme(mgr)
        
    def get_theme(self):
        return ThemeManager.getInstance().get_theme()
    
    def get_theme_hints(self):
        return self.__theme_hints
    
    def _on_theme_changed(self, theme):
        self.emit_paint_needed(0,0,-1,-1)

    def __sync_theme(self, tm):
        theme = tm.get_theme()
        theme.set_properties(self)
        self._on_theme_changed(theme)             
        
class ThemedText(hippo.CanvasText, ThemedWidgetMixin):
    def __init__(self, theme_hints=[], **kwargs):
        super(ThemedText, self).__init__(**kwargs)
        ThemedWidgetMixin.__init__(self, theme_hints=theme_hints)
        
class ThemedLink(hippo.CanvasLink, ThemedWidgetMixin):
    def __init__(self, theme_hints=[], **kwargs):
        super(ThemedLink, self).__init__(**kwargs)
        ThemedWidgetMixin.__init__(self, theme_hints=theme_hints)    

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
        

class GradientHeader(hippo.CanvasGradient, ThemedWidgetMixin):
    def __init__(self, **kwargs):
        hippo.CanvasGradient.__init__(self, 
                                      orientation=hippo.ORIENTATION_HORIZONTAL,
                                      start_color=0xF4F4F4FF, 
                                      end_color=0xC7C7C7FF,
                                      padding_left=4,
                                      color=0x333333FF, **kwargs)        
        
class ActionLink(hippo.CanvasLink, ThemedWidgetMixin):
    def __init__(self, underline=pango.UNDERLINE_NONE, **kwargs):
        if not kwargs.has_key('color'):
            kwargs['color'] = 0x0066DDFF 
        hippo.CanvasLink.__init__(self, **kwargs)
        ThemedWidgetMixin.__init__(self)
        self.set_underline(underline)   

    def set_underline(self, underline):
        if self.get_property('text') is None:
            return
        if underline == pango.UNDERLINE_LOW:
            self.set_property("padding-bottom", 2)
        # TODO: need to change the end index of the underline if the text is changed  
        attrs = self.get_property("attributes") and self.get_property("attributes") or pango.AttrList()
        attrs.insert(pango.AttrUnderline(underline, end_index=len(self.get_property('text'))))
        if len(attrs.get_iterator().get_attrs()) == 1: 
            self.set_property("attributes", attrs)   
    
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
        self.__is_button = False        
        if url:
            self.set_url(url)
        
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
        for k in ['xalign', 'yalign']:
            if k not in kwargs:
                kwargs[k] = hippo.ALIGNMENT_START
        hippo.CanvasImage.__init__(self, **kwargs)
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
        for k in ['xalign', 'yalign']:
            if k not in kwargs:
                kwargs[k] = hippo.ALIGNMENT_START        
        hippo.CanvasImageButton.__init__(self, **kwargs)
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

class PrelightingCanvasBox(hippo.CanvasBox, ThemedWidgetMixin):
    """A box with a background that changes color on mouse hover."""
    def __init__(self, **kwargs):
        self.__hovered = False
        self.__force_prelight = False
        self._prelighted = False        
        hippo.CanvasBox.__init__(self, **kwargs)
        ThemedWidgetMixin.__init__(self)
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
        
    def _on_theme_changed(self, theme):
        self.sync_prelight_color()

    # protected
    def sync_prelight_color(self): 
        if self.__force_prelight or (self.__hovered and self.do_prelight()):
            self.set_property('background-color', self.get_theme().prelight)
            self._prelighted = True
        else:
            self.set_property('background-color', self.get_theme().background)
            self._prelighted = False
            
    # protected
    def do_prelight(self):
        return True
    
class PhotoContentItem(PrelightingCanvasBox):
    """A specialized container that has a photo and some
    corresponding content.  Handles size changes via 
    set_size."""
    def __init__(self, **kwargs):
        if 'spacing' not in kwargs:
            kwargs['spacing'] = 4
        self.__photo = None
        self.__photo_native_width = None
        self.__photo_native_height = None
        self.__child = None
        self.__cb = None            
        PrelightingCanvasBox.__init__(self,
                                      orientation=hippo.ORIENTATION_HORIZONTAL,
                                      **kwargs)

        
    def set_photo(self, photo):
        assert(self.__photo is None)
        self.__photo = photo
        self.__photo_native_width = photo.get_property("scale-width")
        self.__photo_native_height = photo.get_property("scale-height")
        self.append(self.__photo)       
        
    def set_child(self, child):
        assert(self.__child is None)
        self.__child = child
        self.append(self.__child, hippo.PACK_EXPAND)         
        
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

    def set_sync_prelight_callback(self, cb):
        self.__cb = cb

    def sync_prelight_color(self): 
        super(PhotoContentItem, self).sync_prelight_color()
        if self.__cb:
            self.__cb(self._prelighted) 

class IconLink(PrelightingCanvasBox):
    def __init__(self, text="", prelight=True, img_scale_width=20, img_scale_height=20, spacing=4, underline=pango.UNDERLINE_NONE, **kwargs):
        PrelightingCanvasBox.__init__(self,
                                      orientation=hippo.ORIENTATION_HORIZONTAL,
                                      spacing=spacing, **kwargs)
        self.img = hippo.CanvasImage(scale_width=img_scale_width, scale_height=img_scale_height, xalign=hippo.ALIGNMENT_CENTER, yalign=hippo.ALIGNMENT_CENTER)
        self.append(self.img)
        self.link = ActionLink(text=text, underline=underline, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END,)
        self.append(self.link)
        self.__prelight = prelight

    # override
    def do_prelight(self):
        return self.__prelight

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

