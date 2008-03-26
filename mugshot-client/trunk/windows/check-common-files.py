#!/usr/bin/python

import os
import re

DIRS = [
    '../../desktop-data-model/hippo',
    '../../desktop-data-model/engine',
    '../canvas/common/hippo',
    '../common/stacker'
    ]

VCPROJ = 'HippoCommon/HippoCommon.vcproj'

FILE_RE = re.compile('.*\.[ch]$')
RELATIVE_PATH_RE = re.compile('\s*RelativePath\s*=\s*"([^"]+)"\s*')

disk_files = set()

for d in DIRS:
    for f in os.listdir(d):
        if FILE_RE.match(f):
            path = os.path.join('..', d,f)
            windows = re.sub('/', r'\\', path)
            disk_files.add(windows)

vcproj = open(VCPROJ, 'r')

vcproj_files = set()
for l in vcproj:
    m = RELATIVE_PATH_RE.match(l)
    if m:
        vcproj_files.add(m.group(1))

vcproj.close()

disk_only = sorted(disk_files.difference(vcproj_files))
print "Only on disk: %s" % len(disk_only)
for f in disk_only:
    print f

print

vcproj_only = sorted(vcproj_files.difference(disk_files))
print "Only in HippoCommon.vcproj: %s" % len(vcproj_only)
for f in vcproj_only:
    print f


            

