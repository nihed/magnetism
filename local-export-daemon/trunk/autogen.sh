#!/bin/sh

srcdir=`dirname $0`
test -z "$srcdir" && srcdir=.

ORIGDIR=`pwd`

cd $srcdir

ACLOCAL="aclocal $ACLOCAL_FLAGS" autoreconf -i -f

cd $ORIGDIR || exit $?

$srcdir/configure --enable-maintainer-mode "$@"

