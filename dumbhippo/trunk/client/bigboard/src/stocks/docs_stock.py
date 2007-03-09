import logging

import gobject
import hippo

import bigboard, mugshot, google, pango, os
from big_widgets import CanvasMugshotURLImage, PhotoContentItem

class DocDisplay(PhotoContentItem):
    def __init__(self, doc):
        PhotoContentItem.__init__(self, border_right=6)
        self.__doc = None
                
        self.__photo = CanvasMugshotURLImage(scale_width=30, scale_height=30)
        self.set_photo(self.__photo)
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=2, 
                                     border_right=4)
        self.__title = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__description = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        self.__description.set_property("attributes", attrs)        
        self.__box.append(self.__title)
        self.__box.append(self.__description)        
        self.set_child(self.__box)
    
        self.connect("button-press-event", lambda self, event: self.__on_button_press(event))
        
        self.set_doc(doc)
        
    def set_doc(self, doc):
        self.__doc = doc
        #self.__doc.connect("changed", lambda doc: self.__doc_display_sync())
        self.__doc_display_sync()
    
    def __get_title(self):
        if self.__doc is None:
            return "unknown"
        return self.__doc.get_title()
    
    def __str__(self):
        return '<DocDisplay name="%s">' % (self.__get_title())
    
    def __doc_display_sync(self):
        self.__title.set_property("text", self.__doc.get_title())
        #self.__photo.set_url(self.__doc.get_icon_url())
        
    def __on_button_press(self, event):
        if event.button != 1:
            return False
        
        logging.debug("activated doc %s", self)

        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', self.__doc.get_link())

class DocsStock(bigboard.AbstractMugshotStock):
    def __init__(self):
        super(DocsStock, self).__init__("Documents")
        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self._docs = {}

        self.__update_docs()

    def _on_mugshot_initialized(self):
        super(DocsStock, self)._on_mugshot_initialized()

    def get_content(self, size):
        return self._box
            
    def _set_item_size(self, item, size):
        if size == bigboard.Stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        item.set_size(size)            
            
    def set_size(self, size):
        super(DocsStock, self).set_size(size)
        for child in self._box.get_children():
            self._set_item_size(child, size)        

    def __on_load_docs(self, docs):
        self._box.remove_all()
        for doc in docs:
            display = DocDisplay(doc)
            self._box.append(display)

    def __on_failed_load(self, exc_info):
        pass
            
    def __update_docs(self):
        logging.debug("retrieving documents")
        google.Google().fetch_documents(self.__on_load_docs, self.__on_failed_load)
