import logging

import gc
import os
import copy
import re
import weakref

import gtk
import gobject
import hippo
import bigboard

from mugshot import DataModel
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasMugshotURLImageButton, PhotoContentItem, CanvasHBox, CanvasVBox
from bigboard.stock import AbstractMugshotStock
from bigboard.databound import DataBoundItem
import bigboard.libbig as libbig
import bigboard.slideout
import bigboard.profile
import cgi

def _open_aim(aim):
    os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'aim:GoIM?screenname=' + cgi.escape(aim))

class PersonItem(PhotoContentItem, DataBoundItem):
    def __init__(self, user, **kwargs):
        PhotoContentItem.__init__(self, **kwargs)
        DataBoundItem.__init__(self, user)
        
        self.set_clickable(True)
        
        self.__photo = CanvasMugshotURLImage(scale_width=30,
                                            scale_height=30,
                                            border=1,
                                            border_color=0x000000ff)

        self.set_photo(self.__photo)

        self.__details_box = CanvasVBox()
        self.set_child(self.__details_box)

        self.__name = hippo.CanvasText(xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START,
                                      size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.__details_box.append(self.__name)

        self.__presence_box = CanvasHBox()
        self.__details_box.append(self.__presence_box)

        self.connect('button-press-event', self.__handle_button_press)
        self.connect('button-release-event', self.__handle_button_release)
        self.__pressed = False

        self.connect_resource(self.__update)
        self.__update(self.resource)

        self.__aim_icon = None
        self.__aim_buddy = None
        
        self.__local_icon = None
        self.__local_buddy = None

    def __update_color(self):
        if self.__pressed:
            self.set_property('background-color', 0x00000088)
        else:
            self.sync_prelight_color()

    def __handle_button_press(self, self2, event):
        if event.button != 1 or event.count != 1:
            return False
        
        self.__pressed = True
        self.__update_color()

        return False

    def __handle_button_release(self, self2, event):
        if event.button != 1:
            return False

        self.__pressed = False
        self.__update_color()

        return False

    def get_user(self):
        return self.resource

    def set_size(self, size):
        if size == bigboard.stock.SIZE_BULL:
            self.set_child_visible(self.__details_box, True)
            self.__photo.set_property('xalign', hippo.ALIGNMENT_START)
            self.__photo.set_property('yalign', hippo.ALIGNMENT_START)
        else:
            self.set_child_visible(self.__details_box, False)
            self.__photo.set_property('xalign', hippo.ALIGNMENT_CENTER)
            self.__photo.set_property('yalign', hippo.ALIGNMENT_CENTER)

    def __update(self, user):
        self.__name.set_property("text", self.resource.name)
        self.__photo.set_url(self.resource.photoUrl)

    def get_screen_coords(self):
        return self.get_context().translate_to_screen(self)

    def __update_presence_box(self):
        self.__presence_box.clear()
        if self.__aim_buddy != None:
            self.__presence_box.append(hippo.CanvasText(text="AIM"))

    def set_aim_buddy(self, buddy):
        if buddy == self.__aim_buddy:
            return

        if self.__aim_icon:
            self.__aim_icon.destroy()
            self.__aim_icon = None

        self.__aim_buddy = buddy

        if self.__aim_buddy:
            self.__aim_icon = AimIcon(self.__aim_buddy)
            self.__presence_box.append(self.__aim_icon)

    def get_aim_buddy(self):
        return self.__aim_buddy

    def set_local_buddy(self, buddy):
        if buddy == self.__local_buddy:
            return

        if self.__local_icon:
            self.__local_icon.destroy()
            self.__local_icon = None

        self.__local_buddy = buddy

        if self.__local_buddy:
            self.__local_icon = LocalIcon(self.__local_buddy)
            self.__presence_box.append(self.__local_icon)

    def get_local_buddy(self):
        return self.__local_buddy

class ExternalAccountIcon(CanvasHBox):
    def __init__(self, acct):
        super(ExternalAccountIcon, self).__init__()
        self.__acct = None
        self.__img = CanvasMugshotURLImage()
        self.append(self.__img)
        self.connect("activated", lambda s2: self.__launch_browser())
        self.set_clickable(True)
        self.set_acct(acct)
        
    def set_acct(self, acct):
        if self.__acct:
            self.__acct.disconnect(self.__sync)
        self.__acct = acct
        self.__acct.connect(self.__sync)
        self.__sync()
        
    def __sync(self):
        self.__img.set_url(self.__acct.iconUrl)
        
    def __launch_browser(self):
        libbig.show_url(self.__acct.link)

class AimIcon(hippo.CanvasText, DataBoundItem):
    def __init__(self, buddy):
        hippo.CanvasText.__init__(self)
        DataBoundItem.__init__(self, buddy)
        
        self.connect("activated", self.__open_aim)
        self.set_clickable(True)

        self.connect_resource(self.__update)
        self.__update(self.resource)
        
    def __update(self, buddy):
        if buddy.status == "Available":
            markup = "<b>AIM</b> "
        else:
            markup = "AIM "
        self.set_property("markup", markup)
        self.set_property("tooltip", "Chat with %s (%s)" % (buddy.name, buddy.status,))
        
    def __open_aim(self, object):
        _open_aim(self.resource.name)
        return True

class LocalIcon(hippo.CanvasText, DataBoundItem):
    def __init__(self, buddy):
        hippo.CanvasText.__init__(self)
        DataBoundItem.__init__(self, buddy)
        
        self.connect_resource(self.__update)
        self.__update(self.resource)
        
    def __update(self, buddy):
        self.set_property("markup", "<b>LOCAL</b> ")
        
class ProfileItem(hippo.CanvasBox, DataBoundItem):
    def __init__(self, user, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        kwargs['border'] = 1
        kwargs['border-color'] = 0x0000000ff
        hippo.CanvasBox.__init__(self, **kwargs)
        DataBoundItem.__init__(self, user)

        self.__top_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.append(self.__top_box)

        self.__photo = CanvasMugshotURLImage(scale_width=60,
                                            scale_height=60,
                                            border=5)
        self.__top_box.append(self.__photo)

        self.__address_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        self.__top_box.append(self.__address_box)

#        self.__online = hippo.CanvasText(text='Offline')
#        self.append(self.__online)

        self.__ribbon_bar = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL,
                                           spacing=2, border=2)
        self.append(self.__ribbon_bar)

        self.connect_resource(self.__update)
        self.connect_resource(self.__update_loved_accounts, "lovedAccounts")
        
        query = DataModel().query_resource(self.resource.resource_id, "lovedAccounts +")
        query.add_handler(self.__update_loved_accounts)
        query.execute()
        
        self.__update(self.resource)
        self.__update_loved_accounts(self.resource)
            
    def __update_loved_accounts(self, user):
        try:
            accounts = self.resource.lovedAccounts
        except AttributeError:
            accounts = []
        
        self.__ribbon_bar.clear()
        for a in accounts:
            icon = ExternalAccountIcon(a)
            self.__ribbon_bar.append(icon)

    def __update(self, user):
        self.__photo.set_url(self.resource.photoUrl)

#         if profile.get_online():
#             self.__online.set_property('text', 'Online')
#         else:
#             self.__online.set_property('text', 'Offline')

        self.__address_box.remove_all()

        try:
            email = self.resource.email
        except AttributeError:
            email = None
         
        try:
            aim = self.resource.aim
        except AttributeError:
            aim = None
         
        if email != None:
            email = hippo.CanvasLink(text=email, xalign=hippo.ALIGNMENT_START)
            email.connect('activated', self.__on_activate_email)
            self.__address_box.append(email)

        if aim != None:
            aim = hippo.CanvasLink(text=aim, xalign=hippo.ALIGNMENT_START)
            aim.connect('activated', self.__on_activate_aim)
            self.__address_box.append(aim)

    def __on_activate_email(self, canvas_item):
        # email should probably cgi.escape except it breaks if you escape the @
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'mailto:' + self.resource.email)

    def __on_activate_aim(self, canvas_item):
        _open_aim(self.resource.name, self.resource.aim)

class BuddyMonitor(gobject.GObject):
    __gsignals__ = {
        "aim-added": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "aim-removed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "local-added": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "local-removed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
       }
        
    def __init__(self):
        gobject.GObject.__init__(self)
        
        self._model = DataModel()
        self._model.add_connected_handler(self.__on_connected)
        if self._model.connected:
            self.__on_connected()
        self.__aim_buddies = {}
        self.__local_buddies = {}
            
    def __on_connected(self):
        query = self._model.query_resource("online-desktop:/o/global", "onlineBuddies +")
        query.add_handler(self.__on_got_buddies)
        query.execute()
        self.__set_new_buddies([])

    def __on_got_buddies(self, globalResource):
        globalResource.connect(self.__on_buddies_changed, "onlineBuddies")
        self.__on_buddies_changed(globalResource)

    def __on_buddies_changed(self, globalResource):
        self.__set_new_buddies(globalResource.onlineBuddies)

    def __set_new_buddies(self, buddies):
        old_aim_buddies = self.__aim_buddies
        self.__aim_buddies = {}

        for buddy in buddies:
            if buddy.protocol != "aim":
                continue

            canonical = BuddyMonitor.canonicalize_aim(buddy.name)
                
            self.__aim_buddies[canonical] = buddy
            if not canonical in old_aim_buddies:
                self.emit("aim-added", buddy)
            else:
                del old_aim_buddies[canonical]

        for aim in old_aim_buddies:
            self.emit("aim-removed", old_aim_buddies[aim])

        old_local_buddies = self.__local_buddies
        self.__local_buddies = {}

        for buddy in buddies:
            if buddy.protocol != "mugshot-local":
                continue

            self.__local_buddies[buddy.name] = buddy
            if not buddy.name in old_local_buddies:
                self.emit("local-added", buddy)
            else:
                del old_local_buddies[buddy.name]

        for local in old_local_buddies:
            self.emit("local-removed", old_local_buddies[local])

            
    def get_aim_buddy(self, aim):
        try:
            return self.__aim_buddies[aim]
        except KeyError:
            return None

    def get_local_buddy(self, user_id):
        try:
            return self.__local_buddies[user_id]
        except KeyError:
            return None

    @staticmethod
    def canonicalize_aim(aim):
        return aim.replace(" ", "").lower()
        
class PeopleStock(AbstractMugshotStock):
    def __init__(self, *args, **kwargs):
        super(PeopleStock, self).__init__(*args, **kwargs)

        self.__box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)

        self.__items = {}

        self.__contact_by_aim = {}
        self.__aim_by_contact_id = {}

        self.__slideout = None
        self.__slideout_item = None

        self._model = DataModel()
        self._model.add_connected_handler(self.__on_connected)
        if self._model.connected:
            self.__on_connected()

        self.__buddy_monitor = BuddyMonitor()
        self.__buddy_monitor.connect("aim-added", self.__on_aim_added)
        self.__buddy_monitor.connect("aim-removed", self.__on_aim_removed)
        self.__buddy_monitor.connect("local-added", self.__on_local_added)
        self.__buddy_monitor.connect("local-removed", self.__on_local_removed)
        
    def get_authed_content(self, size):
        return self.__box

    def __set_item_size(self, item, size):
        if size == bigboard.stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        
        item.set_size(size)

    def set_size(self, size):
        super(PeopleStock, self).set_size(size)
        for i in self.__items.values():
            self.__set_item_size(i, size)

    def __on_connected(self):
        query = self._model.query_resource(self._model.self_id, "contacts [+;aim;email]")
        query.add_handler(self.__on_got_self)
        query.execute()

        self.__box.remove_all()
        self.__items = {}
        
    def __on_got_self(self, myself):
        myself.connect(self.__on_contacts_changed, "contacts")
        self.__on_contacts_changed(myself)
        
    def __on_contacts_changed(self, myself):
        old_items = copy.copy(self.__items)
        
        for contact in myself.contacts:
            if contact.resource_id in old_items:
                del old_items[contact.resource_id]
            else:
                self.__add_contact(contact)

        for id in old_items:
            item = self.__items[id]
            item.get_user().disconnect(self.__on_aim_changed)
            item.destroy()
            del self.__items[id]

        gc.collect()
            
    def __add_contact(self, contact):
        self._logger.debug("user added to people stock %s" % (contact.name))
        if self.__items.has_key(contact.resource_id):
            return
        
        item = PersonItem(contact)
        self.__box.append(item, hippo.PACK_IF_FITS)
        self.__items[contact.resource_id] = item
        self.__set_item_size(item, self.get_size())
        item.connect('activated', self.__handle_item_pressed)

        contact.connect(self.__on_aim_changed, "contact")
        self.__on_aim_changed(contact)

    def __on_aim_changed(self, contact):
        try:
            old_aim = self.__aim_by_contact_id[contact.resource_id]
            del self.__aim_by_contact_id[contact.resource_id]
            del self.__contact_by_aim[old_aim]
        except KeyError:
            pass
        
        try:
            aim = BuddyMonitor.canonicalize_aim(contact.aim)
        except AttributeError:
            return

        self.__contact_by_aim[aim] = contact
        self.__aim_by_contact_id[contact.resource_id] = aim

        buddy = self.__buddy_monitor.get_aim_buddy(aim)
        self.__items[contact.resource_id].set_aim_buddy(buddy)

        buddy = self.__buddy_monitor.get_local_buddy(contact.resource_id)
        self.__items[contact.resource_id].set_local_buddy(buddy)
        
    def __handle_item_pressed(self, item):
        if self.__slideout:
            self.__slideout.destroy()
            self.__slideout = None
            if self.__slideout_item == item:
                self.__slideout_item = None
                return True

        self.__slideout = bigboard.slideout.Slideout()
        self.__slideout_item = item
        coords = item.get_screen_coords()
        self.__slideout.slideout_from(coords[0] + item.get_allocation()[0] + 4, coords[1])

        p = ProfileItem(item.get_user())
        self.__slideout.get_root().append(p)

        return True

    def __on_aim_added(self, object, buddy):
        try:
            aim = BuddyMonitor.canonicalize_aim(buddy.name)
            contact = self.__contact_by_aim[aim]
        except KeyError:
            return

        item = self.__items[contact.resource_id]
        item.set_aim_buddy(buddy)
        
    def __on_aim_removed(self, object, buddy):
        try:
            aim = BuddyMonitor.canonicalize_aim(buddy.name)
            contact = self.__contact_by_aim[aim]
        except KeyError:
            return
        
        item = self.__items[contact.resource_id]
        item.set_aim_buddy(None)


    # FIXME: Need to handle multiple session for the same resource
    def __on_local_added(self, object, buddy):
        try:
            item = self.__items[buddy.name]
        except KeyError:
            return
            
        item.set_local_buddy(buddy)
        
    def __on_local_removed(self, object, buddy):
        try:
            item = self.__items[buddy.name]
        except KeyError:
            return
            
        item.set_local_buddy(None)
