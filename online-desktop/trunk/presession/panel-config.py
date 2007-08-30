#!/usr/bin/env python 

import os,sys

from pyonlinedesktop import odpanel

# Right now we always frob the existing panel setup.  As far as I know, there
# isn't a good way to handle this.
odpanel.setup_panels()
