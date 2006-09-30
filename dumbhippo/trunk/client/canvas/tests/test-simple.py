import pygtk
pygtk.require('2.0')
import gtk
import hippo

window = gtk.Window()

canvas = hippo.Canvas()
window.add(canvas)
canvas.show()

box = hippo.CanvasBox()
box.set_property("box_height",100)
box.set_property("box_width",100)
box.set_property("background-color", 0xff0000ff)
canvas.set_root(box)
canvas.set_size_request(100, 100)

window.show()

gtk.main()
