import sys, logging, base64

import gobject, gconf, dbus

from ddm import DataModel
import bigboard.globals as globals
import bigboard.keyring as keyring
import libbig.logutil
from libbig.logutil import log_except
from bigboard.libbig.gutil import *

_logger = logging.getLogger("bigboard.Accounts")

class AccountKind(object):
    def __init__(self, id):
        super(AccountKind, self).__init__()        
        self.__id = id

    def get_id(self):
        return self.__id

KIND_GOOGLE = AccountKind("google")

def kind_from_string(s):
    for kind in (KIND_GOOGLE):
        if s == kind.get_id():
            return kind
    return None

class Account(gobject.GObject):
    __gsignals__ = {
        "changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())
        }

    def __init__(self, kind, username='', password='', url='', enabled=True, gconf_dir=None):
        super(Account, self).__init__()
        self.__kind = kind
        self.__username = username
        self.__password = password
        self.__url = url   ## url is used for Google Apps For Your Domain
        self.__enabled = enabled
        self.__gconf_dir = gconf_dir

    def get_kind(self):
        return self.__kind

    def get_username(self):
        return self.__username

    def get_password(self):
        return self.__password

    def get_url(self):
        return self.__url

    def get_enabled(self):
        return self.__enabled

    def _get_gconf_dir(self):
        return self.__gconf_dir

    def _update_from_origin(self, **kwargs):
        """This is the only way to modify an Account object. It should be invoked only on change notification or refreshed data from the original origin of the account."""

        ## check it out!
        changed = False
        for (key,value) in kwargs.items():
            old = self.__dict__['__' + key]
            if old != value:
                self.__dict__['__' + key] = value
                changed = True

        if changed:
            self.emit('changed')

class Accounts(gobject.GObject):
    __gsignals__ = {
        "account-added" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)), 
        "account-removed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
        }
    def __init__(self, *args, **kwargs):
        super(Accounts, self).__init__(*args, **kwargs)

        self.__accounts = set()

        ## this is a hash from AccountKind to (username, password) from the weblogindriver
        self.__weblogin_info = {}

        self.__weblogindriver_proxy = dbus.SessionBus().get_object('org.gnome.WebLoginDriver', '/weblogindriver')
        self.__weblogindriver_proxy.connect_to_signal("SignonChanged",
                                                       self.__on_signon_changed)
        self.__recheck_signons()

        self.__gconf = gconf.client_get_default()
        self.__gconf.add_dir('/apps/bigboard/accounts', gconf.CLIENT_PRELOAD_RECURSIVE)
        self.__gconf.notify_add('/apps/bigboard/accounts', self.__on_gconf_change)

        ## a dict from gconf directory name underneath /apps/bigboard/accounts to
        ## a dict of the gconf values underneath that account directory
        self.__gconf_info = {}

        self.__reload_from_gconf()

    def find_account_by_kind(self, kind):
        for a in self.__accounts:
            if a.get_kind() == kind:
                return a

        return None

    def __find_account_by_gconf_dir(self, gconf_dir):
        for a in self.__accounts:
            if a._get_gconf_dir() == gconf_dir:
                return a

        return None

    def __update_account(self, account):

        ## note that "kind" is never updated (not allowed to change without
        ## making a new Account object)

        was_enabled = account.get_enabled()

        fields = { }

        ## first, look for the info from our local gconf storage
        gconf_base = '/apps/bigboard/accounts'
        gconf_dir = account._get_gconf_dir()
        if gconf_dir and gconf_dir in self.__gconf_info:
            gconf_info = self.__gconf_info[gconf_dir]

            if 'username' in gconf_info:
                fields['username'] = gconf_info['username']

            if 'enabled' in gconf_info:
                fields['enabled'] = gconf_info['enabled']

            if 'url' in gconf_info:
                fields['url'] = gconf_info['url']
        elif gconf_dir and gconf_dir not in self.__gconf_info:
            ## Account object originating with a gconf item
            ## apparently now deleted, so disable. This
            ## does mean if you create a gconf item corresponding
            ## to something from weblogin driver, then delete the
            ## gconf item, the weblogin account doesn't come back
            ## until you restart the process. Clunky.
            ## deleting a gconf dir isn't what we'd normally do though,
            ## we'd normally set enabled=False in gconf, which would
            ## persist across restarts.
            fields['enabled'] = False

        ## second, look at weblogin driver, though we don't want to prefer
        ## its password over keyring, so there's some trickiness
        weblogin_password = None
        if account.get_kind() in self.__weblogin_info:
            (username, weblogin_password) = self.__weblogin_info[account.get_kind()]
            if 'username' not in fields:
                fields['username'] = username

            ## if account was not disabled explicitly by gconf, we enable it
            if 'enabled' not in fields:
                fields['enabled'] = True

        ## third, look for password in keyring
        if False:
            ## FIXME
            if 'password' not in fields:
                fields['password'] = password

        ## fourth, if no password in keyring, use the weblogin one
        if weblogin_password and 'password' not in fields:
            fields['password'] = weblogin_password

        ## after compositing all this information, update our account object
        account._update_from_origin(fields)

        ## and finally remove the account if it's no longer used
        if was_enabled and not account.get_enabled():
            self.__accounts.remove(account)
            self.emit('account-removed', account)

    def __ensure_and_update_account_for_kind(self, kind):
        account = self.find_account_by_kind(kind)
        added = False
        if not account:
            account = Account(kind)
            self.__accounts.add(account)
            added = True
        self.__update_account(account)
        if added:
            self.emit('account-added', account)

    def __try_ensure_and_update_account_for_gconf_dir(self, gconf_dir):
        account = self.__find_account_by_gconf_dir(gconf_dir)
        if account:
            self.__update_account(account)
            return
        
        if gconf_dir not in self.__gconf_info:
            _logger.error("trying to create Account for a gconf dir that doesn't exist")
            return

        gconf_info = self.__gconf_info[gconf_dir]
        if 'kind' not in gconf_info:
            _logger.error("gconf account has no kind setting")
            return
        
        kind = kind_from_string(gconf_info['kind'])
        if not kind:
            _logger.error("unknown account kind in gconf")
            return
            
        account = Account(kind, gconf_dir=gconf_dir)
        self.__accounts.add(account)
        
        self.__update_account(account)

        self.emit('account-added', account)
            
    def __reload_from_gconf(self):
        gconf_dirs = self.__gconf.all_dirs('/apps/bigboard/accounts')

        new_gconf_infos = {}
        for gconf_dir in gconf_dirs:
            base_key = '/apps/bigboard/accounts/' + gconf_dir

            gconf_info = {}
            def get_account_prop(gconf, gconf_info, base_key, prop):
                value = gconf.get_value(base_key + '/' + prop)
                if value:
                    gconf_info[prop] = value
            get_account_prop(self.__gconf, gconf_info, base_key, 'kind')
            get_account_prop(self.__gconf, gconf_info, base_key, 'username')
            get_account_prop(self.__gconf, gconf_info, base_key, 'url')
            get_account_prop(self.__gconf, gconf_info, base_key, 'enabled')            

            new_gconf_infos[gconf_dir] = gconf_info
            
        self.__gconf_info = new_gconf_infos

        ## create any new accounts
        for gconf_dir in self.__gconf_info.keys():
            self.__try_ensure_and_update_account_for_gconf_dir(gconf_dir)

        ## now update any old accounts that are no longer in gconf,
        ## which should result in enabled=False
        for a in self.__accounts:
            gconf_dir = a._get_gconf_dir()
            if gconf_dir and gconf_dir not in self.__gconf_info:
                self.__update_account(a)
        
    @defer_idle_func(timeout=400)        
    def __on_gconf_change(self, *args):
        _logger.debug("gconf change notify for accounts")
        self.__reload_from_gconf()
    
    def __check_signons(self, signons):
        for signon in signons:
            if 'hint' not in signon: continue
            if signon['hint'] == 'GMail':
                username = signon['username']
                password = base64.b64decode(signon['password'])
                self.__weblogin_info[KIND_GOOGLE] = (username, password)
                self.__ensure_and_update_account_for_kind(KIND_GOOGLE)
            
    def __recheck_signons(self):
        self.__weblogindriver_proxy.GetSignons(reply_handler=self.__on_get_signons_reply,
                                               error_handler=self.__on_dbus_error)

    @log_except(_logger)
    def __on_get_signons_reply(self, signondata):
        _logger.debug("got signons reply")
        for hostname,signons in signondata.iteritems():
            self.__check_signons(signons)

    @log_except(_logger)
    def __on_signon_changed(self, signons):
        _logger.debug("signons changed: %s", signons)
        self.__check_signons(signons)

    @log_except(_logger)
    def __on_dbus_error(self, err):
        self.__logger.error("D-BUS error: %s", err)
