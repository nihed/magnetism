#!/bin/sh

targetdir=@@targetdir@@
twiddle="@@twiddle@@"

echo "Stopping Jive Wildfire..."

$twiddle invoke jboss.system:service=MainDeployer undeploy file://$targetdir/deploy/wildfire.sar/ > /dev/null
