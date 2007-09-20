import cgi
import os
import time

import gobject
import gtk
import hippo

import bigboard
from bigboard.databound import DataBoundItem
from bigboard.big_widgets import ActionLink, CanvasMugshotURLImage, CanvasMugshotURLImageButton, PhotoContentItem, CanvasHBox, CanvasVBox
import bigboard.libbig as libbig

from ddm import DataModel

STATUS_MUSIC = 0

def _open_aim(aim):
    os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'aim:GoIM?screenname=' + cgi.escape(aim))

def _open_webdav(url):
    # We pass around WebDAV URL's using the standard http:// scheme, but nautilus wants dav://
    # instead.

    if url.startswith("http:"):
        url = "dav:" + url[5:]
    
    os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', url)

class PersonItem(PhotoContentItem, DataBoundItem):
    def __init__(self, user, **kwargs):
        PhotoContentItem.__init__(self, **kwargs)
        DataBoundItem.__init__(self, user)
        
        model = DataModel(bigboard.globals.server_name)

        self.set_clickable(True)
        
        self.__photo = CanvasMugshotURLImage(scale_width=45,
                                            scale_height=45,
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
        
        self.__statuses = []
        self.__status_box = CanvasHBox()
        self.__details_box.append(self.__status_box)

        self.connect('button-press-event', self.__handle_button_press)
        self.connect('button-release-event', self.__handle_button_release)
        self.__pressed = False

        self.__aim_icon = None

        self.__current_track = None
        self.__current_track_timeout = None

        query = model.query_resource(self.resource.resource_id, "currentTrack +;currentTrackPlayTime")
        query.add_handler(self.__update_current_track)
        query.execute()

        self.connect_resource(self.__update, 'name')
        self.connect_resource(self.__update, 'photoUrl')
        self.connect_resource(self.__update_aim_buddy, 'aimBuddy')
        self.connect_resource(self.__update_current_track, 'currentTrack')
        self.connect_resource(self.__update_current_track, 'currentTrackPlayTime')
        self.__update(self.resource)
        self.__update_aim_buddy(self.resource)
        self.__update_current_track(self.resource)

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

    def __timeout_track(self):
        self.__current_track_timeout = None
        self.__update_current_track(self.resource)
        return False

    def __update_current_track(self, user):
        try:
            current_track = self.resource.currentTrack
            current_track_play_time = self.resource.currentTrackPlayTime / 1000.
        except AttributeError:
            current_track = None
            current_track_play_time = -1

        # current_track_play_time < 0, current_track != None might indicate stale
        # current_track data
        if current_track_play_time < 0:
            current_track = None

        if current_track != None:
            now = time.time()
            if current_track.duration < 0:
                endTime = current_track_play_time + 30 * 60   # Half hour
            else:
                endTime = current_track_play_time + current_track.duration / 1000. # msec => sec

            if now >= endTime:
                current_track = None

        if current_track != self.__current_track:
            self.__current_track = current_track
            
            if self.__current_track_timeout:
                gobject.source_remove(self.__current_track_timeout)

            if current_track != None:
                # We give 30 seconds of lee-way, so that the track is pretty reliably really stopped
                self.__current_track_timeout = gobject.timeout_add(int((endTime + 30 - now) * 1000), self.__timeout_track)

            if current_track != None:
                self.__set_status(STATUS_MUSIC, TrackItem(current_track))
            else:
                self.__set_status(STATUS_MUSIC, None)
            
    def __set_status(self, type, contents):
        if len(self.__statuses) > 0:
            old_contents = self.__statuses[0][1]
        else:
            old_contents = None
        
        for i in range(0,len(self.__statuses)):
            (i_type,i_contents) = self.__statuses[i]
            if i_type == type:
                i_contents.destroy()
                del self.__statuses[i]
                if i == 0:
                    old_contents = None
                break

        if old_contents != None:
            old_contents.set_visible(False)
        
        if contents != None:
            self.__statuses.insert(0, (type, contents))
            self.__status_box.append(contents)
            
        if len(self.__statuses) > 0:
            new_contents = self.__statuses[0][1]
            new_contents.set_visible(True)

    def get_screen_coords(self):
        return self.get_context().translate_to_screen(self)

class ExternalAccountIcon(CanvasHBox):
    def __init__(self, acct):
        super(ExternalAccountIcon, self).__init__(box_width=16, box_height=16)
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

#
# The CanvasText item thinks that you always want the full text as the tooltip when
# ellipsized, but we are doing our own "tooltip" that is cooler and contains album art
#
class NoTooltipText(hippo.CanvasText, hippo.CanvasItem):
    def __init__(self, **kwargs):
        hippo.CanvasText.__init__(self, **kwargs)

    def do_get_tooltip(self,x,y,for_area):
        return ""

gobject.type_register(NoTooltipText)    
    
class TrackItem(hippo.CanvasBox, DataBoundItem):
    __gsignals__ = {
        'motion-notify-event': 'override'
       }
        
    def __init__(self, track):
        hippo.CanvasBox.__init__(self, orientation=hippo.ORIENTATION_HORIZONTAL)
        DataBoundItem.__init__(self, track)

        image = hippo.CanvasImage(yalign=hippo.ALIGNMENT_START,
                                  image_name="bigboard-music.png",
                                  border_right=6)
        self.append(image)
        
        details = CanvasVBox()
        self.append(details)

        artist_text = NoTooltipText(text=track.artist,
                                    font="12px",
                                    color=0x666666ff,
                                    xalign=hippo.ALIGNMENT_START,
                                    size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        details.append(artist_text)

        title_text = NoTooltipText(text=track.name,
                                   font="12px",
                                   color=0x666666ff,
                                   xalign=hippo.ALIGNMENT_START,
                                   size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        details.append(title_text)

        self.__tooltip_timeout = None
        self.__tooltip_popup = None
        
    def do_motion_notify_event(self, event):
        if event.detail == hippo.MOTION_DETAIL_ENTER:
            if self.__tooltip_timeout == None:
                try:
                    tooltip_time = gtk.settings_get_default().get_property("gtk-tooltip-timeout")
                except TypeError: # Old version of GTK+
                    tooltip_time = 500
                    
                self.__tooltip_timeout = gobject.timeout_add(tooltip_time, self.__on_tooltip_timeout)
        elif event.detail == hippo.MOTION_DETAIL_LEAVE:
            if self.__tooltip_timeout != None:
                gobject.source_remove(self.__tooltip_timeout)
                self.__tooltip_timeout = None
            if self.__tooltip_popup != None:
                self.__tooltip_popup.destroy()
                self.__tooltip_popup = None

    def __on_tooltip_timeout(self):
        self.__tooltip_timeout = None

        if self.__tooltip_popup == None:
            self.__tooltip_popup = TrackPopup(self.resource)

        self.__tooltip_popup.popup()
        
class TrackPopup(hippo.CanvasWindow, DataBoundItem):
    __gsignals__ = {
        'motion-notify-event': 'override'
       }
        
    def __init__(self, track):
        hippo.CanvasWindow.__init__(self, gtk.WINDOW_POPUP)
        DataBoundItem.__init__(self, track)

        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(0xffff,0xffff,0xffff))

        root = CanvasHBox(border=1, border_color=0x000000ff)
        self.set_root(root)
        
        box = CanvasHBox(border=5)
        root.append(box)
        
        image = CanvasMugshotURLImage(box_width=track.imageWidth, box_height=track.imageHeight)
        image.set_url(track.imageUrl)
        box.append(image)
        
        details = CanvasVBox(border_left=6)
        box.append(details)

        artist_text = hippo.CanvasText(text=track.artist,
                                       font="13px",
                                       xalign=hippo.ALIGNMENT_START)
        details.append(artist_text)

        title_text = hippo.CanvasText(text=track.name,
                                      font="13px",
                                      xalign=hippo.ALIGNMENT_START)
        details.append(title_text)

    def popup(self):
        (x,y,_) = gtk.gdk.get_default_root_window().get_pointer()
        self.move(x + 10, y + 10)
        self.show()
        
class LocalFilesLink(hippo.CanvasBox, DataBoundItem):
    def __init__(self, buddy):
        hippo.CanvasBox.__init__(self)
        DataBoundItem.__init__(self, buddy)

        self.__link = ActionLink(text="Shared Files")
        self.__link.connect("activated", self.__open_webdav)
        self.append(self.__link)

        self.connect_resource(self.__update)
        self.__update(self.resource)

    def __get_url(self):
        try:
            return self.resource.webdavUrl
        except AttributeError:
            return None

    def __update(self, buddy):
        self.__link.set_visible(self.__get_url() != None)
        
    def __open_webdav(self, object):
        _open_webdav(self.__get_url())

class ProfileItem(hippo.CanvasBox, DataBoundItem):
    __gsignals__ = {
        "close": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
       }
        
    def __init__(self, user, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        hippo.CanvasBox.__init__(self, **kwargs)
        DataBoundItem.__init__(self, user)

        self.__header = hippo.CanvasGradient(orientation=hippo.ORIENTATION_HORIZONTAL,
                                             start_color=0xf2f2f2f2,
                                             end_color=0xc8c8c8ff)
        self.append(self.__header)
        self.__name = hippo.CanvasText(font="22px", padding=6)
        self.__header.append(self.__name)

        mugshot_link = ActionLink(text="Mugshot", padding=6)
        self.__header.append(mugshot_link, flags=hippo.PACK_END)
        mugshot_link.connect("activated", self.__on_activate_web)

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

        self.__contact_status_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL,
                                                    spacing=4, border=4)
        self.append(self.__contact_status_box)
        
#        self.__online = hippo.CanvasText(text='Offline')
#        self.append(self.__online)

        separator = hippo.CanvasBox(box_height=1, background_color=0xAAAAAAFF)
        self.append(separator)

        self.__ribbon_bar = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL,
                                           spacing=2, border=4)
        self.append(self.__ribbon_bar)

        self.__link_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL,
                                          spacing=2, border=4)
        self.append(self.__link_box)
        
        self.__local_files_link = None

        self.connect_resource(self.__update)
        self.connect_resource(self.__update_contact_status, "contactStatus")
        self.connect_resource(self.__update_loved_accounts, "lovedAccounts")
        self.connect_resource(self.__update_local_buddy, "localBuddy")
        
        query = DataModel(bigboard.globals.server_name).query_resource(self.resource.resource_id, "lovedAccounts +")
        query.add_handler(self.__update_loved_accounts)
        query.execute()
        
        self.__update(self.resource)
        self.__update_contact_status(self.resource)
        self.__update_loved_accounts(self.resource)
        self.__update_local_buddy(self.resource)

    def __add_status_link(self, text, current_status, new_status):
        if current_status == new_status:
            link =hippo.CanvasText(text=text)
        else:
            def set_new_status(object):
                model = DataModel(bigboard.globals.server_name)
                query = model.update(("http://mugshot.org/p/contacts", "setContactStatus"),
                                     user=self.resource,
                                     status=new_status)
                query.execute()
        
            link = ActionLink(text=text)
            link.connect("activated", set_new_status)
        
        self.__contact_status_box.append(link)
        
    def __update_contact_status(self, user):
        self.__contact_status_box.remove_all()
        try:
            status = self.resource.contactStatus
        except AttributeError:
            status = 0

        if status == 0:
            status = 3 

        self.__contact_status_box.append(hippo.CanvasText(text="In sidebar: "))
        
        self.__add_status_link("Always", status, 4)
        self.__add_status_link("Auto", status, 3)
        self.__add_status_link("Never", status, 2)

    def __update_loved_accounts(self, user):
        try:
            accounts = self.resource.lovedAccounts
        except AttributeError:
            accounts = []
        
        self.__ribbon_bar.clear()
        for a in accounts:
            icon = ExternalAccountIcon(a)
            self.__ribbon_bar.append(icon)

    def __update_local_buddy(self, user):
        if self.__local_files_link:
            self.__local_files_link.destroy()
            self.__local_files_link = None

        try:
            buddy = self.resource.localBuddy
        except AttributeError:
            return

        self.__local_files_link = LocalFilesLink(buddy)
        self.__link_box.append(self.__local_files_link)

    def __update(self, user):
        self.__name.set_property('text', self.resource.name)
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
        _open_aim(self.resource.aim)
