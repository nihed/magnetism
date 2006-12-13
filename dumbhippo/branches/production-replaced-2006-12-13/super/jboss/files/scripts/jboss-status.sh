#!/bin/sh

jbossdir=@@jbossdir@@
targetdir=@@targetdir@@
twiddle="@@twiddle@@"
bindHost="@@bindHost@@"

#
# Running twiddle can be quite slow, so check first by pid/ps
#
pidfile=$targetdir/run/jboss.pid
if [ \! -f $pidfile ] ; then 
    exit 1
fi

pid=`cat $pidfile`
if ps -p $pid > /dev/null ; then : ; else
    exit 1
fi

result="`$twiddle get jboss.system:type=Server Started --noprefix`"

if [ $? == 0 -a x"$result" == x"true" ] ; then
    exit 0
else
    exit 1
fi
