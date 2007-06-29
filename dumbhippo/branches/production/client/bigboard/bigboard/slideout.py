import hippo
import gtk

class Slideout(hippo.CanvasWindow):
    def __init__(self, widget=None):
        super(Slideout, self).__init__(gtk.WINDOW_TOPLEVEL)

        self.__widget = widget

        self.set_type_hint(gtk.gdk.WINDOW_TYPE_HINT_DOCK)
        self.set_resizable(False)
        self.set_keep_above(1)
        self.set_focus_on_map(0)

        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))

        self._root = hippo.CanvasBox()

        self.set_root(self._root)

    def get_root(self):
        return self._root
    
    def slideout(self):
        assert(self.__widget)
        coords = item.get_screen_coords()
        self.slideout_from(coords[0] + item.get_allocation()[0] + 4, coords[1])
    
    def slideout_from(self, x, y):
        self.move(x, y)
        self.present()
    
