#!/bin/sh
gtkdocize
autoreconf -i
./configure --enable-maintainer-mode "$@"
