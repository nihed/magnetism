#!/bin/sh

targetdir=@@targetdir@@

echo "Starting Jive Wildfire..."

######################################################################

@@if mysqlEnabled
mysqlTargetdir="@@mysqlTargetdir@@"
mysqlOptions="@@mysqlOptions@@"
dbcommand="/usr/bin/mysql $mysqlOptions jive"

if [ -d $mysqlTargetdir/data/jive ] ; then : ; else
    /usr/bin/mysqladmin $mysqlOptions create jive
    $dbcommand < $targetdir/resources/database/wildfire_mysql.sql
fi
@@elif pgsqlEnabled
pgsqlOptions="@@pgsqlOptions@@"
dbcommand="/usr/bin/psql $pgsqlOptions jive"

if [ echo "" | $dbcommand > /dev/null 2>&1 ] ; then : ; else
    /usr/bin/createdb $pgsqlOptions -O dumbhippo jive
    $dbcommand < $targetdir/resources/database/wildfire_mysql.sql
fi
@@else
@@  error "No database"
@@endif

######################################################################

$dbcommand <<EOF
DELETE FROM jiveProperty ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.plain.port', @@jivePlainPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.ssl.port', @@jiveSecurePort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.server.socket.port', @@jiveServerPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.component.socket.port', @@jiveComponentPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.domain', 'dumbhippo.com' ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.client.tls.policy', 'disabled') ;
EOF

cd $targetdir/bin

if [ x"$JAVA_HOME" != x ] ; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
fi

"$JAVA" -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=@@jiveJdwpPort@@,suspend=n \
     -server \
     -Djava.naming.provider.url=jnp://localhost:@@jnpPort@@ \
     -DwildfireHome=$targetdir \
     -Dwildfire.lib.dir=$targetdir/lib \
     -classpath $targetdir/lib/startup.jar \
     -jar $targetdir/lib/startup.jar >$targetdir/logs/stdout.log 2>&1 &

pid=$!

started=false
failed=false

timeout=30
interval=1
while [ $timeout -gt 0 ] ; do
    if ps -p $pid > /dev/null ; then : ; else
	failed=true
	break
    fi
    if $targetdir/scripts/jive-running.py ; then
	started=true
	break
    fi
 
    sleep $interval
    let timeout="$timeout - $interval"
done

if $started ; then
    echo "$pid" > $targetdir/run/jive.pid
    echo "...sucessfully started"
    exit 0
elif $failed ; then
    echo "...failed to start"
    exit 1
else
    echo "...timed out"
    exit 1
fi
