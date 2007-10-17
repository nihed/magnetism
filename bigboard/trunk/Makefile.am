include $(top_srcdir)/Makefile-include.am

SUBDIRS = applet data bigboard scripts po

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

# Included gdata, atom modules
externalsdir = $(pyexecdir)/bigboard_externals
atomdir = $(externalsdir)/atom
atom_PYTHON = atom/__init__.py atom/service.py 
gdatadir = $(externalsdir)/gdata
gdata_PYTHON = gdata/__init__.py gdata/service.py
gdatabasedir = $(gdatadir)/base
gdatabase_PYTHON = gdata/base/__init__.py gdata/base/service.py
gdatacalendardir = $(gdatadir)/calendar
gdatacalendar_PYTHON = gdata/calendar/__init__.py gdata/calendar/service.py
gdatadocsdir = $(gdatadir)/docs/
gdatadocs_PYTHON = gdata/docs/__init__.py gdata/docs/service.py

