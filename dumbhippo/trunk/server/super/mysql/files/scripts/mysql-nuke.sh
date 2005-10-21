#!/bin/sh
targetdir=@@targetdir@@
/usr/bin/mysql --defaults-file=$targetdir/conf/my.cnf <<EOF
drop database dumbhippo
create database dumbhippo
EOF
echo "database nuked; have a nice day!"
