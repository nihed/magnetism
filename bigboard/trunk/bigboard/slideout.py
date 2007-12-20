import logging
import hippo
import gtk
import gobject
from bigboard.big_widgets import ThemedWidgetMixin

_logger = logging.getLogger('bigboard.Slideout')

class Slideout(hippo.CanvasWindow):
    __gsignals__ = {
    	"button-press-event": "override",
    	"key-press-event": "override",
    	"close": (gobject.SIGNAL_RUN_LAST, None, (bool,))
       }
    def __init__(self, widget=None, modal=True):
        super(Slideout, self).__init__(gtk.WINDOW_POPUP)

        self.__widget = widget
        self.__modal = modal

        # We're not really a dock window anymore since we grab the pointer
        #self.set_type_hint(gtk.gdk.WINDOW_TYPE_HINT_DOCK)
        self.set_resizable(False)
        self.set_keep_above(1)
        self.set_focus_on_map(0)

        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))

        self._root = hippo.CanvasBox()

        self.set_root(self._root)

    def get_root(self):
        return self._root
    
    def slideout(self):
        assert(self.__widget)
        coords = item.get_screen_coords()
        self.slideout_from(coords[0] + item.get_allocation()[0] + 4, coords[1])
    
    def slideout_from(self, x, y):
        screen_w = gtk.gdk.screen_width()
        screen_h = gtk.gdk.screen_height()
        (ignorex, ignorey, w, h) = self.get_allocation()
        offscreen_right = x + w - screen_w
        offscreen_bottom = y + h - screen_h
        if offscreen_right > 0:
            x = x - offscreen_right
        if offscreen_bottom > 0:
            y = y - offscreen_bottom
        self.move(x, y)
        self._sync_preslideout_state()
        self.present_with_time(gtk.get_current_event_time())
        if self.__modal:
            if not self.__do_grabs():
                _logger.debug("grab failed")
                return False
            self.set_modal(True)
        return True
    
    def _sync_preslideout_state(self):
        pass
    
    def __do_grabs(self):
        # owner_events=True says "only grab events going to other applications"; treat
        # events going to this application normally; We need that because we want
        # events to subwindows of ourwindow to be passed appropriately
        if gtk.gdk.pointer_grab(self.window,
                                owner_events=True,
                                event_mask=gtk.gdk.BUTTON_PRESS_MASK,
                                time=gtk.get_current_event_time()) != gtk.gdk.GRAB_SUCCESS:
            return False
        # We don't need owner_events here since keyboard events are always delivered to the
        # toplevel window anyways, and we aren't doing keyboard navigation in any case
        if gtk.gdk.keyboard_grab(self.window,
                                 owner_events=False,
                                 time=gtk.get_current_event_time()) != gtk.gdk.GRAB_SUCCESS:
            # Hiding the window removes the pointer grab
            return False
            
        return True
        
    def do_button_press_event(self, event):
        if event.window == self.window and event.x > 0  and event.x < self.allocation.width and event.y > 0 and event.y < self.allocation.height:
            return hippo.CanvasWindow.do_button_press_event(self, event)
        else:
            self.popdown()
            return True
        
    def do_key_press_event(self, event):
        if event.keyval == gtk.keysyms.Escape:
            self.popdown()
            return True
        return False
            
    def popdown(self):
        self.emit('close', False)

class ThemedSlideout(Slideout, ThemedWidgetMixin):
    def __init__(self, theme_hints=[], **kwargs):
        Slideout.__init__(self, **kwargs)
        ThemedWidgetMixin.__init__(self, theme_hints=theme_hints)        
        
    def _on_theme_changed(self, theme):
        (width, color) = theme.slideout_border
        self._root.set_property('border', width)
        self._root.set_property('border-color', color)
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.color_parse("#%6X" % (theme.background >> 8,)))
        self.queue_draw_area(0,0,-1,-1)

    def _sync_preslideout_state(self):
        theme = self.get_theme()
        if theme.have_compositing():
            self.realize()
            self.set_opacity(theme.opacity)
            _logger.debug("have compositing, set opacity to %s", theme.opacity)            
            self.queue_draw_area(0,0,-1,-1)
