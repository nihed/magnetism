import cgi
import os
import time
import logging

import gobject
import gtk
import hippo

import bigboard
from bigboard.databound import DataBoundItem
from bigboard.big_widgets import ActionLink, CanvasMugshotURLImage, CanvasMugshotURLImageButton, PhotoContentItem, CanvasHBox, CanvasVBox
import bigboard.libbig as libbig
import bigboard.globals as globals

from ddm import DataModel

_logger = logging.getLogger("bigboard.stocks.PeopleStock")

STATUS_MUSIC = 0
STATUS_IM = 1

def _open_aim(aim):
    ## purple-remote only allows lowercase "goim" here, unfortunately I'm guessing 
    ## some other gnome-open handlers only allow "GoIM" ...
    os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'aim:goim?screenname=' + cgi.escape(aim))

def _open_xmpp(xmpp):
    # FIXME, is there some less hardcoded way to do this?
    os.spawnlp(os.P_NOWAIT, 'purple-remote', 'purple-remote', 'jabber:goim?screenname=' + cgi.escape(xmpp))

def _open_webdav(url):
    # We pass around WebDAV URL's using the standard http:// scheme, but nautilus wants dav://
    # instead.

    if url.startswith("http:"):
        url = "dav:" + url[5:]
    
    os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', url)

class StatusMessage(hippo.CanvasText):
    def __init__(self):
        hippo.CanvasText.__init__(self, color=0x666666ff)
        self.__buddies = []

        self.connect('destroy', self.__on_destroy)

    def __on_destroy(self, canvas_item):
        for b in self.__buddies:
            b.disconnect(self.__on_buddy_changed)        

    def set_buddies(self, buddies):
        for b in self.__buddies:
            b.disconnect(self.__on_buddy_changed)

        self.__buddies = buddies
        
        for b in self.__buddies:
            b.connect(self.__on_buddy_changed, 'statusMessage')
            self.__on_buddy_changed(b)

    def __on_buddy_changed(self, buddy):
        message = None
        try:
            message = buddy.statusMessage
        except AttributeError:
            pass
        
        if message:
            message = message.strip()
            if message != '':
                self.set_property('text', message)

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

        self.__presence_box = CanvasHBox(spacing=4)
        self.__details_box.append(self.__presence_box)
        
        self.__statuses = []
        self.__status_box = CanvasHBox()
        self.__details_box.append(self.__status_box)

        self.connect('button-press-event', self.__handle_button_press)
        self.connect('button-release-event', self.__handle_button_release)
        self.__pressed = False

        self.__aim_icon = None
        self.__xmpp_icon = None

        self.__current_track = None
        self.__current_track_timeout = None

        if self.person.is_contact:
            try:
                user = self.person.resource.user
            except AttributeError:
                user = None

            if user:
                query = model.query_resource(user, "currentTrack +;currentTrackPlayTime")
                query.execute()

                user.connect(self.__update_current_track, 'currentTrack')
                user.connect(self.__update_current_track, 'currentTrackPlayTime')
                self.__update_current_track(user)
            
        self.person.connect('display-name-changed', self.__update)
        self.person.connect('icon-url-changed', self.__update)
        
        self.person.connect('aim-buddy-changed', self.__update_aim_buddy)
        self.person.connect('xmpp-buddy-changed', self.__update_xmpp_buddy)
        
        self.__update(self.person)
        self.__update_aim_buddy(self.person)
        self.__update_xmpp_buddy(self.person)

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
        self.__name.set_property("text", self.person.display_name) #+ " " + str(self.person._debug_rank))
        self.__photo.set_url(self.person.icon_url)

    def __reset_im_status(self):
        buddies = self.person.aim_buddies + self.person.xmpp_buddies
        if len(buddies) > 0:
            sm = StatusMessage()
            sm.set_buddies(buddies)
            self.__set_status(STATUS_IM, sm)

    def __update_aim_buddy(self, person):
        if person.aim_buddy:
            if not self.__aim_icon:
                self.__aim_icon = AimIcon(person.aim_buddy)
                self.__presence_box.append(self.__aim_icon)
        else:
            if self.__aim_icon:
                self.__aim_icon.destroy()
                self.__aim_icon = None        

        self.__reset_im_status()

    def __update_xmpp_buddy(self, person):
        if person.xmpp_buddy:
            if not self.__xmpp_icon:
                self.__xmpp_icon = XMPPIcon(person.xmpp_buddy)
                self.__presence_box.append(self.__xmpp_icon)
        else:
            if self.__xmpp_icon:
                self.__xmpp_icon.destroy()
                self.__xmpp_icon = None

        self.__reset_im_status()

    def __timeout_track(self):
        self.__current_track_timeout = None
        self.__update_current_track(self.person.resource.user)
        return False

    def __update_current_track(self, user):
        try:
            current_track = user.currentTrack
            current_track_play_time = user.currentTrackPlayTime / 1000.
        except AttributeError:
            current_track = None
            current_track_play_time = -1

        _logger.debug("current track %s" % str(current_track))

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

class IMIcon(hippo.CanvasLink, DataBoundItem):
    def __init__(self, buddy):
        hippo.CanvasLink.__init__(self)
        DataBoundItem.__init__(self, buddy)
        
        self.connect("activated", self.__on_activated)

        self.connect_resource(self.__update)
        self.__update(self.resource)

    def _get_protocol_name(self):
        raise Exception("implement me")

    def _start_conversation(self):
        raise Exception("implement me")

    def __update(self, buddy):
        text = "%s (%s)" % (self._get_protocol_name(), buddy.status)
        self.set_property("text", text)
        self.set_property("tooltip", "Chat with %s (%s)" % (buddy.name, buddy.status,))
        self.set_clickable(buddy.isOnline)

    def __on_activated(self, object):
        self._start_conversation()


class AimIcon(IMIcon):
    def __init__(self, buddy):
        IMIcon.__init__(self, buddy)        
        
    def _get_protocol_name(self):
        return "AIM"

    def _start_conversation(self):
        _open_aim(self.resource.name)
        return True

class XMPPIcon(IMIcon):
    def __init__(self, buddy):
        IMIcon.__init__(self, buddy)        
        
    def _get_protocol_name(self):
        if 'gmail.com' in self.resource.name:
            return "GTalk"
        else:
            return "XMPP"

    def _start_conversation(self):
        _open_xmpp(self.resource.name)
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
        "close": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (bool,))
       }
        
    def __init__(self, person, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        hippo.CanvasBox.__init__(self, **kwargs)
        self.person = person

        self.__header = hippo.CanvasGradient(orientation=hippo.ORIENTATION_HORIZONTAL,
                                             start_color=0xf2f2f2f2,
                                             end_color=0xc8c8c8ff)

        self.append(self.__header)

        name_vbox = hippo.CanvasBox(padding=6)
        self.__name = hippo.CanvasText(font="22px")
        name_vbox.append(self.__name)
        rename_link = ActionLink(text='rename', font="10px", xalign=hippo.ALIGNMENT_END)
        name_vbox.append(rename_link)

        rename_link.connect('activated', self.__on_rename_activated)

        self.__header.append(name_vbox)

        if person.is_contact:
            try:
                user = person.resource.user
            except AttributeError:
                user = None
            if user:
                mugshot_link = ActionLink(text="Mugshot", padding=6)
                self.__header.append(mugshot_link, flags=hippo.PACK_END)
                mugshot_link.connect("activated", self.__on_activate_web)

        self.__top_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.append(self.__top_box)

        self.__photo = CanvasMugshotURLImage(scale_width=60,
                                            scale_height=60,
                                            border=5)

        if person.is_contact:
            try:
                user = person.resource.user
            except AttributeError:
                user = None
            if user:
                self.__photo.set_clickable(True)
                self.__photo.connect("activated", self.__on_activate_web)

        self.__top_box.append(self.__photo)

        self.__address_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL)
        self.__top_box.append(self.__address_box)

        self.__contact_status_box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL,
                                                    spacing=4, border=4)
        self.append(self.__contact_status_box)

        if person.is_contact:
            self.__add_link = None
            self.__remove_link = ActionLink()
            self.__remove_link.connect('activated', self.__remove_from_network_clicked)
            self.append(self.__remove_link)
        else:
            self.__remove_link = None
            self.__add_link = ActionLink(text=('Add %s to network' % self.person.display_name))
            self.__add_link.connect('activated', self.__add_to_network_clicked)
            self.append(self.__add_link)
        
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
        if person.is_contact:
            self.person.resource.connect(lambda *args: self.__update(self.person), 'emails')
            self.person.resource.connect(self.__update_contact_status, "status")
            try:
                user = person.resource.user
            except AttributeError:
                user = None
            
            if user:
                user.connect(self.__update_loved_accounts, "lovedAccounts")
        
        query = DataModel(bigboard.globals.server_name).query_resource(self.person.resource, "lovedAccounts +")
        query.add_handler(self.__update_loved_accounts)
        query.execute()

        self.__update(self.person)
        self.__update_local_buddy(self.person)
        
        if self.person.is_contact:
            self.__update_contact_status(self.person.resource)

            try:
                user = person.resource.user
            except AttributeError:
                user = None
            
            if user:
                self.__update_loved_accounts(user)

    def __add_status_link(self, text, current_status, new_status):
        if current_status == new_status:
            link = hippo.CanvasText(text=text)
        else:
            def set_new_status(object):
                model = globals.get_data_model()
                query = model.update(("http://mugshot.org/p/contacts", "setContactStatus"),
                                     contact=self.person.resource,
                                     status=new_status)
                query.execute()
        
            link = ActionLink(text=text)
            link.connect("activated", set_new_status)
        
        self.__contact_status_box.append(link)

    def __remove_from_network_clicked(self, link):

        dialog = gtk.MessageDialog(type=gtk.MESSAGE_QUESTION)
        dialog.set_markup("<b>Remove %s from your network?</b>" % (self.person.display_name))
        dialog.format_secondary_text("This will delete %s's contact information and remove %s from your sidebar" % (self.person.display_name, self.person.display_name))
        dialog.add_buttons("Cancel", gtk.RESPONSE_CANCEL, "Remove", gtk.RESPONSE_ACCEPT)

        def remove_from_network_response(dialog, response_id, person):
            dialog.destroy()

            if response_id == gtk.RESPONSE_ACCEPT:
                _logger.debug("removing from network")

                model = globals.get_data_model()
                query = model.update(("http://mugshot.org/p/contacts", "deleteContact"),
                                     contact=person.resource)
                query.execute()

            else:
                _logger.debug("not removing from network")

        dialog.connect("response", lambda dialog, response_id: remove_from_network_response(dialog, response_id, self.person))                

        # action_taken = False to leave the stock open which seems nicer in this case
        self.emit("close", False)

        dialog.show()

    def __on_rename_activated(self, link):
        dialog = gtk.Dialog(title="Rename a contact")
        
        entry = gtk.Entry()
        entry.set_text(self.person.display_name)
        entry.set_activates_default(True)

        hbox = gtk.HBox(spacing=10)
        hbox.pack_start(gtk.Label('Name:'), False, False)
        hbox.pack_end(entry, True, True)
        
        hbox.show_all()

        dialog.vbox.pack_start(hbox)

        dialog.add_buttons("Cancel", gtk.RESPONSE_CANCEL, "Rename", gtk.RESPONSE_ACCEPT)
        dialog.set_default_response(gtk.RESPONSE_ACCEPT)

        def rename_response(dialog, response_id, person):
            dialog.destroy()

            if response_id == gtk.RESPONSE_ACCEPT:
                _logger.debug("renaming this person")

                name = entry.get_text()

                model = globals.get_data_model()
                query = model.update(("http://mugshot.org/p/contacts", "setContactName"),
                                     contact=person.resource, name=name)
                query.execute()

            else:
                _logger.debug("not renaming")

        dialog.connect("response", lambda dialog, response_id: rename_response(dialog, response_id, self.person))                

        # action_taken = False to leave the stock open which seems nicer in this case
        self.emit("close", False)

        dialog.show()

    def __create_contact(self, addressType, address):
        _logger.debug("creating contact %s %s" % (addressType, address))
        
        model = globals.get_data_model()
        query = model.update(("http://mugshot.org/p/contacts", "createContact"),
                             addressType=addressType,
                             address=address)
        query.execute()

    def __add_to_network_clicked(self, link):
        if self.person.aim:
            self.__create_contact('aim', self.person.aim)
        elif self.person.xmpp:
            self.__create_contact('xmpp', self.person.xmpp)

        # action_taken = False to leave the stock open which seems nicer in this case
        self.emit("close", False)

    def __update_contact_status(self, person):
        self.__contact_status_box.remove_all()
        try:
            status = self.person.resource.status
        except AttributeError:
            status = 0

        if status == 0:
            status = 3 

        self.__contact_status_box.append(hippo.CanvasText(text="In sidebar: "))
        
        self.__add_status_link("Top", status, 4)
        self.__add_status_link("Middle", status, 3)
        self.__add_status_link("Bottom", status, 2)

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

        if self.__remove_link:
            self.__remove_link.set_property('text', 
                                            "Remove %s from network" % self.person.display_name)

        emails = None
        if person.is_contact:
            try:
                emails = self.person.resource.emails
            except AttributeError:
                pass
         
        if emails != None and len(emails) > 0:
            email = hippo.CanvasLink(text=emails[0], xalign=hippo.ALIGNMENT_START)
            email.connect('activated', self.__on_activate_email)
            self.__address_box.append(email)

        if person.aim != None:
            aim = hippo.CanvasLink(text=person.aim, xalign=hippo.ALIGNMENT_START)
            aim.connect('activated', self.__on_activate_aim)
            self.__address_box.append(aim)

        if person.xmpp != None:
            xmpp = hippo.CanvasLink(text=person.aim, xalign=hippo.ALIGNMENT_START)
            xmpp.connect('activated', self.__on_activate_xmpp)
            self.__address_box.append(xmpp)

    def __on_activate_web(self, canvas_item):
        self.emit("close", True)
        libbig.show_url(self.person.resource.user.homeUrl)

    def __on_activate_email(self, canvas_item):
        self.emit("close", True)
        # email should probably cgi.escape except it breaks if you escape the @
        os.spawnlp(os.P_NOWAIT, 'gnome-open', 'gnome-open', 'mailto:' + self.person.resource.email)

    def __on_activate_aim(self, canvas_item):
        self.emit("close", True)
        _open_aim(self.person.aim)
        
    def __on_activate_xmpp(self, canvas_item):
        self.emit("close", True)
        _open_xmpp(self.person.xmpp)
