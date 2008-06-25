#!/usr/bin/python

import os,sys,time

from boto.s3.connection import S3Connection
from boto.s3.key import Key

logdir = sys.argv[1]

# Read in AWS credentials
execfile(os.path.expanduser('~/.aws'))
s3Conn = S3Connection()
logStorageBucket = s3Conn.get_bucket('logs.firehose.svc.verbum.org')

# one week
timedeltaSecs = 7 * 24 * 60 * 60

curtime = time.time()
for name in os.listdir(logdir):
  path = os.path.join(logdir, name)
  stbuf = os.stat(path)
  mtime = stbuf.st_mtime
  if (curtime - mtime) < timedeltaSecs:
    continue
  k = Key(logStorageBucket)
  k.key = name
  print "Uploading %s" % (path,)
  k.set_contents_from_filename(path)
  print "Upload complete, unlinking %s" % (path,)
  os.unlink(path)
print "done"
