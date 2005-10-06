#!/bin/sh

targetdir=@@targetdir@@

echo "Stopping Jive Messenger..."

if [ \! -f $targetdir/run/jive.pid ] ; then 
    echo "... not running"
    exit 0
fi

pid=`cat $targetdir/run/jive.pid`

if kill $pid ; then : ; else
    echo "... not running"
    exit 0
fi

timeout=30
interval=1
while [ $timeout -gt 0 ] ; do
    if ps -p $pid > /dev/null ; then : ; else
	echo "... stopped"
	exit 0
    fi

    sleep $interval
    let timeout="$timeout - $interval"
done

echo "...timed out"
exit 1

