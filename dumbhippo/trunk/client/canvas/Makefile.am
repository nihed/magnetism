## this is a teenie bit weird; COMMONSRCDIR can be either ../common (build from CVS)
## or common-dist in the current dir (build from tarball). We rename it to common-dist
## when including it to be sure we don't mix the two up.
if COMMON_OUTSIDE_TREE
COMMONSRCDIR=$(top_srcdir)/../common
else
COMMONSRCDIR=$(top_srcdir)/common-dist
endif

if LINUX_OUTSIDE_TREE
LINUXSRCDIR=$(top_srcdir)/../linux
else
LINUXSRCDIR=$(top_srcdir)/linux-dist
endif

# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-DHIPPO_COMPILATION -DHIPPO_OS_LINUX -I$(top_builddir)/config -I$(COMMONSRCDIR) -I$(COMMONSRCDIR)/canvas

# the "config" dir seems like a convenient place that's already 
# in everyone's include path ... I dunno
BUILT_SRC_DIR=$(top_builddir)/config/hippo-canvas
# Generated marshallers go into this directory as well
MARSHAL_DIR=$(BUILT_SRC_DIR)

EXTRA_DIST = LICENSE

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

lib_LTLIBRARIES += libhippocanvas.la

libhippocanvas_la_CPPFLAGS =		\
	$(AM_CPPFLAGS)			\
	-I $(MARSHAL_DIR)		\
	$(LIBHIPPOCANVAS_CFLAGS)

libhippocanvas_la_LIBADD = 		\
	$(LIBHIPPOCANVAS_LIBS)

COMMONCANVASSRCDIR=$(COMMONSRCDIR)/canvas
LINUXCANVASSRCDIR=$(LINUXSRCDIR)/canvas

CANVAS_MARSHAL_LIST=$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-marshal.list
CANVAS_MARSHAL_HEADER=$(MARSHAL_DIR)/hippo-canvas-marshal.h
CANVAS_MARSHAL_BODY=$(MARSHAL_DIR)/hippo-canvas-marshal.c

$(CANVAS_MARSHAL_HEADER): $(CANVAS_MARSHAL_LIST)
	mkdir $(MARSHAL_DIR) || true
	$(GLIB_GENMARSHAL) --prefix=hippo_canvas_marshal $(CANVAS_MARSHAL_LIST) --header > $(CANVAS_MARSHAL_HEADER)

$(CANVAS_MARSHAL_BODY): $(CANVAS_MARSHAL_LIST)
	mkdir $(MARSHAL_DIR) || true
	(echo "#include \"hippo-canvas-marshal.h\""; $(GLIB_GENMARSHAL) --prefix=hippo_canvas_marshal $(CANVAS_MARSHAL_LIST) --body) > $(CANVAS_MARSHAL_BODY)

COMMON_CANVAS_HEADERFILES = 					\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-box.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-context.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-gradient.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-image.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-image-button.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-item.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-link.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-text.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-widgets.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-graphics.h

LINUX_CANVAS_HEADERFILES =					\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas.h		\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-widget.h

COMMON_CANVAS_SOURCEFILES = 					\
	$(COMMON_CANVAS_HEADERFILES)				\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-internal.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-box.c		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-context.c	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-gradient.c	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-image.c	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-image-button.c	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-item.c		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-link.c		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-text.c		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-graphics.c

LINUX_CANVAS_SOURCEFILES =					\
	$(LINUX_CANVAS_HEADERFILES)				\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-widget.c	\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-widgets.c

CANVAS_BUILT_SOURCEFILES =			\
	$(CANVAS_MARSHAL_HEADER)		\
	$(CANVAS_MARSHAL_BODY)

MAINTAINERCLEANFILES +=	$(CANVAS_BUILT_SOURCEFILES)
BUILT_SOURCES += $(CANVAS_BUILT_SOURCEFILES)

## we need to nodist these because otherwise automake would copy 
## ../common to distdir/../common which puts common outside of distdir.
## so we handle the disting manually so the destination is always distdir/common-dist

libhippocanvasincludedir = $(includedir)/hippo-canvas-1.0/hippo-canvas
nodist_libhippocanvasinclude_HEADERS=$(COMMON_CANVAS_HEADERFILES) $(LINUX_CANVAS_HEADERFILES)

nodist_libhippocanvas_la_SOURCES=$(COMMON_CANVAS_SOURCEFILES) $(LINUX_CANVAS_SOURCEFILES) $(CANVAS_BUILT_SOURCEFILES)
dist-hook:
	-mkdir $(distdir)/common-dist
	mkdir $(distdir)/common-dist/canvas
	mkdir $(distdir)/common-dist/canvas/hippo
	mkdir $(distdir)/linux-dist
	mkdir $(distdir)/linux-dist/canvas
	mkdir $(distdir)/linux-dist/canvas/hippo
	cp $(COMMONCANVASSRCDIR)/hippo/hippo-canvas-marshal.list $(distdir)/common-dist/canvas/hippo
	cp $(COMMON_CANVAS_SOURCEFILES) $(distdir)/common-dist/canvas/hippo
	cp $(LINUX_CANVAS_SOURCEFILES) $(distdir)/linux-dist/canvas/hippo
