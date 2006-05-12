dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(src/hippo-platform-impl.h)
AC_CONFIG_AUX_DIR(config)

AC_CANONICAL_TARGET

AM_INIT_AUTOMAKE(mugshot, 0.1)

AM_CONFIG_HEADER(config/config.h)

# Honor aclocal flags
ACLOCAL="$ACLOCAL $ACLOCAL_FLAGS"

GETTEXT_PACKAGE=mugshot
AC_SUBST(GETTEXT_PACKAGE)
AC_DEFINE_UNQUOTED(GETTEXT_PACKAGE,"$GETTEXT_PACKAGE",[The name of the gettext domain])

 ## must come before we use the $USE_MAINTAINER_MODE variable later
AM_MAINTAINER_MODE

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

PKG_CHECK_MODULES(LIBHIPPO, gobject-2.0 >= $GLIB2_REQUIRED gthread-2.0 loudmouth-1.0 >= $LOUDMOUTH_REQUIRED)
PKG_CHECK_MODULES(LIBGTKCOPY, gtk+-2.0 >= $GTK2_REQUIRED gtk+-x11-2.0)
PKG_CHECK_MODULES(MUGSHOT, gtk+-2.0 >= $GTK2_REQUIRED gthread-2.0 loudmouth-1.0 >= $LOUDMOUTH_REQUIRED)

AC_OUTPUT([
Makefile
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
