#!/bin/sh

targetdir=@@targetdir@@

echo "Starting IM Bot..."

cd $targetdir/bin

if [ x"$JAVA_HOME" != x ] ; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
fi

"$JAVA" -Xdebug 				\
     -server 					\
     -classpath $targetdir/lib/startup.jar 	\
     -jar $targetdir/lib/startup.jar >/dev/null 2>&1 &

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
