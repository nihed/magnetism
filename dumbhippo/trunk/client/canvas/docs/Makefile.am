### Begin cut-and-paste from the toplevel Makefile.am (this is all screwy because 
### the gtk-docs.make is not intended to work with our nonrecursive build setup)

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

AM_CPPFLAGS=-DHIPPO_COMPILATION -DHIPPO_OS_LINUX -I$(top_builddir)/config -I$(COMMONSRCDIR)/canvas -I$(LINUXSRCDIR)/canvas

### End cut-and-paste

DOC_MODULE=hippo-canvas

DOC_MAIN_SGML_FILE=hippo-canvas.xml

SCAN_OPTIONS=

DOC_SOURCE_DIR=../../common/canvas/hippo

HFILE_GLOB=
CFILE_GLOB=

IGNORE_HFILES=

INCLUDES= 			 \
	$(AM_CPPFLAGS) 		 \
        $(LIBHIPPOCANVAS_CFLAGS)

GTKDOC_LIBS=../libhippocanvas-1.la

MKDB_OPTIONS=--sgml-mode --outputformat=xml

content_files=

HTML_IMAGES=

FIXXREF_OPTIONS=

include ../gtk-doc.make
