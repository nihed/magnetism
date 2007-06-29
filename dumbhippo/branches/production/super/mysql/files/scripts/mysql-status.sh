#!/bin/sh

targetdir=@@targetdir@@

#
# Check first by pid/ps
#
pidfile=$targetdir/run/mysqld.pid
if [ \! -f $pidfile ] ; then 
    exit 1
fi

pid=`cat $pidfile`
if ps -p $pid > /dev/null ; then : ; else
    exit 1
fi

#'ping' works fine if there is no such user, but requires login if there 
# is a user. (Security leak? If this is fixed, we need to grep the
# response on failure)
if /usr/bin/mysqladmin -S $targetdir/run/mysql.sock -uUNKNOWN_MYSQL_USER ping > /dev/null 2>&1 ; then
    exit 0
else
    exit 1
fi
