#! /bin/bash

set -e

SRCDIR="$1"
shift
GLOB_WE_SHOULD_HAVE="$1"
shift
FILES_WE_HAVE="$*"

#echo "we should have $GLOB_WE_SHOULD_HAVE and we have $FILES_WE_HAVE"

FILES_WE_SHOULD_HAVE=`(cd $SRCDIR && echo $GLOB_WE_SHOULD_HAVE)`

FILES_WE_SHOULD_HAVE=`echo $FILES_WE_SHOULD_HAVE | sed -e 's/ /\n/g' | sort | uniq`
FILES_WE_HAVE=`echo $FILES_WE_HAVE | sed -e 's/ /\n/g' | sort | uniq`

#echo "we have files: $FILES_WE_HAVE"
#echo "we should have files: $FILES_WE_SHOULD_HAVE"

FILES_NOT_IN_BOTH=`echo -e "$FILES_WE_SHOULD_HAVE\n$FILES_WE_HAVE" | sort | uniq -u`

if ! test x"$FILES_NOT_IN_BOTH" = x"" ; then
    echo "Missing files in Makefile: $FILES_NOT_IN_BOTH"
    exit 1
else
    echo "All python files $GLOB_WE_SHOULD_HAVE seem to be in the Makefile"
    exit 0
fi



