#!/bin/sh

jbossdir=@@jbossdir@@
targetdir=@@targetdir@@
jnpPort=@@jnpPort@@
jdwpPort=@@jdwpPort@@

mysqlTargetdir=@@mysqlTargetdir@@
mysqlOptions=@@mysqlOptions@@

echo "Starting jboss..."

if [ -d $mysqlTargetdir/data/dumbhippo ] ; then : ; else
    echo "... dumbhippo database doesn't exist, creating ..."
    /usr/bin/mysql $mysqlOptions <<EOF
create database dumbhippo character set utf8 collate utf8_bin
EOF
fi

JAVA_OPTS="-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=$jdwpPort,suspend=n" \
$jbossdir/bin/run.sh -Djboss.server.home.dir=$targetdir -Djboss.server.home.url=file://$targetdir > /dev/null &
pid=$!
started=false
for i in `seq 1 30` ; do
    if ps -p $pid > /dev/null ; then : ; else
	break
    fi
    sleep 2
    result="`$jbossdir/bin/twiddle.sh -s jnp://localhost:$jnpPort get jboss.system:type=Server Started --noprefix`"
    if [ $? == 0 -a x"$result" == x"true" ] ; then
	started=true
	break
    fi
done

if $started ; then
    echo $pid > $targetdir/run/jboss.pid
    echo "...sucessfully started"
    exit 0
elif test i = 30 ; then
    echo "...timed out"
    exit 1
else
    echo "...failed to start"
    exit 1
fi

