## this is a teenie bit weird; COMMONSRCDIR can be either ../common (build from CVS)
## or common-dist in the current dir (build from tarball). We rename it to common-dist
## when including it to be sure we don't mix the two up.
if COMMON_OUTSIDE_TREE
COMMONSRCDIR=$(top_srcdir)/../common
else
COMMONSRCDIR=$(top_srcdir)/common-dist
endif

noinst_LTLIBRARIES = libhippo.la libgtkcopy.la
bin_PROGRAMS = mugshot

# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-DHIPPO_COMPILATION -I$(top_builddir)/config -I$(COMMONSRCDIR)

# -Itop_srcdir gets our cut-and-pasted gtk
mugshot_CPPFLAGS = 			\
	$(AM_CPPFLAGS)			\
	$(top_srcdir)			\
	$(MUGSHOT_CFLAGS)

libhippo_la_CPPFLAGS =			\
	$(AM_CPPFLAGS)			\
	$(LIBHIPPO_CFLAGS)

libgtkcopy_la_CPPFLAGS =		\
	$(AM_CPPFLAGS)			\
	$(LIBGTKCOPY_CFLAGS)

mugshot_LDFLAGS=$(MUGSHOT_LIBS)
mugshot_LDADD= libhippo.la libgtkcopy.la

libhippo_la_LIBADD = 			\
	$(LIBHIPPO_LIBS)

# none of this actually matters since it's just a convenience library, keeping it 
# for future reference
libhippo_la_LDFLAGS= -export-symbols-regex "^[^_].*" -version-info $(LT_CURRENT):$(LT_REVISION):$(LT_AGE) -no-undefined

libgtkcopy_la_LIBADD = 			\
	$(LIBGTKCOPY_LIBS)

mugshot_SOURCES	=				\
	src/hippo-cookies-linux.c		\
	src/hippo-cookies-linux.h		\
	src/hippo-platform-impl.c		\
	src/hippo-platform-impl.h		\
	src/hippo-status-icon.c			\
	src/hippo-status-icon.h			\
	src/main.c				\
	src/main.h

LIBHIPPO_SOURCEFILES =						\
	$(COMMONSRCDIR)/hippo/hippo-basics.c			\
	$(COMMONSRCDIR)/hippo/hippo-basics.h			\
	$(COMMONSRCDIR)/hippo/hippo-chat-room.c			\
	$(COMMONSRCDIR)/hippo/hippo-chat-room.h			\
	$(COMMONSRCDIR)/hippo/hippo-common.h			\
	$(COMMONSRCDIR)/hippo/hippo-connection.c		\
	$(COMMONSRCDIR)/hippo/hippo-connection.h		\
	$(COMMONSRCDIR)/hippo/hippo-cookies.c			\
	$(COMMONSRCDIR)/hippo/hippo-cookies.h			\
	$(COMMONSRCDIR)/hippo/hippo-data-cache.c		\
	$(COMMONSRCDIR)/hippo/hippo-data-cache.h		\
	$(COMMONSRCDIR)/hippo/hippo-data-cache-internal.h	\
	$(COMMONSRCDIR)/hippo/hippo-entity.c			\
	$(COMMONSRCDIR)/hippo/hippo-entity.h			\
	$(COMMONSRCDIR)/hippo/hippo-entity-protected.h		\
	$(COMMONSRCDIR)/hippo/hippo-myspace.c			\
	$(COMMONSRCDIR)/hippo/hippo-myspace.h			\
	$(COMMONSRCDIR)/hippo/hippo-person.c			\
	$(COMMONSRCDIR)/hippo/hippo-person.h			\
	$(COMMONSRCDIR)/hippo/hippo-platform.c			\
	$(COMMONSRCDIR)/hippo/hippo-platform.h			\
	$(COMMONSRCDIR)/hippo/hippo-post.c			\
	$(COMMONSRCDIR)/hippo/hippo-post.h

## we need to nodist these because otherwise automake would copy 
## ../common to distdir/../common which puts common outside of distdir.
## so we handle the disting manually so the destination is always distdir/common-dist
nodist_libhippo_la_SOURCES=$(LIBHIPPO_SOURCEFILES)
dist-hook:
	mkdir $(distdir)/common-dist
	mkdir $(distdir)/common-dist/hippo
	cp $(LIBHIPPO_SOURCEFILES) $(distdir)/common-dist/hippo

libgtkcopy_la_SOURCES=				\
	gtk/gtksimulated.h			\
	gtk/gtkstatusicon.c			\
	gtk/gtkstatusicon.h			\
	gtk/gtktrayicon.h			\
	gtk/gtktrayicon-x11.c
