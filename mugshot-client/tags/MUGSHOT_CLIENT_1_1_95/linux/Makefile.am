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
AM_CPPFLAGS=-DHIPPO_COMPILATION -DHIPPO_OS_LINUX -DVERSION_FILE=\"$(pkgdatadir)/version\" -I$(top_builddir)/config -I$(COMMONSRCDIR) -I$(COMMONSRCDIR)/canvas

# the "config" dir seems like a convenient place that's already 
# in everyone's include path ... I dunno
BUILT_SRC_DIR=$(top_builddir)/config/hippo
# Generated marshallers go into this directory as well
MARSHAL_DIR=$(BUILT_SRC_DIR)
ENUMS_DIR=$(BUILT_SRC_DIR)

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

dist-hook: libhippo-dist-hook libhippoipc-dist-hook libhippofirefox-dist-hook mugshot-dist-hook libstacker-dist-hook
.PHONY: libhippo-dist-hook libhippoipc-dist-hook libhippofirefox-dist-hook mugshot-dist-hook libstacker-dist-hook

# These are added to using += in the included files
bin_PROGRAMS = 
libexec_PROGRAMS = 
noinst_PROGRAMS =
noinst_LTLIBRARIES =
lib_LTLIBRARIES =
DISTCLEANFILES =
MAINTAINERCLEANFILES =
BUILT_SOURCES =
pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA =
pkgdata_DATA =
TESTS =

include Makefile-libhippoipc.am
include Makefile-libhippo.am
include Makefile-libstacker.am
include Makefile-libgtkcopy.am
include Makefile-libhippofirefox.am
include Makefile-mugshot.am
include Makefile-mugshot-uri-handler.am
include Makefile-test-hippo-ipc.am
include Makefile-icons.am

DISTCHECK_CONFIGURE_FLAGS=--with-included-canvas

.PHONY: tag
tag:
	@cd $(top_srcdir)/.. && \
	(svn status | grep '^[MA]' > /dev/null && echo "Uncommitted changes, can't tag") || \
	(V=`echo $(VERSION) | sed s/\\\\./_/g` && \
	URL=`svn info --xml | grep '<url>' | sed 's@</\?url>@@g'` && \
	PACKAGE=`echo $$URL | perl -pe 's@.*/(.*)/.*@$$1@; tr/a-z-/A-Z_/;'` && \
	TAGURL=`echo $$URL | sed "s@trunk@tags/$${PACKAGE}_$$V@"` && \
	echo Tagging as $$TAGURL && \
	svn cp -m "Tag as $${PACKAGE}_$$V" $$URL $$TAGURL)
