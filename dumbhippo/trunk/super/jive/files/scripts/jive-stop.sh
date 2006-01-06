#!/bin/sh

targetdir=@@targetdir@@

pidfile=$targetdir/run/jive.pid

echo "Stopping Jive Wildfire..."

if [ \! -f $pidfile ] ; then 
    echo "... not running"
    exit 0
fi

pid=`cat $pidfile`

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

