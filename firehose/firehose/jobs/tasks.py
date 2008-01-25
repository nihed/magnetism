#!/usr/bin/python

import os,sys,re

class TaskKey(object):
    family = property(lambda self: self._family)
    id = property(lambda self: self._id)
    def __init__(self, keystr):
        (self._family, self._id) = keystr.split('/', 1)
        
    def __cmp__(self, other):
        v = cmp(self.family, other.family)
        if v != 0:
            return v
        return cmp(self.id, other.id)
    
    def __json__(self):
        return {
          "family" : self.family,
          "id" : self.id,
        }