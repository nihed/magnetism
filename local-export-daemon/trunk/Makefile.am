# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-I$(top_builddir)/config

# the "config" dir seems like a convenient place that's already 
# in everyone's include path ... I dunno
#BUILT_SRC_DIR=$(top_builddir)/config/FIXMEheaderdirname
# Generated marshallers go into this directory as well
#MARSHAL_DIR=$(BUILT_SRC_DIR)
#ENUMS_DIR=$(BUILT_SRC_DIR)

EXTRA_DIST=

# if srcdir!=builddir, clean out maintainer-clean files from builddir
# this allows dist to pass.
distclean-local:
	if test $(srcdir) != . ; then		\
	  rm -f $(MAINTAINERCLEANFILES) ;	\
	  rmdir $(BUILT_SRC_DIR) ;		\
	fi

dist-hook: daemon-dist-hook
.PHONY: daemon-dist-hook

# These are added to using += in the included files
bin_PROGRAMS = 
noinst_PROGRAMS =
noinst_LTLIBRARIES =
DISTCLEANFILES =
MAINTAINERCLEANFILES =
BUILT_SOURCES =

include Makefile-daemon.am
