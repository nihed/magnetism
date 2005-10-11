import errno
import os
import stat
import sys

import super.dirtree
from super.dirtree import DirTree

# Flags for <targetAttributes/> element
IGNORE = 1               # Skip in target when checking for updateness
PRESERVE = 2             # Don't erase for 'build', just fir 'init' 
FUZZY = 4                # Do approximate comparisons ... right now,
                         # defined as "ignore trailing whitespace"

class Service:

    """An object representing one service."""
    
    def __init__(self, name, config):
        self.name = name
        self.config = config
        self.params = {}
        self.required_services = {}
        self.merges = []
        self.target_attributes = []

    def get_name(self):
        """Return the name of the service"""
        return self.name

    def add_merge(self, merge):
        """Add a <merge/> element (represented by Merge) to the service."""
        self.merges.append(merge)

    def add_required_service(self, service_name):
        """Add a <requiredService/> element to the service."""
        self.required_services[service_name] = 1

    def add_target_attributes(self, pattern, ignore, preserve, fuzzy):
        flags = 0
        if ignore:
            flags |= IGNORE
        if preserve:
            flags |= PRESERVE
        if fuzzy:
            flags |= FUZZY

        (pattern, pattern_flags) = super.dirtree.compile_pattern(pattern)
            
        self.target_attributes.append((pattern, pattern_flags, flags))

    def get_target_attributes(self):
        return self.target_attributes

    #### Parameter handling. The following methods make up a common
    #### interface with Config.
        
    def set_parameter(self, name, value):
        """Set a parameter value."""
        self.params[name] = value

    def get_parameter(self, name):
        """Get the unexpanded value of a parameter."""
        if self.params.has_key(name):
            return self.params[name]
        else:       
            return self.config.get_parameter(name)

    def has_parameter(self, name):
        """Return True if the given parameter was set."""
        if self.params.has_key(name):
            return True
        else:       
            return self.config.has_parameter(name)

    def expand_parameter(self, name):
        """Return the fully expanded value of the given parameter."""
        return self.config.expand_parameter(name, self)

    def expand(self, str):
        """Return the contents of str with embedded parameters expanded."""
        return self.config.expand(str, self)

    def create_dirtree(self):
        """Return a DirTree object built for the service"""
        target = self.expand_parameter('targetdir')
        dirtree = DirTree(target, self)

        for merge in self.merges:
            merge.add_to_tree(dirtree)

        return dirtree

    def clean(self):
        """Remove files from the target tree not marked to preserve"""
        
        target = self.expand_parameter('targetdir')
        self._clean_recurse('', target)

    #### Methods for different actions #####

    def init(self):
        target = self.expand_parameter('targetdir')
        os.spawnl(os.P_WAIT, '/bin/rm', 'rm', '-rf', target)

        dirtree = self.create_dirtree()
        dirtree.write()

    def build(self):
        self.clean()

        dirtree = self.create_dirtree()
        dirtree.write()

    def start(self):
        if not self.has_parameter('startCommand'):
            return

        startCommand = self.expand_parameter('startCommand')
        os.system(startCommand)

    def stop(self):
        if not self.has_parameter('stopCommand'):
            return

        stopCommand = self.expand_parameter('stopCommand')
        os.system(stopCommand)
        
    def status(self):
        statusCommand = self.expand_parameter('statusCommand')
        return os.system(statusCommand) == 0

    def _clean_recurse(self, path, fullpath):
        # Worker function for clean()
        try:
            s = os.lstat(fullpath)
            isdir = stat.S_ISDIR(s.st_mode)
        except OSError, e:
            if e.errno == errno.ENOENT:
                return True
            raise
        
        if (path != ''):
            for (pattern, flags, attributes) in self.target_attributes:
                if (attributes & super.service.PRESERVE) == 0:
                    continue
                if (flags & super.dirtree.DIRECTORY_ONLY) != 0:
                    if not isdir:
                        continue
                # NEGATE is meaningless here, ignore
                if not pattern.match(path):
                    continue
                return False
            
        if isdir:
            gotall = True
            for f in os.listdir(fullpath):
                if not self._clean_recurse(os.path.join(path, f),
                                           os.path.join(fullpath, f)):
                    gotall = False
            if gotall:
                os.rmdir(fullpath)
                return True
            else:
                return False

        os.remove(fullpath)
        return True
   
def sort(services):
    """Sort services (a list of Service objects) so that
    if the services are started in that argument, <requiredService/>
    elements will be satisfied. Modifies the input list.
    """
    
    # Find a service that doesn't depend on any others
    # move to the front, repeat. This is a really inefficient
    # algorithm (O(n^3)) If we ever have more than 3 services, replace
    # with a real topological sort
    for i in range(0, len(services)):
        to_front = -1
        for j in range(i, len(services)):
            service = services[i]
            no_requires = True
            for k in range(j + 1, len(services)):
                if service.required_services.has_key(services[k].name):
                    no_requires = False
                    break
            if no_requires:
                to_front = j
                break
        if to_front == -1:
            print >>sys.stderr, "Unable to sort according to requiredService"
            sys.exit(1)
        if to_front != i:
            service = services[to_front]
            del services[to_front]
            services.insert(i, service)

