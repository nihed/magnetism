# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-I$(top_builddir)/config

EXTRA_DIST=

# if srcdir!=builddir, clean out maintainer-clean files from builddir
# this allows dist to pass.
distclean-local:
	if test $(srcdir) != . ; then		\
	  rm -f $(MAINTAINERCLEANFILES) ;	\
	fi

dist-hook: daemon-dist-hook
.PHONY: daemon-dist-hook

# These are added to using += in the included files
bin_PROGRAMS = 
libexec_PROGRAMS = 
noinst_PROGRAMS =
noinst_LTLIBRARIES =
DISTCLEANFILES =
MAINTAINERCLEANFILES =
BUILT_SOURCES =

include Makefile-daemon.am
