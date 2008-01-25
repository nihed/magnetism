#!/usr/bin/python
__requires__="TurboGears"
import pkg_resources

from turbogears import config, update_config, start_server
import cherrypy
cherrypy.lowercase_api = True
from os.path import *
import sys

# first look on the command line for a desired config file,
# if it's not on the command line, then
# look for setup.py in this directory. If it's not there, this script is
# probably installed
if len(sys.argv) > 1:
    update_config(configfile=sys.argv[1],
        modulename="firehose.config")
elif exists(join(dirname(__file__), "setup.py")):
    update_config(configfile="dev.cfg",modulename="firehose.config")
else:
    update_config(configfile="prod.cfg",modulename="firehose.config")
config.update(dict(package="firehose"))

from firehose.controllers import Root
start_server(Root())
