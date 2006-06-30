## this is a teenie bit weird; COMMONSRCDIR can be either ../common (build from CVS)
## or common-dist in the current dir (build from tarball). We rename it to common-dist
## when including it to be sure we don't mix the two up.
if COMMON_OUTSIDE_TREE
COMMONSRCDIR=$(top_srcdir)/../common
else
COMMONSRCDIR=$(top_srcdir)/common-dist
endif

noinst_LTLIBRARIES = libhippo.la libgtkcopy.la
bin_PROGRAMS = mugshot mugshot-uri-handler

# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-DHIPPO_COMPILATION -DVERSION_FILE=\"$(pkgdatadir)/version\" -I$(top_builddir)/config -I$(COMMONSRCDIR)

# -Itop_srcdir gets our cut-and-pasted gtk
mugshot_CPPFLAGS = 			\
	$(AM_CPPFLAGS)			\
	-I $(top_srcdir)		\
	$(MUGSHOT_CFLAGS)

mugshot_uri_handler_CPPFLAGS =		\
	$(AM_CPPFLAGS)			\
	$(MUGSHOT_URI_HANDLER_CFLAGS)

libhippo_la_CPPFLAGS =			\
	$(AM_CPPFLAGS)			\
	$(LIBHIPPO_CFLAGS)

libgtkcopy_la_CPPFLAGS =		\
	$(AM_CPPFLAGS)			\
	$(LIBGTKCOPY_CFLAGS)

mugshot_LDFLAGS=$(MUGSHOT_LIBS)
mugshot_LDADD= libhippo.la libgtkcopy.la

mugshot_uri_handler_LDFLAGS=$(MUGSHOT_URI_HANDLER_LIBS)
mugshot_uri_handler_LDADD= libhippo.la

libhippo_la_LIBADD = 			\
	$(LIBHIPPO_LIBS)

# none of this actually matters since it's just a convenience library, keeping it 
# for future reference
# libhippo_la_LDFLAGS= -export-symbols-regex "^[^_].*" -version-info $(LT_CURRENT):$(LT_REVISION):$(LT_AGE) -no-undefined

libgtkcopy_la_LIBADD = 			\
	$(LIBGTKCOPY_LIBS)

# the "config" dir seems like a convenient place that's already 
# in everyone's include path ... I dunno
BUILT_SRC_DIR=$(top_builddir)/config/hippo

EMBEDDEDIMAGEDIR=images
EMBEDDED_IMAGES=				\
	$(EMBEDDEDIMAGEDIR)/bublinkswarm.gif	\
	$(EMBEDDEDIMAGEDIR)/bubgroupupdate.gif	\
	$(EMBEDDEDIMAGEDIR)/blueleftarrow.gif	\
	$(EMBEDDEDIMAGEDIR)/bluerightarrow.gif	\
	$(EMBEDDEDIMAGEDIR)/chaticon.gif	\
	$(EMBEDDEDIMAGEDIR)/grayleftarrow.gif	\
	$(EMBEDDEDIMAGEDIR)/grayrightarrow.gif	\
	$(EMBEDDEDIMAGEDIR)/ignoreicon.png	\
	$(EMBEDDEDIMAGEDIR)/pbubcnr_blmix.png	\
	$(EMBEDDEDIMAGEDIR)/pbubcnr_bl.png	\
	$(EMBEDDEDIMAGEDIR)/pbubcnr_brmix.png	\
	$(EMBEDDEDIMAGEDIR)/pbubcnr_br.png	\
	$(EMBEDDEDIMAGEDIR)/pbubcnr_tlmix.png	\
	$(EMBEDDEDIMAGEDIR)/pbubcnr_tl.png	\
	$(EMBEDDEDIMAGEDIR)/pbubcnr_tr.png	\
	$(EMBEDDEDIMAGEDIR)/pbubedge_bmix.png	\
	$(EMBEDDEDIMAGEDIR)/pbubedge_b.png	\
	$(EMBEDDEDIMAGEDIR)/pbubedge_lmix.png	\
	$(EMBEDDEDIMAGEDIR)/pbubedge_l.png	\
	$(EMBEDDEDIMAGEDIR)/pbubedge_r.png	\
	$(EMBEDDEDIMAGEDIR)/pbubedge_t.png	\
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
	    NAME=`echo "$$I" | sed -e 's/.png//g' | sed -e 's/.gif//g'` ;		\
	    NAME=`echo "$$NAME" | sed -e 's/images\///g'` ;				\
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

## yeah, right now the same marshal.list is used both places ...
## we generate two marshaller files since there are two static libs
## and we don't want to link them to each other.
GTK_MARSHAL_LIST=$(MARSHAL_LIST)
GTK_MARSHAL_HEADER=$(MARSHAL_DIR)/hippo-gtk-marshal.h
GTK_MARSHAL_BODY=$(MARSHAL_DIR)/hippo-gtk-marshal.c

$(GTK_MARSHAL_HEADER): $(GTK_MARSHAL_LIST)
	mkdir $(MARSHAL_DIR) || true
	$(GLIB_GENMARSHAL) --prefix=hippo_gtk_marshal $(GTK_MARSHAL_LIST) --header > $(GTK_MARSHAL_HEADER)

$(GTK_MARSHAL_BODY): $(GTK_MARSHAL_LIST)
	mkdir $(MARSHAL_DIR) || true
	(echo "#include \"hippo-gtk-marshal.h\""; $(GLIB_GENMARSHAL) --prefix=hippo_gtk_marshal $(GTK_MARSHAL_LIST) --body) > $(GTK_MARSHAL_BODY)

BUILT_SOURCES=					\
	$(MARSHAL_HEADER)			\
	$(MARSHAL_BODY)				\
	$(GTK_MARSHAL_HEADER)			\
	$(GTK_MARSHAL_BODY)			\
	$(EMBEDDED_IMAGE_HEADER)

mugshot_SOURCES	=				\
	src/hippo-bubble.c			\
	src/hippo-bubble.h			\
	src/hippo-bubble-manager.c		\
	src/hippo-bubble-manager.h		\
	src/hippo-bubble-util.c			\
	src/hippo-bubble-util.h			\
	src/hippo-chat-window.c		    	\
	src/hippo-chat-window.h		    	\
	src/hippo-cookies-linux.c		\
	src/hippo-cookies-linux.h		\
	src/hippo-dbus-client.c			\
	src/hippo-dbus-client.h			\
	src/hippo-dbus-server.c			\
	src/hippo-dbus-server.h			\
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

mugshot_uri_handler_SOURCES=			\
	src/hippo-dbus-client.c			\
	src/hippo-dbus-client.h			\
	src/uri-handler.c

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

LIBGTKCOPY_BUILT_SOURCEFILES=			\
	$(GTK_MARSHAL_HEADER)			\
	$(GTK_MARSHAL_BODY)

libgtkcopy_la_SOURCES=				\
	gtk/hippo-gtk-renamings.h		\
	gtk/gtksimulated.h			\
	gtk/gtkstatusicon.c			\
	gtk/gtkstatusicon.h			\
	gtk/gtktrayicon.h			\
	gtk/gtktrayicon-x11.c			\
	$(LIBGTKCOPY_BUILT_SOURCEFILES)

HUGEICONSUBDIR=icons/hicolor/128x128/apps
hugeicondir=$(datadir)/$(HUGEICONSUBDIR)
hugeicon_DATA=				\
	$(HUGEICONSUBDIR)/mugshot.png

NORMALICONSUBDIR=icons/hicolor/48x48/apps
normalicondir=$(datadir)/$(NORMALICONSUBDIR)
normalicon_DATA=				\
	$(NORMALICONSUBDIR)/mugshot.gif

MEDIUMICONSUBDIR=icons/hicolor/32x32/apps
mediumicondir=$(datadir)/$(MEDIUMICONSUBDIR)
mediumicon_DATA=				\
	$(MEDIUMICONSUBDIR)/mugshot.gif

SMALLISHICONSUBDIR=icons/hicolor/24x24/apps
smallishicondir=$(datadir)/$(SMALLISHICONSUBDIR)
smallishicon_DATA=				\
	$(SMALLISHICONSUBDIR)/mugshot.gif


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
	$(SMALLERICONSUBDIR)/mugshot.gif		\
	$(SMALLERICONSUBDIR)/mugshot_note_on.png	\
	$(SMALLERICONSUBDIR)/mugshot_note_off.png

## we install a file containing our version, so the running app 
## can check its version vs. the installed app
versiondir=$(pkgdatadir)
version_DATA=version

desktopfilesdir=$(datadir)/gnome/autostart
desktopfiles_in_files=mugshot.desktop.in
desktopfiles_files=$(desktopfiles_in_files:.desktop.in=.desktop)
desktopfiles_DATA = $(desktopfiles_files)
# intltool not here until po exists and all that
#@INTLTOOL_DESKTOP_RULE@
mugshot.desktop: mugshot.desktop.in
	cat $(srcdir)/mugshot.desktop.in | sed -e 's/^_//g' > mugshot.desktop

## no point adding intltool stuff to these uri handler schemas, the
## docs would just be the generic gnome-vfs uri handler docs
schemadir   = @GCONF_SCHEMA_FILE_DIR@
schema_DATA = mugshot-uri-handler.schemas

if GCONF_SCHEMAS_INSTALL
install-data-local:
        GCONF_CONFIG_SOURCE=$(GCONF_SCHEMA_CONFIG_SOURCE) $(GCONFTOOL) --makefile-install-rule $(schema_DATA)
else
install-data-local:
endif

EXTRA_DIST=								\
	$(smallicon_DATA) $(smallericon_DATA) $(smallishicon_DATA)	\
	$(mediumicon_DATA) $(normalicon_DATA) $(hugeicon_DATA)		\
	$(EMBEDDED_IMAGES) version.in LICENSE				\
	$(schema_DATA) $(desktopfiles_in_files)

MAINTAINERCLEANFILES=							\
	$(LIBHIPPO_BUILT_SOURCEFILES) $(LIBGTKCOPY_BUILT_SOURCEFILES)	\
	$(EMBEDDED_IMAGE_HEADER) $(desktopfiles_files)

# if srcdir!=builddir, clean out maintainer-clean files from builddir
# this allows dist to pass.
distclean-local:
	if test $(srcdir) != . ; then		\
	  rm -f $(MAINTAINERCLEANFILES) ;	\
	  rmdir $(BUILT_SRC_DIR) ;		\
	fi
