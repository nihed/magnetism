import re

bus_name = 'org.freedesktop.od.Engine'
server_name = 'online.gnome.org'
_do_autolaunch_raw = True
do_autolaunch = True

def _escape_byte(m):
    return "_%02X" % ord(m.group(0))

def _escape_server_name(server_name):
    if (server_name.index(":") < 0):
        server_name = server_name + ":80"
        
    return re.sub(r"[^a-zA-Z0-9]", _escape_byte, server_name.encode("UTF-8"))

def _make_bus_name(server_name):

    ## FIXME this is broken; in the new plan we use
    ## com.dumbhippo.Client if we mean the stacker and
    ## org.freedesktop.od.Engine if we mean Online Desktop Engine.
    ## which means both the use of Mugshot and dumbhippo.Client
    ## here are wrong.
    
    if server_name == None:
        return "org.mugshot.Mugshot";

    return "com.dumbhippo.Client." + _escape_server_name(server_name)

def set_server_name(value=None):
    global server_name
    global bus_name
    global do_autolaunch
    global _do_autolaunch_raw
    server_name = value
    bus_name = _make_bus_name(value)
    do_autolaunch = _do_autolaunch_raw and server_name == None

def set_do_autolaunch(value):
    global do_autolaunch
    global _do_autolaunch_raw
    do_autolaunch = value
    do_autolaunch = _do_autolaunch_raw and server_name == None

def get_baseurl():
    ## this is wrong (why?)
    if server_name:
        url = "http://" + server_name

    ## for now we don't have a way to do it right (see DataModel.WebBaseUrl
    ## property, but we don't rely on a new client daemon yet)

    ## the old API leaves trailing '/', once we drop deprecated stuff
    ## above this should not be needed
    if url and url.endswith('/'):
        url = url[:-1]

    return url
