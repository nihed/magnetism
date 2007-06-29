#!/bin/bash

# Folder names
DOJO=dojo-`date +%F`
OUT_DIR=../release/

# Build profiles
echo Build profiles...
ant # get it setup
for pfile in $(cd profiles; ls *.profile.js; cd ..)
do
	profile=`echo $pfile | sed 's/.profile.js//g'`
	echo Building profile: $profile
	ant -q -Ddocless=true -Dprofile=$profile release
	proName=dojo-0.1.0-plus-$profile
	cd ../release
	mv dojo $proName
	tar -zcf $OUT_DIR/$proName.tar.gz $proName/
	zip -rq $OUT_DIR/$proName.zip $proName/
	rm -rf proName
	cd ../buildscripts
done
