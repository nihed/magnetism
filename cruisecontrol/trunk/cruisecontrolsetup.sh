#!/bin/bash

## See http://cruisecontrol.sourceforge.net/gettingstarted.html
## The idea of this script is to automate those instructions so we 
## don't have to figure it all out again if we have to re-setup

VERSION=2.3.0.1
CRUISE_CHECKOUT=/root/cruisecontrol
INSTALL_ROOT="$CRUISE_CHECKOUT"/install_root
INSTALL_DIR=$INSTALL_ROOT/cruisecontrol-$VERSION
WORK_DIR=$INSTALL_ROOT/work

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

(cd $CHECKOUT_DIR && svn co http://dumbhippo.com/svn/testhippo/trunk testhippo || die "could not check out project")

cp -f "$CRUISE_CHECKOUT"/config.xml "$WORK_DIR" || die "could not copy in config.xml"

echo "Now (cd $WORK_DIR ; bash $INSTALL_DIR/main/bin/cruisecontrol.sh)"

