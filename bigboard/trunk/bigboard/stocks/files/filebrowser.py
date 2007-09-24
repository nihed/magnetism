import logging, subprocess, urllib

import gtk
import hippo

from bigboard.big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, ActionLink, PrelightingCanvasBox

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
    
        self.__box = CanvasHBox(xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)

        browse_text = hippo.CanvasText(text="Browse:", font="14px", padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
        self.__box.append(browse_text)

        browse_options = CanvasVBox(xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
        self.__box.append(browse_options)

        local_files_link = ActionLink(text="Local Files", font="14px", padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
        local_files_link.connect("activated", self.__on_browse_local_files_clicked)
        browse_options.append(local_files_link)
 
        for google_account in self.__stock.googles.itervalues():
            google_docs_link = ActionLink(text=google_account.get_auth()[0] + " Docs", font="14px", padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
            google_docs_link.connect("activated", self.__on_link_clicked, create_account_url(google_account.get_auth()[0]))
            browse_options.append(google_docs_link)

        self.set_root(self.__box)

    def __on_browse_local_files_clicked(self, canvas_item):
        subprocess.Popen(['nautilus', '--browser', self.__stock.desktop_path])

    def __on_link_clicked(self, canvas_item, url):
        subprocess.Popen(['gnome-open', url])
