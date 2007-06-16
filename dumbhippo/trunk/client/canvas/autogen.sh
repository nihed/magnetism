#!/bin/sh
gtkdocize
ACLOCAL="aclocal $ACLOCAL_FLAGS" autoreconf -i
./configure --enable-maintainer-mode "$@"
