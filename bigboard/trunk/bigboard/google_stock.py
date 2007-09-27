import bigboard.google as google
import bigboard.libbig.polling as polling

polling_periodicity_seconds = 120

class GoogleStock(polling.Task):
    def __init__(self, *args, **kwargs):
        # A dictionary of authenticated google accounts, with keys that are used
        # to identify those accounts within the stock.
        self.googles = set()

        polling.Task.__init__(self, polling_periodicity_seconds * 1000)

        gobj_list = google.get_googles()
        for gobj in gobj_list:
            gobj.connect("auth", self.on_google_auth)
            if gobj.have_auth():
                self.on_google_auth(gobj, True) 
            else:
                gobj.request_auth()

    def on_google_auth(self, gobj, have_auth):
        if have_auth:           
            self.googles.add(gobj)
            self.update_google_data(gobj)
            if not self.is_running():
                self.start()
        elif gobj in self.googles:
          self.stop()
          self.remove_google_data(gobj)
          self.googles.remove(gobj)

    def do_periodic_task(self):
        self.update_google_data() 


