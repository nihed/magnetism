# this file handles user's "mugshots" (like their /person page or google gadget)

import logging
import xml.sax
import libbig, mugshot

# object representing a profile; also a sax handler that can parse the xml form
class Profile(libbig.AutoStruct, xml.sax.ContentHandler):
    def __init__(self, guid):
        libbig.AutoStruct.__init__(self,
                                   { 'name' : None, 'photo' : None, 'who' : str(guid), 'online' : None,
                                     'homeUrl' : None, 'onlineIcon' : None, 'accounts' : [], 'stack' : [],
                                     'email' : None, 'aim' : None})

    def startElement(self, name, attrs):
        #print name
        #print attrs.getNames() # .getValue('foo')

        if name == 'rsp':
            if attrs.getValue('stat') != 'ok':
                raise xml.sax.SAXException, 'failed'
        elif name == 'userSummary':
            online = False
            if attrs.getValue('online') == 'true':
                online = True
            self.update({ 'name' : attrs.getValue('name'),
                          'photo' : attrs.getValue('photo'),
                          'online' : online,
                          'homeUrl' : attrs.getValue('homeUrl') })

        elif name == 'externalAccount':
            accounts = self.get_accounts()
            accounts.append({ 'link' : attrs.getValue('link'),
                              'type' : attrs.getValue('type'),
                              'linkText' : attrs.getValue('linkText'),
                              'icon' : attrs.getValue('icon') })
        elif name == 'address':
            type = attrs.getValue('type')
            if type == 'email':
                self.update({ 'email' : attrs.getValue('value') })
            elif type == 'aim':
                self.update({ 'aim' : attrs.getValue('value') })

    def characters(self, content):
        #print content
        pass

class ProfileFactory:

    def __init__(self):
        self._fetcher = libbig.AsyncHTTPFetcher()
        self._mugshot = mugshot.get_mugshot()
        self._baseurl = None
        self._mugshot.connect("initialized", lambda mugshot: self._sync_baseurl())
        self._profiles = {}
        self._callbacks = {}

    def _sync_baseurl(self):
        self._baseurl = self._mugshot.get_baseurl()

    def _download_summary(self, guid):
        ## FIXME include our login cookie
        self._fetcher.fetch(self._baseurl + 'xml/userSummary?includeStack=true&who=' + guid,
                            lambda url, data: self._do_load(url, data, guid),
                            lambda url, exc_info: self._do_load_error(url, exc_info, guid))

    def _notify(self, guid):
        p = None
        if self._profiles.has_key(guid):
            p = self._profiles[guid] # note that if we failed earlier, p = None here

        callbacks = []
        if self._callbacks.has_key(guid):
            callbacks = self._callbacks[guid]
            self._callbacks[guid] = []

        for c in callbacks:
            c(p)
        
    def _do_load(self, url, data, guid):
        logging.debug("retrieved '%s' (%d bytes) for %s", url, len(data), guid)
        p = Profile(guid)
        try:
            xml.sax.parseString(data, p)
            self._profiles[guid] = p
        except xml.sax.SAXException, e:
            self._profiles[guid] = None

        self._notify(guid)
        
    def _do_load_error(self, url, exc_info, guid):
        logging.exception("Caught exception retrieving '%s'", url)

        self._notify(guid)

    def fetch_profile(self, guid, callback):
        guid = str(guid)
        if self._profiles.has_key(guid):
            callback(self._profiles[guid])
        else:
            if self._callbacks.has_key(guid):
                self._callbacks[guid].append(callback)
            else:
                self._callbacks[guid] = [callback]
                
            self._download_summary(guid)
    
