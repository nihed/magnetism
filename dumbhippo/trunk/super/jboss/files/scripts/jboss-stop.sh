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

timeout=15
interval=1
while [ $timeout -gt 0 ] ; do
    if ps -p $pid > /dev/null ; then : ; else
	echo "... stopped"
	rm -f "${pidfile}"
	exit 0
    fi

    sleep $interval
    let timeout="$timeout - $interval"
done

echo "...timed out, initiating forced termination of pid $pid (\"Hasta la vista, baby.\")"
kill -9 $pid

timeout=5
interval=1
while [ $timeout -gt 0 ] ; do
    if ps -p $pid > /dev/null ; then : ; else
	echo "... stopped"
	rm -f "${pidfile}"
	exit 0
    fi

    sleep $interval
    let timeout="$timeout - $interval"
done

echo "...timed out"
exit 1

