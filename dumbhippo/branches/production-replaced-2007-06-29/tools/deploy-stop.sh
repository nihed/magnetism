#!/bin/sh

set -e

if ! [ $# = 1 ] ; then
    echo "Usage: deploy-stop.sh  user@server,user@server,..." 1>&2
    exit 1
fi

servers="$1"

IFS="${IFS= 	}"; save_ifs="$IFS"; IFS=","

if [ "$servers" = "" ] ; then
    echo "Must deploy to at least one server" 1>&2
    exit 1
fi
 
for server in $servers; do
    echo "Stopping $server" 1>&2
    ssh -T $server '(test -x bin/super/super && bin/super/super stop all) || true' < /dev/null
done

IFS="$save_ifs"
