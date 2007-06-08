dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(src/main.c)
AC_CONFIG_AUX_DIR(config)

AC_CANONICAL_TARGET

AM_INIT_AUTOMAKE(local-export-daemon, 0.1)

AM_CONFIG_HEADER(config/config.h)

# Honor aclocal flags
ACLOCAL="$ACLOCAL $ACLOCAL_FLAGS"

GETTEXT_PACKAGE=local-export-daemon
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
AM_PROG_CC_C_O
AM_DISABLE_STATIC
#AC_PROG_CXX
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

GLIB2_REQUIRED=2.8
AVAHI_REQUIRED=0.6.0
DBUS_GLIB_REQUIRED=0.70

PKG_CHECK_MODULES(LOCAL_EXPORT_DAEMON, gobject-2.0 >= $GLIB2_REQUIRED avahi-glib >= $AVAHI_REQUIRED avahi-client >= $AVAHI_REQUIRED dbus-glib-1 >= $DBUS_GLIB_REQUIRED)

GLIB_GENMARSHAL=`$PKG_CONFIG --variable=glib_genmarshal glib-2.0`
AC_SUBST(GLIB_GENMARSHAL)

GLIB_MKENUMS=`$PKG_CONFIG --variable=glib_mkenums glib-2.0`
AC_SUBST(GLIB_MKENUMS)

## used for .desktop file in theory, disabled until we have actual translations
## IT_PROG_INTLTOOL([0.34.90])

#### define absolute path to srcdir for debugging-only code
ABSOLUTE_TOP_SRCDIR=`cd ${srcdir} && pwd`
AC_DEFINE_UNQUOTED(ABSOLUTE_TOP_SRCDIR, "$ABSOLUTE_TOP_SRCDIR", [full path to srcdir])

AC_OUTPUT([
Makefile
])

dnl ==========================================================================
echo "

        Local Export Daemon $VERSION
	==================

        prefix:                   ${prefix}
        compiler:                 ${CC}
        LOCAL_EXPORT_DAEMON_CFLAGS:  ${LOCAL_EXPORT_DAEMON_CFLAGS}
        LOCAL_EXPORT_DAEMON_LIBS:    ${LOCAL_EXPORT_DAEMON_LIBS}

        Now type 'make' to build $PACKAGE
"
