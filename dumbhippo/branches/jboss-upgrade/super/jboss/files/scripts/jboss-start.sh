#!/bin/sh

jbossdir=@@jbossdir@@
targetdir=@@targetdir@@
jnpPort=@@jnpPort@@
jdwpPort=@@jdwpPort@@
javaMaxHeap=@@javaMaxHeap@@

echo "Starting jboss..."

######################################################################

@@if mysqlEnabled
mysqlTargetdir="@@mysqlTargetdir@@"
mysqlOptions="@@mysqlOptions@@"
dbcommand="/usr/bin/mysql $mysqlOptions jive"

if [ -d $mysqlTargetdir/data/dumbhippo ] ; then : ; else
    echo "... dumbhippo database doesn't exist, creating ..."
    /usr/bin/mysql $mysqlOptions <<EOF
create database dumbhippo character set utf8 collate utf8_bin ;
EOF
fi
@@elif pgsqlEnabled
pgsqlOptions="@@pgsqlOptions@@"
dbcommand="/usr/bin/psql $pgsqlOptions dumbhippo"

if echo "" | $dbcommand > /dev/null 2>&1 ; then : ; else
    echo "... dumbhippo database doesn't exist, creating ..."
    /usr/bin/createdb $pgsqlOptions -O dumbhippo dumbhippo
fi
@@else
@@  error "No database"
@@endif

######################################################################

# The lucene indices get written relative to the current working directory,
# so change the cwd to the right place; when we upgrade hibernate-annotations
# to a newer version, we can set a property for the index location
cd $targetdir/data
JAVA_OPTS="-Xmx${javaMaxHeap}m -Xms${javaMaxHeap}m -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=$jdwpPort,suspend=n" \
$jbossdir/bin/run.sh -Djboss.server.home.dir=$targetdir -Djboss.server.home.url=file://$targetdir > /dev/null 2>&1 &
pid=$!
started=false
for i in `seq 1 30` ; do
    if ps -p $pid > /dev/null ; then : ; else
	break
    fi
    sleep 2
    result="`JAVA_OPTS=-Dorg.jboss.logging.Logger.pluginClass=org.jboss.logging.NullLoggerPlugin $jbossdir/bin/twiddle.sh -s jnp://localhost:$jnpPort get jboss.system:type=Server Started --noprefix`"
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

