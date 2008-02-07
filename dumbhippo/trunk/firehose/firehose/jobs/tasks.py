#!/usr/bin/python

import os,sys,re

class TaskEntry(object):
    def set_family(self, v):
        self._family = v
    family = property(lambda self: self._family, set_family)
    
    def set_id(self, v):
        self._id = v
    id = property(lambda self: self._id, set_id)
    
    def set_hash(self, v):
        self._hash = v
    prev_hash = property(lambda self: self._prev_hash, set_hash)
    
    def set_timestamp(self, v):
        self._timestamp = v        
    prev_timestamp = property(lambda self: self._prev_timestamp, set_timestamp)
        
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