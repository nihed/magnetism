import os
import re
import string
import sys
import xml.sax

from super.confighandler import ConfigHandler;

class Config:
    def __init__(self, superdir, conffile):
        self.params= { 'superdir' : superdir }
        self.services={}

        self.load_config(os.path.join(superdir, 'base.conf'), True)
        if conffile != None:
            self.load_config(conffile, True)
        elif (os.environ['HOME']):
            self.params['home'] = os.environ['HOME'];
            self.load_config(os.path.join(os.environ['HOME'], '.super.conf'), False)

    def run_action(self, action, services):
        if action == 'init':
            for service_name in services:
                print >>sys.stderr, "Initializing", service_name
                self.services[service_name].init()
        elif action == 'build':
            for service_name in services:
                print >>sys.stderr, "Building", service_name
                self.services[service_name].build()
        elif action == 'start':
            for service_name in services:
                print >>sys.stderr, "Starting", service_name
                self.services[service_name].start()
        elif action == 'stop':
            for service_name in services:
                print >>sys.stderr, "Stopping", service_name
                self.services[service_name].stop()
        elif action == 'restart':
            self.run_action('stop', services)
            self.run_action('start', services)
        elif action == 'reload':
            self.run_action('stop', services)
            self.run_action('start', services)
        elif action == 'status':
            for service_name in services:
                self.services[service_name].status()

    def load_config(self, filename, must_exist):
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

    def add_service(self, service):
        self.services[service.get_name()] = service
        
    def has_service(self, name):
        return self.services.has_key(name)

    def get_service(self, name):
        return self.services[name]

    def list_services(self):
        return self.services.keys()
    
    def set_parameter(self, name, value):
        self.params[name] = value

    def get_parameter(self, name):
        return self.params[name]

    def has_parameter(self, name):
        return self.params.has_key(name)

    def expand_parameter(self, name, scope=None):
        if scope == None:
            scope = self

        return self.expand(scope.get_parameter(name), scope)

    def expand(self, str, scope=None):
        if scope == None:
            scope = self

        ident = '[a-zA-Z_][a-zA-Z_0-9]*'
        arithmetic = '\$\(\(\s*(%s)\s*\+\s*([0-9]+)\s*\)\)' % ident
        
        def repl(m):
            match_str = m.group(0)
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
        return re.sub('%s|\$%s' % (arithmetic, ident), repl, str)
