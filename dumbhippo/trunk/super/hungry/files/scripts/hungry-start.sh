#!/bin/sh

targetdir=@@targetdir@@

echo "this doesn't do anything; just do super build to get a hungry conf file, then run hungry in-place with ant"
exit 0

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

"$JAVA" -Xdebug 						\
     	-server

pid=$!

# catch an immediate failure if any
sleep 2
if ps -p $pid > /dev/null ; then : ; else
	echo "... failed"
	exit 1
fi

echo "$pid" > $targetdir/run/hungry.pid
echo "...sucessfully started"
exit 0
