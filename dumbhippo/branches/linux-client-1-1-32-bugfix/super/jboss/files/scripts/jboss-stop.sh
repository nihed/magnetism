#!/bin/sh

targetdir=@@targetdir@@
jbossdir=@@jbossdir@@
jnpPort=@@jnpPort@@
bindHost="@@bindHost@@"

pidfile=$targetdir/run/jboss.pid

echo "Stopping JBoss..."

if [ \! -f $pidfile ] ; then 
    echo "... not running"
    exit 0
fi

pid=`cat $pidfile`

$jbossdir/bin/shutdown.sh -s jnp://$bindHost:$jnpPort > /dev/null &

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

