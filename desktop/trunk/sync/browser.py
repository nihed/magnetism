#! /usr/bin/python

import gtk
import gobject
import sys

class BrowserDialog (gtk.Dialog):
    
    def __init__(self):
        gtk.Dialog.__init__(self, title="Browser")
        self.connect("delete_event", self.delete_event)
        self.connect("destroy", self.destroy)

        self.set_border_width(5)
        self.set_resizable(False)
        self.set_has_separator(False)

        self.vbox.set_spacing(2)

        label = gtk.Label()
        label.set_markup("<b>What's Your Browser?</b>")
        label.set_alignment(0.0, 0.5)
        self.vbox.pack_start(label, 0)

        radio1 = gtk.RadioButton(None, "Firefox")
        self.vbox.pack_start(radio1, 0)

        radio2 = gtk.RadioButton(radio1, "Epiphany")
        self.vbox.pack_start(radio2, 0)

        check1 = gtk.CheckButton("Store bookmarks online with del.icio.us")
        self.vbox.pack_start(check1, 0)

        label = gtk.Label()
        label.set_markup("<small>(We'll store your choice online and won't ask again.)</small>")
        label.set_alignment(1.0, 0.5)
        self.vbox.pack_start(label, 0)

        self.vbox.show_all()

        self.add_button(gtk.STOCK_CANCEL, gtk.RESPONSE_CANCEL)
        self.add_button('Launch Browser', gtk.RESPONSE_OK)

    def delete_event(self, widget, event, data=None):
        return False
        
    def destroy(self, widget, data=None):
        gtk.main_quit()
            
def main():

    dialog = BrowserDialog()
    dialog.show()
    
    gtk.main()

main()
