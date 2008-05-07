#!/usr/bin/python

import os,sys,subprocess

def main():
    srcdir = sys.argv[1]
    entries = os.listdir(srcdir)
    entries.sort()
    items = {}
    for entry in entries:
        if entry.endswith('.diff') or entry.endswith('.tmp'):
            continue
        (key, ts) = entry.rsplit('.', 1)        
        if key not in items:
            items[key] = []
        items[key].append(ts)
    for key,tslist in items.iteritems():
        for ts1, ts2 in zip(tslist[:-1], tslist[1:]):
            ts1path = key + '.' + ts1
            ts2path = key + '.' + ts2
            diffpath = os.path.join(srcdir, '%s.%s-%s.diff' % (key, ts1, ts2))
            if os.path.exists(diffpath):
                continue
            diff_f = open(diffpath, 'w')
            subprocess.call(['diff', '-u', ts1path, ts2path], cwd=srcdir, stdout=diff_f, stderr=subprocess.STDOUT)
    
if __name__ == '__main__':
    main()