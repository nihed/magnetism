include $(top_srcdir)/Makefile-include.am

SUBDIRS = applet data bigboard scripts po mugshot-python

mainscript_PYTHON = main.py

MAINTAINERCLEANFILES =

DISTCLEANFILES = \
	intltool-extract \
	intltool-merge \
	intltool-update

EXTRA_DIST = \
	intltool-update.in \
	intltool-extract.in	\
	intltool-merge.in

## Schema goo
schemadir = $(GCONF_SCHEMA_FILE_DIR)
schema_in_files = bigboard.schemas.in
schema_DATA = $(schema_in_files:.schemas.in=.schemas)
@INTLTOOL_SCHEMAS_RULE@
if GCONF_SCHEMAS_INSTALL
install-data-local: install-schemas
install-schemas:
	GCONF_CONFIG_SOURCE=$(GCONF_SCHEMA_CONFIG_SOURCE) \
		$(GCONFTOOL) --makefile-install-rule $(schema_DATA)
endif
DISTCLEANFILES += $(schema_DATA)
EXTRA_DIST += $(schema_in_files)

## this globally runs the daemon for all users, which is not what we want long-term. 
## But, it won't (or shouldn't) do anything if you aren't running mugshot.
## we'll have to clean this up later.

autostartdir=$(datadir)/gnome/autostart
autostart_in_files=gconf-online-sync.desktop.in
autostart_files=$(autostart_in_files:.desktop.in=.desktop)
autostart_DATA = $(autostart_files)

# intltool not here until po exists and all that
#@INTLTOOL_DESKTOP_RULE@
gconf-online-sync.desktop: gconf-online-sync.desktop.in
	cat $(srcdir)/gconf-online-sync.desktop.in | sed -e 's/^_//g' > gconf-online-sync.desktop

EXTRA_DIST += $(autostart_in_files)
MAINTAINERCLEANFILES +=	$(autostart_files)
