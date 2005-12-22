#!/bin/sh

DIRECTORIES="Applets dependencies HippoExplorer HippoShellExt HippoUI HippoUtil WiXInstaller loudmouth TestLoudmouth TestShareLink WiXInstaller"

version=`grep VERSION HippoUI/Version.h  | sed 's/[^"]*"\([^"]*\).*/\1/'`

FILES="`find $DIRECTORIES -name .svn -prune -o -name '*.dll' -o -print`"

rm -f DumbHippo-$version-source.zip
zip DumbHippo-$version-source.zip $FILES-x .svn
