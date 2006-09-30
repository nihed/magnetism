import pygtk
pygtk.require('2.0')
import gtk
import hippocanvas

window = gtk.Window()

canvas = hippocanvas.Canvas()
window.add(canvas)
canvas.show()

box = hippocanvas.CanvasBox()
print dir(box)
box.set_property("box_height",100)
box.set_property("box_width",100)
box.set_property("background-color", 200)
canvas.set_root(box)
canvas.set_size_request(100, 100)

window.show()

gtk.main()
