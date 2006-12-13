#!/usr/bin/python
#
# Very, very, simple version of an XMPP ping. We use this to
# tell if the server is plausibly started
#
import socket
import sys

result = False
try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(("@@bindHost@@", @@jivePlainPort@@))

    s.send("<stream/>")
    got = s.recv(1024)

    s.close()

    # Whether we succeed, or not (we won't), the reply will
    # have a <stream> tag with this namespace
    i = got.index('http://etherx.jabber.org/streams')
    
    result = True
except:
    pass

if result:
    sys.exit(0)
else:
    sys.exit(1)
