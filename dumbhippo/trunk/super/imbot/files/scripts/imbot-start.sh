#!/bin/sh

targetdir=@@targetdir@@

echo "Starting IM Bot..."

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

echo "classpath $deps"

"$JAVA" -Xdebug 					\
     -server 						\
     -classpath $deps:$targetdir/conf:$targetdir/dumbhippo-imbot.jar 	\
     -Dimbot.logdir="${targetdir}/logs" com.dumbhippo.aimbot.Main 2>&1 &

pid=$!

# catch an immediate failure if any
sleep 2
if ps -p $pid > /dev/null ; then : ; else
	echo "... failed"
	exit 1
fi

echo "$pid" > $targetdir/run/imbot.pid
echo "...sucessfully started"
exit 0
