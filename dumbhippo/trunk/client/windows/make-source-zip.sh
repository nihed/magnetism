#!/bin/sh

PROJECT=Mugshot-windows

DIRECTORIES="Applets dependencies HippoExplorer HippoShellExt HippoUI HippoUtil loudmouth TestLoudmouth WiXInstaller"

version=`grep VERSION HippoUI/Version.h  | sed 's/[^"]*"\([^"]*\).*/\1/'`

IMAGE_TEST="-name '*.png'"

NONIMAGE_FILES="`find $DIRECTORIES -name .svn -prune -o -name '*.dll' -o -name '*.lib' -o -name '*~' -o -name '*.png' -o -name '*.bmp' -o -name '*.ico' -o -name '*.jpg' -o -print`"
IMAGE_FILES="`find $DIRECTORIES -name '*.png' -o -name '*.bmp' -o -name '*.ico' -o -name '*.jpg'`"

rm -f $PROJECT-$version-source.zip
zip -l $PROJECT-$version-source.zip $NONIMAGE_FILES
zip $PROJECT-$version-source.zip $IMAGE_FILES
