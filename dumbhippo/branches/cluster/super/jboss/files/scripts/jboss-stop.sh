#!/bin/sh

targetdir=@@targetdir@@
jbossdir=@@jbossdir@@
jnpPort=@@jnpPort@@
jbossBind="@@jbossBind@@"

pidfile=$targetdir/run/jboss.pid

echo "Stopping JBoss..."

if [ \! -f $pidfile ] ; then 
    echo "... not running"
    exit 0
fi

pid=`cat $pidfile`
if test "$jbossBind" = 'all'; then
  jnphost="localhost"
else
  jnphost="$jbossBind"
fi

$jbossdir/bin/shutdown.sh -s jnp://$jnphost:$jnpPort > /dev/null &

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

