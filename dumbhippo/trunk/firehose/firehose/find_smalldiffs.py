#!/usr/bin/python

import os,sys,subprocess

import simplejson

def main():
    srcdir = sys.argv[1]
    entries = [path for path in os.listdir(srcdir) if path.endswith('.diff')]
    entries.sort()
    entry_lines = {}
    for path in entries:
        c = 0
        fullpath = os.path.join(srcdir, path)
        f = open(fullpath)
        for line in f:
            c += 1
        if c > 3:
            c -= 3
        if c > 0 and c < 13:
            entry_lines[path] = c
    simplejson.dump(entry_lines, sys.stdout, sort_keys=True, indent=4)
        
if __name__ == '__main__':
    main()