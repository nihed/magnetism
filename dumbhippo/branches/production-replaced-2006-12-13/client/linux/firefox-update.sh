#!/bin/sh
# 
# This script is used to add and remove our extension from the Firefox
# directory, and is run from 'triggers' when Firefox is installed or
# upgraded, as well as when our package is installed. It is needed because
# Firefox is installed into versioned directories in /usr/lib/firefox
#
if [ "$1" = "install" ] ; then
    # Add symlinks to any firefox directory that looks like it is part of a
    # currently installed package
    for d in /usr/lib/firefox* ; do
	if [ -e $d/firefox-bin -a -d $d/extensions -a ! -L $d/extensions/firefox@mugshot.org ] ; then
	    ln -s /usr/lib/mugshot/firefox $d/extensions/firefox@mugshot.org
	fi
    done
elif [ "$1" = "remove" ] ; then
    # Remove any symlinks we've created into any firefox directory
    for d in /usr/lib/firefox* ; do
	if [ -L $d/extensions/firefox@mugshot.org ] ; then
	    rm $d/extensions/firefox@mugshot.org
	fi
    done
else
    echo "Usage firefox-update.sh [install/remove]"
fi
