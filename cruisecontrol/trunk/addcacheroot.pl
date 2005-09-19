#!/usr/bin/perl -iw

$doing = 1;
while (<>) { 
    if ($doing && s@</context-param>@</context-param><context-param><param-name>cacheRoot</param-name><param-value>$ENV{'CACHE_DIR'}</param-value></context-param>@) {
	$doing = 0;
    }
    print;
}

