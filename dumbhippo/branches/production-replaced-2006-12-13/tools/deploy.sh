#!/bin/sh

set -e

if ! [ $# = 2 ] ; then
    echo "Usage: deploy.sh foo.dar user@server,user@server,..." 1>&2
    exit 1
fi

darfile="$1"
servers="$2"

if ! [ -e $darfile ] ; then 
    echo "DAR file '$1' doesn't exist" 1>&2
    exit 1
fi

if [ "$servers" = "" ] ; then
    echo "Must deploy to at least one server" 1>&2
    exit 1
fi
 
IFS="${IFS= 	}"; save_ifs="$IFS"; IFS=","

for server in $servers; do
    echo "Stopping $server" 1>&2
    ssh -T $server '(test -x bin/super/super && bin/super/super stop all) || true' < /dev/null
done

for server in $servers; do
    echo "Deploying $darfile to $server" 1>&2
    ssh -T $server 'rm -rf bin ; mkdir bin; cd bin ; tar xfz -' < $1
done

for server in $servers; do
    echo "Restarting $server" 1>&2
    ssh -T $server 'bin/super/super start all' < /dev/null
done

IFS="$save_ifs"
