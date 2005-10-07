#!/bin/sh

targetdir=@@targetdir@@
mysqlTargetdir=@@mysqlTargetdir@@
mysqlOptions=@@mysqlOptions@@

echo "Starting Jive Messenger..."

if [ -d $mysqlTargetdir/data/jive ] ; then : ; else
    /usr/bin/mysqladmin $mysqlOptions create jive
    /usr/bin/mysql $mysqlOptions jive < $targetdir/resources/database/messenger_mysql.sql
fi

/usr/bin/mysql $mysqlOptions jive <<EOF
DELETE FROM jiveProperty ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.plain.port', @@jivePlainPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.secure.port', @@jiveSecurePort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.server.socket.port', @@jiveServerPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.component.socket.port', @@jiveComponentPort@@ ) ;
EOF

cd $targetdir/bin

if x"$JAVA_HOME" != x ; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
fi

"$JAVA" -server \
     -DmessengerHome=$targetdir \
     -Dmessenger.lib.dir=$targetdir/lib \
     -classpath $targetdir/lib/startup.jar \
     -jar $targetdir/lib/startup.jar >/dev/null 2>&1 &

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
