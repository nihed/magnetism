import os
import re
import stat
import sys

NEGATE = 1
DIRECTORY_ONLY = 2

def verbose(msg):
#    print >>sys.stderr, msg
    pass

DIR = 1
SYMLINK = 2
EXPAND = 4

class Item:
    def __init__(self, src, flags):
        self.src = src
        self.flags = flags
        if (flags & DIR) != 0:
            self.children = []

class DirTree:
    def __init__(self, target, scope):
        self.target = target
        self.items = { '' : Item(None, DIR) }
        self.scope = scope

    def add_as_child(self, path):
        dirname = os.path.dirname(path)
        self.items[dirname].children.append(path)

    def add_file(self, path, src, expand, symlink):
        if not self.items.has_key(path):
            self.add_as_child(path)
        flags = 0
        if symlink:
            flags = flags | SYMLINK
        if expand:
            flags = flags | EXPAND
        self.items[path] = Item(src, flags)

    def test_flag(self, path, flag):
        return (self.items[path].flags & flag) != 0

    def clear_flag(self, path, flag):
        self.items[path].flags = self.items[path].flags & ~flag

    def add_dir(self, path, src, symlink):
        if not self.items.has_key(path):
            self.add_as_child(path)
        # We never want to symlink if a directory is shared
        # by multiple sources
        if self.items.has_key(path) and self.test_flag(path, DIR):
            self.clear_flag(path, SYMLINK)
            self.items[path].src = None
        else:
            flags = DIR
            if symlink:
                flags = flags | SYMLINK
            self.items[path] = Item(src, flags)

    def compile_one(self, line):
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

        return (re.compile(pattern), flags)

    def compile_excludes(self, filename):
        f = open(filename)

        result = []
        
        for line in f.readlines():
            line = re.sub('(^|[^\\\\]*)(#.*)', '\\1', line)
            line = line.strip()
            if line == "":
                continue
            
            result.append(self.compile_one(line))

        f.close()
        return result

    def standard_excludes(self):
        return [
            self.compile_one(".svn"),
            self.compile_one("*~"),
          ]
            
    def add_tree_recurse(self, path, src, symlink, expand, excludes):
        src_stat = os.stat(src)
        src_is_dir = stat.S_ISDIR(src_stat.st_mode)

        if path != '':
            for (pattern, flags) in excludes:
                if (flags & DIRECTORY_ONLY) != 0:
                    if not src_is_dir:
                        continue
                if pattern.search(path):
                    if (flags & NEGATE) != 0:
                        verbose("Including %s" % path)
                        break
                    else:
                        verbose("Excluding %s" % path)
                        return False

        if src_is_dir:
            self.add_dir(path, src, symlink)
                
            result = True
            for f in os.listdir(src):
                if not self.add_tree_recurse(os.path.join(path, f),
                                             os.path.join(src, f),
                                             symlink, expand, excludes):
                    result = False

            # We can't symlink if we didn't add the entire directory
            if not result:
                self.clear_flag(path, SYMLINK)

            return result
        else:
            self.add_file(path, src, expand, symlink)
            return True

    def add_tree(self, path, src, symlink=False, expand=False, excludes=[]):
        if path == None:
            path = ''

        # When merging into the middle of the tree, we need to clear the
        # symlink attribute on all ancestors, since we have to merge into
        # a real directory, not a symlinked shadow
        parent = path
        while parent != '':
            parent = os.path.dirname(parent)
            self.clear_flag(parent, SYMLINK)

        self.add_tree_recurse(path, src, symlink, expand, excludes)
        
        
    def symlink(self, path):
        src = self.items[path].src
        dest = os.path.join(self.target, path)
        os.symlink(src, dest)

    def copy_file(self, path):
        src = self.items[path].src
        dest = os.path.join(self.target, path)
        os.spawnl(os.P_WAIT, '/bin/cp', 'cp', '-a', src, dest)

    def expand_file(self, path):
        src = self.items[path].src
        dest = os.path.join(self.target, path)
        
        f_src = open(src, "r")
        f_dest = open(dest, "w")

        subst = re.compile("@@([a-zA-Z_][a-zA-Z0-9_]*)@@")
        def repl(m):
            return self.scope.expand_parameter(m.group(1))

        while True:
            line = f_src.readline()
            if (line == ""):
                break

            f_dest.write(subst.sub(repl, line))

        f_src.close()
        f_dest.close()

        src_stat = os.stat(src)
        os.chmod(dest, stat.S_IMODE(src_stat.st_mode))

    def write_dir(self, path):
        if (path != ''):
            dest = os.path.join(self.target, path)
        else:
            dest = self.target
            
        dest_stat = None
        try:
            dest_stat = os.stat(dest)
        except OSError:
            pass

        if dest_stat == None or not stat.S_ISDIR(dest_stat.st_mode):
            os.mkdir(dest)
        
        for f in self.items[path].children:
            self.write_path(f)
        
    def write_path(self, path):
        if self.test_flag(path, SYMLINK):
            self.symlink(path)
        elif self.test_flag(path, DIR):
            self.write_dir(path)
        elif self.test_flag(path, EXPAND):
            self.expand_file(path)
        else:
            self.copy_file(path)
        
    def write(self):
        self.write_path('')
       
