dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(src/hippo-ui.h)
AC_CONFIG_AUX_DIR(config)

AC_CANONICAL_TARGET

AM_INIT_AUTOMAKE(mugshot, 1.1.94)

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

#
# A little cut and paste to work with old versions of pkg-config
# PKG_CHECK_EXISTS is "new" in 0.19. If writing this configure.ac from
# scratch to work with those versions, I'd just write if $PKG_CONFIG --exists,
# but adding this is easier than rewriting.
#
AC_DEFUN([HIPPO_PKG_PROG_PKG_CONFIG], [AC_PATH_PROG(PKG_CONFIG, pkg-config)])

#
# This macro is:
#
# Copyright © 2004 Scott James Remnant <scott@netsplit.com>.
#
# And licensed under the GNU General Public License
#
AC_DEFUN([HIPPO_PKG_CHECK_EXISTS],
[AC_REQUIRE([HIPPO_PKG_PROG_PKG_CONFIG])dnl
if test -n "$PKG_CONFIG" && \
    AC_RUN_LOG([$PKG_CONFIG --exists --print-errors "$1"]); then
  m4_ifval([$2], [$2], [:])
m4_ifvaln([$3], [else
  $3])dnl
fi])

AC_PROG_CC
AM_PROG_CC_C_O
# This disables building a static library for the firefox plugin, while having
# no effect on our convenience libraries. (I don't know why libtool builds
# static libraries when -module is supplied on a platform supporting dlopen...)
AM_DISABLE_STATIC
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

  case " $result " in
  *[\ \	]-fno-strict-aliasing[\ \	]*) ;;
  *) result="$result -fno-strict-aliasing" ;;
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

# res_init() can be used to work around the inability to see changes
# in /etc/resolv.conf. We need to use AC_TRY_LINK since on GNU libc
# it's a #define for for __res_init
hippo_save_LIBS="$LIBS"
LIBS="-lresolv $LIBS"

AC_MSG_CHECKING(for res_init() in -lresolv)
AC_TRY_LINK([#include <resolv.h>], [
res_init();
], have_res_init=yes, have_res_init=no)
AC_MSG_RESULT($have_res_init)

if test "$have_res_init" = "yes"; then
    RESOLV_LIBS="-lresolv"
    AC_DEFINE(HAVE_RES_INIT, 1, [Define to 1 if you have res_init()])
fi

LIBS="$hippo_save_LIBS"

# sigsetjmp is a macro on some platforms, so AC_CHECK_FUNCS is not reliable
AC_MSG_CHECKING(for sigsetjmp)
AC_TRY_LINK([#include <setjmp.h>], [
sigjmp_buf env;
sigsetjmp(env, 0);
], have_sigsetjmp=yes, have_sigsetjmp=no)
AC_MSG_RESULT($have_sigsetjmp)
if test "$have_sigsetjmp" = "yes"; then
  AC_DEFINE(HAVE_SIGSETJMP, 1, [Define to 1 if you have sigsetjmp()])
fi


AC_CHECK_LIB(jpeg, jpeg_destroy_decompress,
      jpeg_ok=yes,
      jpeg_ok=no
      AC_MSG_ERROR(*** JPEG library not found ***))

if test "$jpeg_ok" = yes; then
    AC_MSG_CHECKING([for jpeglib.h])
    AC_TRY_CPP(
[#include <stdio.h>
#undef PACKAGE
#undef VERSION
#undef HAVE_STDLIB_H
#include <jpeglib.h>],
        jpeg_ok=yes,
        jpeg_ok=no)
    AC_MSG_RESULT($jpeg_ok)
    if test "$jpeg_ok" = yes; then
        JPEG_LIBS='-ljpeg'
    else
        AC_MSG_ERROR(*** JPEG header not found ***)
    fi
fi

AC_ARG_WITH(maemo,
	    AC_HELP_STRING([--with-maemo=[yes/no]],
		           [Whether to use Maemo version of Mugshot client]),
	    ,
	    with_maemo=no)

GLIB2_REQUIRED=2.6.0
GTK2_REQUIRED=2.6.0
# unfortunately this breaks us on FC4
DBUS_REQUIRED=0.60
DBUS_GLIB_REQUIRED=0.60
# earlier might work, but this is what's been tested
CURL_REQUIRED=7.13.1
DDM_REQUIRED=1.2.0

## We don't just use PKG_CHECK_MODULES because libcurl.pc has all kinds of crap in it like openssl and kerberos
## on the link line... we don't want to link to all that stuff. So just do -lcurl
## For now people doing static linking or putting libcurl in a funny prefix will have to h4x0r the following
## lines (try just "PKG_CHECK_MODULES(CURL, libcurl >= $CURL_REQUIRED)" if you don't care about linking to 
## all the extra goo)
## Also, older curl has curl-config instead of a .pc file... mumble grumble
CURL_LIBS=-lcurl
CURL_CFLAGS=
HIPPO_PKG_CHECK_EXISTS(libcurl >= $CURL_REQUIRED, have_curl=true, have_curl=false)
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

XSCREENSAVER_PACKAGES=""
XSCREENSAVER_LIBS=""
XSCREENSAVER_CFLAGS=""

# First check to see if we have a .pc file for the xscreensaver extensio
HIPPO_PKG_CHECK_EXISTS(xscrnsaver, have_xscreensaver=true, have_xscreensaver=false)
if $have_xscreensaver ; then
    XSCREENSAVER_PACKAGES="xscrnsaver"
else
    # No, check the old way
    AC_PATH_XTRA
    if test "x$no_x" = xyes ; then
        AC_MSG_ERROR([Can't find path to the X development files])
    fi

    hippo_save_CPPFLAGS="$CPPFLAGS"
    CPPFLAGS="$CPPFLAGS $X_CFLAGS"

    hippo_save_LIBS="$LIBS"
    LIBS="$X_LIBS $LIBS"

    have_xscreensaver=true
    AC_CHECK_HEADERS([X11/extensions/scrnsaver.h], :, [have_xscreensaver=false])
    AC_CHECK_LIB(Xss, XScreenSaverQueryExtension, :, [have_xscreensaver=false], -lXext -lX11 $X_EXTRA_LIBS)

    CFLAGS="$hippo_save_CFLAGS"
    LIBS="$hippo_save_LIBS"

    if test "x$with_maemo" != xno ; then        
        if ! $have_xscreensaver ; then
            AC_MSG_ERROR([XScreenSaver extension is required - X11/extensions/scnsaver.h, libXss.so])
        fi
        XSCREENSAVER_LIBS="-lXss -lXext -lX11 $X_EXTRA_LIBS"
        XSCREENSAVER_CFLAGS="$X_CFLAGS"
    else
        XSCREENSAVER_LIBS=""
        XSCREENSAVER_CFLAGS=""
    fi
fi

extra_mugshot_packages=""

if test "x$with_maemo" != xno ; then
   extra_mugshot_packages="libosso >= 0.9.19"
fi

PKG_CHECK_MODULES(LIBHIPPOCANVAS, gtk+-2.0 >= $GTK2_REQUIRED gobject-2.0 >= $GLIB2_REQUIRED cairo pango)
PKG_CHECK_MODULES(LIBHIPPO, gobject-2.0 >= $GLIB2_REQUIRED gthread-2.0 ddm-1 >= $DDM_REQUIRED)
PKG_CHECK_MODULES(LIBSTACKER, gobject-2.0 >= $GLIB2_REQUIRED gthread-2.0 cairo pango hippo-canvas-1 ddm-1 >= $DDM_REQUIRED)
PKG_CHECK_MODULES(LIBHIPPOIPC, gobject-2.0 dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_REQUIRED)
PKG_CHECK_MODULES(LIBGTKCOPY, gtk+-2.0 >= $GTK2_REQUIRED gtk+-x11-2.0)
PKG_CHECK_MODULES(MUGSHOT_BASE, gtk+-2.0 >= $GTK2_REQUIRED gthread-2.0 dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_GLIB_REQUIRED $XSCREENSAVER_PACKAGES hippo-canvas-1  ddm-1 >= $DDM_REQUIRED $extra_mugshot_packages)

MUGSHOT_LIBS="$MUGSHOT_BASE_LIBS $XSCREENSAVER_LIBS $CURL_LIBS $JPEG_LIBS $RESOLV_LIBS"
MUGSHOT_CFLAGS="$MUGSHOT_BASE_CFLAGS $XSCREENSAVER_CFLAGS $CURL_CFLAGS"
AC_SUBST(MUGSHOT_LIBS)
AC_SUBST(MUGSHOT_CFLAGS)

PKG_CHECK_MODULES(MUGSHOT_URI_HANDLER, gtk+-2.0 >= $GTK2_REQUIRED gthread-2.0 dbus-1 >= $DBUS_REQUIRED)
PKG_CHECK_MODULES(TEST_HIPPO_IPC, gobject-2.0 dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_REQUIRED)

if test "x$with_maemo" != xno ; then
    AC_DEFINE(WITH_MAEMO, , [whether Maemo is active])
fi
AM_CONDITIONAL(WITH_MAEMO, test "x$with_maemo" != xno)

GLIB_GENMARSHAL=`$PKG_CONFIG --variable=glib_genmarshal glib-2.0`
AC_SUBST(GLIB_GENMARSHAL)

GLIB_MKENUMS=`$PKG_CONFIG --variable=glib_mkenums glib-2.0`
AC_SUBST(GLIB_MKENUMS)

HIPPO_PKG_CHECK_EXISTS(gtk+-2.0 < 2.9.0, have_gtk210=false, have_gtk210=true)
AM_CONDITIONAL(HAVE_GTK210, $have_gtk210)

HIPPO_PKG_CHECK_EXISTS(dbus-1 < 1.0, have_dbus10=false, have_dbus10=true)
if $have_dbus10 ; then
    AC_DEFINE(HAVE_DBUS_1_0, , [whether we have dbus 1.0 or greater])
fi

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
need_separate_js=false

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

# Look for libxul or libxul-unstable

AC_MSG_CHECKING([for libxul])

have_xulrunner=false
for p in libxul libxul-instable ; do
    if $PKG_CONFIG --exists $p '>=' 1.9 ; then
       have_xulrunner=true
       xulrunner_package=$p
    fi
done

AM_CONDITIONAL(USE_XULRUNNER, test "x${have_xulrunner}" = "xtrue")
if test "x${have_xulrunner}" = "xtrue"; then
    AC_MSG_RESULT([yes])
    AC_DEFINE(HAVE_XULRUNNER, 1, [whether we have XULRunner])
    idldir=`$PKG_CONFIG --variable=idldir $xulrunner_package`
    GECKO_IDLDIR="${idldir}"
    GECKO_INCLUDEDIR=`$PKG_CONFIG --variable=includedir $xulrunner_package`
    # This needs rethinking on the Gecko end.... I'm not sure that
    # both of these are still relevant, but we need a real way to find
    # xpidl from the .pc file
    for i in `$PKG_CONFIG --variable=sdkdir $xulrunner_package`/bin/xpidl `$PKG_CONFIG --variable=libdir $xulrunner_package`/xpidl ; do
       if test -x $i ; then
           XPIDL=$i
       fi
    done         
    if test x"XPIDL" = x ; then 
       AC_MSG_ERROR([Can't find xpidl based on libxulrunner .pc file])
    fi
else
    AC_MSG_RESULT([no])
fi

# if the firefox-devel RPM is installed, set paths from that
if test \( x"$GECKO_IDLDIR" = x -o "x$GECKO_INCLUDEDIR" = x -o "x$XPIDL" = x \) -a -x /bin/rpm ; then
   AC_MSG_CHECKING([for Gecko SDK via the firefox-devel package])
   FIREFOX_VERSION="`/bin/rpm -q --qf '%{version} ' firefox-devel`"
   if  test $? = 0 ; then
       # In multi-arch situations, there might be multiple versions of firefox-devel
       # installed; we assume it doesn't matter which one we find; ideally we'd
       # like to find the one that matches the architecture we are building for
       # but that's hard to do, and what we use shouldn't be dependent on
       # architecture or exact version. The only problem would be if the two versions 
       # are for wildly different versions of firefox.
       #
       FIREFOX_VERSION=`(set $FIREFOX_VERSION && echo $1)`

       AC_MSG_RESULT([yes])
       if test x"$GECKO_IDLDIR" = x ; then
           GECKO_IDLDIR=/usr/share/idl/firefox-$FIREFOX_VERSION
       fi
       if test x"$GECKO_INCLUDEDIR" = x ; then
           GECKO_INCLUDEDIR=/usr/include/firefox-$FIREFOX_VERSION
       fi
       if test x"$XPIDL" = x ; then
           if test -x /usr/lib/firefox-$FIREFOX_VERSION/xpidl ; then
               XPIDL=/usr/lib/firefox-$FIREFOX_VERSION/xpidl
	   elif test -x /usr/lib64/firefox-$FIREFOX_VERSION/xpidl ; then
               XPIDL=/usr/lib64/firefox-$FIREFOX_VERSION/xpidl
	   else
	       AC_MSG_WARN([Found firefox-devel RPM, but couldn't find xpidl binary])
           fi
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

if test "x${have_xulrunner}" != xtrue; then
   AC_DEFINE(HAVE_PRE_XULRUNNER_SDK, 1, [Define if not using XULRunner])
fi

if test "x$GECKO_INCLUDEDIR" != x ; then
   AC_MSG_CHECKING([for "full gecko" style includes])
   if test -f "$GECKO_INCLUDEDIR/caps/nsIScriptSecurityManager.h" ; then
       have_full_gecko=true
       AC_MSG_RESULT([yes])
   elif test -f "$GECKO_INCLUDEDIR/nsIURI.h" ; then
       AC_MSG_RESULT([no])
   else
       AC_MSG_ERROR([Cannot find Gecko headers in $GECKO_INCLUDEDIR])
   fi
else
    AC_MSG_ERROR([Must either specify --with-gecko-sdk=DIR or --with-gecko-headers=DIR])
fi

# If we are using the cut-and-paste version of nsIXPCSecurityManager.h, we don't
# need the Javascript headers, otherwise we do. Debian packages the Javascript
# files from XulRunner into a separate directory, so detect that and we'll look
# for them later
if $have_full_gecko ; then
   if test -f "$GECKO_INCLUDEDIR/js/jspubtd.h" ; then
      :
   else
       need_separate_js=true
   fi
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
    for pkg in firefox-nspr xulrunnner-nspr nspr microb-engine-nspr; do
        if test "x$NSPR_PACKAGES" = x ; then
	    HIPPO_PKG_CHECK_EXISTS($pkg, NSPR_PACKAGES=$pkg)
	fi
    done
    if test "x$NSPR_PACKAGES" = x ; then
	AC_MSG_ERROR([Can't find nspr])
    fi
fi

if $need_separate_js ; then
    for pkg in firefox-js xulrunner-js microb-engine-js; do
        if test "x$JS_PACKAGES" = x ; then
	    HIPPO_PKG_CHECK_EXISTS($pkg, JS_PACKAGES=$pkg)
	fi
    done
    if test "x$JS_PACKAGES" = x ; then
	AC_MSG_ERROR([Can't find Gecko Javascript package])
    fi
fi

PKG_CHECK_MODULES(LIBHIPPOFIREFOX, gobject-2.0 dbus-1 >= $DBUS_REQUIRED dbus-glib-1 >= $DBUS_REQUIRED $NSPR_PACKAGES $JS_PACKAGES gtk+-2.0)

if test "x${have_xulrunner}"; then
  LIBHIPPOFIREFOX_CFLAGS="`$PKG_CONFIG --cflags $xulrunner_package` ${LIBHIPPOFIREFOX_CFLAGS}"
fi
# Should have -lxpcom here, but that upsets libtool for unknown reasons. Or
# should we use -lxpcomglue instead? We don't actually link to anything private, though
# we use some private interfaces
LIBHIPPOFIREFOX_LIBS="$LIBHIPPOFIREFOX_LIBS $NSPR_LIBS"
if test "x${have_xulrunner}" = "xtrue"; then
  # We need to match the mozilla version by compiling with short-wchar
  LIBHIPPOFIREFOX_LIBS="${LIBHIPPOFIREFOX_LIBS} `$PKG_CONFIG --libs $xulrunner_package`"
fi

#### define absolute path to srcdir for debugging-only code
ABSOLUTE_TOP_SRCDIR=`cd ${srcdir} && pwd`
AC_DEFINE_UNQUOTED(ABSOLUTE_TOP_SRCDIR, "$ABSOLUTE_TOP_SRCDIR", [full path to srcdir])

AC_ARG_WITH(about_message, 
 	    AC_HELP_STRING([--with-about-message=MESSAGE],
			   [Additional message for the about dialog]))

AC_DEFINE_UNQUOTED(HIPPO_ABOUT_MESSAGE, "$with_about_message", [additional message for the about dialog])

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
        LIBHIPPOFIREFOX_CFLAGS:   ${LIBHIPPOFIREFOX_CFLAGS}

        LIBHIPPO_LIBS:            ${LIBHIPPO_LIBS}
        MUGSHOT_LIBS:             ${MUGSHOT_LIBS}
        LIBHIPPOFIREFOX_LIBS:     ${LIBHIPPOFIREFOX_LIBS}

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
