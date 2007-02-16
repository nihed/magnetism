import os, code, sys, traceback

import cairo, gtk, gobject

import hippo

from libgimmie import DockWindow

import bignative

def set_log_handler(handler):
    bignative.set_log_handler(handler)

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

        # Always occupy the full height
        req.height = geom.height 

        # Never take more than available size
        req.width = min(geom.width, req.width)
        
        return ret



class CommandShell(gtk.Window):
    def __init__(self):
        gtk.Window.__init__(self, type=gtk.WINDOW_TOPLEVEL)
        
        box = gtk.VBox()
        self.output = gtk.TextBuffer()
        self.output_view = gtk.TextView(self.output)
        self.output_view.set_property("editable", False)
        scroll = gtk.ScrolledWindow()
        scroll.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)
        scroll.add(self.output_view)
        box.pack_start(scroll, True, True)
    
        self.input = gtk.TextBuffer()
        self.input_view = gtk.TextView(self.input)
        scroll = gtk.ScrolledWindow()
        scroll.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)        
        scroll.add(self.input_view)        
        box.pack_start(scroll, True, True)
        eval_button = gtk.Button("Eval")
        eval_button.connect("clicked", self.do_eval)
        box.pack_start(eval_button, False)
        self.add(box)

        self.set_size_request(400, 600)
        self.set_focus(self.input_view)
    
    def do_eval(self, entry):
        try:
            code_obj = code.compile_command(self.input.get_property("text"))
            exec code_obj
        except:
            print "exception!"
            self.output.set_property("text", "Unexpected Error: \n" + traceback.format_exc())
