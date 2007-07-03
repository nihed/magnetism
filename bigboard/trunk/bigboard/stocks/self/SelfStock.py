import logging, os, subprocess

import gobject, gtk, pango
import gnome.ui
import dbus, dbus.glib
import hippo

from mugshot import DataModel
import bigboard.globals
import bigboard.slideout
import bigboard.mugshot as mugshot
import bigboard.libbig as libbig
from bigboard.stock import Stock, AbstractMugshotStock
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasVBox, CanvasHBox, ActionLink, Separator

class FixedCountWrapBox(CanvasVBox):
    def __init__(self, max_row_count, spacing=0, **kwargs):
        super(FixedCountWrapBox, self).__init__(**kwargs)
        self.__spacing = spacing
        self.__max_row_count = max_row_count     
        
    def __row(self, i=None):
        if i is None:
            index = len(self.get_children())-1
        else:
            index = i
        return self.get_children()[index]
        
    def append(self, child):
        if len(self.get_children()) == 0:
            super(FixedCountWrapBox,self).append(CanvasHBox(spacing=self.__spacing)) 
        if len(self.__row().get_children()) >= self.__max_row_count:
            super(FixedCountWrapBox, self).append(CanvasHBox(spacing=self.__spacing))
        row = self.__row()
        row.append(child)

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

class LocalUserDisplay(CanvasVBox):
    def __init__(self, dispname, loginname, displays):
        super(LocalUserDisplay, self).__init__()
        
        self.set_clickable(True)
        
        self.__dispname = hippo.CanvasText(xalign=hippo.ALIGNMENT_END, text=dispname)
        self.append(self.__dispname)
        self.__loginname = hippo.CanvasText(xalign=hippo.ALIGNMENT_END, font='10px Italic', 
                                            text=loginname) 
        attrs = pango.AttrList()
        attrs.insert(pango.AttrForeground(0x6666, 0x6666, 0x6666, 0, 0xFFFF))
        self.__loginname.set_property("attributes", attrs)        

        self.append(self.__loginname)

        self.displays = displays

class SelfSlideout(CanvasVBox):
    __gsignals__ = {
        "logout" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),
        "minimize" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),
        "close" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, [])
    }
    def __init__(self, stock, myself, fus=None, logger=None):
        super(SelfSlideout, self).__init__(border=1, border_color=0x0000000ff,
                                           spacing=4, padding=4)

        self._logger = logger
        
        self.__stock = stock

        self.__personal_box = CanvasHBox()
        self.append(self.__personal_box)
       
        self.__photo = CanvasMugshotURLImage(scale_width=48, scale_height=48)

        self.__personal_box.append(self.__photo)

        self.__personal_box_right = CanvasVBox()
        self.__personal_box.append(self.__personal_box_right)
        
        self.__name_logout = CanvasHBox()
        self.__name = hippo.CanvasText(font="14px Bold",
                                       xalign=hippo.ALIGNMENT_START)
        
        self.__name_logout.append(self.__name)
        self.__logout = hippo.CanvasLink(text='[logout]', xalign=hippo.ALIGNMENT_END,
                                         padding_left=10)
        self.__logout.connect("activated", self.__on_logout)
        self.__name_logout.append(self.__logout, hippo.PACK_EXPAND)
        self.__personal_box_right.append(self.__name_logout)

        self.append(Separator())

        self.__personalization_box = CanvasVBox()
        self.append(self.__personalization_box)
        self.__personalization_box.append(hippo.CanvasText(text='Personalization',
                                                           font='12px Bold',
                                                           xalign=hippo.ALIGNMENT_START))

        self.__mugshot_link = hippo.CanvasLink(xalign=hippo.ALIGNMENT_START)
        self.__mugshot_link.connect("activated", self.__show_mugshot_link)
        self.__personalization_box.append(self.__mugshot_link)
        
        link = hippo.CanvasLink(text='System preferences', xalign=hippo.ALIGNMENT_START)
        link.connect("activated", self.__on_system_preferences)
        self.__personalization_box.append(link)
        link = hippo.CanvasLink(text='Minimize sidebar', xalign=hippo.ALIGNMENT_START)
        link.connect("activated", self.__on_minimize_mode)
        self.__personalization_box.append(link)

        if fus:
            self.__fus = dbus.Interface(fus, 'org.gnome.FastUserSwitch')
            self.__fus.connect_to_signal('UsersChanged', self.__handle_fus_change)
            self.__fus.connect_to_signal('DisplaysChanged', self.__handle_fus_change)
            self.__fus.RecheckDisplays()

            self.append(Separator())
            self.__fus_box = CanvasVBox()
            self.append(self.__fus_box)

            self.__fus_users_box = CanvasVBox()
            self.__fus_box.append(self.__fus_users_box)
            
            link = hippo.CanvasLink(text='Log in as another user', xalign=hippo.ALIGNMENT_START)
            link.connect("activated", self.__do_fus_login_other_user)
            self.__fus_box.append(link)
            self.__fus_users = []
            self.__handle_fus_change()
            
        self.update_self(myself)

    def update_self(self, myself):
        self.__myself = myself
        if myself:
            self.__photo.set_url(myself.photoUrl)
            self.__name.set_property("text", myself.name)
            self.__mugshot_link.set_property("text", 'Visit account page')
        else:
            self.__photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
            self.__name.set_property("text", "Nobody")
            self.__mugshot_link.set_property("text", 'Sign in')
        
    def __show_mugshot_link(self, l):
        if self.__myself:
            url = "/account"
        else:
            url = "/who-are-you"
            
        libbig.show_url(mugshot.get_mugshot().get_baseurl() + url)
        self.emit('close')

    def __on_minimize_mode(self, l): 
        self.emit('minimize')
        self.emit('close')

    def __on_system_preferences(self, l):
        subprocess.Popen(['gnome-control-center'])
        self.emit('close')

    def __on_logout(self, l):
        self.emit('logout')
        self.emit('close')

    def __on_dbus_error(self, err):
        self._logger.exception("D-BUS error: %s", err)

    def __handle_fus_change(self):
        self._logger.debug("Handling FUS change, requesting ListUsers")
        self.__fus.ListUsers(reply_handler=self.__on_fus_users_reply,
                             error_handler=self.__on_dbus_error) 

    def __on_localdisplay_activated(self, disp):
        displays = disp.displays
        display = displays[0] # there should basically never be > 1 display for a user.
                              # If there is, we just choose randomly.
        self.__fus.ActivateDisplay(display)
        self.__fus.RecheckDisplays()
        self.emit('close')

    def __on_fus_users_reply(self, users):
        self.__fus_users_box.remove_all()
        self._logger.debug("Got FUS reply: %s", users)
        self.__fus_users = users
        for user in users:
            proxy = dbus.SessionBus().get_object('org.gnome.FastUserSwitch', user)
            prop_iface = dbus.Interface(proxy, 'org.freedesktop.DBus.Properties')
            props = prop_iface.GetAll('org.gnome.FastUserSwitch.User')
            display_name = props[u'DisplayName']
            login_name = props[u'UserName'] 
            displays = props[u'Displays'] 
            is_logged_in = len(displays) > 0
            if is_logged_in:
                disp = LocalUserDisplay(display_name, login_name, displays) 
                disp.connect('activated', self.__on_localdisplay_activated)
                self.__fus_users_box.append(disp)
        
    def __do_fus_login_other_user(self, l):
        self._logger.debug("Doing NewConsole")
        self.__fus.NewConsole()
        self.emit('close')

class SelfStock(AbstractMugshotStock):
    """Shows a user's Mugshot personal information."""
    def __init__(self, *args, **kwargs):
        super(SelfStock,self).__init__(*args, **kwargs)

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)

        self._namephoto_box = PhotoContentItem()
        self._namephoto_box.set_clickable(True)        
        
        self._photo = CanvasMugshotURLImage(scale_width=48, scale_height=48)
        self._photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
        self._photo.connect("button-press-event", lambda button, event: self.__on_activate())
            
        self._namephoto_box.set_photo(self._photo)
        
        self._name = hippo.CanvasText(text="Nobody")
        self._name.set_property("font", "14px Bold")
        self._name.connect("button-press-event", lambda button, event: self.__on_activate())        
        self._namephoto_box.set_child(self._name)        
        
        self._box.append(self._namephoto_box)
        
        self._whereim_box = FixedCountWrapBox(7, spacing=2)
        self._whereim_box.set_property("padding-top", 4)
        
        self._box.append(self._whereim_box)

        self._signin = ActionLink(text="Please Login or Signup")
        self._box.append(self._signin)
        self._signin.connect("button-press-event", lambda signin, event: self.__on_activate())

        self._model = DataModel(bigboard.globals.server_name)
        
        self.__myself = None
        self._model.add_connected_handler(self.__on_connected)
        if self._model.connected:
            self.__on_connected()

        self.__slideout = None
        self.__slideout_display = None

        self.__create_fus_proxy()

    def __create_fus_proxy(self):
        try:
            self.__fus_service = dbus.SessionBus().get_object('org.gnome.FastUserSwitch',
                                                              '/org/gnome/FastUserSwitch')
            fus = dbus.Interface(self.__fus_service, 'org.gnome.FastUserSwitch')
            fus.RecheckDisplays()
        except dbus.DBusException, e:
            self._logger.debug("Couldn't find org.gnome.FastUserSwitch service, ignoring")
            self.__fus_service = None
            pass

    def __on_connected(self):
        self._box.set_child_visible(self._signin, self._model.self_id == None)
        self._box.set_child_visible(self._whereim_box, self._model.self_id != None)

        query = self._model.query_resource(self._model.self_id, "+;lovedAccounts +")
        query.add_handler(self.__on_got_self)
        query.execute()
        
    def __on_got_self(self, myself):
        self.__myself = myself
        myself.connect(self.__on_self_changed)
        self.__on_self_changed(myself)
        
    def __handle_mugshot_connection_status(self, auth, xmpp, contacts):
        self._box.set_child_visible(self._whereim_box, not not auth)
        self._box.set_child_visible(self._signin, not auth)
            
    def __do_logout(self):
        self._panel.Logout()

    def __do_minimize(self):
        self._panel.Unexpand()
    
    def __on_activate(self):
        if self.__slideout:
            self.__slideout.destroy()
            self.__slideout = None
            return

        self.__create_fus_proxy()
        self.__slideout = bigboard.slideout.Slideout()
        widget = self._box
        coords = widget.get_context().translate_to_screen(widget)
        self.__slideout.slideout_from(coords[0] + widget.get_allocation()[0] + 4, coords[1])
        self.__slideout_display = SelfSlideout(self, self.__myself, fus=self.__fus_service, logger=self._logger)
        self.__slideout_display.connect('minimize', lambda s: self.__do_minimize())
        self.__slideout_display.connect('logout', lambda s: self.__do_logout())
        self.__slideout_display.connect('close', lambda s: self.__on_activate())
        self.__slideout.get_root().append(self.__slideout_display)
        
    def get_authed_content(self, size):
        return self._box

    def get_unauthed_content(self, size):
        return self._box
    
    def set_size(self, size):
        super(SelfStock, self).set_size(size)
        self._namephoto_box.set_size(size)
    
    def __on_self_changed(self, myself):
        self._logger.debug("self (%s) changed" % (myself.resource_id,))
        self._photo.set_url(myself.photoUrl)
        self._name.set_property("text", myself.name)
        
        self._whereim_box.remove_all()
        for acct in myself.lovedAccounts:
            icon = ExternalAccountIcon(acct)
            self._logger.debug("appending external account %s" % (acct.accountType,))
            self._whereim_box.append(icon)

        if self.__slideout_display != None:
            self.__slideout_display.update_self(myself)
