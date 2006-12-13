dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(LICENSE)
AC_CONFIG_AUX_DIR(config)

AC_CANONICAL_TARGET

AM_INIT_AUTOMAKE(hippo-canvas, 0.1.5)

AM_CONFIG_HEADER(config/config.h)

# Honor aclocal flags
ACLOCAL="$ACLOCAL $ACLOCAL_FLAGS"

GETTEXT_PACKAGE=hippo-canvas
AC_SUBST(GETTEXT_PACKAGE)
AC_DEFINE_UNQUOTED(GETTEXT_PACKAGE,"$GETTEXT_PACKAGE",[The name of the gettext domain])

 ## must come before we use the $USE_MAINTAINER_MODE variable later
AM_MAINTAINER_MODE

## don't rerun to this point if we abort
AC_CACHE_SAVE

AC_PROG_CC
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
changequote([,])dnl

if test "x$GCC" = "xyes"; then
  CFLAGS="`addCommonWarnings $CFLAGS`"
  CFLAGS="`addCOnlyWarnings $CFLAGS`"
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

AC_MSG_CHECKING(For "$srcdir/../linux" or "$srcdir/linux-dist")
if test -d "$srcdir"/linux ; then
        AC_MSG_RESULT(found in wrong place)
        AC_MSG_ERROR([The "linux" directory should not be here; it should be a sibling of srcdir or be called linux-dist and a subdir of it])
elif test -d "$srcdir"/linux-dist ; then
        linux_outside_tree=false
        AC_MSG_RESULT(found linux-dist inside tree)
elif test -d "$srcdir"/../linux ; then
        linux_outside_tree=true
        AC_MSG_RESULT(found outside tree)
else
        AC_MSG_RESULT(not found)
        AC_MSG_ERROR([The "linux" or "linux-dist" directory wasn't found])
fi
AM_CONDITIONAL(LINUX_OUTSIDE_TREE, $linux_outside_tree)


GLIB2_REQUIRED=2.6.0
GTK2_REQUIRED=2.6.0

PKG_CHECK_MODULES(LIBHIPPOCANVAS, gobject-2.0 >= $GLIB2_REQUIRED gtk+-2.0 >= $GTK2_REQUIRED cairo pango)

GLIB_GENMARSHAL=`$PKG_CONFIG --variable=glib_genmarshal glib-2.0`
AC_SUBST(GLIB_GENMARSHAL)

GLIB_MKENUMS=`$PKG_CONFIG --variable=glib_mkenums glib-2.0`
AC_SUBST(GLIB_MKENUMS)

## Python bindings
AM_PATH_PYTHON
AM_CHECK_PYTHON_HEADERS(,[AC_MSG_ERROR(could not find Python headers)])

AC_PATH_PROG(PYGTK_CODEGEN, pygtk-codegen-2.0, no)

PKG_CHECK_MODULES(PYTHON_BINDINGS, pygtk-2.0 pycairo)

PYGTK_DEFSDIR=`$PKG_CONFIG --variable=defsdir pygtk-2.0`
AC_SUBST(PYGTK_DEFSDIR)

AC_OUTPUT([
Makefile
])

dnl ==========================================================================
echo "

        hippo-canvas $VERSION
	==================

        prefix:                   ${prefix}
        compiler:                 ${CC}
        LIBHIPPOCANVAS_CFLAGS:    ${LIBHIPPOCANVAS_CFLAGS}

        Now type 'make' to build $PACKAGE
"
