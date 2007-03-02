# this file handles user's "mugshots" (like their /person page or google gadget)

import logging

import libbig, mugshot

class Profile:
    def __init__(self):
        pass

class ProfileFactory:

    def __init__(self):
        self._fetcher = libbig.AsyncHTTPFetcher()
        self._mugshot = mugshot.get_mugshot()
        self._baseurl = None
        self._mugshot.connect("initialized", lambda mugshot: self._sync_baseurl())

    def _sync_baseurl(self):
        self._baseurl = self._mugshot.get_baseurl()

    def _download_summary(self, guid):
        ## FIXME include our login cookie
        self._fetcher.fetch(self._baseurl + 'xml/userSummary?includeStack=true&who=' + guid, self._do_load, self._do_load_error)

    def _do_load(self, url, data):
        logging.debug("retrieved '%s' (%d bytes)", url, len(data))
        
    def _do_load_error(self, url, exc_info):
        logging.exception("Caught exception retrieving '%s'", url)

    def get_profile(self, guid):
        self._download_summary(guid)
        return None
    
