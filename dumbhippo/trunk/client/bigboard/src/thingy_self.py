import hippo

import bigboard, identity_spider

class SelfThingy(bigboard.Thingy):
    def __init__(self):
        bigboard.Thingy.__init__(self, "Self")
        
        spider = identity_spider.IdentitySpider()
        
        self.box = hippo.CanvasBox()
        self.box.set_property("orientation", hippo.ORIENTATION_HORIZONTAL)
        
        self.photo = hippo.CanvasImage()          
        self.photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
        self.photo.set_property("xalign", hippo.ALIGNMENT_START)
        self.photo.set_property("yalign", hippo.ALIGNMENT_START)
            
        self.box.append(self.photo)
        
        self.name = hippo.CanvasText()
        self.name.set_property("text", spider.get_self_name())
        self.box.append(self.name)        
        
    def get_content(self):
        return self.box