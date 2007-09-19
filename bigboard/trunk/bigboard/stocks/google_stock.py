import bigboard.google as google
import bigboard.libbig.polling as polling

polling_periodicity_seconds = 120

class GoogleStock(polling.Task):
    def __init__(self, *args, **kwargs):
        # A dictionary of authenticated google accounts, with keys that are used
        # to identify those accounts within the stock.
        self._googles = {}
        self.__google_key = 0; 

        polling.Task.__init__(self, polling_periodicity_seconds * 1000)

        gobj_list = google.get_googles()
        for gobj in gobj_list:
            gobj.connect("auth", self.on_google_auth)
            if gobj.have_auth():
                self.on_google_auth(gobj, True) 
            else:
                gobj.request_auth()

    def get_google_key(self, gobj):
        for google_item in self._googles.items():
            if google_item[1] == gobj:
                return google_item[0]
        return None 

    def on_google_auth(self, gobj, have_auth):
        if have_auth:           
            if self._googles.values().count(gobj) == 0:
                self._googles[self.__google_key] = gobj   
            self.update_google_data(self.__google_key)
            self.__google_key = self.__google_key + 1
            if not self.is_running():
                self.start()
        else:
            key = self.get_google_key(gobj)
            if key is not None:
                if len(self._googles) == 1: 
                    self.stop()
                self.remove_google_data(key)
                del self._googles[key]                   

    def do_periodic_task(self):
        self.update_google_data() 


