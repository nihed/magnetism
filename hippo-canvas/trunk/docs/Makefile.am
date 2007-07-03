### Begin cut-and-paste from the toplevel Makefile.am (this is all screwy because 
### the gtk-docs.make is not intended to work with our nonrecursive build setup)

COMMONSRCDIR=$(top_srcdir)/common
LINUXSRCDIR=$(top_srcdir)/linux

AM_CPPFLAGS=-DHIPPO_COMPILATION -DHIPPO_OS_LINUX -I$(top_builddir)/config -I$(COMMONSRCDIR) -I$(LINUXSRCDIR)

### End cut-and-paste

DOC_MODULE=hippo-canvas

DOC_MAIN_SGML_FILE=hippo-canvas.xml

SCAN_OPTIONS=

# The directory containing the source code. Relative to $(srcdir)
DOC_SOURCE_DIR=../common/hippo

HFILE_GLOB=
CFILE_GLOB=

IGNORE_HFILES=

INCLUDES= 			 \
	$(AM_CPPFLAGS) 		 \
        $(LIBHIPPOCANVAS_CFLAGS)

GTKDOC_LIBS=$(top_builddir)/libhippocanvas-1.la

MKDB_OPTIONS=--sgml-mode --outputformat=xml

content_files=

HTML_IMAGES=

FIXXREF_OPTIONS=

include ../gtk-doc.make
