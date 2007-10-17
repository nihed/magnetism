# note that target-specific CPPFLAGS will REPLACE
# not augment these default flags by default,
# so we manually put AM_CPPFLAGS in the target flags
AM_CPPFLAGS=-I$(top_builddir)/config
#ACLOCAL_AMFLAGS = -I m4

EXTRA_DIST=					\
	check-have-all-files.sh			\
	intltool-update.in			\
	intltool-extract.in			\
	intltool-merge.in

DISTCLEANFILES = \
	intltool-extract \
	intltool-merge \
	intltool-update

## Schema goo
schemadir = $(GCONF_SCHEMA_FILE_DIR)
schema_in_files = online-desktop.schemas.in
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

SUBDIRS = po

# if srcdir!=builddir, clean out maintainer-clean files from builddir
# this allows dist to pass.
distclean-local:
	if test $(srcdir) != . ; then		\
	  rm -f $(MAINTAINERCLEANFILES) ;	\
	fi

# These are added to using += in the included files
bin_SCRIPTS =
bin_PROGRAMS =
noinst_PROGRAMS =
noinst_LTLIBRARIES =
noinst_PYTHON = 
DISTCLEANFILES =
MAINTAINERCLEANFILES =
BUILT_SOURCES =

.PHONY: pyonlinedesktop-check-missing pyddm-check-missing
all-local: pyonlinedesktop-check-missing pyddm-check-missing
dist-hook: pyonlinedesktop-check-missing pyddm-check-missing

include Makefile-gmail.am
include Makefile-mailto.am
include Makefile-desktop.am
include Makefile-icons.am
include Makefile-pyonlinedesktop.am
include Makefile-pyddm.am
include Makefile-session.am
include Makefile-gconf.am
include Makefile-pidgin.am
include Makefile-weblogindriver.am
include Makefile-online-prefs-sync.am

