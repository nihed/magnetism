import stat
import os
import sys
import re

NEGATE = 1
DIRECTORY_ONLY = 2

def verbose(msg):
#    print >>sys.stderr, msg
    pass

class Merge:
    def __init__(self, service, src, target, exclude, expand, symlink, hot):
        self.service = service
        self.src = src
        self.target = target
        self.exclude = exclude
        self.expand = expand
        self.symlink = symlink
        self.hot = hot

    def copy_file(self, src, target):
        os.system('/bin/cp', src, target)
        
    def expand_file(self, src, target):
        f_src = open(src, "r")
        f_dest = open(target, "w")

        subst = re.compile("@@([a-zA-Z_][a-zA-Z0-9_]*)@@")
        def repl(m):
            return self.service.expand_parameter(m.group(1))

        while True:
            line = f_src.readline()
            if (line == ""):
                break

            f_dest.write(subst.sub(repl, line))

        f_src.close()
        f_dest.close()
        
    def recurse(self, src_base, target_base, rel = None):
        if rel:
            src = os.path.join(src_base, rel)
            target = os.path.join(target_base, rel)
        else:
            src = src_base        
            target = target_base

        stat_src = os.stat(src)

        if rel:
            for (pattern, flags) in self.patterns:
                if (flags & DIRECTORY_ONLY) != 0:
                    if not stat.S_ISDIR(stat_src.st_mode):
                        continue
                if pattern.search(rel):
                    if (flags & NEGATE) != 0:
                        verbose("Including %s" % rel)
                        break;
                    else:
                        verbose("Excluding %s" % rel)
                        return
        
        stat_target = None
        try:
            stat_target = os.stat(target)
        except OSError:
            pass

        if stat.S_ISDIR(stat_src.st_mode):
            if stat_target == None or not stat.S_ISDIR(stat_target.st_mode):
                verbose("Creating %s" % target)
                os.mkdir(target)
            for f in os.listdir(src):
                if rel:
                    self.recurse(src_base, target_base,
                                 os.path.join(rel, f))
                else:
                    self.recurse(src_base, target_base, f)
        else:
            if stat_target:
                os.remove(target)
            if self.expand:
                self.expand_file(src, target)
            elif self.symlink:
                os.symlink(src, target)
            else:
                self.copy_file(src, target)

    def compile_exclusions(self):
        exclude = self.service.expand(self.exclude)
        f = open(exclude)

        result = []
        
        for line in f.readlines():
            line = line.strip()
            if line == "":
                continue
            
            anchored=False
            flags = 0
                
            if re.match("\+ ", line):
                flags = flags | NEGATE
                line = line[2:]
                
            if re.match(".*/$", line):
                flags = flags | DIRECTORY_ONLY
                line = line[:-1]
                    
            if re.match("/", line):
                anchored = True
                line = line[1:]

            def repl(m):
                if m.group(1) != None:
                    return '.*'
                else:
                    return '[^/]*'
            pattern = re.sub('(\*\*)|(\*)', repl, line)

            if anchored:
                pattern = "^" + pattern + '$'
            else:
                pattern = "(^|/)" + pattern + '$'
                
            result.append((re.compile(pattern), flags))

        f.close()
        return result
            
    def run(self):
        if self.exclude != None:
            self.patterns = self.compile_exclusions()
        else:
            self.patterns = []
        
        src = self.service.expand(self.src)
        target = self.service.expand(self.target)

        self.recurse(src, target)

        
