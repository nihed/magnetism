#!/usr/bin/python

import os,sys,re

class TaskEntry(object):
    family = property(lambda self: self._family)
    id = property(lambda self: self._id)
    prev_hash = property(lambda self: self._prev_hash)
    prev_timestamp = property(lambda self: self._prev_timestamp)    
    def __init__(self, keystr, prev_hash, prev_ts):
        super(TaskEntry, self).__init__()
        (self._family, self._id) = keystr.split('/', 1)
        (self._prev_hash, self._prev_timestamp) = prev_hash, prev_ts
        
    def __cmp__(self, other):
        v = cmp(self.family, other.family)
        if v != 0:
            return v
        return cmp(self.id, other.id)
    
    def __str__(self):
        return self.family + '/' + self.id
    
    def __json__(self):
        return {
          "family" : self.family,
          "id" : self.id,
        }