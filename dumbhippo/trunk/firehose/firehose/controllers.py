import logging

import simplejson
from turbogears import controllers, expose, flash
import cherrypy
# from model import *

_logger = logging.getLogger("firehose.controllers")

class Root(controllers.RootController):
    @expose(template="firehose.templates.welcome")
    def index(self):
        import time
        # log.debug("Happy TurboGears Controller Responding For Duty")
        flash("Your application is now running")
        return dict(now=time.ctime())

    @expose("json")
    def addfeed(self, feedurl=None):
        if cherrypy.request.method != 'POST':
            raise Exception("Must invoke this method using POST")
        if feedurl is None:
            _logger.debug("no feed url specified")
            return {}
        
        from firehose.jobs.master import MasterPoller        
        master = MasterPoller.get()
        master.add_feed(feedurl)
        return {}
    
    @expose("json")
    def settasks(self):
        if cherrypy.request.method != 'POST':
            raise Exception("Must invoke this method using POST")        
        from firehose.jobs.master import MasterPoller        
        master = MasterPoller.get()        
        master.set_tasks(cherrypy.request.body)
        return {}
        
    @expose("json")
    def addtasks(self):
        if cherrypy.request.method != 'POST':
            raise Exception("Must invoke this method using POST")        
        from firehose.jobs.master import MasterPoller
        master = MasterPoller.get()        
        master.add_tasks(cherrypy.request.body) 
        return {}        
    
    @expose("json")
    def requeue(self):
        if cherrypy.request.method != 'POST':
            raise Exception("Must invoke this method using POST")
        from firehose.jobs.master import MasterPoller        
        master = MasterPoller.get()
        master.requeue()
        return {}        
    
    @expose("json")
    def taskset_status(self):
        if cherrypy.request.method != 'POST':
            raise Exception("Must invoke this method using POST")
        from firehose.jobs.master import MasterPoller        
        master = MasterPoller.get()
        status = simplejson.load(cherrypy.request.body)
        master.taskset_status(status)
        return {}
    
    @expose(template="firehose.templates.shell")
    def shell(self):
        return {}
    
    @expose("json")
    def shell_exec(self):
        if cherrypy.request.method != 'POST':
            raise Exception("Must invoke this method using POST")
        from firehose.rewrite import rewrite_and_compile
        def handle_output(myself, *args):
            myself['result'] = args[-1]
        localdict={}
        localdict['_hotwire_handle_output'] = handle_output
        localdict['_hotwire_handle_output_self'] = {'result': None}
        code = cherrypy.request.body.read()
        _logger.debug("executing code: %r", code)
        (compiled, mutated) = rewrite_and_compile(code, 
                                                  output_func_name='_hotwire_handle_output', 
                                                  output_func_self='_hotwire_handle_output_self')
        exec compiled in localdict
        result = localdict['_hotwire_handle_output_self']['result']
        return {'type': repr(type(result)), 'repr': repr(result)}
