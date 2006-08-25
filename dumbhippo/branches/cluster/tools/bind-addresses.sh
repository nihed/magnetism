#!/bin/sh

set -e

prompt() {
   success=false
   while ! $success ; do
       echo -n "$2 ($3)? "
       read v
       if [ x$v = x"" ] ; then
	   v=$3
       fi

       if [ "${4+set}" != "set" ] ; then
	   success=true
       else
	   if echo "$v" | grep -E "^$4$" > /dev/null ; then
	       success=true
	   fi
       fi
   done
   eval $1=$v
}

prompt N "How many addresses do you want to bind" 3 "[0-9]+"
prompt BASE_ADDR "What is the network address base" 192.168.1.30 "[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+"

setvars() {
    start=`echo $BASE_ADDR | sed -r s/\[0-9]\+$//`
    end=`echo $BASE_ADDR | sed -r 's/.*\.([0-9]+)$/\1/'`
    addr="$start$((end+$i-1))"
}

ifnumber=0
for i in `seq 1 $N` ; do
    setvars

    # Find the first unbound interface
    while /sbin/ifconfig eth0:$ifnumber | grep 'inet addr' > /dev/null ; do
        ifnumber=$((ifnumber + 1))
    done
    
    # Try to bind the address to it
    set +e
    sudo /sbin/ifconfig eth0:$ifnumber $addr > /dev/null 2>&1 
    res=$?
    set -e
    if [ $res = 0 ] ; then
	echo 1>&2 "Bound $addr to eth0:$ifnumber"
    else
	echo 1>&2 "Could not bind $addr, probably already bound"
    fi
done
