## this is a teenie bit weird; COMMONSRCDIR can be either ../common (build from CVS)
## or common-dist in the current dir (build from tarball). We rename it to common-dist
## when including it to be sure we don't mix the two up.
if COMMON_OUTSIDE_TREE
COMMONSRCDIR=$(top_srcdir)/../common
else
COMMONSRCDIR=$(top_srcdir)/common-dist
endif

# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-DHIPPO_COMPILATION -DVERSION_FILE=\"$(pkgdatadir)/version\" -I$(top_builddir)/config -I$(COMMONSRCDIR)

# the "config" dir seems like a convenient place that's already 
# in everyone's include path ... I dunno
BUILT_SRC_DIR=$(top_builddir)/config/hippo
# Generated marshallers go into this directory as well
MARSHAL_DIR=$(BUILT_SRC_DIR)

## we install a file containing our version, so the running app 
## can check its version vs. the installed app
versiondir=$(pkgdatadir)
version_DATA=version

EXTRA_DIST = version.in LICENSE

# if srcdir!=builddir, clean out maintainer-clean files from builddir
# this allows dist to pass.
distclean-local:
	if test $(srcdir) != . ; then		\
	  rm -f $(MAINTAINERCLEANFILES) ;	\
	  rmdir $(BUILT_SRC_DIR) ;		\
	fi

# These are added to using += in the included files
bin_PROGRAMS = 
noinst_LTLIBRARIES =
MAINTAINERCLEANFILES =
BUILT_SOURCES =

include Makefile-libhippo.am
include Makefile-libgtkcopy.am
include Makefile-mugshot.am
include Makefile-mugshot-uri-handler.am
include Makefile-icons.am
