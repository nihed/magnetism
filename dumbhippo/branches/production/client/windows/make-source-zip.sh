#!/bin/sh

PROJECT=Mugshot

DIRECTORIES="Applets dependencies HippoExplorer HippoShellExt HippoUI HippoUtil loudmouth TestLoudmouth WiXInstaller"

version=`grep VERSION HippoUI/Version.h  | sed 's/[^"]*"\([^"]*\).*/\1/'`

IMAGE_TEST="-name '*.png'"

NONIMAGE_FILES="`find $DIRECTORIES -name .svn -prune -o -name '*.dll' -o -name '*.lib' -o -name '*~' -o -name '*.png' -o -name '*.bmp' -o -name '*.ico' -o -name '*.jpg' -o -print`"
IMAGE_FILES="`find $DIRECTORIES -name '*.png' -o -name '*.bmp' -o -name '*.ico' -o -name '*.jpg'`"

NONIMAGE_FILES="$NONIMAGE_FILES LICENSE.txt"

rm -f $PROJECT-$version-source.zip
zip -l $PROJECT-$version-source.zip $NONIMAGE_FILES
zip $PROJECT-$version-source.zip $IMAGE_FILES

# Add in a common/ subdirectory; this isn't in the right place for the project
# files, but people will probably be building out of subversion anyways
cd ..
( cd common && make clean ) 
NONIMAGE_FILES="`find common -name .svn -prune -o -name '*.dll' -o -name '*.lib' -o -name '*~' -o -name '*.png' -o -name '*.bmp' -o -name '*.ico' -o -name '*.jpg' -o -print`"
zip -l windows/$PROJECT-$version-source.zip $NONIMAGE_FILES

