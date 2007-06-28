include $(top_srcdir)/Makefile-include.am

SUBDIRS = applet bigboard scripts po

mainscript_PYTHON = main.py

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
