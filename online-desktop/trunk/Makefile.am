# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-I$(top_builddir)/config
#ACLOCAL_AMFLAGS = -I m4

EXTRA_DIST=

#SUBDIRS = po

# if srcdir!=builddir, clean out maintainer-clean files from builddir
# this allows dist to pass.
distclean-local:
	if test $(srcdir) != . ; then		\
	  rm -f $(MAINTAINERCLEANFILES) ;	\
	  rmdir $(BUILT_SRC_DIR) ;		\
	fi

#dist-hook: libhippo-dist-hook libhippoipc-dist-hook libhippofirefox-dist-hook mugshot-dist-hook
#.PHONY: libhippo-dist-hook libhippoipc-dist-hook libhippofirefox-dist-hook mugshot-dist-hook

# These are added to using += in the included files
bin_PROGRAMS = 
noinst_PROGRAMS =
noinst_LTLIBRARIES =
DISTCLEANFILES =
MAINTAINERCLEANFILES =
BUILT_SOURCES =

include Makefile-gmail.am

