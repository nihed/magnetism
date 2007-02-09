import pygtk
pygtk.require('2.0')
import gtk
import hippo

window = gtk.Window()

canvas = hippo.Canvas()
window.add(canvas)
canvas.show()

box = hippo.CanvasBox(background_color=0xff0000ff, xalign=hippo.ALIGNMENT_END,
		      yalign=hippo.ALIGNMENT_END)
canvas.set_root(box)
canvas.set_size_request(100, 100)

box2 = hippo.CanvasBox(background_color=0xffff00ff, box_width=20, box_height=20)
box.append(box2)

window.show()

gtk.main()
