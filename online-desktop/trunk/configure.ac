dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(apps/gmail/gmail.desktop.in)
AC_CONFIG_AUX_DIR(config)

AM_INIT_AUTOMAKE(online-desktop, 0.2.5)

AM_CONFIG_HEADER(config/config.h)

# Honor aclocal flags
ACLOCAL="$ACLOCAL $ACLOCAL_FLAGS"

GETTEXT_PACKAGE=online-desktop
AC_SUBST(GETTEXT_PACKAGE)
AC_DEFINE_UNQUOTED(GETTEXT_PACKAGE,"$GETTEXT_PACKAGE",[The name of the gettext domain])

 ## must come before we use the $USE_MAINTAINER_MODE variable later
AM_MAINTAINER_MODE

## don't rerun to this point if we abort
AC_CACHE_SAVE

## used for .desktop file in theory, disabled until we have actual translations
## IT_PROG_INTLTOOL([0.34.90])

AM_PATH_PYTHON

#### define absolute path to srcdir for debugging-only code
ABSOLUTE_TOP_SRCDIR=`cd ${srcdir} && pwd`
AC_DEFINE_UNQUOTED(ABSOLUTE_TOP_SRCDIR, "$ABSOLUTE_TOP_SRCDIR", [full path to srcdir])

AC_OUTPUT([
Makefile
])

dnl ==========================================================================
echo "

        Online Desktop $VERSION
	==================

        prefix:                   ${prefix}

        Now type 'make' to build $PACKAGE
"
