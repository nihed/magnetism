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

def _open_xmpp(xmpp):
    # FIXME
    pass

def _open_webdav(url):
    # We pass around WebDAV URL's using the standard http:// scheme, but nautilus wants dav://
    # instead.

    if url.startswith("http:"):
        url = "dav:" + url[5:]
    
    os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', url)

class PersonItem(PhotoContentItem):
    def __init__(self, person, **kwargs):
        PhotoContentItem.__init__(self, **kwargs)
        self.person = person
        
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

        if self.person.is_user:
            query = model.query_resource(self.person.resource, "currentTrack +;currentTrackPlayTime")
            query.add_handler(self.__update_current_track)
            query.execute()

            self.person.resource.connect(self.__update_current_track, 'currentTrack')
            self.person.resource.connect(self.__update_current_track, 'currentTrackPlayTime')
            self.__update_current_track(self.person.resource)
            
        self.person.connect('display-name-changed', self.__update)
        self.person.connect('icon-url-changed', self.__update)
        
        self.person.connect('aim-buddy-changed', self.__update_aim_buddy)
        
        self.__update(self.person)
        self.__update_aim_buddy(self.person)

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

    def get_person(self):
        return self.person

    def set_size(self, size):
        if size == bigboard.stock.SIZE_BULL:
            self.set_child_visible(self.__details_box, True)
            self.__photo.set_property('xalign', hippo.ALIGNMENT_START)
            self.__photo.set_property('yalign', hippo.ALIGNMENT_START)
        else:
            self.set_child_visible(self.__details_box, False)
            self.__photo.set_property('xalign', hippo.ALIGNMENT_CENTER)
            self.__photo.set_property('yalign', hippo.ALIGNMENT_CENTER)

    def __update(self, person):
        self.__name.set_property("text", self.person.display_name)
        self.__photo.set_url(self.person.icon_url)

    def __update_aim_buddy(self, person):
        if person.aim_buddy:
            if not self.__aim_icon:
                self.__aim_icon = AimIcon(person.aim_buddy)
                self.__presence_box.append(self.__aim_icon)
        else:
            if self.__aim_icon:
                self.__aim_icon.destroy()
                self.__aim_icon = None

    def __timeout_track(self):
        self.__current_track_timeout = None
        self.__update_current_track(self.person.resource)
        return False

    def __update_current_track(self, person):
        try:
            current_track = self.person.resource.currentTrack
            current_track_play_time = self.person.resource.currentTrackPlayTime / 1000.
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
                                  image_name="bigboard-music",
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

class ProfileItem(hippo.CanvasBox):
    __gsignals__ = {
        "close": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
       }
        
    def __init__(self, person, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        hippo.CanvasBox.__init__(self, **kwargs)
        self.person = person

        self.__header = hippo.CanvasGradient(orientation=hippo.ORIENTATION_HORIZONTAL,
                                             start_color=0xf2f2f2f2,
                                             end_color=0xc8c8c8ff)
        self.append(self.__header)
        self.__name = hippo.CanvasText(font="22px", padding=6)
        self.__header.append(self.__name)

        if person.is_user:
            mugshot_link = ActionLink(text="Mugshot", padding=6)
            self.__header.append(mugshot_link, flags=hippo.PACK_END)
            mugshot_link.connect("activated", self.__on_activate_web)

        self.__top_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.append(self.__top_box)

        self.__photo = CanvasMugshotURLImage(scale_width=60,
                                            scale_height=60,
                                            border=5)

        if person.is_user:
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

        self.person.connect('display-name-changed', self.__update)
        self.person.connect('icon-url-changed', self.__update)
        self.person.connect('aim-changed', self.__update)
        self.person.connect('local-buddy-changed', self.__update_local_buddy)
        self.person.connect('xmpp-changed', self.__update)
        if person.is_user:
            self.person.resource.connect(lambda *args: self.__update(self.person), 'email')
            self.person.resource.connect(self.__update_contact_status, "contactStatus")
            self.person.resource.connect(self.__update_loved_accounts, "lovedAccounts")
        
        query = DataModel(bigboard.globals.server_name).query_resource(self.person.resource, "lovedAccounts +")
        query.add_handler(self.__update_loved_accounts)
        query.execute()

        self.__update(self.person)
        self.__update_local_buddy(self.person)
        
        if self.person.is_user:
            self.__update_contact_status(self.person.resource)
            self.__update_loved_accounts(self.person.resource)

    def __add_status_link(self, text, current_status, new_status):
        if current_status == new_status:
            link =hippo.CanvasText(text=text)
        else:
            def set_new_status(object):
                model = DataModel(bigboard.globals.server_name)
                query = model.update(("http://mugshot.org/p/contacts", "setContactStatus"),
                                     person=self.person.resource,
                                     status=new_status)
                query.execute()
        
            link = ActionLink(text=text)
            link.connect("activated", set_new_status)
        
        self.__contact_status_box.append(link)
        
    def __update_contact_status(self, person):
        self.__contact_status_box.remove_all()
        try:
            status = self.person.resource.contactStatus
        except AttributeError:
            status = 0

        if status == 0:
            status = 3 

        self.__contact_status_box.append(hippo.CanvasText(text="In sidebar: "))
        
        self.__add_status_link("Always", status, 4)
        self.__add_status_link("Auto", status, 3)
        self.__add_status_link("Never", status, 2)

    def __update_loved_accounts(self, person):
        try:
            accounts = self.person.resource.lovedAccounts
        except AttributeError:
            accounts = []
        
        self.__ribbon_bar.clear()
        for a in accounts:
            icon = ExternalAccountIcon(a)
            self.__ribbon_bar.append(icon)

    def __update_local_buddy(self, person):
        if self.__local_files_link:
            self.__local_files_link.destroy()
            self.__local_files_link = None

        try:
            buddy = person.local_buddy
        except AttributeError:
            return

        if buddy != None:
            self.__local_files_link = LocalFilesLink(buddy)
            self.__link_box.append(self.__local_files_link)

    def __update(self, person):
        self.__name.set_property('text', self.person.display_name)
        self.__photo.set_url(self.person.icon_url)

        self.__address_box.remove_all()

        email = None
        if person.is_user:
            try:
                email = self.person.resource.email
            except AttributeError:
                pass
         
        if email != None:
            email = hippo.CanvasLink(text=email, xalign=hippo.ALIGNMENT_START)
            email.connect('activated', self.__on_activate_email)
            self.__address_box.append(email)

        if person.aim != None:
            aim = hippo.CanvasLink(text=person.aim, xalign=hippo.ALIGNMENT_START)
            aim.connect('activated', self.__on_activate_aim)
            self.__address_box.append(aim)

        if person.xmpp != None:
            aim = hippo.CanvasLink(text=person.aim, xalign=hippo.ALIGNMENT_START)
            aim.connect('activated', self.__on_activate_xmpp)
            self.__address_box.append(xmpp)

    def __on_activate_web(self, canvas_item):
        self.emit("close")
        libbig.show_url(self.person.resource.homeUrl)

    def __on_activate_email(self, canvas_item):
        self.emit("close")
        # email should probably cgi.escape except it breaks if you escape the @
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'mailto:' + self.person.resource.email)

    def __on_activate_aim(self, canvas_item):
        self.emit("close")
        _open_aim(self.person.aim)
        
    def __on_activate_xmpp(self, canvas_item):
        self.emit("close")
        _open_xmpp(self.person.xmpp)
