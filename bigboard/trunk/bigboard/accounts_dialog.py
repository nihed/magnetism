import sys, logging

import gobject, gtk
import bigboard.accounts as accounts

import bigboard.globals as globals
import libbig.logutil
from libbig.logutil import log_except

_logger = logging.getLogger("bigboard.AccountsDialog")

class AccountEditor(gtk.VBox):
    def __init__(self, *args, **kwargs):
        if 'account' in kwargs:
            self.__account = kwargs['account']
            del kwargs['account']
        else:
            raise Error("must provide account to AccountEditor")
        
        super(AccountEditor, self).__init__(*args, **kwargs)

        self.__username_entry = gtk.Entry()
        self.__password_entry = gtk.Entry()
        self.__password_entry.set_visibility(False)

        self.__username_entry.connect('changed',
                                      self.__on_username_entry_changed)
        self.__password_entry.connect('changed',
                                      self.__on_password_entry_changed)

        hbox = gtk.HBox(spacing=10)
        label = gtk.Label("Email")
        label.set_alignment(0.0, 0.5)
        hbox.pack_start(label)
        hbox.pack_end(self.__username_entry, False)
        self.pack_start(hbox)

        hbox = gtk.HBox(spacing=10)
        label = gtk.Label("Password")
        label.set_alignment(0.0, 0.5)
        hbox.pack_start(label)    
        hbox.pack_end(self.__password_entry, False)
        self.pack_start(hbox)

        self.show_all()

        self.__on_account_changed(self.__account)
        self.__account.connect('changed', self.__on_account_changed)

        self.__password_entry.set_activates_default(True)

    def __on_account_changed(self, account):
        self.__username_entry.set_text(account.get_username())
        self.__password_entry.set_text(account.get_password())

    def __on_username_entry_changed(self, entry):
        text = entry.get_text()
        accounts.get_accounts().save_account_changes(self.__account,
                                                     { 'username' : text })

    def __on_password_entry_changed(self, entry):
        text = entry.get_text()
        accounts.get_accounts().save_account_changes(self.__account,
                                                     { 'password' : text })

class Dialog(gtk.Dialog):
    def __init__(self, *args, **kwargs):
        super(Dialog, self).__init__(*args, **kwargs)        
        
        self.set_title('Google Accounts')

        self.connect('delete-event', self.__on_delete_event)
        self.connect('response', self.__on_response)

        self.add_button(gtk.STOCK_CLOSE,
                        gtk.RESPONSE_OK)
        self.set_default_response(gtk.RESPONSE_OK)

        self.__editors_by_account = {}

        accts = accounts.get_accounts().get_accounts_with_kind(accounts.KIND_GOOGLE)
        if len(accts) == 0:
            accounts.get_accounts().create_account(accounts.KIND_GOOGLE)
        else:
            for a in accts:
                self.__editors_by_account[a] = AccountEditor(account=a)
                self.vbox.pack_start(self.__editors_by_account[a])

    def __on_delete_event(self, dialog, event):
        self.hide()
        return True

    def __on_response(self, dialog, response_id):
        _logger.debug("response = %d" % response_id)
        self.hide()
        
__dialog = None

def open_dialog():
    global __dialog
    if not __dialog:
        __dialog = Dialog()
    __dialog.present()
    

if __name__ == '__main__':

    import gtk, gtk.gdk

    import bigboard.libbig
    try:
        import bigboard.bignative as bignative
    except:
        import bignative

    import dbus.glib

    import bigboard.google as google

    gtk.gdk.threads_init()

    libbig.logutil.init('DEBUG', ['AsyncHTTP2LibFetcher', 'bigboard.Keyring', 'bigboard.Google', 'bigboard.Accounts', 'bigboard.AccountsDialog'])

    bignative.set_application_name("BigBoard")
    bignative.set_program_name("bigboard")

    google.init()

    open_dialog()

    gtk.main()
    
