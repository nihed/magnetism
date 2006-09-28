#!/bin/bash

set -e

targetdir=@@targetdir@@

pid=`cat $targetdir/run/balancer.pid`

if ps -p $pid > /dev/null ; then
	exit 0;
else
	exit 1;
fi
