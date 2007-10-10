import os,sys

import gobject, gnomevfs

def iterdir(path):
  for fname in os.listdir(path):
    yield os.path.join(path, fname)

def iterdir_d(name):
  """Iterate over a data directory containing files."""
  datadir_env = os.getenv('XDG_DATA_DIRS')
  if datadir_env:
    datadirs = datadir_env.split(':')
  else:
    datadirs = ['/usr/share/']
  for dirname in datadirs:
    dirpath = os.path.join(dirname, name)
    if os.access(dirpath, os.R_OK):
      for name in os.listdir(dirpath):
        fname = os.path.join(dirpath, name)
        yield fname

class VfsMonitor(object):
  """Avoid some locking oddities in gnomevfs monitoring"""
  def __init__(self, path, montype, cb):
    self.__path = path
    self.__cb = cb
    self.__idle_id = 0
    self.__monid = gnomevfs.monitor_add(path, montype, self.__on_vfsmon)
  
  def __idle_emit(self):
    self.__idle_id = 0
    self.__cb()

  def __on_vfsmon(self, *args):
    if not self.__monid:
      return
    if self.__idle_id == 0:
      self.__idle_id = gobject.timeout_add(300, self.__idle_emit)

  def cancel(self):
    if self.__idle_id:
      gobject.source_remove(self.__idle_id)
      self.__idle_id = 0
    if self.__monid:
      gnomevfs.monitor_cancel(self.__monid)
      self.__monid = Nones
