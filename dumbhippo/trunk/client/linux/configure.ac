dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(src/hippo-platform-impl.h)
AC_CONFIG_AUX_DIR(config)

AC_CANONICAL_TARGET

AM_INIT_AUTOMAKE(mugshot, 1.1.6)

AM_CONFIG_HEADER(config/config.h)

# Honor aclocal flags
ACLOCAL="$ACLOCAL $ACLOCAL_FLAGS"

GETTEXT_PACKAGE=mugshot
AC_SUBST(GETTEXT_PACKAGE)
AC_DEFINE_UNQUOTED(GETTEXT_PACKAGE,"$GETTEXT_PACKAGE",[The name of the gettext domain])

 ## must come before we use the $USE_MAINTAINER_MODE variable later
AM_MAINTAINER_MODE

## don't rerun to this point if we abort
AC_CACHE_SAVE

# libtool versioning
#
# See http://sources.redhat.com/autobook/autobook/autobook_91.html#SEC91 for details
#

## increment if the interface has additions, changes, removals.
LT_CURRENT=0

## increment any time the source changes; set to
##  0 if you increment CURRENT
LT_REVISION=0

## increment if any interfaces have been added; set to 0
## if any interfaces have been changed or removed. removal has
## precedence over adding, so set to 0 if both happened.
LT_AGE=0

AC_SUBST(LT_CURRENT)
AC_SUBST(LT_REVISION)
AC_SUBST(LT_AGE)


AC_PROG_CC
AC_PROG_CXX
AC_PROG_LIBTOOL
AC_ISC_POSIX
AC_HEADER_STDC

## don't rerun to this point if we abort
AC_CACHE_SAVE

## example of how to do an arg
## AC_ARG_ENABLE(qt,               [  --enable-qt           enable Qt-friendly client library],enable_qt=$enableval,enable_qt=auto)

#### gcc warning flags

if test "x$GCC" = "xyes"; then
  changequote(,)dnl
  case " $CFLAGS " in
  *[\ \	]-Wall[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wall" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wchar-subscripts[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wchar-subscripts" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wdeclaration-after-statement[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wdeclaration-after-statement" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wmissing-declarations[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wmissing-declarations" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wmissing-prototypes[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wmissing-prototypes" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wnested-externs[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wnested-externs" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wpointer-arith[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wpointer-arith" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wcast-align[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wcast-align" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wfloat-equal[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wfloat-equal" ;;
  esac

  case " $CFLAGS " in
  *[\ \	]-Wsign-compare[\ \	]*) ;;
  *) CFLAGS="$CFLAGS -Wsign-compare" ;;
  esac

  if test "x$enable_ansi" = "xyes"; then
    case " $CFLAGS " in
    *[\ \	]-ansi[\ \	]*) ;;
    *) CFLAGS="$CFLAGS -ansi" ;;
    esac

    case " $CFLAGS " in
    *[\ \	]-D_POSIX_C_SOURCE*) ;;
    *) CFLAGS="$CFLAGS -D_POSIX_C_SOURCE=199309L" ;;
    esac

    case " $CFLAGS " in
    *[\ \	]-D_BSD_SOURCE[\ \	]*) ;;
    *) CFLAGS="$CFLAGS -D_BSD_SOURCE" ;;
    esac

    case " $CFLAGS " in
    *[\ \	]-pedantic[\ \	]*) ;;
    *) CFLAGS="$CFLAGS -pedantic" ;;
    esac
  fi
  if test x$enable_gcov = xyes; then
    case " $CFLAGS " in
    *[\ \	]-fprofile-arcs[\ \	]*) ;;
    *) CFLAGS="$CFLAGS -fprofile-arcs" ;;
    esac
    case " $CFLAGS " in
    *[\ \	]-ftest-coverage[\ \	]*) ;;
    *) CFLAGS="$CFLAGS -ftest-coverage" ;;
    esac

    ## remove optimization
    CFLAGS=`echo "$CFLAGS" | sed -e 's/-O[0-9]*//g'`
  fi
  changequote([,])dnl
else
  if test x$enable_gcov = xyes; then
    AC_MSG_ERROR([--enable-gcov can only be used with gcc])
  fi
fi

AC_SUBST(CFLAGS)
AC_SUBST(LDFLAGS)

## we prefer to find common in ../common when building from 
## subversion, but the distribution has it in top_srcdir
AC_MSG_CHECKING(For "$srcdir/../common" or "$srcdir/common-dist")
if test -d "$srcdir"/common ; then
        AC_MSG_RESULT(found in wrong place)
        AC_MSG_ERROR([The "common" directory should not be here; it should be a sibling of srcdir or be called common-dist and a subdir of it])
elif test -d "$srcdir"/common-dist ; then
        common_outside_tree=false
        AC_MSG_RESULT(found common-dist inside tree)
elif test -d "$srcdir"/../common ; then
        common_outside_tree=true
        AC_MSG_RESULT(found outside tree)
else
        AC_MSG_RESULT(not found)
        AC_MSG_ERROR([The "common" or "common-dist" directory wasn't found])
fi
AM_CONDITIONAL(COMMON_OUTSIDE_TREE, $common_outside_tree)

GLIB2_REQUIRED=2.6.0
GTK2_REQUIRED=2.6.0
LOUDMOUTH_REQUIRED=1.0
# unfortunately this breaks us on FC4
DBUS_REQUIRED=0.61
DBUS_GLIB_REQUIRED=0.61
# earlier might work, but this is what's been tested
CURL_REQUIRED=7.13.1

## We don't just use PKG_CHECK_MODULES because libcurl.pc has all kinds of crap in it like openssl and kerberos
## on the link line... we don't want to link to all that stuff. So just do -lcurl
## For now people doing static linking or putting libcurl in a funny prefix will have to h4x0r the following
## lines (try just "PKG_CHECK_MODULES(CURL, libcurl >= $CURL_REQUIRED)" if you don't care about linking to 
## all the extra goo)
## Also, older curl has curl-config instead of a .pc file... mumble grumble
CURL_LIBS=-lcurl
CURL_CFLAGS=
PKG_CHECK_EXISTS(libcurl >= $CURL_REQUIRED, have_curl=true, have_curl=false)
warn_about_curl_version=false
if $have_curl ; then
    AC_MSG_NOTICE([Found libcurl, using link flags $CURL_LIBS])
else
    AC_MSG_WARN([no pkg-config enabled libcurl found; checking for old-timey version of curl])

    AC_CHECK_HEADERS([curl/curl.h])
    AC_CHECK_HEADERS([curl/easy.h], [have_curl_headers=true], [have_curl_headers=false],
        [#if HAVE_CURL_CURL_H
         #include <curl/curl.h>
         #endif
        ])
    if $have_curl_headers ; then
        AC_CHECK_LIB(curl, curl_easy_perform, have_curl=true, have_curl=false)
    else
        have_curl=false
    fi
    if $have_curl ; then
        warn_about_curl_version=true
        AC_MSG_WARN([libcurl seems to exist, but we don't know what version it is; if you get weird problems then try upgrading...])
    else
        AC_MSG_ERROR([no libcurl found on system, on some systems you could solve this by installing the curl-devel package])
    fi
fi

PKG_CHECK_MODULES(LIBHIPPO, gobject-2.0 >= $GLIB2_REQUIRED gthread-2.0 loudmouth-1.0 >= $LOUDMOUTH_REQUIRED)
PKG_CHECK_MODULES(LIBGTKCOPY, gtk+-2.0 >= $GTK2_REQUIRED gtk+-x11-2.0)
PKG_CHECK_MODULES(MUGSHOT_BASE, gtk+-2.0 >= $GTK2_REQUIRED gthread-2.0 loudmouth-1.0 >= $LOUDMOUTH_REQUIRED dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_GLIB_REQUIRED)
MUGSHOT_LIBS="$MUGSHOT_BASE_LIBS $CURL_LIBS"
MUGSHOT_CFLAGS="$MUGSHOT_BASE_CFLAGS $CURL_CFLAGS"
AC_SUBST(MUGSHOT_LIBS)
AC_SUBST(MUGSHOT_CFLAGS)
PKG_CHECK_MODULES(MUGSHOT_URI_HANDLER, gtk+-2.0 >= $GTK2_REQUIRED gthread-2.0 dbus-1 >= $DBUS_REQUIRED)

GLIB_GENMARSHAL=`$PKG_CONFIG --variable=glib_genmarshal glib-2.0`
AC_SUBST(GLIB_GENMARSHAL)

PKG_CHECK_EXISTS(gtk+-2.0 < 2.9.0, have_gtk210=false, have_gtk210=true)

## used for .desktop file in theory, disabled until we have actual translations
## IT_PROG_INTLTOOL([0.34.90])

## we just need these checks to get the gconf schemas stuff
AC_PATH_PROG(GCONFTOOL, gconftool-2, no)
if test x"$GCONFTOOL" = xno; then
    AC_MSG_ERROR([gconftool-2 executable not found in your path - should be installed with GConf])
fi

AM_GCONF_SOURCE_2


#### define absolute path to srcdir for debugging-only code
ABSOLUTE_TOP_SRCDIR=`cd ${srcdir} && pwd`
AC_DEFINE_UNQUOTED(ABSOLUTE_TOP_SRCDIR, "$ABSOLUTE_TOP_SRCDIR", [full path to srcdir])

AC_OUTPUT([
Makefile
version
])

dnl ==========================================================================
echo "

        Mugshot $VERSION
	==================

        prefix:                   ${prefix}
        compiler:                 ${CC}
        LIBHIPPO_CFLAGS:          ${LIBHIPPO_CFLAGS}
        MUGSHOT_CFLAGS:           ${MUGSHOT_CFLAGS}
        LIBHIPPO_LIBS:            ${LIBHIPPO_LIBS}
        MUGSHOT_LIBS:             ${MUGSHOT_LIBS}

        Now type 'make' to build $PACKAGE
"

if "$warn_about_curl_version" ; then
    echo "
        ***** libcurl was found but we couldn't tell what version it is.
        ***** we've tested with versions $CURL_REQUIRED and above.
        ***** If you have an older version, the build might fail or 
        ***** there may be bugs at runtime, or, it might work - we don't know!
    "
fi

if "$have_gtk210" ; then
    echo "
        ***** GTK+ 2.9 or above may conflict with cut-and-pasted GTK+ 2.9
        ***** code in the gtk/ subdirectory. 
        ***** We tried to avoid the problem but haven't tested it yet.
        ***** Try it and let us know.
    "
fi
