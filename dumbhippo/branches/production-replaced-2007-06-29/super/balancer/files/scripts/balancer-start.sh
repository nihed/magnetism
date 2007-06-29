#!/bin/sh

targetdir=@@targetdir@@
xmppHost="@@serverHost@@"
xmppPort="@@jivePlainPort@@"
clusterHosts="@@clusterHosts@@"
balancerBind="@@balancerBind@@"

echo "Starting XMPP connection balancer..."

cd $targetdir

if [ x"$JAVA_HOME" != x ] ; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
fi

deps=""
for D in $targetdir/lib/*.jar ; do
        deps="$D:$deps" ;
done

"$JAVA" -Xdebug 					\
     -server 						\
     -classpath $deps:$targetdir/dumbhippo-xmpp-balancer.jar    \
     com.dumbhippo.xmpp.Balancer "$balancerBind" "$xmppHost" "$xmppPort" $clusterHosts >$targetdir/logs/balancer.log 2>&1 &

pid=$!

# catch an immediate failure if any
sleep 2
if ps -p $pid > /dev/null ; then : ; else
	echo "... failed"
	exit 1
fi

echo "$pid" > $targetdir/run/balancer.pid
echo "...sucessfully started"
exit 0
