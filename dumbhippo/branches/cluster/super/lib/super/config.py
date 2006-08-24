import copy
import os
import re
import string
import sys
import xml.sax

from super.confighandler import ConfigHandler;
from super.deployer import Deployer;
import super.service

class Config:
    
    """The main object for Super. It represents the state from
    parsing all relevant config files. The run_action() method
    is called to do the work of 'super' after config files have
    been parsed.
    """
    
    def __init__(self, superdir, conffile, init_params):
        """Create a new config object, loading in config files.

        superdir -- the directory in which the 'super' script is located
        conffile -- config file passed as --conf= on the command line
        init_params -- dictionary of extra parameter settings.
           Use servicename.foo as the key to set a service-specific settings
        """
        self.params= { 'superdir' : superdir }
        self.services={}

        self._load_config(os.path.join(superdir, 'base.conf'), True)
        if (os.environ.has_key('HOME')):
            self.params['home'] = os.environ['HOME'];
            
        if conffile is not None:
            self._load_config(conffile, True)
        elif (os.environ.has_key('HOME')):
            self._load_config(os.path.join(os.environ['HOME'], '.super.conf'), False)
        for (name, value) in init_params.items():
            self._set_init_parameter(name, value)

    def filter_runnable(self, services):
        """Filter out non-runnable services from a list of services"""
        def is_runnable(service_name):
            return self.services[service_name].is_runnable()

        return filter(is_runnable, services)

    def run_action(self, action, services):
        """Run an action on a set of services"""
        
        # Sort services in dependency order
        services = self._sort_services(services)
        
        if action == 'init':
            for service_name in services:
                print >>sys.stderr, "Initializing", service_name
                self.services[service_name].init()
        elif action == 'build':
            for service_name in services:
                print >>sys.stderr, "Building", service_name
                self.services[service_name].build()
        elif action == 'deploy':
            deployer = Deployer(self)
            for service_name in services:
                service = self.services[service_name]
                deployer.add_dirtree(service.create_dirtree())
            deployer.write()
        elif action == 'start':
            for service_name in services:
                print >>sys.stderr, "Checking tree for:", service_name
                service = self.services[service_name]
                dirtree = service.create_dirtree()
                (result, hot_files) = dirtree.check(service.get_target_attributes())
                if result == super.dirtree.UPDATE_NEEDED:
                    print >>sys.stderr, "Was out-of-date; rebuilding"
                    service.clean()
                    dirtree.write()
                elif result == super.dirtree.UPDATE_HOT:
                    print >>sys.stderr, "Was out-of-date; hot updating (before starting)"
                    dirtree.hot_update(hot_files)
                else:
                    print >>sys.stderr, "Was OK"
                if service.is_runnable():
                    print >>sys.stderr, "Starting:", service_name
                    service.start()
        elif action == 'stop':
            services = self.filter_runnable(services)
            
            # Stop services before their dependencies
            stop_order = copy.copy(services)
            stop_order.reverse()
            for service_name in stop_order:
                service = self.services[service_name]
                if service.status():
                    print >>sys.stderr, "Stopping", service_name
                    service.stop()
                else:
                    print >>sys.stderr, service_name, "is not running"
        elif action == 'restart':
            services = self.filter_runnable(services)
            
            self.run_action('stop', services)
            self.run_action('start', services)
        elif action == 'reload':
            all_services = {}
            running_services = {}
            stop_services = {}
            
            for service_name in services:
                service = self.services[service_name]
                all_services[service_name] = 1
                if service.is_runnable() and service.status():
                    print >>sys.stderr, "%s is running" % service_name
                    running_services[service_name] = 1
            rebuild_services = []
            rebuild_trees = []
            hot_services = []
            hot_trees = []
            hot_file_lists = []

            # Check for services that needs updates
            for service_name in services:
                service = self.services[service_name]
                dirtree = service.create_dirtree()
                (result, hot_files) = dirtree.check(service.get_target_attributes())
                if result == super.dirtree.UPDATE_NEEDED:
                    print >>sys.stderr, "%s needs rebuild" % service_name
                    rebuild_services.append(service_name)
                    rebuild_trees.append(dirtree)
                    if running_services.has_key(service_name):
                        stop_services[service_name] = 1
                elif result == super.dirtree.UPDATE_HOT:
                    print >>sys.stderr, "%s needs hot update for %s" % (service_name, ", ".join(hot_files))
                    hot_services.append(service_name)
                    hot_trees.append(dirtree)
                    hot_file_lists.append(hot_files)

            # Find out what additional services to stop for dependencies
            # The sorting of services will make transitive dependencies
            # work.
            for service_name in services:
                if running_services.has_key(service_name):
                    for required in self.services[service_name].required_services.keys():
                        if all_services.has_key(required) and (not running_services.has_key(required) or
                                                               stop_services.has_key(required)):
                            print >>sys.stderr, "Will restart %s because of dependency on %s" % (service_name, required)
                            
                            stop_services[service_name] = 1
                            break

            did_something = False
                        
            # Now stop all services that need to be stopped
            stop_order = copy.copy(services)
            stop_order.reverse()
            for service_name in stop_order:
                if stop_services.has_key(service_name):
                    print >>sys.stderr, "Stopping %s" % service_name
                    self.services[service_name].stop()
                    del running_services[service_name]
                    did_something = True

            # Do updates
            for i in range(0, len(rebuild_services)):
                service = self.services[rebuild_services[i]]
                print >>sys.stderr, "Rebuilding %s" % service.name
                service.clean()
                rebuild_trees[i].write()
                did_something = True

            for i in range(0, len(hot_services)):
                service = self.services[hot_services[i]]
                print >>sys.stderr, "Hot updating %s" % service.name
                hot_trees[i].hot_update(hot_file_lists[i])
                did_something = True

            # And start everything that isn't running
            for service_name in services:
                service = self.services[service_name]
                if not running_services.has_key(service_name) and service.is_runnable():
                    print >>sys.stderr, "Starting %s" % service_name
                    service.start()
                    did_something = True

            if not did_something:
                print "Nothing to do"
            
        elif action == 'status':
            services = self.filter_runnable(services)

            for service_name in services:
                if self.services[service_name].status():
                    print >>sys.stdout, "%s is running" % service_name
                else:
                    print >>sys.stdout, "%s is stopped" % service_name

        elif action == 'nuke':
            for service_name in services:
                service = self.services[service_name]
                if not service.has_nuke():
                    print >>sys.stderr, "Don't know how to nuke state for %s" % service_name
                    sys.exit(1)

                print >>sys.stderr, "Really nuke state for %s? [N/y]" % service_name
                ans = sys.stdin.readline().strip()
                if (ans == 'y'):
                    service.nuke()
                else:
                    print >>sys.stderr, "Not nuking state"
                    
        elif action == 'console':
            if len(services) != 1:
                print >>sys.stderr, "Can only run the console for one service at a time"
                sys.exit(1)
            self.services[services[0]].console()
            
        elif action == 'watch':
            if len(services) > 1:
                print >>sys.stderr, "You can only watch one service"
                sys.exit(1)
            self.services[services[0]].watch()
                    
    def add_service(self, service):
        """Add service to the list of services."""
        self.services[service.get_name()] = service
        
    def has_service(self, name):
        """Check if a service with the given name is present."""
        return self.services.has_key(name)

    def get_service(self, name):
        """Return the configured service with the given name."""
        return self.services[name]

    def list_services(self):
        """Return all configured services."""
        result = []
        for service in self.services.values():
            if (service.get_enabled()):
                result.append(service.get_name())
        return result

    #### Parameter handling. The following methods make up a common
    #### interface with Service.
        
    def set_parameter(self, name, value):
        """Set a parameter value."""
        self.params[name] = value

    def get_parameter(self, name):
        """Get the unexpanded value of a parameter."""
        return self.params[name]

    def has_parameter(self, name):
        """Return True if the given parameter was set."""
        return self.params.has_key(name)

    def expand_parameter(self, name, scope=None):
        """Return the fully expanded value of the given parameter.
        see expand() for the definition of the scope argument.
        """
        if scope is None:
            scope = self

        return self.expand(scope.get_parameter(name), scope)

    def expand(self, str, scope=None):
        """Return the contents of str with embedded parameters expanded.

        str -- the string to expand
        scope -- object in which parameter lookups will be done
        must have has_parameter() and get_parameter() methods.
        (if not specified, then the scope is the toplevel config file)
        """
        if scope is None:
            scope = self

        ident = '[a-zA-Z_][a-zA-Z_0-9]*'
        arithmetic = '\$\(\(\s*(%s)\s*\+\s*([0-9]+)\s*\)\)' % ident
        
        def repl(m):
            match_str = m.group(0)
            if match_str == '\x5C$':
                return '$';
            else:
                arithmetic_m = re.match(arithmetic, match_str)
                if arithmetic_m:
                    param_name = arithmetic_m.group(1)
                    increment = arithmetic_m.group(2)

                    param = self.expand_parameter(param_name, scope)

                    return "%d" % (string.atoi(param) + string.atoi(increment))
                else:
                    param_name = match_str[1:]

                    return self.expand_parameter(param_name, scope)

        ident = "[a-zA-Z_][a-zA-Z_0-9]*"
        # \x5C = backslash, trying to avoid "\\\\\\\\\\\\" syndrome
        expr = '\x5C\x5C\x5C$|%s|\$%s' % (arithmetic, ident)
        return re.sub(expr, repl, str)

    def is_true(self, str):
        """Return whether a string is a true value without expansion"""
        val = str.strip()
        
        if (val == 'yes'):
            return True
        elif (val == 'no'):
            return False
        else:
            raise ValueError("Boolean value must be 'yes' or 'no', not '%s'" % val)

    def is_true_expanded(self, str):
        """Expand string and checks if it is true"""
        return self.is_true(self.expand(str))

    def is_true_parameter(self, name):
        """Gets the (expanded) value of a parameter and checks if it is true"""
        return self.is_true(self.expand_parameter(name))

    def _load_config(self, filename, must_exist):
        """Load a single config file and merge it into the existing state.

        filename -- filename to load
        must_exist -- if true, die if the file doesn't exist, otherwise
                      non-existant files are ignored.
        """
        handler = ConfigHandler(self)
        f = None
        try: 
            f = open(filename)
        except IOError:
            if must_exist:
                print >>sys.stderr, "Cannot open config file '%s'" % filename
                sys.exit(1)
        if f:
            try:
                xml.sax.parse(f, handler)
            except xml.sax.SAXParseException, e:
                print >>sys.stderr, e
                sys.exit(1)
            f.close()

    def _sort_services(self, service_names):
        """Sorts a list of service names by dependency order.
        the first service to be started (last to be stopped)
        is sorted first"""
        services = map(lambda x: self.services[x], service_names)
        super.service.sort(services)
        return map(lambda x: x.name, services)
    
    def _set_init_parameter(self, name, value):
        """Set a parameter passed on the commandline. Honors servicename.foo
        as a parameter name"""
        m = re.match('([^.]+)\.(.+)', name)
        if m:
            service_name = m.group(1)
            param_name = m.group(2)
            if not self.services.has_key(service_name):
                print >>sys.stderr, "Unknown service", service_name
                sys.exit(1)
            self.services[service_name].set_parameter(param_name, value)
        else:
            self.set_parameter(name, value)
