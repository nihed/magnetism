#!/bin/sh
mysqlOptions=@@mysqlOptions@@

/usr/bin/mysql $mysqlOptions <<EOF
drop database dumbhippo ;
create database dumbhippo character set utf8 collate utf8_bin ;
EOF
echo "database nuked; have a nice day!"
