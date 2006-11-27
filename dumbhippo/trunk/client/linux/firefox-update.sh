#!/bin/sh
# 
# This script is used to add and remove our extension from the Firefox
# directory, and is run from 'triggers' when Firefox is installed or
# upgraded, as well as when our package is installed. It is needed because
# Firefox is installed into versioned directories in /usr/lib[64]/firefox
#
if [ "$1" = "install" ] ; then
    for libdir in /usr/lib /usr/lib64 ; do
	# Add symlinks to any firefox directory that looks like it is part of a
	# currently installed package
	for d in $libdir/firefox* ; do
	    if [ d = "$libdir/firefox*" ] ; then
		continue
	    fi
	    link=$d/extensions/firefox@mugshot.org
	    target=$libdir/mugshot/firefox
	    if [ -e $target -a -e $d/firefox-bin -a -d $d/extensions -a ! -L $link ] ; then
		ln -s $target $link
	    fi
	done
    done
elif [ "$1" = "remove" ] ; then
    for libdir in /usr/lib /usr/lib64 ; do
	# Remove any symlinks we've created into any firefox directory
	for d in $libdir/firefox* ; do
	    if [ d = "$libdir/firefox*" ] ; then
		continue
	    fi
	    link=$d/extensions/firefox@mugshot.org
	    if [ -L $link ] ; then
		rm $link
	    fi
	done
     done
else
    echo "Usage firefox-update.sh [install/remove]"
fi
