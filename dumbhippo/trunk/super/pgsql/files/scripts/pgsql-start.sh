#!/bin/sh

targetdir=@@targetdir@@
datadir=$targetdir/data
pgsqlOptions="@@pgsqlOptions@@"
dbcommand="/usr/bin/psql $pgsqlOptions"

chmod 0700 $datadir
chmod 0700 $targetdir/run

if [ -f $datadir/PG_VERSION ] ; then : ; else
    echo "Initializing database..."
    /usr/bin/initdb -D $datadir -U postgres
    $dbcommand postgres <<EOF
create user dumbhippo login nosuperuser nocreatedb password '@@dbPassword@@'
EOF
fi

pg_ctl -D $targetdir/conf start

# pg_ctl doesn't actually wait until the postgreSQL is accepting
# connections so we have to check for that ourselves

started=false
failed=false

timeout=30
interval=1
while [ $timeout -gt 0 ] ; do
    # FIXME: get rid of pg_ctl usage so that we can see if postmaster 
    # exits without waiting for the timeout
    
    if echo "" | $dbcommand postgres > /dev/null 2>&1 ; then
	started=true
	break
    fi
 
    sleep $interval
    let timeout="$timeout - $interval"
done

if $started ; then
    echo "...sucessfully started"
    exit 0
elif $failed ; then
    echo "...failed to start"
    exit 1
else
    echo "...timed out"
    exit 1
fi
