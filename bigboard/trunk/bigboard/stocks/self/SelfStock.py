import logging, os, subprocess, urlparse

import gobject, gtk, pango
import gconf
import gnome.ui
import dbus, dbus.glib
import hippo

from mugshot import DataModel
import bigboard.globals as globals
import bigboard.slideout
import bigboard.libbig as libbig
from bigboard.workboard import WorkBoard
from bigboard.stock import Stock, AbstractMugshotStock
from bigboard.big_widgets import CanvasMugshotURLImage, PhotoContentItem, CanvasVBox, CanvasHBox, ActionLink, Separator
import bigboard.google

_logger = logging.getLogger('bigboard.stocks.SelfStock')

GCONF_PREFIX = '/apps/bigboard/'

class LoginItem(hippo.CanvasBox):
    __gsignals__ = {
        "login" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT, gobject.TYPE_PYOBJECT))
    }
    def __init__(self, svcname):
        super(LoginItem, self).__init__(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)        

        self.svcname = svcname

        # I can't get Pango to take just "Bold" without a size; not sure what the problem is here
        self.append(hippo.CanvasText(text=svcname, xalign=hippo.ALIGNMENT_START, font="Bold 12px"))
        self.__reauth_text = hippo.CanvasText(text='Login incorrect', xalign=hippo.ALIGNMENT_START,
                                              color=0xFF0000FF)
        self.append(self.__reauth_text)
        self.set_child_visible(self.__reauth_text, False)

        box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.append(box)

        box.append(hippo.CanvasText(text="Email:", xalign=hippo.ALIGNMENT_START, border_right=29))
        self.__username_entry = hippo.CanvasEntry()
        self.__username_entry.set_property("xalign", hippo.ALIGNMENT_FILL)
        box.append(self.__username_entry, hippo.PACK_EXPAND)

        box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL)
        self.append(box)

        box.append(hippo.CanvasText(text="Password:", xalign=hippo.ALIGNMENT_START, border_right=3))
        self.__password_entry = hippo.CanvasEntry()
        self.__password_entry.set_property("xalign", hippo.ALIGNMENT_FILL)
        box.append(self.__password_entry, hippo.PACK_EXPAND)
        self.__password_entry.connect('key-press-event', self.__on_password_keypress)
        self.__password_entry.set_property("password-mode", True)
        
        self.__ok_button = hippo.CanvasButton()
        # why don't keywords work on CanvasButton constructor?
        self.__ok_button.set_property("text", "Login")
        self.__ok_button.set_property("xalign", hippo.ALIGNMENT_END)
        self.append(self.__ok_button)       

        self.__ok_button.connect("activated", self.__on_login_activated)
        
    def __on_password_keypress(self, pw, event):
        if event.key == hippo.KEY_RETURN:
            self.__on_login_activated(None)

    def __on_login_activated(self, somearg):
        self.emit('login', self.__username_entry.get_property("text"),
                  self.__password_entry.get_property("text"))
        
    def get_login(self):
        return (self.__username_entry.get_property("text"), self.__password_entry.get_property("text"))
        
    def set_username(self, name):
        curtext = self.__username_entry.get_property("text")
        if not curtext:
            self.__username_entry.set_property('text', name)
        
    def set_reauth(self):
        self.set_child_visible(self.__reauth_text, True)
        self.__password_entry.set_property('text', '')
        
    def focus_password(self):
        self.__password_entry.get_property('widget').grab_focus()  

class LoginSlideout(CanvasVBox):
    __gsignals__ = {
        "visible" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_BOOLEAN,))
    }    
    def __init__(self):
        super(LoginSlideout, self).__init__(border=1, border_color=0x0000000ff,
                                           spacing=4, padding=4)
        self.__requests_ok = {}
        self.__requests_cancel = {}
        self.__myself = None
        # This is necessary because we need to hold a ref to our own
        # bound method; the workboard uses weak references.
        self.__svcfn = self.__service_pwauth
        WorkBoard().observe(self.__svcfn, 'service.pwauth')
    
    def set_myself(self, myself):
        self.__myself = myself
        myself.connect(self.__on_self_changed)
        self.__sync()
    
    def __service_pwauth(self, svc, cb_ok=None, cb_cancel=None, username=None, reauth=False):
        _logger.debug("handling pwauth for %s username=%s reauth=%s", svc, username, reauth)
        if svc in self.__requests_ok and cb_ok is not None:
            self.__requests_ok[svc][1].add(cb_ok)
            if reauth:
                self.__requests_ok[svc][0].set_reauth()
        elif cb_ok is not None: 
            req = LoginItem(svc)
            self.__requests_ok[svc] = (req, set([cb_ok]))
            self.append(req)
            if username:
                req.set_username(username)
            if reauth:
                req.set_reauth()
            req.connect('login', self.__on_req_login)

        if svc in self.__requests_cancel and cb_cancel is not None:
            self.__requests_cancel[svc].add(cb_cancel)
        elif cb_cancel is not None:
            self.__requests_cancel[svc] = set([cb_cancel])

        _logger.debug("length self.__requests_ok %s", len(self.__requests_ok)) 
        self.emit("visible", len(self.__requests_ok) > 0)
        self.__sync()
        
    def __on_req_login(self, r, username, password):
        pwhidden = ''.join(['*' for x in xrange(len(password))]) # elaborate, but fun
        _logger.debug("got u=%s p=%s for svc=%s", username, pwhidden, r.svcname) 
        (_, cbs) = self.__requests_ok[r.svcname]
        for cb in cbs:
            cb(username, password)
        del self.__requests_ok[r.svcname]
        self.remove(r)
        self.emit("visible", len(self.__requests_ok) > 0)
        
    def logout_requested(self):
        _logger.debug("email logout requested")
        # this logout currently affects all services
        for cb_cancel_list in self.__requests_cancel.values():
            for cb_cancel in cb_cancel_list:
                cb_cancel()
        self.__requests_cancel = {}      

        for cb_requests_info in self.__requests_ok.values(): 
            self.remove(cb_requests_info[0])            
        self.__requests_ok = {}  

    def __accountsvc_username_from_external(self, external):
        if external.accountType == 'PICASA':
            return os.path.split(external.link)[1]
        return None    
        
    def __sync(self):
        if not self.__myself:
            return
        for (svc,(req, cbs)) in self.__requests_ok.iteritems():
            for external in self.__myself.lovedAccounts:
                uname = self.__accountsvc_username_from_external(external)
                if uname:
                    _logger.debug("matched account %s to svc %s yielding username %s", external, svc, uname)
                    req.set_username(uname)
                    break

    def __on_self_changed(self, myself):
        self.__sync()
        
    def focus(self):
        for (svc,(req, cbs)) in self.__requests_ok.iteritems():
            (username, password) = req.get_login()
            if username:
                req.focus_password()
                break

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
        "account" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, []),                    
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
        self.__personal_box.append(self.__personal_box_right, hippo.PACK_EXPAND)
        
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
        visible = gconf.client_get_default().get_bool(GCONF_PREFIX + 'visible')        
        link = hippo.CanvasLink(text='%s sidebar' % (visible and 'Minimize' or 'Show'), xalign=hippo.ALIGNMENT_START)
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
            if myself.photoUrl:
                self.__photo.set_url(myself.photoUrl)
            if myself.name:
                self.__name.set_property("text", myself.name)
            self.__mugshot_link.set_property("text", 'Visit account page')
        else:
            self.__photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
            self.__name.set_property("text", "Nobody")
            self.__mugshot_link.set_property("text", 'Sign in')
        
    def __show_mugshot_link(self, l):
        self.emit('account')
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
        self._namephoto_box.connect("button-press-event", lambda button, event: self.__on_activate())
        
        self._photo = CanvasMugshotURLImage(scale_width=48, scale_height=48)
        self._photo.set_property("image-name", '/usr/share/pixmaps/nobody.png')
        self._namephoto_box.set_photo(self._photo)
        
        self._name = hippo.CanvasText(text="Nobody")
        self._name.set_property("font", "14px Bold")
        self._namephoto_box.set_child(self._name)        
        
        self._box.append(self._namephoto_box)
        
        self._whereim_box = FixedCountWrapBox(7, spacing=2)
        self._whereim_box.set_property("padding-top", 4)
        
        self._box.append(self._whereim_box)

        self._signin = ActionLink(text="Please Login or Signup")
        self._box.append(self._signin)
        self._signin.connect("button-press-event", lambda signin, event: self.__do_account())

        self._model = DataModel(globals.server_name)
        
        self.__myself = None
        self._model.add_connected_handler(self.__on_connected)
        if self._model.connected:
            self.__on_connected()
        else:
            _logger.debug("datamodel not connected, deferring")            

        self.__slideout = None
        self.__slideout_display = None

        self.__auth_section_container = CanvasVBox(spacing=4)
        self.__auth_section_container.append(Separator())        
        self.__auth_section = CanvasHBox()
        self.__auth_section_container.append(self.__auth_section)
        self._box.append(self.__auth_section_container)
        authq_image = hippo.CanvasImage(xalign=hippo.ALIGNMENT_CENTER, yalign=hippo.ALIGNMENT_CENTER,
                                        scale_width=30, scale_height=30)
        authq_image.set_property('image-name', 'gtk-dialog-question')
        self.__auth_section.append(authq_image)
        self.__authq_button = authq_button = hippo.CanvasButton()
        authq_button.set_property('text', 'Login To Your Accounts')
        authq_button.connect('activated', self.__on_authq_button_activated)
        self.__auth_section.append(authq_button)

        self.__logout_link = ActionLink(text="Logout From Your Accounts", xalign=hippo.ALIGNMENT_START)
        self.__logout_link.connect("button-press-event", lambda b,e: self.__on_logout_activated())
        self.__auth_section_container.append(self.__logout_link, hippo.PACK_EXPAND)        

        self.__authq_slideout_visible = False
        self.__authq_slideout_window = None
        self.__authq_slideout = LoginSlideout()
        self.__authq_slideout.connect('visible', self.__on_authq_visible)
        self.__on_authq_visible(None, False)        

        self.__create_fus_proxy()

    def __on_authq_visible(self, authq, vis):
        self.__authq_visible = vis
        self.__sync_authq_visible()
        
    def __sync_authq_visible(self):
        self._box.set_child_visible(self.__auth_section_container, not not self.__myself)        
        can_auth = self.__authq_visible and self.__myself
        if not can_auth and self.__authq_slideout_window:
            self.__authq_slideout_window.hide()
            self.__authq_slideout_visible = False
                 
        if self.__myself:
            if self.__authq_visible:
                self.__auth_section_container.set_child_visible(self.__auth_section, True)
                self.__auth_section_container.set_child_visible(self.__logout_link, False)     
            elif self.__auth_section_container.get_child_visible(self.__auth_section):
                # offer a link to log out from accounts 
                self.__auth_section_container.set_child_visible(self.__auth_section, False)
                self.__auth_section_container.set_child_visible(self.__logout_link, True)     

    def __on_logout_activated(self):
        self.__authq_slideout.logout_requested()

    def __on_authq_button_activated(self, b):
        if not self.__authq_slideout_visible:
            if not self.__authq_slideout_window:
                self.__authq_slideout_window = self.__do_slideout(self.__authq_slideout, widget=self.__authq_button)
            self.__authq_slideout_window.present_with_time(gtk.get_current_event_time())
            self.__authq_slideout_visible = True
        else:
            self.__authq_slideout_window.hide()
            self.__authq_slideout_visible = False

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
        _logger.debug("doing datamodel connected handler")
        self._box.set_child_visible(self._signin, self._model.self_id == None)
        self._box.set_child_visible(self._whereim_box, self._model.self_id != None)

        query = self._model.query_resource(self._model.self_id, "+;lovedAccounts +")
        query.add_handler(self.__on_got_self)
        query.execute()
        
    def __on_got_self(self, myself):
        self.__myself = myself
        self.__authq_slideout.set_myself(myself)        
        myself.connect(self.__on_self_changed)
        self.__on_self_changed(myself)
        self.__sync_authq_visible()        
        
    def __handle_mugshot_connection_status(self, auth, xmpp, contacts):
        self._box.set_child_visible(self._whereim_box, not not auth)
        self._box.set_child_visible(self._signin, not auth)
            
    def __do_slideout(self, display, widget=None):
        slideout = bigboard.slideout.Slideout()        
        widget_src = widget or self._box
        (box_x, box_y) = self._box.get_context().translate_to_screen(self._box)
        (src_x, src_y) = widget_src.get_context().translate_to_screen(widget_src)
        slideout.slideout_from(box_x + self._box.get_allocation()[0] + 4, src_y)
        if hasattr(display, 'focus'):
            display.focus()
        slideout.get_root().append(display)
        slideout.set_size_request(200, -1)
        return slideout
         
    def __do_logout(self):
        self._panel.Logout()

    def __do_minimize(self):
        self._panel.toggle_expand()
    
    def __do_account(self):
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
        self.__slideout_display = SelfSlideout(self, self.__myself, fus=self.__fus_service, logger=self._logger)
        self.__slideout_display.connect('account', lambda s: self.__do_account())        
        self.__slideout_display.connect('minimize', lambda s: self.__do_minimize())
        self.__slideout_display.connect('logout', lambda s: self.__do_logout())
        self.__slideout_display.connect('close', lambda s: self.__on_activate())
        self.__slideout = self.__do_slideout(self.__slideout_display)   
        
    def get_authed_content(self, size):
        return self._box

    def get_unauthed_content(self, size):
        return self._box
    
    def set_size(self, size):
        super(SelfStock, self).set_size(size)
        self._namephoto_box.set_size(size)
    
    def __on_self_changed(self, myself):
        self._logger.debug("self (%s) changed", myself.resource_id)
        self._photo.set_url(myself.photoUrl)
        self._name.set_property("text", myself.name)
        
        self._whereim_box.remove_all()
        for acct in myself.lovedAccounts:
            icon = ExternalAccountIcon(acct)
            self._logger.debug("appending external account %s", acct.accountType)
            self._whereim_box.append(icon)

        if self.__slideout_display != None:
            self.__slideout_display.update_self(myself)
