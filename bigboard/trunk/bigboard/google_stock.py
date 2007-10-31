import logging
import hippo
import bigboard.google as google
import bigboard.accounts as accounts
import accounts_dialog
import bigboard.libbig.gutil as gutil

_logger = logging.getLogger("bigboard.Google")

## this base class is messed up, because the polling and account data
## tracking should be in a model object, not in the view (stock).
## Some stuff in here is view-specific though, like the _create_login_button
## method.

class GoogleStock(object):
    def __init__(self, action_id, **kwargs):
        super(GoogleStock, self).__init__(**kwargs)

        # A dictionary of authenticated google accounts, with keys that are used
        # to identify those accounts within the stock.
        self.googles = set()
        self.__googles_by_account = {} ## map accounts.Account => google.Google

        self.__action_id = action_id

        self.__connections = gutil.DisconnectSet()

        accts = accounts.get_accounts()
        for a in accts.get_accounts_with_kind(accounts.KIND_GOOGLE):
            self.__on_account_added(a)
        id = accts.connect('account-added', self.__on_account_added)
        self.__connections.add(accts, id)
        id = accts.connect('account-removed', self.__on_account_removed)
        self.__connections.add(accts, id)

        

    ## we can't just override _on_delisted() because of multiple inheritance,
    ## so our subclasses have to override it then call this
    def _delist_google(self):
        self.__connections.disconnect_all()

        ## detach from all the accounts
        accts = self.__googles_by_account.keys()
        for a in accts:
            self.__on_account_removed(a)

    def __on_account_added(self, acct):
        gobj = google.get_google_for_account(acct)
        gobj.add_poll_action_func(self.__action_id, lambda gobj: self.update_google_data(gobj))
        self.googles.add(gobj)
        self.__googles_by_account[acct] = gobj

        ## update_google_data() should be called in the poll action
    
    def __on_account_removed(self, acct):
        ## we keep our own __googles_by_account because google.get_google_for_account()
        ## will have dropped the Google before this point
        gobj = self.__googles_by_account[acct]
        gobj.remove_poll_action(self.__action_id)
        self.googles.remove(gobj)
        del self.__googles_by_account[acct]

        ## hook for derived classes
        self.remove_google_data(gobj)

    def have_one_good_google(self):
        for g in self.googles:
            if not g.get_current_auth_credentials_known_bad():
                return True

        return False

    def __open_login_dialog(self):
        accounts_dialog.open_dialog()

    def _create_login_button(self):
        button = hippo.CanvasButton(text="Login to Google")
        button.connect('activated', lambda button: self.__open_login_dialog())
        return button

    def update_google_data(self, gobj=None):
        pass

    def remove_google_data(self, gobj):
        pass
    
