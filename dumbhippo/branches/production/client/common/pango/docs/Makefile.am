## Process this file with automake to create Makefile.in.

# The name of the module.
DOC_MODULE=pango

# The top-level SGML file.
DOC_MAIN_SGML_FILE=pango-docs.sgml

# Extra options to supply to gtkdoc-scan
SCAN_OPTIONS=--deprecated-guards="PANGO_DISABLE_DEPRECATED"

# The directory containing the source code. Relative to $(srcdir)
DOC_SOURCE_DIR=../pango

# Used for dependencies
HFILE_GLOB=$(top_srcdir)/pango/*.h
CFILE_GLOB=$(top_srcdir)/pango/*.c

# Headers to ignore
IGNORE_HFILES=			\
	ftglue.h		\
	mini-fribidi		\
	opentype		\
	module-defs.h		\
	modules.h		\
	pangocairo-private.h	\
	pangocairo-fc.h		\
	pangocairo-win32.h	\
	pangocairo-atsui.h	\
	pango-color-table.h	\
	pango-engine-private.h	\
	pango-impl-utils.h	\
	pango-glyph-item-private.h	\
	pango-layout-private.h	\
	pango-script-table.h	\
	pangofc-private.h	\
	pangoft2-private.h	\
	pangowin32-private.h	\
	pangoatsui-private.h	\
	pangox-private.h	\
	pangoxft-private.h	\
	pango-ot-private.h

# CFLAGS and LDFLAGS for compiling scan program. Only needed
# if $(DOC_MODULE).types is non-empty.
INCLUDES =               	\
	-DPANGO_ENABLE_BACKEND	\
	-DPANGO_ENABLE_ENGINE	\
	-I$(top_srcdir)		\
	$(GLIB_CFLAGS)		\
	$(XFT_CFLAGS)		\
	$(CAIRO_CFLAGS)		\
	$(FREETYPE_CFLAGS)	\
	$(X_CFLAGS)

# libpangoxft.la pulls in libpango.la and libpangoft2.la;
# We're assuming here that we'll only regenerate the
# HTML docs on Unix. We don't get introspection on Win32
# specific types, but that isn't a bug deal.
GTKDOC_LIBS =						\
	$(top_builddir)/pango/libpangoxft-1.0.la

if HAVE_CAIRO
GTKDOC_LIBS += $(top_builddir)/pango/libpangocairo-1.0.la
endif

# Extra options to supply to gtkdoc-mkdb
MKDB_OPTIONS=--sgml-mode --output-format=xml


# Non-autogenerated SGML files to be included in $(DOC_MAIN_SGML_FILE)
content_files =				\
	pango_markup.sgml		\
	version.xml			\
	pango-querymodules.xml

# Images to copy into HTML directory
HTML_IMAGES =				\
	layout.gif			\
	rotated-text.png

# Extra options to supply to gtkdoc-fixref
FIXXREF_OPTIONS=

include $(top_srcdir)/gtk-doc.make

########################################################################

man_MANS = pango-querymodules.1 

if ENABLE_MAN

%.1 : %.xml 
	@XSLTPROC@ -nonet http://docbook.sourceforge.net/release/xsl/current/manpages/docbook.xsl $<

endif

EXTRA_DIST +=				\
	layout.fig			\
	layout.eps			\
	version.xml.in

BUILT_EXTRA_DIST = $(man_MANS)

dist-hook-local:
	files='$(BUILT_EXTRA_DIST)';				\
	for f in $$files; do					\
	  if test -f $$f; then d=.; else d=$(srcdir); fi;	\
	  cp $$d/$$f $(distdir) || exit 1; done;		\
								\
	mkdir $(distdir)/TEXT;					\
	for f in $(srcdir)/TEXT/* ; do				\
	  test -f $$f && cp -p $$f $(distdir)/TEXT;		\
	done
