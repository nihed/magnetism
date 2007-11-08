#!/usr/bin/python

from optparse import OptionParser
import os
import re
import sys

from ddm.TwistedModel import *

def on_name_change(resource):
    print "The name changed: ", resource.name

def on_change(resource):
    print "The resource changed: "
    resource._dump()

def on_query_success(resource):
    resource._dump()
    resource.connect(on_name_change, "name")
    resource.connect(on_change)

def on_query_failure(code, message):
    print message

def online_changed(resource):
    print "Online: ", resource.online
    
def on_ready():
    print "Ready"

    # FIXME: having to include the port means won't work on non-debug servers
    query = model.query_resource(model.self_resource,
                                 "+;contacts +;contacters +;lovedAccounts +")
    query.add_handler(on_query_success)
    query.add_error_handler(on_query_failure)
    query.execute()

    model.global_resource.connect(online_changed, "online")
    online_changed(model.global_resource)

parser = OptionParser()
parser.add_option("-s", "--server", default="localinstance.mugshot.org:21020", help="Mugshot web server to connect to (default localinstance.mugshot.org)")
parser.add_option("-x", "--xmpp-server", help="XMPP server to connect to (if not specified, derived from --server)")
parser.add_option("-u", "--user", help="User to connect to the server as (default from Firefox cookies)")
parser.add_option("-p", "--password", help="Password to use to connect to the server (default from Firefox cookies)")
(options, args) = parser.parse_args()
if len(args) > 0:
    parser.print_usage()
    sys.exit(1)

model = TwistedModel(web_server=options.server, xmpp_server=options.xmpp_server, username=options.user, password=options.password)

model.add_ready_handler(on_ready)
model.run()
