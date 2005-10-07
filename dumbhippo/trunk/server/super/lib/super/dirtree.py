import os
import re
import stat
import sys

def verbose(msg):
#    print >>sys.stderr, msg
    pass

# Flags for directory tree nodes
DIR = 1      # Node is a directory (and thus has children)

SYMLINK = 2  # Node can be directly symlinked to the source tree.
             # for a directory, this means that all of the
             # children are the same as for the source tree; nothing
             # is excluded or modified

EXPAND = 4   # When copying from the source tree, expand parameters

class Node:

    """Record used when storing the tree of objects

    src -- source location for a file or directory. May be None
    flags -- a bitfield holding one or more of the flags:
             DIR/SYMLINK/EXPAND that represents attributes of a
             node of the directory tree.
    children -- children of the node. Only true if the DIR flag is set
    """
    
    def __init__(self, src, flags):
        self.src = src
        self.flags = flags
        if (flags & DIR) != 0:
            self.children = []

# Flags used for compiled 'exclude' patterns
NEGATE = 1           # Pattern started with + - include and stop processing
DIRECTORY_ONLY = 2   # Pattern ended with / ... matches only directories

class DirTree:

    """The DirTree class represents the tree of files build up from
    the <merge/> statements in a service definition. After a tree
    is built we can compare it to the current contents of the
    target location or write it out, replacing the target location
    """
    
    def __init__(self, target, scope):
        """Create a new DirTree object

        scope -- scope for parameter lookups for <merge expand="yes"/>.
                 Typically a Service object.
        target -- target directory where output will be written by write()
        """
        self.target = target
        self.nodes = { '' : Node(None, DIR) }
        self.scope = scope

    def standard_excludes(self):
        """Return a list of standard files to always exclude, in a
        compiled form suitable for add_tree()"""
        return [
            self._compile_one(".svn"),
            self._compile_one("*~"),
          ]
            
    def compile_excludes(self, filename):
        """Compile exclude patterns from a file into a list suitable
        for add_tree()"""
        
        f = open(filename)

        result = []
        
        for line in f.readlines():
            line = re.sub('(^|[^\\\\]*)(#.*)', '\\1', line)
            line = line.strip()
            if line == "":
                continue
            
            result.append(self._compile_one(line))

        f.close()
        return result

    def add_tree(self, path, src, symlink=False, expand=False, excludes=[]):
        """Add a source tree into the output.
        
        path -- path at which to add the file, relative to the
            top of the target directory
        src -- source directory or file
        symlink, expand -- options from <merge/>
        excludes -- compiled list of patterns to exclude (see
            standard_excludes(), compile_excludes())
        """
        if path is None:
            path = ''

        # When merging into the middle of the tree, we need to clear the
        # symlink attribute on all ancestors, since we have to merge into
        # a real directory, not a symlinked shadow
        parent = path
        while parent != '':
            parent = os.path.dirname(parent)
            self._clear_flag(parent, SYMLINK)

        self._add_tree_recurse(path, src, symlink, expand, excludes)
        
    def write(self):
        """Write the tree out into the output location. Any cleanup
        of existing content must be done beforehand. """
        self._write_path('')
        
    def _compile_one(self, line):
        """Compile a single pattern."""
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
            if m.group(1) is not None:
                return '.*'
            else:
                return '[^/]*'
        pattern = re.sub('(\*\*)|(\*)', repl, line)

        if anchored:
            pattern = "^" + pattern + '$'
        else:
            pattern = "(^|/)" + pattern + '$'

        return (re.compile(pattern), flags)

    def _add_as_child(self, path):
        """Add the node given by path to the parent's list of children.
        Must be called only once per node."""
        dirname = os.path.dirname(path)
        self.nodes[dirname].children.append(path)

    def _add_file(self, path, src, expand, symlink):
        """Add a file (non-directory) to the tree. If a file or
        directory was already at the path, it will be replaced."""
        if not self.nodes.has_key(path):
            self._add_as_child(path)
        flags = 0
        if symlink:
            flags = flags | SYMLINK
        if expand:
            flags = flags | EXPAND
        self.nodes[path] = Node(src, flags)

    def _add_dir(self, path, src, symlink):
        """Add a directory to the tree. Will replace any existing
        file and be merged with an existing directory."""
        if not self.nodes.has_key(path):
            self._add_as_child(path)
        # We never want to symlink if a directory is shared
        # by multiple sources
        if self.nodes.has_key(path) and self._test_flag(path, DIR):
            self._clear_flag(path, SYMLINK)
            self.nodes[path].src = None
        else:
            flags = DIR
            if symlink:
                flags = flags | SYMLINK
            self.nodes[path] = Node(src, flags)

    def _add_tree_recurse(self, path, src, symlink, expand, excludes):
        """Add a source directory and children to the tree. The
        worker function for add_tree()."""
        src_stat = os.stat(src)
        src_is_dir = stat.S_ISDIR(src_stat.st_mode)

        # Check for excludes; if path is excluded, skip it and children
        if path != '':
            for (pattern, flags) in excludes:
                if (flags & DIRECTORY_ONLY) != 0:
                    if not src_is_dir:
                        continue
                if pattern.search(path):
                    if (flags & NEGATE) != 0:
                        # When we hit a '+ /foo/bar' pattern, we
                        # ignore subsequent patterns
                        verbose("Including %s" % path)
                        break
                    else:
                        verbose("Excluding %s" % path)
                        return False

        if src_is_dir:
            self._add_dir(path, src, symlink)
                
            result = True
            for f in os.listdir(src):
                if not self._add_tree_recurse(os.path.join(path, f),
                                              os.path.join(src, f),
                                              symlink, expand, excludes):
                    result = False

            # We can't symlink if we didn't add the entire directory
            if not result:
                self._clear_flag(path, SYMLINK)

            return result
        else:
            self._add_file(path, src, expand, symlink)
            return True

    def _symlink(self, path):
        """When writing, create a symlink."""
        src = self.nodes[path].src
        dest = os.path.join(self.target, path)
        os.symlink(src, dest)

    def _copy_file(self, path):
        """When writing, copy a file without expansion."""
        src = self.nodes[path].src
        dest = os.path.join(self.target, path)
        os.spawnl(os.P_WAIT, '/bin/cp', 'cp', '-a', src, dest)

    def _expand_file(self, path):
        """When writing, copy a file with expansion."""
        src = self.nodes[path].src
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

        # preserve permissions to the extent possible
        src_stat = os.stat(src)
        os.chmod(dest, stat.S_IMODE(src_stat.st_mode))

    def write_dir(self, path):
        """Write out a directory node with its children."""
        if (path != ''):
            dest = os.path.join(self.target, path)
        else:
            dest = self.target
            
        dest_stat = None
        try:
            dest_stat = os.stat(dest)
        except OSError:
            pass

        if dest_stat is None or not stat.S_ISDIR(dest_stat.st_mode):
            os.mkdir(dest)
        
        for f in self.nodes[path].children:
            self._write_path(f)
        
    def _write_path(self, path):
        """Write out a node, including children, if any."""
        if self._test_flag(path, SYMLINK):
            self._symlink(path)
        elif self._test_flag(path, DIR):
            self.write_dir(path)
        elif self._test_flag(path, EXPAND):
            self._expand_file(path)
        else:
            self._copy_file(path)
       
    def _test_flag(self, path, flag):
        """Check if the given node has the flag set."""
        return (self.nodes[path].flags & flag) != 0

    def _clear_flag(self, path, flag):
        """Clear the flag from the given node."""
        self.nodes[path].flags = self.nodes[path].flags & ~flag

