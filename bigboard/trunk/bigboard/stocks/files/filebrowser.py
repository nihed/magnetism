import logging, subprocess, urllib

import gtk
import hippo

from bigboard.big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, ActionLink, IconLink, PrelightingCanvasBox
from bigboard.overview_table import OverviewTable

_logger = logging.getLogger("bigboard.FileBrowser")

def create_account_url(account):
    account = urllib.unquote(account)
    domain = account[account.find("@") + 1:]
    if domain == "gmail.com":
        return "http://docs.google.com"
    else:
        return "https://docs.google.com/a/" + domain

class FileBrowser(hippo.CanvasWindow):
    def __init__(self, stock):
        super(FileBrowser, self).__init__(gtk.WINDOW_TOPLEVEL)
        
        self.__stock = stock
        
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        

        self.set_title('Files')
        self.set_default_size(750, 600)
    
        self.__box = CanvasVBox(xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START, box_width=750, box_height=600)

        browse_text = hippo.CanvasText(text="Browse:", font="14px", padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
        self.__box.append(browse_text)

        browse_options = CanvasVBox(xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START, box_width=750)
        self.__box.append(browse_options)

        local_files_link = ActionLink(text="Local Files", font="14px", padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
        local_files_link.connect("activated", self.__on_browse_local_files_clicked)
        browse_options.append(local_files_link)
 
        for google_account in self.__stock.googles.itervalues():
            google_docs_link = ActionLink(text=google_account.get_auth()[0] + " Docs", font="14px", padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
            google_docs_link.connect("activated", self.__on_link_clicked, create_account_url(google_account.get_auth()[0]))
            browse_options.append(google_docs_link)

        self.__files_outter_box = CanvasHBox(box_height=500)
        self.__box.append(self.__files_outter_box, hippo.PACK_EXPAND)

        self.__right_scroll = hippo.CanvasScrollbars()
        self.__right_scroll.set_policy(hippo.ORIENTATION_HORIZONTAL,
                                       hippo.SCROLLBAR_NEVER)
        self.__files_box = CanvasVBox(border=0, background_color=0xFFFFFFFF)
        self.__files_outter_box.append(self.__right_scroll, hippo.PACK_EXPAND)
        
        self.__file_list = OverviewTable()
        self.__files_box.append(self.__file_list, hippo.PACK_EXPAND)

        self.__right_scroll.set_root(self.__files_box) 
 
        files_section = 0
        section = self.__file_list.add_section_head(files_section, "Recent Files")
        section.set_visible(True)

        for a_file in self.__stock.get_files():         
            if a_file.is_valid():                          
                link = IconLink(a_file.get_name())
                link.img.set_property('image-name', a_file.get_image_name())
                link.link.connect("activated", self.__on_link_clicked, a_file.get_url())
                self.__file_list.add_column_item(files_section, link)

        self.connect("delete-event", lambda *args: self.__hide() or True)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))

        self.set_root(self.__box)

    def __on_browse_local_files_clicked(self, canvas_item):
        subprocess.Popen(['nautilus', '--browser', self.__stock.desktop_path])

    def __on_link_clicked(self, canvas_item, url):
        subprocess.Popen(['gnome-open', url])

    def __hide(self):
        self.hide()
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.__hide()
