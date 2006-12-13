#!/bin/sh

pgsqlOptions="@@pgsqlOptions@@"
dbcommand="/usr/bin/psql $pgsqlOptions"

$dbcommand postgres <<EOF
drop database dumbhippo ;
create database dumbhippo with owner dumbhippo ;
EOF
