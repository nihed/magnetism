COMMONSRCDIR=$(top_srcdir)/common
LINUXSRCDIR=$(top_srcdir)/linux

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
	$(LIBHIPPOCANVAS_CFLAGS)

libhippocanvas_1_la_LIBADD = 		\
	$(LIBHIPPOCANVAS_LIBS)

CANVAS_MARSHAL_LIST=$(COMMONSRCDIR)/hippo/hippo-canvas-marshal.list
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
	(cd $(COMMONSRCDIR) && $(GLIB_MKENUMS) \
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
	(cd $(COMMONSRCDIR) && $(GLIB_MKENUMS) \
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
	$(COMMONSRCDIR)/hippo/hippo-canvas-box.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-container.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-context.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-gradient.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-image.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-image-button.h	\
	$(COMMONSRCDIR)/hippo/hippo-canvas-item.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-link.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-style.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-text.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-widgets.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-util.h		\
	$(COMMONSRCDIR)/hippo/hippo-event.h			\
	$(COMMONSRCDIR)/hippo/hippo-graphics.h

LINUX_CANVAS_HEADERFILES =					\
	$(LINUXSRCDIR)/hippo/hippo-canvas.h			\
	$(LINUXSRCDIR)/hippo/hippo-canvas-window.h		\
	$(LINUXSRCDIR)/hippo/hippo-canvas-widget.h

COMMON_CANVAS_SOURCEFILES = 					\
	$(COMMON_CANVAS_HEADERFILES)				\
	$(COMMONSRCDIR)/hippo/hippo-canvas-internal.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-box.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-container.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-context.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-gradient.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-image.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-image-button.c	\
	$(COMMONSRCDIR)/hippo/hippo-canvas-item.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-link.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-style.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-text.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-test.c		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-test.h		\
	$(COMMONSRCDIR)/hippo/hippo-canvas-util.c		\
	$(COMMONSRCDIR)/hippo/hippo-event.c			\
	$(COMMONSRCDIR)/hippo/hippo-graphics.c

LINUX_CANVAS_SOURCEFILES =					\
	$(LINUX_CANVAS_HEADERFILES)				\
	$(LINUXSRCDIR)/hippo/hippo-canvas.c			\
	$(LINUXSRCDIR)/hippo/hippo-canvas-window.c		\
	$(LINUXSRCDIR)/hippo/hippo-canvas-helper.c		\
	$(LINUXSRCDIR)/hippo/hippo-canvas-helper.h		\
	$(LINUXSRCDIR)/hippo/hippo-canvas-window-child.c	\
	$(LINUXSRCDIR)/hippo/hippo-canvas-window-child.h	\
	$(LINUXSRCDIR)/hippo/hippo-canvas-widget.c		\
	$(LINUXSRCDIR)/hippo/hippo-canvas-widgets.c

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

libhippocanvasincludedir = $(includedir)/hippo-canvas-1.0/hippo
nodist_libhippocanvasinclude_HEADERS=$(COMMON_CANVAS_HEADERFILES) $(LINUX_CANVAS_HEADERFILES)

nodist_libhippocanvas_1_la_SOURCES=$(COMMON_CANVAS_SOURCEFILES) $(LINUX_CANVAS_SOURCEFILES) $(CANVAS_BUILT_SOURCEFILES)
dist-hook:
	-mkdir $(distdir)/common-dist
	mkdir $(distdir)/common-dist/canvas
	mkdir $(distdir)/common-dist/canvas/hippo
	mkdir $(distdir)/linux-dist
	mkdir $(distdir)/linux-dist/canvas
	mkdir $(distdir)/linux-dist/canvas/hippo
	cp $(COMMONSRCDIR)/hippo/hippo-canvas-marshal.list $(distdir)/common-dist/canvas/hippo
	cp $(COMMON_CANVAS_SOURCEFILES) $(distdir)/common-dist/canvas/hippo
	cp $(LINUX_CANVAS_SOURCEFILES) $(distdir)/linux-dist/canvas/hippo

include Makefile-python.am
