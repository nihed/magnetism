import errno
import os
import re
import stat
import sys

import super.service
from super.expander import Expander

def verbose(msg):
   # print >>sys.stderr, msg
    pass

# Return values for DirTree.check()
UPDATE_OK = 0       # Target directory matched tree, nothing needed
UPDATE_HOT = 1      # Only hot-updated files changed
UPDATE_NEEDED = 2   # The trees differed

# Flags for directory tree nodes
DIR = 1      # Node is a directory (and thus has children)
SYMLINK = 2  # Node can be directly symlinked to the source tree.
             # for a directory, this means that all of the
             # children are the same as for the source tree; nothing
             # is excluded or modified
EXPAND = 4   # When copying from the source tree, expand parameters
HOT = 8      # We can redeploy by copying over without restarting

# Return values from _check_perms
PERMS_OK = 0         # Everything OK
PERMS_MISSING = 1    # Destination wasn't there
PERMS_MISMATCH = 2   # Permission mismatch
PERMS_OK_AND_SAME_TIMESTAMP = 3 # perms are good, and the mtime matches

class Node:

    """Record used when storing the tree of objects

    src -- source location for a file or directory. May be None
    flags -- a bitfield holding one or more of the flags:
             DIR/SYMLINK/EXPAND that represents attributes of a
             node of the directory tree.
    children -- children of the node. Only true if the DIR flag is set
    times -- tuple of (atime,mtime) if it's a DIR, used to set target dir to match (for files we just use cp -a)
    """
    
    def __init__(self, src, flags):
        self.src = src
        self.flags = flags
        if (flags & DIR) != 0:
            self.children = []
            self.times = None

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
            compile_pattern(".svn"),
            compile_pattern("*~"),
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
            
            result.append(compile_pattern(line))

        f.close()
        return result

    def add_tree(self, path, src,
                 symlink=False, expand=False, hot=False,
                 excludes=[]):
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

        self._add_tree_recurse(path, src, symlink, expand, hot, excludes)
        
    def write(self):
        """Write the tree out into the output location. Any cleanup
        of existing content must be done beforehand. """
        self._write_path('', False)
        
    def check(self, target_attributes):
        """Check that the contents of the output location match the tree.

        Returns a tuple of (result_code, hot_files). result_code is
        one of:
        
        UPDATE_OK -- Target directory matched tree, nothing needed
        UPDATE_HOT --  Only hot-updated files changed, a list is
                       returned as the second item in the tuple
        UPDATE_NEEDED --  The trees differed, write() must be called
        """
        (result, hotcopies) = self._check_path('', target_attributes)
        move_to_end = []
        for path in hotcopies:
            hot_update_last = self._has_target_attribute(path, target_attributes, super.service.HOT_UPDATE_LAST)
            if hot_update_last:
                move_to_end.append(path)
        for path in move_to_end:
            hotcopies.remove(path)
            hotcopies.append(path)
        return (result, hotcopies)

    def hot_update(self, hot_files):
        """Do a hot update based on the files returned from check()"""
        # the "files" can also be directories that need their timestamp updated
        for path in hot_files:
            self._write_path(path, True)

    def list_sources(self, path=''):
        """Return an array of files and directories (excluding symlinked
        files) that are sources for the directory tree"""
        result = []
        
        if self._test_flag(path, SYMLINK):
            pass
        else:
            if self.nodes[path].src:
                result.append(self.nodes[path].src)
            if self._test_flag(path, DIR):
                for f in self.nodes[path].children:
                    result.extend(self.list_sources(f))
                
        return result
    
    def _add_as_child(self, path):
        """Add the node given by path to the parent's list of children.
        Must be called only once per node."""
        dirname = os.path.dirname(path)
        self.nodes[dirname].children.append(path)

    def _add_file(self, path, src, expand, symlink, hot):
        """Add a file (non-directory) to the tree. If a file or
        directory was already at the path, it will be replaced."""
        if not self.nodes.has_key(path):
            self._add_as_child(path)
        flags = 0
        if symlink:
            flags = flags | SYMLINK
        if expand:
            flags = flags | EXPAND
        if hot:
            flags = flags | HOT
        self.nodes[path] = Node(src, flags)

    def _add_dir(self, path, src, symlink, times, hot):
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
            if hot:
    	        flags = flags | HOT
            n = Node(src, flags)
            n.times = times
            self.nodes[path] = n

    def _add_tree_recurse(self, path, src, symlink, expand, hot, excludes):
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
            self._add_dir(path, src, symlink, (src_stat.st_atime, src_stat.st_mtime), hot)
                
            result = True
            for f in os.listdir(src):
                if not self._add_tree_recurse(os.path.join(path, f),
                                              os.path.join(src, f),
                                              symlink, expand, hot,
                                              excludes):
                    result = False

            # We can't symlink if we didn't add the entire directory
            if not result:
                self._clear_flag(path, SYMLINK)

            return result
        else:
            self._add_file(path, src, expand, symlink, hot)
            return True

    def _symlink(self, path):
        """When writing, create a symlink."""
        src = self.nodes[path].src
        dest = os.path.join(self.target, path)
        try:
            os.symlink(src, dest)
        except OSError, e:
            if e.errno == errno.EEXIST:
                current_src = os.readlink(dest)
                if current_src != src:
                    print >>sys.stderr, 'symlink %s->%s is messed up' % (dest,src)
                    raise e
            else:
                raise e

    def _copy_file(self, path):
        """When writing, copy a file without expansion."""
        src = self.nodes[path].src
        dest = os.path.join(self.target, path)
        os.spawnl(os.P_WAIT, '/bin/cp', 'cp', '-a', src, dest)

    def _expand_file(self, path):
        """When writing, copy a file with expansion."""
        cond_stack = []
        
        src = self.nodes[path].src
        dest = os.path.join(self.target, path)
        
        f_src = open(src, "r")
        f_dest = open(dest, "w")

        for line in Expander(self.scope, f_src, path):
            f_dest.write(line)

        f_src.close()
        f_dest.close()

        # preserve permissions to the extent possible
        src_stat = os.stat(src)
        os.chmod(dest, stat.S_IMODE(src_stat.st_mode))

    def _write_dir(self, path, hot):
        """Write out a directory node, including its children if not hot, without children if hot"""
        if (path != ''):
            dest = os.path.join(self.target, path)
        else:
            dest = self.target
            
        dest_stat = None
        try:
            dest_stat = os.lstat(dest)
        except OSError:
            pass

        if dest_stat is None or not stat.S_ISDIR(dest_stat.st_mode):
            os.mkdir(dest)

        if not hot:
            for f in self.nodes[path].children:
                self._write_path(f)
        
        n = self.nodes[path]
        
        # set the directory's timestamps to match the source,
        # AFTER writing child files so writing the children
        # doesn't break things
        if n.times:
            os.utime(dest, n.times)
        
    def _write_path(self, path, hot):
        """Write out a node, including children, if any. hot=True if we're walking hot update list so need not recurse."""
        if self._test_flag(path, SYMLINK):
            self._symlink(path)
        elif self._test_flag(path, DIR):
            self._write_dir(path, hot)
        elif self._test_flag(path, EXPAND):
            self._expand_file(path)
        else:
            self._copy_file(path)

    def _has_target_attribute(self, path, target_attributes, attribute):
        """See if path has the given target attribute."""

        for (pattern, flags, attributes) in target_attributes:
            if (attributes & attribute) == 0:
                continue
            if (flags & DIRECTORY_ONLY) != 0:
                child_dest = os.path.join(self.target, path)
                child_dest_stat = os.stat(child_dest)
                if not stat.S_ISDIR(child_dest_stat.st_mode):
                    continue
            # NEGATE is meaningless here, ignore
            if not pattern.match(path):
                continue
            return True
        
        return False

    def _check_symlink(self, path):
        """Check a symlink node"""
        src = self.nodes[path].src
        dest = os.path.join(self.target, path)

        link = None
        try:
            link = os.readlink(dest)
        except OSError, e:
            if e.errno == errno.ENOENT:
                verbose("%s does not exist (should be symlink)" % path)
                return (UPDATE_NEEDED, None)
            elif e.errno == errno.EINVAL:
                verbose("%s should be symlink)" % path)
                return (UPDATE_NEEDED, None)
            else:
                raise

        if link != src:
            verbose("%s points to %s (should point to %s)" % (path, link, src))
            return (UPDATE_NEEDED, None)
        else:
            return (UPDATE_OK, None)

    def _check_perms(self, path, src, dest):
        """Check that the destination is a regular file with permissions
        matching the source"""
        src_stat = os.stat(src)
        try:
            dest_stat = os.lstat(dest)
        except OSError, e:
            if e.errno == errno.ENOENT:
                verbose("%s does not exist" % path)
                return PERMS_MISSING
            else:
                raise
        
        if not stat.S_ISREG(dest_stat.st_mode):
            verbose("%s is not a regular file" % path)
            return PERMS_MISMATCH

        if stat.S_IMODE(src_stat.st_mode) != stat.S_IMODE(dest_stat.st_mode):
            verbose("Permissions on %s do not match source" % path)
            return PERMS_MISMATCH

        if src_stat.st_mtime == dest_stat.st_mtime:
            # this size check will catch almost all cases where the mtime
            # would be safer than a file content comparison.
            if src_stat.st_size != dest_stat.st_size:
                raise Exception('files have the same mtime but different size, should not happen! %s' % path)
            return PERMS_OK_AND_SAME_TIMESTAMP
        else:
            return PERMS_OK
        
    def _check_expand(self, path, target_attributes):
        """Check a node that is copied with expansion"""
        src = self.nodes[path].src
        dest = os.path.join(self.target, path)

        perms_result = self._check_perms(path, src, dest)
        if perms_result == PERMS_OK or perms_result == PERMS_OK_AND_SAME_TIMESTAMP:
            pass
        else:
            return (UPDATE_NEEDED, None)
        
        f_src = open(src, "r")
        try:
            f_dest = open(dest, "r")
        except IOError, e:
            if e.errno == errno.ENOENT:
                verbose("%s does not exist" % path)
                return (UPDATE_NEEDED, None)
            else:
                raise

        fuzzy = self._has_target_attribute(path, target_attributes, super.service.FUZZY)

        ok = True
        for src_line in Expander(self.scope, f_src, path):
            dest_line = f_dest.readline()
            
            if dest_line == "":  # destination ended early
                ok = False
                break

            if (fuzzy):
                src_line = src_line.strip()
                dest_line = dest_line.strip()

            if src_line != dest_line:
                ok = False
                break

        dest_line = f_dest.readline()
        if dest_line != "": # source ended early
            ok = False

        if not ok:
            verbose("%s doesn't match expanded source" % path)

        f_src.close()
        f_dest.close()

        if ok:
            return (UPDATE_OK, None)
        else:
            return (UPDATE_NEEDED, None)

    def _check_copy(self, path):
        """Check a node that is copied literally"""
        src = self.nodes[path].src
        dest = os.path.join(self.target, path)

        # We allow hot redeployment if the destination isn't there,
        # but if the destination is a symlink, or
        # has the wrong permissions, we don't trust deployers
        # to do the right thing.
        perms_result = self._check_perms(path, src, dest)
        if (perms_result == PERMS_MISSING and
            self._test_flag(path, HOT)):
            verbose("Hot redeploy for %s" % path)
            return (UPDATE_HOT, [path])
        elif perms_result == PERMS_OK_AND_SAME_TIMESTAMP:
            return (UPDATE_OK, None)
        elif perms_result != PERMS_OK:
            return (UPDATE_NEEDED, None)
        else:
            # timestamps differ.
            verbose("%s doesn't match source timestamp" % path)
            if self._test_flag(path, HOT):
                verbose("Hot redeploy for %s" % path)
                return (UPDATE_HOT, [path])
            else:
                return (UPDATE_NEEDED, None)

            # if we want to check by content it would look like this, but
            # it's really slow and annoying to do this... maybe it
            # should be a "paranoid mode" set in super.conf ? check_perms
            # already verifies that the files are the same size as a paranoia
            # check, so the main virtue of this is not safety but to avoid
            # updates when _only_ the mtime differs. It's a lot better to
            # update gratuitously once in a while due to mtime change, than
            # always be super-slow due to running this cmp though.
            # the mtimes should not get pointlessly updated unless
            # an ant script is busted, or you make a change in an editor
            # and manually revert it.
            #if (os.spawnl(os.P_WAIT, "/usr/bin/cmp", "cmp", "-s", src, dest) != 0):

    def _check_dir(self, path, target_attributes):
        """Check a directory node and children"""

        dest = os.path.join(self.target, path)

        dest_stat = None
        try:
            dest_stat = os.lstat(dest)
        except OSError:
            verbose("Directory %s not present in target" % path)
            return (UPDATE_NEEDED, None)

        if not stat.S_ISDIR(dest_stat.st_mode):
            verbose("Non-directory %s where directory is needed" % path)
            return (UPDATE_NEEDED, None)

        # See if there are any children of the output directory
        # that aren't in our child list (new excludes, say)
        children = {}
        for child_path in self.nodes[path].children:
            children[child_path] = 1
        
        for f in os.listdir(dest):
            child_path = os.path.join(path, f)
            if (not children.has_key(child_path) and
                not self._has_target_attribute (child_path, target_attributes, super.service.IGNORE)):
                    verbose("Extra file in target dir: %s" % child_path)
                    return (UPDATE_NEEDED, None)
        
        result = UPDATE_OK
        hotcopies = []

        for child_path in self.nodes[path].children:
            (child_result, child_hotcopies) = self._check_path(child_path, target_attributes)
            if (child_result == UPDATE_NEEDED):
                result = UPDATE_NEEDED
                break
            elif (child_result == UPDATE_HOT):
                result = UPDATE_HOT
                hotcopies.extend(child_hotcopies)

	# check timestamp on the directory only for hot-updated directories
        if self._test_flag(path, HOT):
            n = self.nodes[path]
            if n.times and n.times[1] != dest_stat.st_mtime:
                verbose("target dir %s has wrong mtime" % dest)
                # hot-update if allowed
                if result == UPDATE_HOT or result == UPDATE_OK:
                    result = UPDATE_HOT
                    # dir added to hotcopies further down
                else:
                    pass # result should already be UPDATE_NEEDED
            else:
                verbose("target dir %s has correct mtime" % dest)

        if result == UPDATE_HOT:
            # if we update any files in a dir we need to also hot update
            # the directory, otherwise creating the files might break
            # the mtime on the dir
            if path != '': # root node has path of ''
                hotcopies.extend([path])
            return (result, hotcopies)
        else:
            return (result, [])

    def _check_path(self, path, target_attributes):
        """Checks a node and its children"""
        if self._test_flag(path, SYMLINK):
            return self._check_symlink(path)
        elif self._test_flag(path, DIR):
            return self._check_dir(path, target_attributes)
        elif self._test_flag(path, EXPAND):
            return self._check_expand(path, target_attributes)
        else:
            return self._check_copy(path)
            
    def _test_flag(self, path, flag):
        """Check if the given node has the flag set."""
        return (self.nodes[path].flags & flag) != 0

    def _clear_flag(self, path, flag):
        """Clear the flag from the given node."""
        self.nodes[path].flags = self.nodes[path].flags & ~flag


def compile_pattern(line):
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

