#! /usr/bin/python

import gtk
import sys
import cairo
import struct

PADDING = 10

class ImageWindow (gtk.Window):

    # are given drawing area coords a pixel inside the pixbuf
    def _coords_inside_pixbuf(self, x, y):
        pixbuf_x = (x - PADDING) / self.scale_x
        pixbuf_y = (y - PADDING) / self.scale_y

        if pixbuf_x < 0 or pixbuf_y < 0:
            return 0

        if pixbuf_x >= self.pixbuf.get_width():
            return 0

        if pixbuf_y >= self.pixbuf.get_height():
            return 0

        return 1
        
    def _drawing_to_pixbuf_x(self, x):
        pixbuf_x = (x - PADDING) / self.scale_x

        if pixbuf_x < 0:
            pixbuf_x = 0

        if pixbuf_x > self.pixbuf.get_width():
            pixbuf_x = self.pixbuf.get_width()

        return int(round(pixbuf_x))

    def _drawing_to_pixbuf_y(self, y):
        pixbuf_y = (y - PADDING) / self.scale_y

        if pixbuf_y < 0:
            pixbuf_y = 0
        
        if pixbuf_y > self.pixbuf.get_height():
            pixbuf_y = self.pixbuf.get_height()

        return int(round(pixbuf_y))

    def _pixbuf_to_drawing_x(self, x):
        return int(round(x * self.scale_x + PADDING))

    def _pixbuf_to_drawing_y(self, y):
        return int(round(y * self.scale_y + PADDING))
    
    def __init__(self, filename):
        gtk.Window.__init__(self)

        self.filename = filename
    
        self.pixbuf = gtk.gdk.pixbuf_new_from_file(filename)

        self.scale_x = -1
        self.scale_y = -1

        self.pixbuf_x = -1
        self.pixbuf_y = -1
        self.inside_pixbuf = 0

        self.click_pixbuf_x = -1
        self.click_pixbuf_y = -1

        vbox = gtk.VBox()
        self.add(vbox)

        self.statusbar = gtk.Statusbar()
        self.statusbar_message_id = -1
        vbox.pack_end(self.statusbar, 0, 0)

        self.status_color_box = gtk.DrawingArea()
        self.status_color_box.set_size_request(16, 16)
        self.statusbar.pack_start(self.status_color_box, 0, 0)
        self.statusbar.reorder_child(self.status_color_box, 0)
        
        sb = gtk.ScrolledWindow()
        sb.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_AUTOMATIC)
        self.drawing = gtk.DrawingArea()
        sb.add_with_viewport(self.drawing)
        vbox.pack_start(sb, 1, 1)

        self.drawing.add_events(gtk.gdk.POINTER_MOTION_MASK |
            gtk.gdk.POINTER_MOTION_HINT_MASK | gtk.gdk.BUTTON_PRESS_MASK |
                                gtk.gdk.BUTTON_RELEASE_MASK)

        self.drawing.connect("expose-event", self.on_drawing_expose)
        self.drawing.connect("motion-notify-event", self.on_drawing_motion)
        self.drawing.connect("button-press-event", self.on_drawing_press)
        self.drawing.connect("button-release-event", self.on_drawing_release)

        self.add_events(gtk.gdk.KEY_PRESS_MASK)
        self.connect("key-press-event", self.on_window_key)

        default_scale = 3
        
        self.set_scale(default_scale)

        vbox.show_all()

        w = self.pixbuf.get_width()*default_scale + 60
        h = self.pixbuf.get_height()*default_scale + 60
        if w > 1000:
            w = 1000
        if h > 700:
            h = 700
        self.set_default_size(w, h)

    def set_scale(self, factor):
        if factor == self.scale_x and factor == self.scale_y:
            return

        self.scale_x = factor
        self.scale_y = factor
        
        w = self.pixbuf.get_width()
        h = self.pixbuf.get_height()

        self.drawing.set_size_request(w * self.scale_x + PADDING*2,
                                      h * self.scale_y + PADDING*2)
        
        self.set_title("%s %dx%d scaled %d%%" %
                       (self.filename, w, h, factor*100))

        self.drawing.queue_draw()
        if (self.drawing.flags() & gtk.REALIZED) != 0:
            self.update_from_mouse_location()

    def on_drawing_expose(self, obj, event):
        cr = event.window.cairo_create()
        cr.rectangle(event.area.x, event.area.y, event.area.width, event.area.height)
        cr.clip()

        cr.set_antialias(cairo.ANTIALIAS_NONE)

        cr.translate(PADDING, PADDING)
        cr.scale(self.scale_x, self.scale_y)
        cr.set_source_pixbuf(self.pixbuf, 0, 0)
        try:
            ## try to avoid fuzzy scaling since we care about pixels
            cr.get_source().set_filter(cairo.FILTER_NEAREST)
        except:
            ## set_filter not in the fc5 version of pygtk
            pass
        cr.paint()

        cr.set_line_join(cairo.LINE_JOIN_MITER)
        cr.set_line_cap(cairo.LINE_CAP_BUTT)

        if self.scale_x >= 4:
            ## draw a grid
            w = self.pixbuf.get_width()
            h = self.pixbuf.get_height()

            cr.set_source_rgba(0.0, 0.0, 0.0, 0.3)


            cr.set_line_width(1.0/self.scale_x)
            i = 0
            while i < w:
                x = i

                cr.move_to(x, 0)
                cr.line_to(x, h)
                cr.stroke()
                
                i = i + 1
            
            cr.set_line_width(1.0/self.scale_y)
            i = 0
            while i < h:
                y = i

                cr.move_to(0, y)
                cr.line_to(w, y)
                cr.stroke()
                
                i = i + 1

        ## Draw a ruler line if we clicked
        if self.click_pixbuf_x >= 0 and self.click_pixbuf_y >= 0:
            dx = self.pixbuf_x - self.click_pixbuf_x
            dy = self.pixbuf_y - self.click_pixbuf_y

            cr.set_source_rgba(0.0, 0.0, 0.0, 0.8)
            cr.set_line_width(1.0)
            if abs(dx) >= abs(dy):
                # horizontal
                cr.move_to(self.click_pixbuf_x, self.click_pixbuf_y)
                cr.line_to(self.pixbuf_x, self.click_pixbuf_y)
                cr.stroke()
            else:
                # vertical
                cr.move_to(self.click_pixbuf_x, self.click_pixbuf_y)
                cr.line_to(self.click_pixbuf_x, self.pixbuf_y)
                cr.stroke()

        if self.inside_pixbuf and self.pixbuf_x < self.pixbuf.get_width() and \
             self.pixbuf_y < self.pixbuf.get_height():
                
            ## highlight the pixel the mouse is over            
            cr.set_line_width(1.0)
            
            cr.set_source_rgba(1.0, 1.0, 1.0, 0.3)
            cr.rectangle(self.pixbuf_x, self.pixbuf_y,
                         1, 1)
            cr.fill()

            cr.set_source_rgba(0.0, 0.0, 0.0, 0.4)
            cr.rectangle(self.pixbuf_x - .5, self.pixbuf_y - .5,
                         2, 2)
            cr.stroke()

            cr.set_source_rgba(1.0, 1.0, 1.0, 0.4)
            cr.rectangle(self.pixbuf_x - 1.5, self.pixbuf_y - 1.5,
                         4, 4)
            cr.stroke()


        return 0

    def repaint_ruler_line(self):
        if self.click_pixbuf_x >= 0 and self.click_pixbuf_y >= 0 and \
               self.pixbuf_x >= 0 and self.pixbuf_y >= 0:
            old_x1 = self._pixbuf_to_drawing_x(self.click_pixbuf_x)
            old_y1 = self._pixbuf_to_drawing_y(self.click_pixbuf_y)
            old_x2 = self._pixbuf_to_drawing_x(self.pixbuf_x)
            old_y2 = self._pixbuf_to_drawing_x(self.pixbuf_y)

            if old_x1 > old_x2:
                tmp = old_x1
                old_x1 = old_x2
                old_x2 = tmp
            if old_y1 > old_y2:
                tmp = old_y1
                old_y1 = old_y2
                old_y2 = tmp

            self.drawing.queue_draw_area(old_x1 - 2 * self.scale_x,
                                         old_y1 - 2 * self.scale_y,
                                         old_x2 - old_x1 + 4 * self.scale_x,
                                         old_y2 - old_y1 + 4 * self.scale_y)

    def update_from_mouse_location(self):

        (x, y, mask) = self.drawing.window.get_pointer()

        pixbuf_x = self._drawing_to_pixbuf_x(x)
        pixbuf_y = self._drawing_to_pixbuf_y(y)

        self.inside_pixbuf = self._coords_inside_pixbuf(x, y)

        self.set_pixbuf_coords(pixbuf_x, pixbuf_y)

        return 0

    def on_drawing_motion(self, obj, event):
        self.update_from_mouse_location()
        return 0

    def on_drawing_press(self, obj, event):

        if event.button != 1:
            return 0

        gtk.gdk.pointer_grab(self.drawing.window,
                             time=event.time,
                             event_mask = (gtk.gdk.POINTER_MOTION_MASK |
                             gtk.gdk.POINTER_MOTION_HINT_MASK | gtk.gdk.BUTTON_PRESS_MASK |
                             gtk.gdk.BUTTON_RELEASE_MASK))

        pixbuf_x = self._drawing_to_pixbuf_x(event.x)
        pixbuf_y = self._drawing_to_pixbuf_y(event.y)

        self.click_pixbuf_x = pixbuf_x
        self.click_pixbuf_y = pixbuf_y

        self.repaint_ruler_line()

        return 1

    def on_drawing_release(self, obj, event):
        if self.click_pixbuf_x >= 0 and self.click_pixbuf_y >= 0:
            gtk.gdk.pointer_ungrab(time=event.time)
            self.drawing.queue_draw() # just repaint it all

        self.click_pixbuf_x = -1
        self.click_pixbuf_x = -1

        return 1

    def on_window_key(self, obj, event):
        keyname = gtk.gdk.keyval_name(event.keyval)
        if keyname == "plus" or keyname == "equal":
            if self.scale_x < 20:
                self.set_scale(self.scale_x + 1)
        elif keyname == "minus":
            if self.scale_x >= 2:
                self.set_scale(self.scale_x - 1)
        elif keyname == "escape":
            gtk.gdk.pointer_ungrab(time=event.time)
        else:
            print "No action for key " + keyname
            return 0

        return 1

    def repaint_pointer_highlight(self, pixbuf_x, pixbuf_y):
        drawing_x = self._pixbuf_to_drawing_x(pixbuf_x - 3)
        drawing_y = self._pixbuf_to_drawing_y(pixbuf_y - 3)
        self.drawing.queue_draw_area(drawing_x, drawing_y,
                                     7 * self.scale_x,
                                     7 * self.scale_y)

    def set_status(self, text):
        if self.statusbar_message_id >= 0:
            self.statusbar.remove(0, self.statusbar_message_id)
            self.statusbar_message_id = -1

        self.statusbar_message_id = self.statusbar.push(0, text)

    def set_pixbuf_coords(self, pixbuf_x, pixbuf_y):
        if pixbuf_x == self.pixbuf_x and pixbuf_y == self.pixbuf_y:
            return

        # be sure we undraw the old
        self.repaint_ruler_line()
        self.repaint_pointer_highlight(self.pixbuf_x, self.pixbuf_y)

        self.pixbuf_x = pixbuf_x
        self.pixbuf_y = pixbuf_y

        # and draw the new
        self.repaint_ruler_line()
        self.repaint_pointer_highlight(self.pixbuf_x, self.pixbuf_y)

        if pixbuf_x < self.pixbuf.get_width() and pixbuf_y < self.pixbuf.get_height():
            ## this probably copies the whole image around - very peppy
            pixels = self.pixbuf.get_pixels()
            offset = pixbuf_y * self.pixbuf.get_rowstride() + pixbuf_x * self.pixbuf.get_n_channels()
            data = pixels[offset:offset+self.pixbuf.get_n_channels()]

            if len(data) == 4:
                (r, g, b, a) = struct.unpack("BBBB", data)
            elif len(data) == 3:
                (r, g, b) = struct.unpack("BBB", data)
                a = 255
            else:
                raise "somehow data has len %d" % len(data)

            self.status_color_box.modify_bg(gtk.STATE_NORMAL,
                                            gtk.gdk.Color(r*255, g*255, b*255))

            pixel_desc = "%2x%2x%2x alpha %2x at coordinates %d,%d" % (r, g, b, a, self.pixbuf_x, self.pixbuf_y)

        else:
            pixel_desc = "no pixel"
            self.status_color_box.modify_bg(gtk.STATE_NORMAL, None)

        delta_desc = ""
        if self.click_pixbuf_x >= 0 and self.click_pixbuf_y >= 0:
            dx = self.pixbuf_x - self.click_pixbuf_x
            dy = self.pixbuf_y - self.click_pixbuf_y

            if abs(dx) >= abs(dy):
                # horizontal
                dy = 0
            else:
                # vertical
                dx = 0
            
            delta_desc = "dx %d dy %d" % (dx, dy)


        self.set_status("%s %s" % (pixel_desc, delta_desc))

def main():

    if len(sys.argv) < 2:
        raise "have to provide image filenames"

    for arg in sys.argv[1:]:
        window = ImageWindow(arg)
        window.show()
        window.connect("destroy", lambda object: gtk.main_quit())
    
    gtk.main()

main()

