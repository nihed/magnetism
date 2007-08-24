import os,sys

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

