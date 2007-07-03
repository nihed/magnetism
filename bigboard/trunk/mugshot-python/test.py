#!/usr/bin/python

from optparse import OptionParser
import os
import re
import sys

from mugshot.TwistedModel import *

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

def on_connect():
    print "Connected"

    # FIXME: having to include the port means won't work on non-debug servers
    query = model.query_resource("http://%s:8080/o/user/%s" % (model.server,  model.username),
                                 "+;contacts +;contacters +;lovedAccounts +")
    query.add_handler(on_query_success)
    query.add_error_handler(on_query_failure)
    query.execute()

parser = OptionParser()
parser.add_option("-s", "--server", default="localinstance.mugshot.org:21020", help="Mugshot server to connect to (default localinstance.mugshot.org:21020)")
parser.add_option("-u", "--user", help="User to connect to the server as (default from Firefox cookies)")
parser.add_option("-p", "--password", help="Password to use to connect to the server (default from Firefox cookies)")
(options, args) = parser.parse_args()
if len(args) > 0:
    parser.print_usage()
    sys.exit(1)

model = TwistedModel(options.server, options.user, options.password)

model.add_connected_handler(on_connect)
model.run()
