#!/bin/sh
mysqlOptions=@@mysqlOptions@@

name=`date +%Y%m%d-%H%M%S`
mysqldump $mysqlOptions -d dumbhippo > dumbhippo-schema-${name}
mysqldump $mysqlOptions -t -c dumbhippo > dumbhippo-data-${name}
