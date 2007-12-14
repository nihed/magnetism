import logging, os, subprocess, urlparse
from distutils.version import LooseVersion as Version

import gobject, gtk, pango
import gconf
import gnome.ui
import dbus, dbus.glib
import hippo

from ddm import DataModel
import bigboard.globals as globals
from bigboard.slideout import ThemedSlideout
import bigboard.libbig as libbig
from bigboard.workboard import WorkBoard
from bigboard.stock import Stock, AbstractMugshotStock
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasVBox, CanvasHBox
from bigboard.big_widgets import ActionLink, IconLink, Separator, ThemedText, ThemedLink
import bigboard.google

import portfoliomanager

_logger = logging.getLogger('bigboard.stocks.SelfStock')

COMPATIBLE_PROTOCOL_VERSION = "0"

GCONF_PREFIX = '/apps/bigboard/'

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

class SelfSlideout(ThemedSlideout):
    __gsignals__ = {
        "account" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),                    
        "logout" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),
        "sidebar-controls" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),
    }
    def __init__(self, stock, myself, fus=None, logger=None):
        super(SelfSlideout, self).__init__()
    
        vbox = CanvasVBox(border=1, border_color=0x0000000ff, spacing=4, padding=4)
        self.get_root().append(vbox)

        self._logger = logger
        
        self.__stock = stock

        self.__personal_box = CanvasHBox(spacing=4)
        vbox.append(self.__personal_box)
       
        self.__photo = CanvasMugshotURLImage(scale_width=48, scale_height=48)

        self.__personal_box.append(self.__photo)

        self.__personal_box_right = CanvasVBox()
        self.__personal_box.append(self.__personal_box_right, hippo.PACK_EXPAND)
        
        self.__name = ThemedText(font="14px Bold",
                                 xalign=hippo.ALIGNMENT_START,
                                 size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        
        self.__personal_box_right.append(self.__name)

        vbox.append(Separator())

        self.__personalization_box = CanvasVBox(spacing=2)
        vbox.append(self.__personalization_box)
        self.__personalization_box.append(ThemedText(text='Personalization',
                                                     font='12px Bold',
                                                     xalign=hippo.ALIGNMENT_START))

        self.__mugshot_link = IconLink(img_scale_width=22, img_scale_height=22, xalign=hippo.ALIGNMENT_START, themed=True)
        self.__mugshot_link.link.connect("activated", self.__show_mugshot_link)
        self.__mugshot_link.img.set_property('image-name', '/usr/share/icons/gnome/22x22/apps/web-browser.png')
        self.__personalization_box.append(self.__mugshot_link)

        link = IconLink(text='Desktop Preferences...', img_scale_width=22, img_scale_height=22, xalign=hippo.ALIGNMENT_START, themed=True)
        link.link.connect("activated", self.__on_system_preferences)
        link.img.set_property('image-name', '/usr/share/icons/gnome/22x22/categories/preferences-system.png')
        self.__personalization_box.append(link)    
        link = IconLink(text='Sidebar Preferences...', img_scale_width=22, img_scale_height=22, xalign=hippo.ALIGNMENT_START, themed=True)
        link.link.connect("activated", self.__on_sidebar_controls)
        link.img.set_property('image-name', '/usr/share/icons/gnome/22x22/categories/preferences-desktop.png')
        self.__personalization_box.append(link)

        vbox.append(Separator())

        if fus:
            self.__fus = dbus.Interface(fus, 'org.gnome.FastUserSwitch')
            self.__fus.connect_to_signal('UsersChanged', self.__handle_fus_change)
            self.__fus.connect_to_signal('DisplaysChanged', self.__handle_fus_change)
            self.__fus.RecheckDisplays()

            self.__fus_box = CanvasVBox()
            vbox.append(self.__fus_box)

            self.__fus_users_box = CanvasVBox()
            self.__fus_box.append(self.__fus_users_box)
            
            link = IconLink(text='Log in as Another User...', img_scale_width=22, img_scale_height=22, xalign=hippo.ALIGNMENT_START, themed=True)
            link.link.connect("activated", self.__do_fus_login_other_user)
            link.img.set_property('image-name', '/usr/share/icons/gnome/22x22/apps/system-users.png')
            self.__fus_box.append(link)
            self.__fus_users = []
            self.__handle_fus_change()
            
        self.__logout_controls_box = CanvasVBox()
        vbox.append(self.__logout_controls_box)

        link = IconLink(text='Logout or Shutdown...', img_scale_width=22, img_scale_height=22, xalign=hippo.ALIGNMENT_START, themed=True)
        link.link.connect("activated", self.__on_logout)
        link.img.set_property('image-name', '/usr/share/icons/gnome/22x22/apps/gnome-shutdown.png')
        self.__logout_controls_box.append(link)

        self.update_self(myself)

    def update_self(self, myself):
        self.__myself = myself
        if myself:
            if myself.photoUrl:
                self.__photo.set_url(myself.photoUrl)
            if myself.name:
                self.__name.set_property("text", myself.name)
            self.__mugshot_link.link.set_property("text", 'Visit Account Page...')
        else:
            self.__photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
            self.__name.set_property("text", "Nobody")
            self.__personalization_box.set_child_visible(self.__mugshot_link, False)
        
    def __show_mugshot_link(self, l):
        self.emit('account')
        self.emit('close', True)

    def __on_sidebar_controls(self, l): 
        self.emit('sidebar-controls')
        self.emit('close', True)

    def __on_system_preferences(self, l):
        subprocess.Popen(['gnome-control-center'])
        self.emit('close', True)

    def __on_logout(self, l):
        self.emit('logout')
        self.emit('close', True)

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
        self.emit('close', True)

class SelfStock(AbstractMugshotStock):
    """Shows a user's Mugshot personal information."""
    __gsignals__ = {
        "info-loaded" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, [])                  
    }
    def __init__(self, *args, **kwargs):
        super(SelfStock,self).__init__(*args, **kwargs)

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)

        self._namephoto_box = PhotoContentItem()
        self._namephoto_box.set_themed()
        self._namephoto_box.set_clickable(True) 
        self._namephoto_box.connect("button-press-event", lambda button, event: self.__on_activate())
        
        self._photo = CanvasMugshotURLImage(scale_width=48, scale_height=48)
        self._photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
        self._namephoto_box.set_photo(self._photo)
        
        self._namephoto_box_child = CanvasHBox()
        self._name = ThemedText(text="Nobody", size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self._name.set_property("font", "14px Bold")
        self._namephoto_box_child.append(self._name)  

        self._bulb = hippo.CanvasImage(xalign=hippo.ALIGNMENT_END, yalign=hippo.ALIGNMENT_CENTER)
        self._bulb.set_property("image-name", 'bigboard-info')
        self._namephoto_box_child.append(self._bulb, hippo.PACK_EXPAND)
        
        self._namephoto_box.set_child(self._namephoto_box_child)      
        
        self._box.append(self._namephoto_box)
        
        self._whereim_box = FixedCountWrapBox(9, spacing=2)
        self._whereim_box.set_property("padding-top", 4)
        
        self._box.append(self._whereim_box)

        self._signin = ThemedLink(text="Enable Online Desktop")
        self._box.append(self._signin)
        self._signin.connect("button-press-event", lambda signin, event: self.__do_account())

        self._model = DataModel(globals.server_name)
        
        self.__myself = None
        self._model.add_ready_handler(self.__on_ready)

        self.info_loaded = False

        self.__slideout = None
        
        self.__portfolio_manager = None

        self.__create_fus_proxy()

        #TODO: need to make this conditional on knowing firefox has started already somehow
        #gobject.timeout_add(2000, self.__idle_first_time_signin_check)

        if self._model.ready:
            self.__on_ready()

    def __idle_first_time_signin_check(self):
        ws = dbus.SessionBus().get_object('org.freedesktop.od.Engine', '/org/gnome/web_services')
        cookiejar = ws.GetCookiesToSend('http://online.gnome.org')
        _logger.debug("got cookies %s", cookiejar)
        if not cookiejar:
            self.__do_account()

    def __create_fus_proxy(self):
        try:
            self.__fus_service = dbus.SessionBus().get_object('org.gnome.FastUserSwitch',
                                                              '/org/gnome/FastUserSwitch')
            fus = dbus.Interface(self.__fus_service, 'org.gnome.FastUserSwitch')
            fus.RecheckDisplays()
        except dbus.DBusException, e:
            _logger.debug("Couldn't find org.gnome.FastUserSwitch service, ignoring")
            self.__fus_service = None
            pass

    def __info_now_loaded(self):
        if not self.info_loaded:
            self.info_loaded = True
            self.emit('info-loaded')

    def __on_ready(self):
        try:
            protocol_version = self._model.global_resource.ddmProtocolVersion
        except AttributeError, e:
            protocol_version = "0"
        if Version(protocol_version) > Version(COMPATIBLE_PROTOCOL_VERSION):
            text = hippo.CanvasText(text="Upgrade required", font='14px Bold', border=1, border_color=0xFF0000FF)
            self._box.append(text)        
            errorbox = CanvasVBox()
            errorbox.append(hippo.CanvasText(text='Upgrade required'))
            self._box.set_child_visible(self._signin, False)
            return
        
        self._box.set_child_visible(self._signin, self._model.self_resource == None)
        self._box.set_child_visible(self._whereim_box, self._model.self_resource != None)

        if self._model.self_resource != None:
            query = self._model.query_resource(self._model.self_resource, "+;lovedAccounts +")
            query.add_handler(self.__on_got_self)
            query.add_error_handler(self.__on_self_datamodel_error)
            query.execute()
        else:
            self.__info_now_loaded()
        
    def __on_self_datamodel_error(self, code, str):
        _logger.error("datamodel error %s: %s", code, str)
        self.__info_now_loaded()

    def __on_got_self(self, myself):
        self.__myself = myself     
        myself.connect(self.__on_self_changed)
        self.__on_self_changed(myself)
        self.__info_now_loaded()
        
    def __handle_mugshot_connection_status(self, auth, xmpp, contacts):
        self._box.set_child_visible(self._whereim_box, not not auth)
        self._box.set_child_visible(self._signin, not auth)
            
    def __do_slideout(self, slideout, widget=None):
        widget_src = widget or self._box
        (box_x, box_y) = self._box.get_context().translate_to_screen(self._box)
        (src_x, src_y) = widget_src.get_context().translate_to_screen(widget_src)
        slideout.slideout_from(box_x + self._box.get_allocation()[0] + 4, src_y)
        slideout.set_size_request(200, -1)
         
    def __do_logout(self):
        self._panel.action_taken()
        self._panel.Logout()

    def __do_sidebar_controls(self):
        # Don't call this here; this ensures that we keep the sidebar visible while
        # stocks are being manipulated
        #self._panel.action_taken()        
        if not self.__portfolio_manager:
            self.__portfolio_manager = portfoliomanager.PortfolioManager(self._panel)
        self.__portfolio_manager.present()
    
    def __do_account(self):
        self._panel.action_taken()        
        if self.__myself:
            url = "/account"
        else:
            url = "/who-are-you"
        libbig.show_url(urlparse.urljoin(globals.get_baseurl(), url))
            
    def __on_activate(self):
        if self.__slideout:
            self.__slideout.destroy()
            self.__slideout = None
            return

        self.__create_fus_proxy()
        self.__slideout = SelfSlideout(self, self.__myself, fus=self.__fus_service, logger=_logger)
        self.__slideout.connect('account', lambda s: self.__do_account())        
        self.__slideout.connect('sidebar-controls', lambda s: self.__do_sidebar_controls())
        self.__slideout.connect('logout', lambda s: self.__do_logout())
        self.__slideout.connect('close', lambda s, a: self.__on_activate())
        self.__do_slideout(self.__slideout)
        
    def get_authed_content(self, size):
        return self._box

    def get_unauthed_content(self, size):
        return self._box
    
    def set_size(self, size):
        super(SelfStock, self).set_size(size)
        self._namephoto_box.set_size(size)
    
    def __on_self_changed(self, myself):
        _logger.debug("self (%s) changed", myself.resource_id)
        _logger.debug("photoUrl: %s", myself.photoUrl)
        self._photo.set_url(myself.photoUrl)
        self._name.set_property("text", myself.name)
        
        self._whereim_box.remove_all()
        for acct in myself.lovedAccounts:
            icon = ExternalAccountIcon(acct)
            _logger.debug("appending external account %s", acct.accountType)
            self._whereim_box.append(icon)

        if self.__slideout != None:
            self.__slideout.update_self(myself)
