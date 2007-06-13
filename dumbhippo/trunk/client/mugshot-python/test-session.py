#!/usr/bin/python

from optparse import OptionParser
import os
import re
import sys

from mugshot import DataModel
from dbus.mainloop.glib import DBusGMainLoop
import gobject

DBusGMainLoop(set_as_default=True)

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

    query = model.query_resource(model.self_id, "+;contacts +;contacters +;lovedAccounts +")
    query.add_handler(on_query_success)
    query.add_error_handler(on_query_failure)
    query.execute()

def on_disconnect():
    print "Disconnected"

parser = OptionParser()
parser.add_option("-s", "--server", default="localinstance.mugshot.org:8080", help="Mugshot server to connect to (default localinstance.mugshot.org:21020)")
(options, args) = parser.parse_args()
if len(args) > 0:
    parser.print_usage()
    sys.exit(1)

model = DataModel(options.server)
model.add_connected_handler(on_connect)
model.add_disconnected_handler(on_disconnect)
if model.connected:
    on_connect()

loop = gobject.MainLoop()
loop.run()
