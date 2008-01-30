from turbogears import controllers, expose, flash
import cherrypy
# from model import *
# import logging
# log = logging.getLogger("firehose.controllers")

class Root(controllers.RootController):
    @expose(template="firehose.templates.welcome")
    def index(self):
        import time
        # log.debug("Happy TurboGears Controller Responding For Duty")
        flash("Your application is now running")
        return dict(now=time.ctime())

    @expose("json")
    def addtask(self, taskid=None):
        if cherrypy.request.method != 'POST':
            raise Exception("Must invoke this method using POST")
        if taskid is None:
            return {}
        
        from firehose.jobs.master import MasterPoller        
        master = MasterPoller.get()
        master.add_task(taskid)
        return {}
    
    @expose("json")
    def taskset_status(self, results=None):
        if cherrypy.request.method != 'POST':
            raise Exception("Must invoke this method using POST")
        from firehose.jobs.master import MasterPoller        
        master = MasterPoller.get()
        master.taskset_status(results)
        return {}                