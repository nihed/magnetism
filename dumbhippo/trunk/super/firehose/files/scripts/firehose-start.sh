#!/bin/sh

targetdir=@@targetdir@@

echo "Starting Firehose"

cd $targetdir

./start-firehose.py ./conf/mugshot.cfg &

pid=$!

# catch an immediate failure if any
sleep 2
if ps -p $pid > /dev/null ; then : ; else
	echo "... failed"
	exit 1
fi

echo "$pid" > $targetdir/run/firehose.pid
echo "...sucessfully started"
exit 0
