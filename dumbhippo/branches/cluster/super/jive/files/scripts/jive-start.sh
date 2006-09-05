#!/bin/sh

targetdir=@@targetdir@@
twiddle="@@twiddle@@"
slaveMode="@@slaveMode@@"
dbpath="@@dbpath@@"
dbpassword="@@dbpassword@@"

echo "Starting Jive Wildfire..."

######################################################################

cat > $targetdir/hsqldb.rc << EOF
urlid jivedb
url jdbc:hsqldb:file:$dbpath
username sa
password
EOF

(cat wildfire/src/database/wildfire_hsqldb.sql wildfire/src/database/upgrade/2.3_to_2.4/wildfire_hsqldb.sql && echo 'commit;' && echo 'shutdown;') | java -cp wildfire/build/lib/dist/hsqldb.jar org.hsqldb.util.SqlTool --autoCommit --stdinput --rcfile $targetdir/hsqldb.rc jivedb - >/dev/null 

sleep 2
perl -pi -e "s/CREATE USER SA PASSWORD .*\$/CREATE USER SA PASSWORD \"$dbpassword\"/" $dbpath.script

cat > $targetdir/hsqldb.rc << EOF
urlid jivedb
url jdbc:hsqldb:file:$dbpath
username sa
password $dbpassword
EOF

java -cp wildfire/build/lib/dist/hsqldb.jar org.hsqldb.util.SqlTool --autoCommit --stdinput --rcfile $targetdir/hsqldb.rc jivedb 1>/dev/null <<EOF
DELETE FROM jiveProperty ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.plain.interface', '@@bindHost@@') ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.plain.port', @@jivePlainPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.ssl.interface', '@@bindHost@@') ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.ssl.port', @@jiveSecurePort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.server.socket.interface', '@@bindHost@@') ;
INSERT INTO jiveProperty VALUES ( 'xmpp.server.socket.port', @@jiveServerPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.component.socket.interface', '@@bindHost@@') ;
INSERT INTO jiveProperty VALUES ( 'xmpp.component.socket.port', @@jiveComponentPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.domain', 'dumbhippo.com' ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.client.tls.policy', 'disabled') ;
EOF

eval $twiddle invoke jboss.system:service=MainDeployer deploy file://$targetdir/deploy/wildfire.sar/ > /dev/null

started=false

timeout=30
interval=1
while [ $timeout -gt 0 ] ; do
    if $targetdir/scripts/jive-running.py ; then
	started=true
	break
    fi
    timeout=$((timeout - 1))
    sleep 1
done

if $started ; then
    echo "...sucessfully started"
    exit 0
else
    echo "...timed out"
    exit 1
fi
