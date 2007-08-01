#!/bin/sh

targetdir=@@targetdir@@
twiddle="@@twiddle@@"

echo "Stopping Jive Openfire..."

$twiddle invoke jboss.system:service=MainDeployer undeploy file://$targetdir/deploy/openfire.sar/ > /dev/null
