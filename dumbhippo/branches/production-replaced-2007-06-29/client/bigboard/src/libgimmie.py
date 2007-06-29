# Stuff imported from gimmie

import os, logging
from gettext import gettext as _

import gobject
import gtk

# gimmie_gui.py
class DockWindow(gtk.Window):
    __gsignals__ = {
        'realize' : 'override'
        }

    def __init__(self, edge_gravity):
        gtk.Window.__init__(self, gtk.WINDOW_TOPLEVEL)

        self.set_type_hint(gtk.gdk.WINDOW_TYPE_HINT_DOCK)
        self.set_resizable(False)
        self.set_keep_above(1)
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

        self.content_align = gtk.Alignment(xscale=1.0, yscale=1.0)
        self.content_align.add(self.content)
        self.content_align.show()
        self.add(self.content_align)

    def get_content(self):
        return self.content

    def get_content_alignment(self):
        return self.content_align

    def get_edge_gravity(self):
        return self.edge_gravity

    def get_win_placement(self):
        '''This will place the window on the edge corresponding to the edge gravity'''
        
        width, height = self.size_request()
        geom = self.get_screen().get_monitor_geometry(0)
        eg = self.edge_gravity
            
        if eg in (gtk.gdk.GRAVITY_SOUTH, gtk.gdk.GRAVITY_NORTH):
            x = (geom.width / 2) - (width / 2)
        elif eg == gtk.gdk.GRAVITY_EAST:
            x = geom.width - width
        elif eg == gtk.gdk.GRAVITY_WEST:
            x = 0

        if eg in (gtk.gdk.GRAVITY_EAST, gtk.gdk.GRAVITY_WEST):
            y = (geom.height / 2) - (height / 2)
        elif eg == gtk.gdk.GRAVITY_SOUTH:
            y = geom.height - height
        elif eg == gtk.gdk.GRAVITY_NORTH:
            y = 0

        return [geom.x + x, geom.y + y] # Compensate for multiple monitors

    def move_to_position(self):
        apply(self.move, self.get_win_placement())

    def do_realize(self):
        ret = gtk.Window.do_realize(self)
        self.move_to_position()
        self.do_set_wm_strut()
        return ret      

    # thanks to Gimmie (Alex Graveley) for this method
    def do_set_wm_strut(self):
        '''
        Set the _NET_WM_STRUT window manager hint, so that maximized windows
        and/or desktop icons will not overlap this window\'s allocated area.
        See http://standards.freedesktop.org/wm-spec/latest for details.
        '''
        if self.edge_gravity != gtk.gdk.GRAVITY_WEST:
            raise "haven't implemented gravities other than WEST"
        
        if self.window:
            # values are left, right, top, bottom
            propvals = [0, 0, 0, 0]
            
            geom = self.get_screen().get_monitor_geometry(0)
            alloc = self.allocation

            logging.debug("setting west strut to %d width" % (alloc.width,))

            propvals[0] = alloc.width

            # tell window manager to not overlap buttons with maximized window
            self.window.property_change("_NET_WM_STRUT",
                                        "CARDINAL",
                                        32,
                                        gtk.gdk.PROP_MODE_REPLACE,
                                        propvals)

