import pygtk
pygtk.require('2.0')
import gtk
import hippo

window = gtk.Window()

canvas = hippo.Canvas()
window.add(canvas)
canvas.show()

box = hippo.CanvasBox(background_color=0xffffffff, xalign=hippo.ALIGNMENT_FILL,
		      yalign=hippo.ALIGNMENT_FILL, orientation=hippo.ORIENTATION_HORIZONTAL)
canvas.set_root(box)
canvas.set_size_request(100, 100)

box2 = hippo.CanvasBox(background_color=0xffff00ff, box_width=20, box_height=20)
box.append(box2)

box2 = hippo.CanvasBox(background_color=0x000000ff, box_width=20, box_height=20)
box.append(box2)

box2 = hippo.CanvasBox(background_color=0x444444ff, box_width=20, box_height=20)
box.append(box2)

box2 = hippo.CanvasBox(background_color=0x666666ff, box_width=20, box_height=20)
box.append(box2)

box2 = hippo.CanvasBox(background_color=0xaaaaaaff, box_width=20, box_height=20)
box.append(box2)

window.show()

gtk.main()
