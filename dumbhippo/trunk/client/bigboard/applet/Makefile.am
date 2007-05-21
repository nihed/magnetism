serverdir = $(libdir)/bonobo/servers
server_in_files = BigBoard_Applet.server.in
server_DATA = $(server_in_files:.server.in=.server)

@INTLTOOL_SERVER_RULE@

$(server_in_files): $(server_in_files:.server.in=.server.in.in)
	sed -e "s|\@LIBEXECDIR\@|$(libexecdir)|" $< > $@

DISTCLEANFILES = $(server_in_files)

EXTRA_DIST = $(server_in_files:.server.in=.server.in.in) 
