# this file handles user's "mugshots" (like their /person page or google gadget)

import libbig

class Profile:
    def __init__(self):
        pass

class ProfileFactory:

    def __init__(self):
        self._fetcher = libbig.AsyncHTTPFetcher()

    def _download_summary(self, guid):
        ## FIXME use the right base url and include our login cookie
        self._fetcher.fetch('http://dogfood.mugshot.org:9080/xml/userSummary?includeStack=true&who=' + guid, self._do_load, self._do_load_error)

    def _do_load(self, url, data):
        print data
        
    def _do_load_error(self, url, exc_info):
        print exc_info

    def get_profile(self, guid):
        self._download_summary(guid)
        return None
    
