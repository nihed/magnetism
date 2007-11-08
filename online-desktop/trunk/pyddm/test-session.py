#!/usr/bin/python

from optparse import OptionParser
import logging
import os
import re
import sys

from ddm import DataModel
from dbus.mainloop.glib import DBusGMainLoop
import gobject

logging.basicConfig(level=logging.DEBUG)

DBusGMainLoop(set_as_default=True)

def on_name_change(resource):
    print "The name changed: ", resource.name

def on_self_change(resource):
    print "The resource changed: "
    resource._dump()

def on_self_query_success(resource):
    resource._dump()
    resource.connect(on_name_change, "name")
    resource.connect(on_self_change)

def on_self_query_failure(code, message):
    print message

def on_buddy_changed(buddy):
    buddy._dump()

def on_buddies_changed(global_resource):
    for buddy in global_resource.onlineBuddies:
        buddy.disconnect(on_buddy_changed) # In case we connected previously
        buddy.connect(on_buddy_changed)
        on_buddy_changed(buddy)
    
def on_global_query_success(global_resource):
    global_resource.connect(on_buddies_changed, "onlineBuddies")
    on_buddies_changed(global_resource)

def on_global_query_failure(code, message):
    print message
    
def online_changed(resource):
    print "Online: ", resource.online
    
def on_ready():
    print "Ready"

    if model.self_resource != None:
        query = model.query_resource(model.self_resource, "+;contacts +;contacters +;lovedAccounts +;email;aim")
        query.add_handler(on_self_query_success)
        query.add_error_handler(on_self_query_failure)
        query.execute()

    query = model.query_resource(model.global_resource, "onlineBuddies +")
    query.add_handler(on_global_query_success)
    query.add_error_handler(on_global_query_failure)
    query.execute()
    
    model.global_resource.connect(online_changed, "online")
    online_changed(model.global_resource)

parser = OptionParser()
parser.add_option("-s", "--server", default="localinstance.mugshot.org:8080", help="Mugshot server to connect to (default localinstance.mugshot.org:8080)")
(options, args) = parser.parse_args()
if len(args) > 0:
    parser.print_usage()
    sys.exit(1)

model = DataModel(options.server)
model.add_ready_handler(on_ready)
if model.ready:
    on_ready()

loop = gobject.MainLoop()
loop.run()
