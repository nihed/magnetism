SUBDIRS=. docs

CANVASSRCDIR=$(top_srcdir)

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = hippo-canvas-1.pc

# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-DHIPPO_COMPILATION -DHIPPO_OS_LINUX -I$(top_builddir)/config -I$(COMMONSRCDIR) -I$(LINUXSRCDIR)

# the "config" dir seems like a convenient place that's already 
# in everyone's include path ... I dunno
BUILT_SRC_DIR=$(top_builddir)/config/hippo-canvas
# Generated marshallers and enums go into this directory as well
MARSHAL_DIR=$(BUILT_SRC_DIR)
ENUMS_DIR=$(BUILT_SRC_DIR)

EXTRA_DIST = LICENSE hippo-canvas-1.pc.in docs/Makefile.am

# if srcdir!=builddir, clean out maintainer-clean files from builddir
# this allows dist to pass.
distclean-local:
	if test $(srcdir) != . ; then		\
	  rm -f $(MAINTAINERCLEANFILES) ;	\
	  rmdir $(BUILT_SRC_DIR) ;		\
	fi

# These are added to using += in the included files
lib_LTLIBRARIES =
DISTCLEANFILES =
MAINTAINERCLEANFILES =
BUILT_SOURCES =

lib_LTLIBRARIES += libhippocanvas-1.la

libhippocanvas_1_la_CPPFLAGS =		\
	$(AM_CPPFLAGS)			\
	-I $(MARSHAL_DIR)		\
	-I $(top_srcdir)/common		\
	-I $(top_srcdir)/linux		\
	-I common/hippo			\
	$(LIBHIPPOCANVAS_CFLAGS)

libhippocanvas_1_la_LIBADD = 		\
	$(LIBHIPPOCANVAS_LIBS)

include Makefile-canvas-sources.am

libhippocanvasincludedir = $(includedir)/hippo-canvas-1/hippo
libhippocanvasinclude_HEADERS=$(COMMON_CANVAS_HEADERFILES) $(LINUX_CANVAS_HEADERFILES)

libhippocanvas_1_la_SOURCES=$(COMMON_CANVAS_SOURCEFILES) $(LINUX_CANVAS_SOURCEFILES)
nodist_libhippocanvas_1_la_SOURCES=$(CANVAS_BUILT_SOURCEFILES)

if HAVE_PYTHON
include Makefile-python.am
endif

DISTCHECK_CONFIGURE_FLAGS=--enable-gtk-doc
