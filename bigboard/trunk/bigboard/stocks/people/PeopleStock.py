import logging

import gc
import cgi
import os
import weakref

import gtk
import gobject
import hippo
import bigboard
from mugshot import DataModel

from bigboard.big_widgets import CanvasMugshotURLImage, CanvasMugshotURLImageButton, PhotoContentItem, CanvasHBox, CanvasVBox
from bigboard.people_tracker import PeopleTracker
from bigboard.stock import AbstractMugshotStock
from bigboard.databound import DataBoundItem
import bigboard.globals
import bigboard.libbig as libbig
import bigboard.slideout
import bigboard.profile

import peoplebrowser

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

        self.__aim_icon = None
        self.__local_icon = None

        self.connect_resource(self.__update, 'name')
        self.connect_resource(self.__update, 'photoUrl')
        self.connect_resource(self.__update_aim_buddy, 'aimBuddy')
        self.connect_resource(self.__update_local_buddy, 'localBuddy')
        self.__update(self.resource)
        self.__update_aim_buddy(self.resource)
        self.__update_local_buddy(self.resource)

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

    def __update_aim_buddy(self, user):
        if self.__aim_icon:
            self.__aim_icon.destroy()
            self.__aim_icon = None

        try:
            buddy = self.resource.aimBuddy
        except AttributeError:
            return

        self.__aim_icon = AimIcon(buddy)
        self.__presence_box.append(self.__aim_icon)

    def __update_local_buddy(self, buddy):
        if self.__local_icon:
            self.__local_icon.destroy()
            self.__local_icon = None

        try:
            buddy = self.resource.localBuddy
        except AttributeError:
            return

        self.__local_icon = LocalIcon(buddy)
        self.__presence_box.append(self.__local_icon)

    def get_screen_coords(self):
        return self.get_context().translate_to_screen(self)

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

        self.__slideout = None
        self.__slideout_item = None

        self.__people_browser = None
        self._add_more_link(self.__on_more_link)        

        self.__update_separators()
        
        self.__tracker = PeopleTracker()
        self.__tracker.contacts.connect("added", self.__on_contact_added)
        self.__tracker.contacts.connect("removed", self.__on_contact_removed)
        self.__tracker.local_users.connect("added", self.__on_local_user_added)
        self.__tracker.local_users.connect("removed", self.__on_local_user_removed)

        self.__model = DataModel(bigboard.globals.server_name)

        for user in self.__tracker.contacts:
            self.__on_contact_added(self.__tracker.contacts, user)
            
        for user in self.__tracker.local_users:
            self.__on_local_user_added(self.__tracker.local_users, user)

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

        self.__update_separators()

    def __remove_user(self, user, box, map):
        try:
            item = map[user.resource_id]
        except KeyError:
            return
        
        item.destroy()
        del map[user.resource_id]
        
        self.__update_separators()

    def __on_contact_added(self, list, contact):
        self.__add_user(contact, self.__contact_box, self.__contact_items)
        
    def __on_contact_removed(self, list, contact):
        self.__remove_user(contact, self.__contact_box, self.__contact_items)
        
    def __on_local_user_added(self, list, user):
        if user.resource_id == self.__model.self_id:
            return
        
        self.__add_user(user, self.__local_box, self.__local_items)
        
    def __on_local_user_removed(self, list, user):
        self.__remove_user(user, self.__local_box, self.__local_items)
        
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

    def __on_more_link(self):
        if self.__people_browser is None:
            self.__people_browser = peoplebrowser.PeopleBrowser(self)
        self.__people_browser.present()
