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