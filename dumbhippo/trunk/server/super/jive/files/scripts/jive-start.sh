#!/bin/sh

targetdir=@@targetdir@@
mysqlTargetdir=@@mysqlTargetdir@@
mysqlOptions=@@mysqlOptions@@

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

java -server \
     -DmessengerHome=$targetdir \
     -Dmessenger.lib.dir=$targetdir/lib \
     -classpath $targetdir/lib/startup.jar \
     -jar $targetdir/lib/startup.jar &

echo "$!" > $targetdir/run/jive.pid


