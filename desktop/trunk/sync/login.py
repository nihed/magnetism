#! /usr/bin/python

import gtk
import gobject
import sys
import wnck

class LoginDock (gtk.Window):

    __gsignals__ = {
        'realize' : 'override'
        }
    
    def __init__(self):
        gtk.Window.__init__(self)

        self.set_type_hint(gtk.gdk.WINDOW_TYPE_HINT_DOCK)
        self.set_keep_above(1)
        self.set_focus_on_map(0)
        self.set_property("accept-focus", 0)        

        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.color_parse("#FFFFFF"))

        self.set_default_size(gtk.gdk.screen_width(), 100)

        hbox = gtk.HBox()
        hbox.set_border_width(10)
        hbox.set_spacing(20)
        self.add(hbox)

        label = gtk.Label("> Google Browser Sync")
        label.set_alignment(0.0, 0.5)
        hbox.pack_start(label, 0)

        label = gtk.Label("> Mugshot")
        label.set_alignment(0.0, 0.5)
        hbox.pack_start(label, 0)

        label = gtk.Label("> Log In")
        label.set_alignment(0.0, 0.5)
        hbox.pack_start(label, 0)

        hbox.show_all()

    # thanks to Gimmie (Alex Graveley) for this method
    def do_set_wm_strut(self):
        '''
        Set the _NET_WM_STRUT window manager hint, so that maximized windows
        and/or desktop icons will not overlap this window\'s allocated area.
        See http://standards.freedesktop.org/wm-spec/latest for details.
        '''
        if self.window:
            # values are left, right, top, bottom
            propvals = [0, 0, 0, 0]
            
            geom = self.get_screen().get_monitor_geometry(0)
            alloc = self.allocation

            propvals[3] = alloc.height

            # tell window manager to not overlap buttons with maximized window
            self.window.property_change("_NET_WM_STRUT",
                                        "CARDINAL",
                                        32,
                                        gtk.gdk.PROP_MODE_REPLACE,
                                        propvals)

    def do_realize(self):
        self.move(0, gtk.gdk.screen_height() - self.get_allocation().height)
        
        ret = gtk.Window.do_realize(self)

        self.do_set_wm_strut()

        return ret

class LoginBackground (gtk.Window):
    
    def __init__(self):
        gtk.Window.__init__(self)

        self.set_type_hint(gtk.gdk.WINDOW_TYPE_HINT_DESKTOP)
        self.set_focus_on_map(0)
        self.set_property("accept-focus", 0)        

        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.color_parse("#FFFFFF"))

        self.move(0,0)
        self.set_default_size(gtk.gdk.screen_width(), gtk.gdk.screen_height())

        vbox = gtk.VBox()
        self.add(vbox)
        vbox.set_border_width(50)

        vbox2 = gtk.VBox()
        vbox2.set_spacing(20)
        vbox.pack_start(vbox2, 0)

        label = gtk.Label()
        label.set_markup('<span size="x-large"><b>Welcome to Fedora Live CD</b></span>')
        label.set_alignment(0.0, 0.5)
        vbox2.pack_start(label, 0)

        label = gtk.Label()
        label.set_text("Fedora can store your data and settings online. You'll need to create or log in to a Mugshot account. If you use Google Browser Sync, install and configure it first, since your Mugshot login may be stored with Google.")
        label.set_line_wrap(1)
        label.set_alignment(0.0, 0.5)
        vbox2.pack_start(label, 0)
        
        vbox.show_all()
        

class WindowTracker:

    def __init__(self):

        self.screen = wnck.screen_get(0)

        self.screen.connect('window-opened', self.window_opened)
        self.screen.connect('window-closed', self.window_closed)

    def window_opened(self, screen, window):
        #print window.get_class_group().get_res_class()
        if window.get_class_group().get_res_class() == "Firefox-bin":
            window.maximize()
        #print window.get_name() + " opened"

    def window_closed(self, screen, window):
        #print window.get_name() + " closed"
        pass

def main():

    sidebar = LoginDock()
    sidebar.show()

    bg = LoginBackground()
    bg.show()

    tracker = WindowTracker()

    #gobject.spawn_async(argv=["firefox",
    #                          "http://www.google.com/tools/firefox/browsersync/"],
    #                    flags = gobject.SPAWN_SEARCH_PATH)

    #gobject.spawn_async(argv=["firefox",
    #                          "http://mugshot.org/who-are-you"],
    #                    flags = gobject.SPAWN_SEARCH_PATH)

    
    gtk.main()

main()
