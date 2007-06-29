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
import bigboard.globals
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
    __gsignals__ = {
        "close": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
       }
        
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
        self.__photo.set_clickable(True)
        self.__photo.connect("activated", self.__on_activate_web)
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
        
        query = DataModel(bigboard.globals.server_name).query_resource(self.resource.resource_id, "lovedAccounts +")
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

    def __on_activate_web(self, canvas_item):
        self.emit("close")
        libbig.show_url(self.resource.homeUrl)

    def __on_activate_email(self, canvas_item):
        self.emit("close")
        # email should probably cgi.escape except it breaks if you escape the @
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'mailto:' + self.resource.email)

    def __on_activate_aim(self, canvas_item):
        self.emit("close")
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
        
        self._model = DataModel(bigboard.globals.server_name)
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
        
        self.__local_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self.__box.append(self.__local_box)

        self.__separator = hippo.CanvasBox(box_height=1, xalign=hippo.ALIGNMENT_FILL, background_color=0xccccccff)
        self.__box.append(self.__separator)
        
        self.__contact_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)
        self.__box.append(self.__contact_box)

        self.__local_items = {}
        self.__contact_items = {}

        self.__items_by_aim = {}

        self.__slideout = None
        self.__slideout_item = None

        self.__update_separators()
        
        self._model = DataModel(bigboard.globals.server_name)
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
        for i in self.__local_items.values():
            self.__set_item_size(i, size)
        for i in self.__contact_items.values():
            self.__set_item_size(i, size)

    def __on_connected(self):
        query = self._model.query_resource(self._model.self_id, "contacts [+;aim;email]")
        query.add_handler(self.__on_got_self)
        query.execute()

        self.__contact_box.remove_all()
        self.__contact_items = {}
        
    def __on_got_self(self, myself):
        myself.connect(self.__on_contacts_changed, "contacts")
        self.__on_contacts_changed(myself)
        
    def __on_contacts_changed(self, myself):
        old_items = copy.copy(self.__contact_items)
        
        for contact in myself.contacts:
            if contact.resource_id in old_items:
                del old_items[contact.resource_id]
            else:
                self.__add_contact(contact)

        for id in old_items:
            self.__remove_contact(id)

    def __update_separators(self):
        show_separator = len(self.__local_items) != 0 and len(self.__contact_items) != 0
        self.__box.set_child_visible(self.__separator, show_separator)

    def __add_user(self, user, box, map):
        self._logger.debug("user added to people stock %s" % (user.name))
        if map.has_key(user.resource_id):
            return
        
        item = PersonItem(user)
        box.append(item, hippo.PACK_IF_FITS)
        map[user.resource_id] = item
        self.__set_item_size(item, self.get_size())
        item.connect('activated', self.__handle_item_pressed)

        def on_aim_changed(user):
            self.__on_aim_changed(user, item)

        user.connect(on_aim_changed, "aim")
        item.aim = None
        self.__on_aim_changed(user, item)

        buddy = self.__buddy_monitor.get_local_buddy(user.resource_id)
        item.set_local_buddy(buddy)

        self.__update_separators()

    def __remove_user(self, id, box, map):
        item = map[id]
        item.get_user().disconnect(self.__on_aim_changed)
        item.destroy()
        del map[id]
        
    def __add_contact(self, contact):
        self.__add_user(contact, self.__contact_box, self.__contact_items)
        
    def __remove_contact(self, id):
        self.__remove_user(id, self.__contact_box, self.__contact_items)
        
    def __add_local(self, contact):
        self.__add_user(contact, self.__local_box, self.__local_items)
        
    def __remove_local(self, id):
        self.__remove_user(id, self.__local_box, self.__local_items)
        
    def __on_aim_changed(self, user, item):
        try:
            aim = BuddyMonitor.canonicalize_aim(user.aim)
        except AttributeError:
            aim = None

        if item.aim != None:
            self.__items_by_aim[item.aim].remove(item)

        item.aim = aim

        if item.aim != None:
            try:
                self.__items_by_aim[item.aim].append(item)
            except KeyError:
                self.__items_by_aim[item.aim] = [item]

        if aim != None:
            buddy = self.__buddy_monitor.get_aim_buddy(aim)
            item.set_aim_buddy(buddy)
        else:
            item.set_aim_buddy(None)
        
    def __on_aim_added(self, object, buddy):
        aim = BuddyMonitor.canonicalize_aim(buddy.name)

        try:
            items = self.__items_by_aim[aim]
        except KeyError:
            return

        for item in items:
            item.set_aim_buddy(buddy)
                
    def __on_aim_removed(self, object, buddy):
        aim = BuddyMonitor.canonicalize_aim(buddy.name)

        try:
            items = self.__items_by_aim[aim]
        except KeyError:
            return

        for item in items:
            item.set_aim_buddy(buddy)

    def __close_slideout(self, *args):
        if self.__slideout:
            self.__slideout.destroy()
            self.__slideout = None
            self.__slideout_item = None
                
    def __handle_item_pressed(self, item):
        same_item = self.__slideout_item == item
        self.__close_slideout()
        if same_item:
            return True

        self.__slideout = bigboard.slideout.Slideout()
        self.__slideout_item = item
        coords = item.get_screen_coords()
        self.__slideout.slideout_from(coords[0] + item.get_allocation()[0] + 4, coords[1])

        p = ProfileItem(item.get_user())
        self.__slideout.get_root().append(p)
        p.connect("close", self.__close_slideout)

        return True

    # FIXME: Need to handle multiple session for the same resource
    def __on_local_added(self, object, buddy):
        # FIXME: Need to handle self_id changes; easiest thing to do is to
        # repopulate the local box on reconnect
        if buddy.name == self._model.self_id:
            return
        
        try:
            self.__contact_items[buddy.name].set_local_buddy(buddy)
        except KeyError:
            pass

        query = self._model.query_resource(buddy.name, "+;aim;email")
        query.add_handler(self.__on_got_local_user)
        query.execute()
        
    def __on_local_removed(self, object, buddy):
        try:
            self.__contact_items[buddy.name].set_local_buddy(buddy)
        except KeyError:
            pass

        if self.__local_items.has_key(buddy.name):
            self.__remove_local(buddy.name)

    def __on_got_local_user(self, user):
        buddy = self.__buddy_monitor.get_local_buddy(user.resource_id)
        if buddy == None: # Already gone
            return

        self.__add_local(user)
