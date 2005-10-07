import os
import sys

from super.dirtree import DirTree

class Service:
    def __init__(self, name, config):
        self.name = name
        self.config = config
        self.params = {}
        self.required_services = {}
        self.merges = []

    def get_name(self):
        return self.name

    def add_merge(self, merge):
        self.merges.append(merge)

    def add_required_service(self, service_name):
        self.required_services[service_name] = 1

    def set_parameter(self, name, value):
        self.params[name] = value

    def has_parameter(self, name):
        if self.params.has_key(name):
            return True
        else:       
            return self.config.has_parameter(name)

    def get_parameter(self, name):
        if self.params.has_key(name):
            return self.params[name]
        else:       
            return self.config.get_parameter(name)

    def expand_parameter(self, name):
        return self.config.expand_parameter(name, self)

    def expand(self, str):
        return self.config.expand(str, self)


    def init(self):
        pass

    def build(self):
        target = self.expand_parameter('targetdir')
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
        pass

# Really stupid sort algorithm for requires; O(n^3). If we
# ever have more than 3 services, replace with a real
# topological sort
def sort(services):
    # Find a service that doesn't depend on any others
    # move to the front, repeat
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

