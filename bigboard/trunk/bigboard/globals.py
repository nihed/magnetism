import re

from ddm import DataModel

bus_name = 'org.freedesktop.od.Engine'
server_name = None
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

## the DataModel code already returns a singleton; the purpose of
## this is just to save typing "DataModel(bigboard.globals.server_name)"
## everywhere.
def get_data_model():
    return DataModel(server_name)

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

## note that the base URL is never supposed to have trailing '/'
def get_baseurl():
    ## first we prefer the base url from the model we're actually using.
    ## this happens when we just connect to "org.freedesktop.od.Engine"
    ## and don't know in advance whether a dogfood or production or whatever
    ## server instance owns that bus name.
    ## Note that this is _supposed_ to work offline as well - the od.Engine
    ## is supposed to have an offline mode.
    url = None
    model = get_data_model()
    if model.connected:
        url = model.get_web_base_url()
        
    ## next we fall back to the server name set by command line option,
    ## see set_server_name() above which is called from main.py
    if server_name and not url:
        url = "http://" + server_name

    ## finally we fall back to a hardcoded URL, since it's probably better
    ## than crashing and would normally be right in production, but never
    ## right when testing on dogfood.
    if not url:
        url = "http://online.gnome.org"

    return url
