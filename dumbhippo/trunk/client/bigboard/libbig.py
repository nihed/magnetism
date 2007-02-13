import os

import cairo, gtk

import hippo

class CanvasContext(hippo.CanvasContext):
    def load_image(canvas_context, path):
        pixbuf = gtk.gdk.pixbuf_new_from_file(path)
        return hippo.cairo_surface_from_gdk_pixbuf(pixbuf)
