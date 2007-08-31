serverdir = $(libdir)/bonobo/servers
server_in_files = GNOME_OnlineDesktop_BigBoardFactory.server.in
server_DATA = $(server_in_files:.server.in=.server)

uidir    = $(datadir)/gnome-2.0/ui
ui_DATA  = \
	GNOME_OnlineDesktop_BigBoardButtonApplet.xml

@INTLTOOL_SERVER_RULE@

CLEANFILES = $(server_DATA)

DISTCLEANFILES = $(server_in_files)

EXTRA_DIST = $(server_in_files:.server.in=.server.in.in) \
	$(ui_DATA)

$(server_in_files): $(server_in_files).in Makefile
	sed -e "s|\@LOCATION\@|$(APPLET_LOCATION)|" \
	    -e "s|\@TYPE\@|$(APPLET_TYPE)|" \
	    -e "s|\@VERSION\@|$(PACKAGE_VERSION)|" \
	    -e "s|\@LIBEXECDIR\@|$(libexecdir)|" \
	    $< > $@

BIGBOARD_APPLET_SOURCES=			\
	bigboard-button.c			\
	hippo-dbus-helper.c			\
	hippo-dbus-helper.h

BIGBOARD_APPLET_CPPFLAGS =					\
	-DGNOMELOCALEDIR=\""$(prefix)/$(DATADIRNAME)/locale"\"	\
	$(BIGBOARD_APPLET_CFLAGS) 

if APPLET_INPROCESS
APPLET_TYPE     = shlib
APPLET_LOCATION = $(pkglibdir)/libbigboard-applets.so

appletlibdir = $(pkglibdir)
appletlib_LTLIBRARIES = libbigboard-applets.la
libbigboard_applets_la_SOURCES = $(BIGBOARD_APPLET_SOURCES)
libbigboard_applets_la_LIBADD = $(BIGBOARD_APPLET_LIBS)
libbigboard_applets_la_LDFLAGS = -module -avoid-version
libbigboard_applets_la_CPPFLAGS = $(BIGBOARD_APPLET_CPPFLAGS) -DAPPLET_INPROCESS=1
else
APPLET_TYPE     = exe
APPLET_LOCATION = $(libexecdir)/bigboard-applets

appletdir = $(libexecdir)
applet_PROGRAMS = bigboard-applets
bigboard_applets_SOURCES = $(BIGBOARD_APPLET_SOURCES)
bigboard_applets_LDADD = $(BIGBOARD_APPLET_LIBS)
bigboard_applets_CPPFLAGS = $(BIGBOARD_APPLET_CPPFLAGS)
endif
