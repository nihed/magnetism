#!/bin/sh

targetdir=@@targetdir@@

echo "Stopping Jive Wildfire..."

$twiddle invoke jboss.system:service=MainDeployer undeploy $targetdir/deploy/wildfire.sar
