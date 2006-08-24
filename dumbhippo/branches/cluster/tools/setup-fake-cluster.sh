#!/bin/sh

set -e

prompt() {
   echo -n "$2 ($3)? "
   read v
   if [ x$v = x"" ] ; then
       eval $1=$3
   else
       eval $1=$v
   fi
}

prompt N "How many test instances do you want to create" 3
prompt BASE_ADDR "What is the network address base" 192.168.1.50
prompt SERVER_NAME "What should be the cluster's public name" localinstance.mugshot.org

group=$USER-test

setvars() {
    user=$USER-test$i
    home=/home/$user
    if [ $i = 1 ] ; then
	slave=false
    else
	slave=true
    fi
    start=`echo $BASE_ADDR | sed -r s/\[0-9]\+$//`
    end=`echo $BASE_ADDR | sed -r 's/.*\.([0-9]+)$/\1/'`
    addr="$start$((end+$i-1))"
}

echo
echo "Group: $group"
for i in `seq 1 $N` ; do
    setvars
    echo "User: $user"
    echo "    Slave: $slave" 
    echo "    Home: $home" 
    echo "    Network address: $addr"
done
echo
prompt ok "Look good?" y
if [ x$ok != xy ] ; then
    echo "Aborting"
    exit
fi

sudo groupadd -f $USER-test
sudo usermod -a -G $USER-test $USER

for i in `seq 1 $N` ; do
    setvars
    if ! id $user 1>/dev/null 2>/dev/null ; then
	sudo useradd -g $group $user
    fi
    sudo chmod g+rxs $home
    if ! [ -d $home/.ssh ] ; then
        sudo mkdir $home/.ssh
    fi
    sudo chown $user:$group $home/.ssh
    sudo chmod 0700 $home/.ssh
    sudo cp $HOME/.ssh/authorized_keys $home/.ssh/
    sudo chown $user:$group $home/.ssh/authorized_keys
    if ! [ -d $home/dhdeploy ] ; then
	sudo mkdir $home/dhdeploy
    fi
    sudo chmod g+w $home/dhdeploy
    sudo touch $home/.super.conf
    sudo chown $user:$group $home/.super.conf
    sudo chmod 0664 $home/.super.conf
    if ! $slave ; then
	cat <<EOF > $home/.super.conf
<?xml version="1.0" encoding="UTF-8"?><!-- -*- tab-width: 4; indent-tabs-mode: t -*- -->
<superconf>
    <parameter name="serverHost">$SERVER_NAME</parameter>
    <service name="jboss">
        <parameter name="jbossBind">$addr</parameter>
    </service>
</superconf>
EOF
    else
	cat <<EOF > $home/.super.conf
<?xml version="1.0" encoding="UTF-8"?><!-- -*- tab-width: 4; indent-tabs-mode: t -*- -->
<superconf>
    <parameter name="serverHost">$SERVER_NAME</parameter>
    <parameter name="slaveMode">yes</parameter>
    <service name="jboss">
       <parameter name="jbossBind">$addr</parameter>
    </service>
    <service name="jive" enabled="no"/>
    <service name="mysql" enabled="no"/>
    <service name="imbot" enabled="no"/>
</superconf>
EOF
    fi
done
