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

# linking this to libhippo.la is just to get the marshalers, it's kind of lame
# but it all ends up in one big static link in the end anyhow
libgtkcopy_la_LIBADD = 			\
	$(LIBGTKCOPY_LIBS) libhippo.la

# the "config" dir seems like a convenient place that's already 
# in everyone's include path ... I dunno
BUILT_SRC_DIR=$(top_builddir)/config/hippo

EMBEDDEDIMAGEDIR=images
EMBEDDED_IMAGES=				\
	$(EMBEDDEDIMAGEDIR)/obubcnr_blmix.png	\
	$(EMBEDDEDIMAGEDIR)/obubcnr_bl.png	\
	$(EMBEDDEDIMAGEDIR)/obubcnr_brmix.png	\
	$(EMBEDDEDIMAGEDIR)/obubcnr_br.png	\
	$(EMBEDDEDIMAGEDIR)/obubcnr_tlmix.png	\
	$(EMBEDDEDIMAGEDIR)/obubcnr_tl.png	\
	$(EMBEDDEDIMAGEDIR)/obubcnr_tr.png	\
	$(EMBEDDEDIMAGEDIR)/obubedge_bmix.png	\
	$(EMBEDDEDIMAGEDIR)/obubedge_b.png	\
	$(EMBEDDEDIMAGEDIR)/obubedge_lmix.png	\
	$(EMBEDDEDIMAGEDIR)/obubedge_l.png	\
	$(EMBEDDEDIMAGEDIR)/obubedge_r.png	\
	$(EMBEDDEDIMAGEDIR)/obubedge_t.png

EMBEDDED_IMAGE_HEADER=$(BUILT_SRC_DIR)/hippo-embedded-image-data.h
$(EMBEDDED_IMAGE_HEADER):	$(EMBEDDED_IMAGES)
	cat </dev/null > $(EMBEDDED_IMAGE_HEADER) || exit 1 ;				\
	ALL_NAMES="" ;									\
	for I in $(EMBEDDED_IMAGES); do							\
	    IMG=$(srcdir)/$$I ;								\
	    NAME=`echo "$$I" | sed -e 's/.png//g' | sed -e 's/images\///g'` ;		\
	    echo "Writing embedded image $$IMG with name $$NAME" ;			\
	    gdk-pixbuf-csource --struct --raw --static "$$IMG"				\
                                --name="$$NAME"						\
				>> $(EMBEDDED_IMAGE_HEADER) || exit 1;			\
	    ALL_NAMES="$$ALL_NAMES $$NAME" ;						\
        done ;										\
        echo 'struct { const char *name; '  >> $(EMBEDDED_IMAGE_HEADER) ;		\
        echo 'GdkPixbuf *pixbuf; '  >> $(EMBEDDED_IMAGE_HEADER) ;			\
	echo 'const GdkPixdata *pixdata; } catalog[] = {' >> $(EMBEDDED_IMAGE_HEADER) ;	\
	for I in $$ALL_NAMES ; do							\
            echo "{ \"$$I\", NULL, &$$I }," >> $(EMBEDDED_IMAGE_HEADER) ;		\
	done ;										\
	echo '{ NULL, NULL, NULL } };' >> $(EMBEDDED_IMAGE_HEADER) || exit 1

MARSHAL_DIR=$(BUILT_SRC_DIR)
MARSHAL_LIST=$(COMMONSRCDIR)/hippo/hippo-marshal.list
MARSHAL_HEADER=$(MARSHAL_DIR)/hippo-marshal.h
MARSHAL_BODY=$(MARSHAL_DIR)/hippo-marshal.c

$(MARSHAL_HEADER): $(MARSHAL_LIST)
	mkdir $(MARSHAL_DIR) || true
	$(GLIB_GENMARSHAL) --prefix=hippo_marshal $(MARSHAL_LIST) --header > $(MARSHAL_HEADER)

$(MARSHAL_BODY): $(MARSHAL_LIST)
	mkdir $(MARSHAL_DIR) || true
	(echo "#include \"hippo-marshal.h\""; $(GLIB_GENMARSHAL) --prefix=hippo_marshal $(MARSHAL_LIST) --body) > $(MARSHAL_BODY)


BUILT_SOURCES=					\
	$(MARSHAL_HEADER)			\
	$(MARSHAL_BODY)				\
	$(EMBEDDED_IMAGE_HEADER)

mugshot_SOURCES	=				\
	src/hippo-bubble.c			\
	src/hippo-bubble.h			\
	src/hippo-chat-window.c		    	\
	src/hippo-chat-window.h		    	\
	src/hippo-cookies-linux.c		\
	src/hippo-cookies-linux.h		\
	src/hippo-embedded-image.c		\
	src/hippo-embedded-image.h		\
	src/hippo-http.c			\
	src/hippo-http.h			\
	src/hippo-image-cache.c			\
	src/hippo-image-cache.h			\
	src/hippo-person-renderer.c		\
	src/hippo-person-renderer.h		\
	src/hippo-platform-impl.c		\
	src/hippo-platform-impl.h		\
	src/hippo-status-icon.c			\
	src/hippo-status-icon.h			\
	src/main.c				\
	src/main.h

LIBHIPPO_BUILT_SOURCEFILES =					\
	$(MARSHAL_HEADER)					\
	$(MARSHAL_BODY)

LIBHIPPO_SOURCEFILES =						\
	$(COMMONSRCDIR)/hippo/hippo-basics.c			\
	$(COMMONSRCDIR)/hippo/hippo-basics.h			\
	$(COMMONSRCDIR)/hippo/hippo-chat-room.c			\
	$(COMMONSRCDIR)/hippo/hippo-chat-room.h			\
	$(COMMONSRCDIR)/hippo/hippo-common.h			\
        $(COMMONSRCDIR)/hippo/hippo-common-internal.h           \
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
nodist_libhippo_la_SOURCES=$(LIBHIPPO_SOURCEFILES) $(LIBHIPPO_BUILT_SOURCEFILES)
dist-hook:
	mkdir $(distdir)/common-dist
	mkdir $(distdir)/common-dist/hippo
	cp $(COMMONSRCDIR)/hippo/hippo-marshal.list $(distdir)/common-dist/hippo
	cp $(LIBHIPPO_SOURCEFILES) $(distdir)/common-dist/hippo

libgtkcopy_la_SOURCES=				\
	gtk/gtksimulated.h			\
	gtk/gtkstatusicon.c			\
	gtk/gtkstatusicon.h			\
	gtk/gtktrayicon.h			\
	gtk/gtktrayicon-x11.c


MAINTAINERCLEANFILES=$(LIBHIPPO_BUILT_SOURCEFILES)

# if srcdir!=builddir, clean out maintainer-clean files from builddir
# this allows dist to pass.
distclean-local:
	if test $(srcdir) != . ; then		\
	  rm -f $(MAINTAINERCLEANFILES) ;	\
	  rmdir $(MARSHAL_DIR) ;		\
	fi

SMALLICONSUBDIR=icons/hicolor/22x22/apps
smallicondir=$(datadir)/$(SMALLICONSUBDIR)
smallicon_DATA=					\
	$(SMALLICONSUBDIR)/mugshot_swarm_1.png	\
	$(SMALLICONSUBDIR)/mugshot_swarm_2.png	\
	$(SMALLICONSUBDIR)/mugshot_swarm_3.png	\
	$(SMALLICONSUBDIR)/mugshot_swarm_4.png	\
	$(SMALLICONSUBDIR)/mugshot_swarm_5.png

SMALLERICONSUBDIR=icons/hicolor/16x16/apps
smallericondir=$(datadir)/$(SMALLERICONSUBDIR)
smallericon_DATA=					\
	$(SMALLERICONSUBDIR)/mugshot_note_on.png	\
	$(SMALLERICONSUBDIR)/mugshot_note_off.png

EXTRA_DIST= \
	$(smallicon_DATA) $(smallericon_DATA) $(EMBEDDED_IMAGES)
