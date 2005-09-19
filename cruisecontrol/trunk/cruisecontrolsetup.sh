#!/bin/bash

## See http://cruisecontrol.sourceforge.net/gettingstarted.html
## The idea of this script is to automate those instructions so we 
## don't have to figure it all out again if we have to re-setup

## What you want to do is:
##   - change config.xml or cruisecontrolsetup.sh in subversion
##   - svn checkout to $CRUISE_CHECKOUT below
##   - run cruisecontrolsetup.sh from the checkout

SCRIPT_DIR=`pwd`
VERSION=2.3.0.1
CRUISE_CHECKOUT=/root/cruisecontrol
TOMCAT_WEBAPPS=/var/lib/tomcat5/webapps
CACHE_DIR=/var/cache/tomcat5/cruisecontrol
INSTALL_ROOT="$TOMCAT_WEBAPPS"/cruisecontrol_install
INSTALL_DIR="$INSTALL_ROOT"/cruisecontrol-$VERSION
WORK_DIR="$INSTALL_ROOT"/work

function die() {
  echo $* 1>&2
  exit 1
}

if test x"$JAVA_HOME" = x ; then
    die "You need to set JAVA_HOME, for example export JAVA_HOME=/usr/java/jdk1.5.0_04"
fi

echo "Installing to $INSTALL_ROOT"
mkdir -p "$INSTALL_ROOT" || die "could not mkdir $INSTALL_ROOT"

cd "$INSTALL_ROOT" || die "could not cd to $INSTALL_ROOT"
unzip "$CRUISE_CHECKOUT"/cruisecontrol-$VERSION.zip >/dev/null || die "could not unzip"

echo "Done installing to $INSTALL_ROOT"

mkdir -p $WORK_DIR || die "could not create WORK_DIR $WORK_DIR"

CHECKOUT_DIR=$WORK_DIR/checkout
LOG_DIR=$WORK_DIR/logs
ARTIFACTS_DIR=$WORK_DIR/artifacts

for D in "$CHECKOUT_DIR" "$LOG_DIR" "$ARTIFACTS_DIR" ; do
  mkdir -p "$D" || die "could not create $D"
done

(cd $CHECKOUT_DIR && svn co http://dumbhippo.com/svn/testhippo/trunk testhippo || die "could not check out testhippo")
(cd $CHECKOUT_DIR && svn co http://dumbhippo.com/svn/dumbhippo/trunk/server dumbhippo-server || die "could not check out dumbhippo-server")

cp -f "$CRUISE_CHECKOUT"/config.xml "$WORK_DIR" || die "could not copy in config.xml"

WAR_FILE_ORIG="$INSTALL_DIR"/reporting/jsp/dist/cruisecontrol.war
WAR_UNPACKED_DIR="$TOMCAT_WEBAPPS"/cruisecontrol
/bin/rm -rf "$WAR_UNPACKED_DIR" || true
mkdir -p "$WAR_UNPACKED_DIR" || die "could not create war dir $WAR_UNPACKED_DIR"
(cd "$WAR_UNPACKED_DIR" && jar -xvf "$WAR_FILE_ORIG") || die "could not unpack .war"

## munge the servlet config
SERVLET_CONFIG="$WAR_UNPACKED_DIR"/WEB-INF/web.xml

echo "munging servlet config file"
perl -pi -e "s@>logs<@>$LOG_DIR<@g" $SERVLET_CONFIG || die "failed to munge $SERVLET_CONFIG"
perl -pi -e "s@>artifacts<@>$ARTIFACTS_DIR<@g" $SERVLET_CONFIG || die "failed to munge $SERVLET_CONFIG"

(export CACHE_DIR; "$SCRIPT_DIR"/addcacheroot.pl "$SERVLET_CONFIG" || die "failed to munge in cache root to $SERVLET_CONFIG")

grep "cruise" "$SERVLET_CONFIG" || true

## the .war file doesn't include xalan for some reason
cp "$INSTALL_DIR"/main/lib/xalan-2.6.0.jar "$WAR_UNPACKED_DIR"/WEB-INF/lib || die "failed to copy over xalan.jar"

echo "creating $CACHE_DIR"
mkdir -p "$CACHE_DIR" || die "could not create cache dir"
chown root.tomcat "$CACHE_DIR" || die "could not chown cache dir"
chmod 775 "$CACHE_DIR" || die "could not chmod cache dir"

chown root.tomcat "$INSTALL_ROOT" || die "failed to chown $INSTALL_ROOT"
chown root.root "$INSTALL_DIR" || die "failed to chown $INSTALL_DIR"
chown -R root.tomcat "$WORK_DIR" || die "failed to chown $WORK_DIR"
chown -R root.tomcat "$WAR_UNPACKED_DIR" || die "failed to chown $WAR_UNPACKED_DIR"

## give tomcat the same perms as root on the appropriate dirs/files
chmod g+u "$INSTALL_ROOT" || die "failed to chmod $INSTALL_ROOT"
chmod -R g+u "$WORK_DIR" || die "failed to chmod $WORK_DIR"
chmod -R g+u "$WAR_UNPACKED_DIR" || die "failed to chmod $WAR_UNPACKED_DIR"

echo "restarting tomcat to get new config (brute force again...)"
service tomcat5 restart

echo "Now (cd $WORK_DIR ; bash $INSTALL_DIR/main/bin/cruisecontrol.sh)"

echo "To make it a daemon add '2>&1 >/dev/null </dev/null ; disown' for example"
