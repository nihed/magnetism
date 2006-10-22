dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(src/hippo-platform-impl.h)
AC_CONFIG_AUX_DIR(config)

AC_CANONICAL_TARGET

AM_INIT_AUTOMAKE(mugshot, 1.1.19)

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

#### gcc warning flags

changequote(,)dnl
addCommonWarnings() {
  result="$@"

  case " $result " in
  *[\ \	]-Wall[\ \	]*) ;;
  *) result="$result -Wall" ;;
  esac

  case " $result " in
  *[\ \	]-Wchar-subscripts[\ \	]*) ;;
  *) result="$result -Wchar-subscripts" ;;
  esac

  case " $result " in
  *[\ \	]-Wpointer-arith[\ \	]*) ;;
  *) result="$result -Wpointer-arith" ;;
  esac

  case " $result " in
  *[\ \	]-Wcast-align[\ \	]*) ;;
  *) result="$result -Wcast-align" ;;
  esac

  case " $result " in
  *[\ \	]-Wfloat-equal[\ \	]*) ;;
  *) result="$result -Wfloat-equal" ;;
  esac

  case " $result " in
  *[\ \	]-Wsign-compare[\ \	]*) ;;
  *) result="$result -Wsign-compare" ;;
  esac

  if test "x$enable_ansi" = "xyes"; then
    case " $result " in
    *[\ \	]-ansi[\ \	]*) ;;
    *) result="$result -ansi" ;;
    esac

    case " $result " in
    *[\ \	]-D_POSIX_C_SOURCE*) ;;
    *) result="$result -D_POSIX_C_SOURCE=199309L" ;;
    esac

    case " $result " in
    *[\ \	]-D_BSD_SOURCE[\ \	]*) ;;
    *) result="$result -D_BSD_SOURCE" ;;
    esac

    case " $result " in
    *[\ \	]-pedantic[\ \	]*) ;;
    *) result="$result -pedantic" ;;
    esac
  fi
  if test x$enable_gcov = xyes; then
    case " $result " in
    *[\ \	]-fprofile-arcs[\ \	]*) ;;
    *) result="$result -fprofile-arcs" ;;
    esac
    case " $result " in
    *[\ \	]-ftest-coverage[\ \	]*) ;;
    *) result="$result -ftest-coverage" ;;
    esac

    ## remove optimization
    result=`echo "$result" | sed -e 's/-O[0-9]*//g'`
  fi

  echo $result
}

addCOnlyWarnings() {
  result="$@"

  case " $result " in
  *[\ \	]-Wdeclaration-after-statement[\ \	]*) ;;
  *) result="$result -Wdeclaration-after-statement" ;;
  esac

  case " $result " in
  *[\ \	]-Wmissing-declarations[\ \	]*) ;;
  *) result="$result -Wmissing-declarations" ;;
  esac

  case " $result " in
  *[\ \	]-Wmissing-prototypes[\ \	]*) ;;
  *) result="$result -Wmissing-prototypes" ;;
  esac

  case " $result " in
  *[\ \	]-Wnested-externs[\ \	]*) ;;
  *) result="$result -Wnested-externs" ;;
  esac

  echo $result
}

addCXXOnlyWarnings() {
  result="$@"

  case " $result " in
  *[\ \	]-Wno-non-virtual-dtor[\ \	]*) ;;
  *) result="$result -Wno-non-virtual-dtor" ;;
  esac

  echo $result
}
changequote([,])dnl


if test "x$GCC" = "xyes"; then
  CFLAGS="`addCommonWarnings $CFLAGS`"
  CFLAGS="`addCOnlyWarnings $CFLAGS`"
  CXXFLAGS="`addCommonWarnings $CXXFLAGS`"
  CXXFLAGS="`addCXXOnlyWarnings $CXXFLAGS`"
else
  if test x$enable_gcov = xyes; then
    AC_MSG_ERROR([--enable-gcov can only be used with gcc])
  fi
fi

AC_SUBST(CFLAGS)
AC_SUBST(CXXFLAGS)
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

AC_CHECK_HEADERS([X11/extensions/scrnsaver.h])
AC_CHECK_LIB(Xss, XScreenSaverQueryExtension, have_xscreensaver=true, have_xscreensaver=false, -lX11 -lXext)

if ! $have_xscreensaver ; then
    AC_MSG_ERROR([XScreenSaver extension is required - X11/extensions/scnsaver.h, libXss.so])
fi
XSCREENSAVER_LIBS=-lXss

PKG_CHECK_MODULES(LIBHIPPO, gobject-2.0 >= $GLIB2_REQUIRED gthread-2.0 loudmouth-1.0 >= $LOUDMOUTH_REQUIRED cairo pango)
PKG_CHECK_MODULES(LIBHIPPOIPC, gobject-2.0 dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_REQUIRED)
PKG_CHECK_MODULES(LIBGTKCOPY, gtk+-2.0 >= $GTK2_REQUIRED gtk+-x11-2.0)
PKG_CHECK_MODULES(MUGSHOT_BASE, gtk+-2.0 >= $GTK2_REQUIRED gthread-2.0 loudmouth-1.0 >= $LOUDMOUTH_REQUIRED dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_GLIB_REQUIRED)
MUGSHOT_LIBS="$MUGSHOT_BASE_LIBS $CURL_LIBS $XSCREENSAVER_LIBS"
MUGSHOT_CFLAGS="$MUGSHOT_BASE_CFLAGS $CURL_CFLAGS"
AC_SUBST(MUGSHOT_LIBS)
AC_SUBST(MUGSHOT_CFLAGS)
PKG_CHECK_MODULES(MUGSHOT_URI_HANDLER, gtk+-2.0 >= $GTK2_REQUIRED gthread-2.0 dbus-1 >= $DBUS_REQUIRED)
PKG_CHECK_MODULES(TEST_HIPPO_IPC, gobject-2.0 dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_REQUIRED)

GLIB_GENMARSHAL=`$PKG_CONFIG --variable=glib_genmarshal glib-2.0`
AC_SUBST(GLIB_GENMARSHAL)

GLIB_MKENUMS=`$PKG_CONFIG --variable=glib_mkenums glib-2.0`
AC_SUBST(GLIB_MKENUMS)

PKG_CHECK_EXISTS(gtk+-2.0 < 2.9.0, have_gtk210=false, have_gtk210=true)
AM_CONDITIONAL(HAVE_GTK210, $have_gtk210)

## used for .desktop file in theory, disabled until we have actual translations
## IT_PROG_INTLTOOL([0.34.90])

## we just need these checks to get the gconf schemas stuff
AC_PATH_PROG(GCONFTOOL, gconftool-2, no)
if test x"$GCONFTOOL" = xno; then
    AC_MSG_ERROR([gconftool-2 executable not found in your path - should be installed with GConf])
fi

AM_GCONF_SOURCE_2


##
## Checks for Firefox build environment
##
AC_ARG_WITH(gecko_sdk, 
	AC_HELP_STRING([--with-gecko-sdk=DIR],
		       [Path to Gecko SDK]))
AC_ARG_WITH(gecko_full_headers,
	AC_HELP_STRING([--with-gecko-headers=DIR],
		       [Path to Gecko headers]))
AC_ARG_WITH(gecko_full_idl,
	AC_HELP_STRING([--with-gecko-idl=DIR],
		       [Path to Gecko IDL files]))
AC_ARG_WITH(xpidl,
	AC_HELP_STRING([--with-xpidl=SOURCE_DIR],
		       [Path to XPIDL binary]))

have_full_gecko=false
sdk_includes_nspr=false

if test "x$with_gecko_sdk" != x -a "x$with_gecko_sdk" != xyes -a "x$with_gecko_sdk" != xno ; then
   GECKO_IDLDIR="$with_gecko_sdk/idl"
   GECKO_INCLUDEDIR="$with_gecko_sdk/include"
   XPIDL="$with_gecko_sdk/bin/xpidl"

   if test -f "$GECKO_INCLUDEDIR/nspr.h" ; then
       sdk_includes_nspr=true
   fi
fi

if test "x$with_gecko_idl" != x -a "x$with_gecko_idl" != xyes -a "x$with_gecko_idl" != xno ; then
    GECKO_IDLDIR="$with_gecko_idl"
fi

if test "x$with_gecko_headers" != x -a "x$with_gecko_headers" != xyes -a "x$with_gecko_headers" != xno ; then
    GECKO_INCLUDEDIR="$with_gecko_headers"
fi

if test "x$with_xpidl" != x -a "x$with_xpidl" != xyes -a "x$with_xpidl" != xno ; then
    XPIDL="$with_xpidl"
fi

# if the firefox-devel RPM is installed, set paths from that
if test \( x"$GECKO_IDLDIR" = x -o "x$GECKO_INCLUDEDIR" = x -o "x$XPIDL" = x \) -a -x /bin/rpm ; then
   AC_MSG_CHECKING([for Gecko SDK via the firefox-devel package])
   FIREFOX_VERSION="`/bin/rpm -q --qf %{version} firefox-devel`"
   if  test $? = 0 ; then
       AC_MSG_RESULT([yes])
       if test x"$GECKO_IDLDIR" = x ; then
           GECKO_IDLDIR=/usr/share/idl/firefox-$FIREFOX_VERSION
       fi
       if test x"$GECKO_INCLUDEDIR" = x ; then
           GECKO_INCLUDEDIR=/usr/include/firefox-$FIREFOX_VERSION
       fi
       if test x"$XPIDL" = x ; then
           XPIDL=/usr/lib/firefox-$FIREFOX_VERSION/xpidl
       fi
   else
       AC_MSG_RESULT([no])
   fi
fi

if test "x$GECKO_INCLUDEDIR" = x ; then
    AC_MSG_CHECKING([for Gecko SDK in /opt/gecko-sdk])
    if test -f "/opt/gecko-sdk/idl/nsIURI.idl" ; then
        GECKO_IDLDIR="/opt/gecko-sdk/idl"
        GECKO_INCLUDEDIR="/opt/gecko-sdk/include"
        XPIDL="/opt/gecko-sdk/bin/xpidl"
	AC_MSG_RESULT([yes])
    else
	AC_MSG_RESULT([no])
    fi
fi

if test "x$GECKO_IDLDIR" != x ; then
   if ! test -f "$GECKO_IDLDIR/nsIURI.idl" ; then
       AC_MSG_ERROR([Cannot find Gecko IDL files in $GECKO_IDLDIR])
   fi
else
    AC_MSG_ERROR([Must either specify --with-gecko-sdk=DIR or --with-gecko-idl=DIR])
fi

if test "x$GECKO_INCLUDEDIR" != x ; then
   if test -f "$GECKO_INCLUDEDIR/caps/nsIScriptSecurityManager.h" ; then
	have_full_gecko=true
   elif test -f "$GECKO_INCLUDEDIR/nsIURI.h" ; then
	:
   else
       AC_MSG_ERROR([Cannot find Gecko headers in $GECKO_INCLUDEDIR])
   fi
else
    AC_MSG_ERROR([Must either specify --with-gecko-sdk=DIR or --with-gecko-headers=DIR])
fi

if test "x$XPIDL" != x ; then
    if ! test -x "$XPIDL" ; then
        AC_MSG_ERROR([Cannot find xpidl at $XPIDL])
    fi
else
    AC_MSG_ERROR([Must either specify --with-gecko-sdk=DIR or --with-xpidl=DIR])
fi

AM_CONDITIONAL(HAVE_FULL_GECKO, $have_full_gecko)

AC_SUBST(GECKO_IDLDIR)
AC_SUBST(GECKO_INCLUDEDIR)
AC_SUBST(XPIDL)

if $sdk_includes_nspr ; then
    NSPR_LIBS="-L $with_gecko_sdk/lib -lnspr4"
else
    NSPR_PACKAGES=nspr
fi

PKG_CHECK_MODULES(LIBHIPPOFIREFOX, gobject-2.0 dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_REQUIRED $NSPR_PACKAGES)

# Should have -lxpcom here, but that upsets libtool for unknown reasons. Or
# should we use -lxpcomglue instead? We don't actually link to anything private, though
# we use some private interfaces
LIBHIPPOFIREFOX_LIBS="$LIBHIPPOFIREFOX_LIBS $NSPR_LIBS"

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
	xpidl:			  ${XPIDL}
	Gecko includes:		  ${GECKO_INCLUDEDIR}
	Gecko idl files:	  ${GECKO_IDLDIR}
	Full Gecko:	  	  ${have_full_gecko}
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
