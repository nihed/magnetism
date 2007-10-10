import os, sys, cookielib

import pyonlinedesktop
from pyonlinedesktop.fsutil import iterdir

class FirefoxProfile(object):
  def __init__(self):
    self.__ffpath = os.path.expanduser('~/.mozilla/firefox')
    # todo integrate with monitoring from web-login-driver
    self.__profilepath = self.__get_profile_path()

  def __get_profile_path(self):
    if not os.path.isdir(self.__ffpath):
      os.makedirs(self.__ffpath)
    for p in iterdir(self.__ffpath):
      if not p.endswith('.default'): continue
      return p
    raise KeyError("Couldn't find mozilla profile")

  def path_join(self, *args):
    return os.path.join(self.__profilepath, *args)

class FirefoxHTTP(object):
  def __init__(self):
    self.__profile = FirefoxProfile()
    self.__cookies = cookielib.MozillaCookieJar(policy=cookielib.DefaultCookiePolicy(rfc2965=True))
    cookiepath = self.__profile.path_join('cookies.txt')
    _logger.debug("reading cookies from %s", cookiepath)
    self.__cookies.load(cookiepath)
    self.opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(self.__cookies))

  def open(self, url):
    req = urllib2.Request(url, headers={'User-Agent': 'GNOME Online Desktop Widget System 0.1',})
    return self.opener.open(req)
