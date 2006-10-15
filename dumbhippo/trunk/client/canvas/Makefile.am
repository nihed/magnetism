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
AM_CPPFLAGS=-DHIPPO_COMPILATION -DHIPPO_OS_LINUX -I$(top_builddir)/config -I$(COMMONSRCDIR)/canvas -I$(LINUXSRCDIR)/canvas

# the "config" dir seems like a convenient place that's already 
# in everyone's include path ... I dunno
BUILT_SRC_DIR=$(top_builddir)/config/hippo-canvas
# Generated marshallers and enums go into this directory as well
MARSHAL_DIR=$(BUILT_SRC_DIR)
ENUMS_DIR=$(BUILT_SRC_DIR)

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

lib_LTLIBRARIES += libhippocanvas-1.la

libhippocanvas_1_la_CPPFLAGS =		\
	$(AM_CPPFLAGS)			\
	-I $(MARSHAL_DIR)		\
	$(LIBHIPPOCANVAS_CFLAGS)

libhippocanvas_1_la_LIBADD = 		\
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

CANVAS_ENUMS_HEADER=$(ENUMS_DIR)/hippo-canvas-type-builtins.h
CANVAS_ENUMS_BODY=$(ENUMS_DIR)/hippo-canvas-type-builtins.c
CANVAS_ENUMS_HEADER_STAMP=$(ENUMS_DIR)/stamp-hippo-canvas-type-builtins.h
CANVAS_ENUMS_BODY_STAMP=$(ENUMS_DIR)/stamp-hippo-canvas-type-builtins.c

$(CANVAS_ENUMS_BODY): $(CANVAS_ENUMS_BODY_STAMP)
	@true
$(CANVAS_ENUMS_BODY_STAMP): $(COMMON_CANVAS_HEADERFILES)
	(cd $(COMMONCANVASSRCDIR) && $(GLIB_MKENUMS) \
		--fhead "#include \"hippo-canvas-type-builtins.h\"\n\n" \
		--fprod "\n/* enumerations from \"@filename@\" */" \
		--fprod "\n#include \"@filename@\"" \
		--vhead "const G@Type@Value _@enum_name@_values[] = {" \
		--vprod "  { @VALUENAME@, \"@VALUENAME@\", \"@valuenick@\" }," \
		--vtail "  { 0, NULL, NULL }\n};\n\n" \
		--vtail "GType\n@enum_name@_get_type (void)\n{\n" \
		--vtail "  static GType type = 0;\n\n" \
		--vtail "  if (G_UNLIKELY (type == 0))\n" \
		--vtail "    type = g_@type@_register_static (\"@EnumName@\", _@enum_name@_values);\n\n" \
		--vtail "  return type;\n}\n\n" \
		$(^F:%=hippo/%)) > xgen-$(@F) \
	&& ( cmp -s xgen-$(@F) $(CANVAS_ENUMS_BODY) || cp xgen-$(@F) $(CANVAS_ENUMS_BODY)) \
	&& rm -f xgen-$(@F) \
	&& echo timestamp > $(CANVAS_ENUMS_BODY_STAMP)

$(CANVAS_ENUMS_HEADER): $(CANVAS_ENUMS_HEADER_STAMP)
	@true
$(CANVAS_ENUMS_HEADER_STAMP): $(COMMON_CANVAS_HEADERFILES)
	(cd $(COMMONCANVASSRCDIR) && $(GLIB_MKENUMS) \
		--fhead "#ifndef HIPPO_CANVAS_TYPE_BUILTINS_H\n" \
		--fhead "#define HIPPO_CANVAS_TYPE_BUILTINS_H 1\n\n" \
		--fhead "#include <glib-object.h>\n\n" \
		--fhead "G_BEGIN_DECLS\n\n" \
		--ftail "G_END_DECLS\n\n" \
		--ftail "#endif /* HIPPO_CANVAS_TYPE_BUILTINS_H */\n" \
		--fprod "\n/* --- @filename@ --- */" \
		--eprod "#define HIPPO_TYPE_@ENUMSHORT@ @enum_name@_get_type()\n" \
		--eprod "GType @enum_name@_get_type (void);\n" \
		$(^F:%=hippo/%)) > xgen-$(@F) \
	&& ( cmp -s xgen-$(@F) $(CANVAS_ENUMS_HEADER) || cp xgen-$(@F) $(CANVAS_ENUMS_HEADER)) \
	&& rm -f xgen-$(@F) \
	&& echo timestamp > $(CANVAS_ENUMS_HEADER_STAMP)

STAMPS =				\
	$(CANVAS_ENUMS_BODY_STAMP)	\
	$(CANVAS_ENUMS_HEADER_STAMP)

COMMON_CANVAS_HEADERFILES = 					\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-box.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-context.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-gradient.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-image.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-image-button.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-item.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-link.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-style.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-text.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-widgets.h	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-event.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-graphics.h

LINUX_CANVAS_HEADERFILES =					\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas.h		\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-window.h	\
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
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-style.c	\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-text.c		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-test.c		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-canvas-test.h		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-event.c		\
	$(COMMONCANVASSRCDIR)/hippo/hippo-graphics.c

LINUX_CANVAS_SOURCEFILES =					\
	$(LINUX_CANVAS_HEADERFILES)				\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas.c		\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-window.c	\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-helper.c	\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-helper.h	\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-window-child.c	\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-window-child.h	\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas.c		\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-widget.c	\
	$(LINUXCANVASSRCDIR)/hippo/hippo-canvas-widgets.c

CANVAS_BUILT_SOURCEFILES =			\
	$(CANVAS_MARSHAL_HEADER)		\
	$(CANVAS_MARSHAL_BODY)			\
	$(CANVAS_ENUMS_HEADER)			\
	$(CANVAS_ENUMS_BODY)

MAINTAINERCLEANFILES +=	$(CANVAS_BUILT_SOURCEFILES) $(STAMPS)
BUILT_SOURCES += $(CANVAS_BUILT_SOURCEFILES)

## we need to nodist these because otherwise automake would copy 
## ../common to distdir/../common which puts common outside of distdir.
## so we handle the disting manually so the destination is always distdir/common-dist

libhippocanvasincludedir = $(includedir)/hippo-canvas-1.0/hippo-canvas
nodist_libhippocanvasinclude_HEADERS=$(COMMON_CANVAS_HEADERFILES) $(LINUX_CANVAS_HEADERFILES)

nodist_libhippocanvas_1_la_SOURCES=$(COMMON_CANVAS_SOURCEFILES) $(LINUX_CANVAS_SOURCEFILES) $(CANVAS_BUILT_SOURCEFILES)
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

include Makefile-python.am
