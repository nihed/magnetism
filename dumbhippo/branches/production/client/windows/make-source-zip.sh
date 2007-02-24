#!/bin/sh

PROJECT=Mugshot

DIRECTORIES="airbag dependencies cairo HippoExplorer HippoFirefox HippoFirefoxExtension HippoFirefoxStub HippoIpc HippoShellExt HippoUI HippoUtil libglib libgmodule libgobject libgthread libpango libpangocairo libpangowin32 loudmouth Images loudmouth Sheets util WiXInstaller"

version=`grep VERSION HippoUI/Version.h  | sed 's/[^"]*"\([^"]*\).*/\1/'`

IMAGE_TEST="-name '*.png'"

NONIMAGE_FILES="`find $DIRECTORIES -name .svn -prune -o -name '*.dll' -o -name '*.dll.a' -o -name '*.exe' -o -name '*.lib' -o -name '*~' -o -name '*.png' -o -name '*.bmp' -o -name '*.ico' -o -name '*.jpg' -o -print`"
IMAGE_FILES="`find $DIRECTORIES -name '*.png' -o -name '*.bmp' -o -name '*.ico' -o -name '*.jpg'`"

NONIMAGE_FILES="$NONIMAGE_FILES LICENSE.txt"

rm -f $PROJECT-$version-source.zip
zip -l $PROJECT-$version-source.zip $NONIMAGE_FILES
zip $PROJECT-$version-source.zip $IMAGE_FILES

# Add in a common/ subdirectory; this isn't in the right place for the project
# files, but people will probably be building out of subversion anyways
cd ..
NONIMAGE_FILES="`find common/firefox common/hippo common/hippoipc common/images -name .svn -prune -name '*.dll' -o -name '*.dll.a' -o -name '*.exe' -o -name '*.lib' -o -name '*~' -o -name '*.png' -o -name '*.bmp' -o -name '*.ico' -o -name '*.jpg' -o -print`"
zip -l windows/$PROJECT-$version-source.zip $NONIMAGE_FILES

