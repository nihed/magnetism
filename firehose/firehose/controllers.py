from turbogears import controllers, expose, flash
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

    @expose("json", as_format="json", accept_format="text/javascript")
    def addtask(self, taskid):
        from firehose.jobs.master import MasterPoller        
        master = MasterPoller.get()
        master.add_task(taskid)
        return {}