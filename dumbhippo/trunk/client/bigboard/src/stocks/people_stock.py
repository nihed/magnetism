import logging

import hippo

import os
import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage, CanvasMugshotURLImageButton, PhotoContentItem
import slideout
import profile
import cgi

class EntityItem(PhotoContentItem):
    def __init__(self, **kwargs):
        PhotoContentItem.__init__(self, **kwargs)
        
        self.__entity = None

        self.__photo = CanvasMugshotURLImage(scale_width=30,
                                            scale_height=30,
                                            border=1,
                                            border_color=0x000000ff)

        self.set_photo(self.__photo)

        self.__name = hippo.CanvasText(xalign=hippo.ALIGNMENT_FILL, yalign=hippo.ALIGNMENT_START,
                                      size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.set_child(self.__name)

        self.connect('button-press-event', self.__handle_button_press)
        self.connect('button-release-event', self.__handle_button_release)
        self.__pressed = False

    def __update_color(self):
        if self.__pressed:
            self.set_property('background-color', 0x00000088)
        else:
            self.sync_prelight_color()

    def __handle_button_press(self, self2, event):
        if event.button != 1:
            return False
        
        self.__pressed = True

        self.__update_color()

    def __handle_button_release(self, self2, event):
        if event.button != 1:
            return False

        self.__pressed = False

        self.__update_color()
            
    def set_entity(self, entity):
        if self.__entity == entity:
            return
        self.__entity = entity
        self.__update()

    def get_guid(self):
        return self.__entity.get_guid()

    def get_entity(self):
        return self.__entity

    def set_size(self, size):
        if size == bigboard.Stock.SIZE_BULL:
            self.set_child_visible(self.__name, True)
            self.__photo.set_property('xalign', hippo.ALIGNMENT_START)
            self.__photo.set_property('yalign', hippo.ALIGNMENT_START)
        else:
            self.set_child_visible(self.__name, False)
            self.__photo.set_property('xalign', hippo.ALIGNMENT_CENTER)
            self.__photo.set_property('yalign', hippo.ALIGNMENT_CENTER)

    def __update(self):
        if not self.__entity:
            return
        self.__name.set_property("text", self.__entity.get_name())
        if self.__entity.get_photo_url():
            self.__photo.set_url(self.__entity.get_photo_url())

    def get_screen_coords(self):
        return self.get_context().translate_to_screen(self)

class ProfileItem(hippo.CanvasBox):
    def __init__(self, profiles, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        kwargs['border'] = 1
        kwargs['border-color'] = 0x0000000ff
        hippo.CanvasBox.__init__(self, **kwargs)

        self.__profiles = profiles
        self.__entity = None

        self.__top_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.append(self.__top_box)

        self.__photo = CanvasMugshotURLImage(scale_width=60,
                                            scale_height=60,
                                            border=5)
        self.__top_box.append(self.__photo)

        self.__address_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        self.__top_box.append(self.__address_box)

        self.__online = hippo.CanvasText(text='Offline')
        self.append(self.__online)

        self.__ribbon_bar = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL,
                                           spacing=2, border=2)
        self.append(self.__ribbon_bar)

    def set_entity(self, entity):
        if self.__entity == entity:
            return
        self.__entity = entity    

        if self.__entity:

            if self.__entity.get_photo_url():
                self.__photo.set_url(self.__entity.get_photo_url())

            self.__profiles.fetch_profile(self.__entity.get_guid(), self.__on_profile_fetched)

    def __on_activate_email(self, canvas_item, profile):
        # email should probably cgi.escape except it breaks if you escape the @
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'mailto:' + profile.get_email())

    def __on_activate_aim(self, canvas_item, profile):
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'aim:GoIM?screenname=' + cgi.escape(profile.get_aim()))

    def __on_profile_fetched(self, profile):
        if not profile:
            print "failed to fetch profile"
            return
        
        #print str(profile)

        if profile.get_online():
            self.__online.set_property('text', 'Online')
        else:
            self.__online.set_property('text', 'Offline')

        self.__ribbon_bar.remove_all()
        for a in profile.get_accounts():
            badge = CanvasMugshotURLImageButton(scale_width=16, scale_height=16)
            badge.set_url(a['icon'])
            badge.set_property('tooltip', a['linkText']) # doesn't work...
            self.__ribbon_bar.append(badge)

        self.__address_box.remove_all()
        if profile.get_email():
            email = hippo.CanvasLink(text=profile.get_email(), xalign=hippo.ALIGNMENT_START)
            email.connect('activated', self.__on_activate_email, profile)
            self.__address_box.append(email)

        if profile.get_aim():
            aim = hippo.CanvasLink(text=profile.get_aim(), xalign=hippo.ALIGNMENT_START)
            aim.connect('activated', self.__on_activate_aim, profile)
            self.__address_box.append(aim)

class PeopleStock(bigboard.AbstractMugshotStock):
    def __init__(self):
        super(PeopleStock, self).__init__("People")
        
        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)

        self.__items = {}

        self.__slideout = None
        self.__slideout_item = None

        self.__profiles = profile.ProfileFactory()
        
    def _on_mugshot_initialized(self):
        super(PeopleStock, self)._on_mugshot_initialized()
        self._mugshot.connect("entity-added", self.__handle_entity_added)
        self._mugshot.connect("self-changed", self.__handle_self_changed)        
        self._mugshot.get_self()
        self._mugshot.get_network()
        
    def get_content(self, size):
        return self.__box

    def __set_item_size(self, item, size):
        if size == bigboard.Stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        
        item.set_size(size)

    def set_size(self, size):
        super(PeopleStock, self).set_size(size)
        for i in self.__items.values():
            self.__set_item_size(i, size)
            
    def __handle_self_changed(self, mugshot, myself):
        logging.debug("self (%s) changed" % (myself.get_guid(),))
    
    def __handle_entity_added(self, mugshot, entity):
        logging.debug("entity added to people stock %s" % (entity.get_name()))
        if entity.get_type() != 'person':
            return
        item = EntityItem()
        item.set_entity(entity)
        self.__box.append(item)
        self.__items[entity.get_guid()] = item
        self.__set_item_size(item, self.get_size())
        item.connect('button-press-event', self.__handle_item_pressed)

    def __handle_item_pressed(self, item, event):
        if self.__slideout:
            self.__slideout.destroy()
            self.__slideout = None
            if self.__slideout_item == item:
                self.__slideout_item = None
                return

        self.__slideout = slideout.Slideout()
        self.__slideout_item = item
        coords = item.get_screen_coords()
        self.__slideout.slideout_from(coords[0] + item.get_allocation()[0] + 4, coords[1])

        p = ProfileItem(self.__profiles)
        p.set_entity(item.get_entity())
        self.__slideout.get_root().append(p)
