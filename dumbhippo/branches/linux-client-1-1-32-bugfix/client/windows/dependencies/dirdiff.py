#! /usr/bin/python

### run a diff between which files are in two trees, surely I'm missing
### the obvious way to do this...

import sys
import os
import shutil
import tempfile
import fnmatch

def usage():
    print >>sys.stderr,  "Usage: dirdiff.py dir1 dir2"

FILE=1
DIR=2

def listall(top):
    all_files = {}
    for root, dirs, files in os.walk(top, topdown=True):
        for name in files:
            all_files[os.path.join(root, name)] = FILE
        for name in dirs:
            all_files[os.path.join(root, name)] = DIR

    return all_files

def all_files_relative(root, all_files):
    prefix_len = len(root)
    if root.endswith("/"):
        prefix_len = prefix_len + 1
    new_all_files = {}
    for (file, kind) in all_files.items():
        if not file.startswith(root):
            raise "%s does not start with %s" % (file, root)
        new_all_files[file[prefix_len:]] = kind

    return new_all_files

def dump_to_tempfile(tmpfile, all_files):
    list = []
    for (file, kind) in all_files.items():
        # get .svn noise out of the way
        if fnmatch.fnmatch(file, "*.svn*"):
            continue

        # we want dirs to be distinguishable
        if kind == DIR:
            slash = '/'
        else:
            slash = ''
        list.append('%s%s\n' % (file, slash))
    list.sort()
    for file in list:
        tmpfile.write(file)
    tmpfile.flush()

def main():
    dir1 = sys.argv[1]
    dir2 = sys.argv[2]

    all_files_1 = listall(dir1)
    all_files_2 = listall(dir2)

    relative_1 = all_files_relative(dir1, all_files_1)
    relative_2 = all_files_relative(dir2, all_files_2)

    tmpfile_1 = tempfile.NamedTemporaryFile()
    dump_to_tempfile(tmpfile_1, relative_1)
    tmpfile_2 = tempfile.NamedTemporaryFile()
    dump_to_tempfile(tmpfile_2, relative_2)

    os.system("diff -u %s %s" % (tmpfile_1.name, tmpfile_2.name))
    
main()
