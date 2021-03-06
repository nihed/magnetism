# Stuff imported from gimmie

import os, sys, logging
from gettext import gettext as _

import gobject
import gtk

_logger = logging.getLogger("bigboard.DockWindow")

# gimmie_gui.py
class DockWindow(gtk.Window):
    __gsignals__ = {
        'realize' : 'override'
        }

    def __init__(self, edge_gravity):
        gtk.Window.__init__(self, gtk.WINDOW_TOPLEVEL)

        self.set_type_hint(gtk.gdk.WINDOW_TYPE_HINT_DOCK)
        self.set_resizable(False)
        self.stick()
        self.set_focus_on_map(0)
        # self.set_property("accept-focus", 0) # this is probably wrong, we need complex handling (take focus only when certain things are clicked)

        ### Uncomment to have icons grow upwards.
        ### FIXME: This trips metacity bugs and makes the window actually move position
        #self.set_gravity(gtk.gdk.GRAVITY_SOUTH_EAST)

        self.edge_gravity = edge_gravity
        
        if edge_gravity in (gtk.gdk.GRAVITY_EAST, gtk.gdk.GRAVITY_WEST):
            self.content = gtk.VBox (False, 0)
        else:
            self.content = gtk.HBox (False, 0)
        self.content.show()

        self.content_window = gtk.EventBox()
        self.content_align = gtk.Alignment(xscale=1.0, yscale=1.0)
        self.content_align.add(self.content)
        self.content_align.show()
        self.content_window.add(self.content_align)
        self.content_window.show()        
        self.add(self.content_window)

    def get_content(self):
        return self.content
    
    def get_content_window(self):
        return self.content_window

    def get_edge_gravity(self):
        return self.edge_gravity

    def set_gravity(self, gravity):
        self.edge_gravity = gravity

    def do_realize(self):
        ret = gtk.Window.do_realize(self)
        self.do_set_wm_strut()
        return ret
    
    # thanks to Gimmie (Alex Graveley) for this method
    def do_set_wm_strut(self, remove=False):
        '''
        Set the _NET_WM_STRUT window manager hint, so that maximized windows
        and/or desktop icons will not overlap this window\'s allocated area.
        See http://standards.freedesktop.org/wm-spec/latest for details.
        '''
        _logger.debug("Setting WM strut, remove=%d, window=%s" % (remove, str(self.window)))
        
        if self.window and remove:
            self.window.property_delete("_NET_WM_STRUT")
            return
        elif remove:
            return
        
        if not self.edge_gravity in (gtk.gdk.GRAVITY_WEST, gtk.gdk.GRAVITY_EAST):
            raise ValueError("haven't implemented north/south gravity")

        if self.window:
            # values are left, right, top, bottom
            propvals = [0, 0, 0, 0]
            
            geom = self.get_screen().get_monitor_geometry(0)
            (width, height) = self.size_request()

            if self.edge_gravity == gtk.gdk.GRAVITY_WEST:
                _logger.debug("setting WEST strut to %d width" % (width,))
                propvals[0] = width
            elif self.edge_gravity == gtk.gdk.GRAVITY_EAST:
                _logger.debug("setting EAST strut to %d width" % (width,))
                propvals[1] = width 

            # tell window manager to not overlap buttons with maximized window
            self.window.property_change("_NET_WM_STRUT",
                                        "CARDINAL",
                                        32,
                                        gtk.gdk.PROP_MODE_REPLACE,
                                        propvals)


        else:
            _logger.debug("no window, ignoring strut")
            
