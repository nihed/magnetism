import os
import sys

from super.dirtree import DirTree

class Service:

    """An object representing one service."""
    
    def __init__(self, name, config):
        self.name = name
        self.config = config
        self.params = {}
        self.required_services = {}
        self.merges = []

    def get_name(self):
        """Return the name of the service"""
        return self.name

    def add_merge(self, merge):
        """Add a <merge/> element (represented by Merge) to the service."""
        self.merges.append(merge)

    def add_required_service(self, service_name):
        """Add a <requiredService/> element to the service."""
        self.required_services[service_name] = 1

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

    #### Methods for different actions #####

    def init(self):
        # Not yet implemented
        pass

    def build(self):
        target = self.expand_parameter('targetdir')

        # We really should pay attention to attributes set
        # on <directory/> elements and preserve some parts
        # of the tree, but for now, we just remove it all.
        os.spawnl(os.P_WAIT, '/bin/rm', 'rm', '-rf', target)
        
        dirtree = DirTree(target, self)
        for merge in self.merges:
            merge.add_to_tree(dirtree)
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
        # Not yet implemented
        pass

   
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

