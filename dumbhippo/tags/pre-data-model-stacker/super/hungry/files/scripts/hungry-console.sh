#!/bin/sh

set -e

enable_destructive="@@enableDestructive@@"
target_dir="@@targetdir@@"
source_dir="@@svndir@@/hungry"

n_commands=0

add_command() {
    n_commands="$((n_commands + 1))"
    eval "text$n_commands=\"[$n_commands] $1\""
    eval "command$n_commands=\"$2\""
}

run_readonly() { 
    echo "Running Read-only Tests"

    (
	cd $source_dir
	HUNGRY_PROPERTIES=$target_dir/conf/hungry.properties ant test-readonly
    )
}

add_command "Run read-only tests" "run_readonly"

run_destructive() { 
    echo "Running Destructive Tests"

    (
	cd $source_dir
	HUNGRY_PROPERTIES=$target_dir/conf/hungry.properties ant test-destructive
    )
}

if $enable_destructive ; then
    add_command "Run destructive tests" "run_destructive"
fi

create_performance_data() {
    echo "Creating performance data"

    (
	cd $source_dir
	HUNGRY_PROPERTIES=$target_dir/conf/hungry.properties ant performance-data
    )
}

if $enable_destructive ; then
    add_command "Create performance data" "create_performance_data"
fi

run_performance() {
    echo "Running Performance Tests"

    (
	cd $source_dir
	HUNGRY_PROPERTIES=$target_dir/conf/hungry.properties ant test-performance
    )
}

if $enable_destructive ; then
    add_command "Run performance tests" "run_performance"
fi

while true ; do
    for i in `seq 1 $n_commands` ; do
	eval "echo \$text$i"
    done
    echo "[q] Quit"

    read command
    if [ "x$command" = 'xq' ] ; then
	exit 0
    fi

    for i in `seq 1 $n_commands` ; do
	if [ "x$command" = x$i ] ; then
	    eval "\$command$i"
	    break
	fi
    done
done
