dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(LICENSE)
AC_CONFIG_AUX_DIR(config)

AC_CANONICAL_TARGET

AM_INIT_AUTOMAKE(hippo-canvas, 0.2.24)

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

GLIB2_REQUIRED=2.6.0
GTK2_REQUIRED=2.6.0

PKG_CHECK_MODULES(LIBHIPPOCANVAS, gobject-2.0 >= $GLIB2_REQUIRED gtk+-2.0 >= $GTK2_REQUIRED cairo pango)

GLIB_GENMARSHAL=`$PKG_CONFIG --variable=glib_genmarshal glib-2.0`
AC_SUBST(GLIB_GENMARSHAL)

GLIB_MKENUMS=`$PKG_CONFIG --variable=glib_mkenums glib-2.0`
AC_SUBST(GLIB_MKENUMS)

GTK_DOC_CHECK([1.6])

## Python bindings
AM_PATH_PYTHON
AM_CHECK_PYTHON_HEADERS(,[AC_MSG_ERROR(could not find Python headers)])

AC_PATH_PROG(PYGTK_CODEGEN, pygtk-codegen-2.0, no)

PKG_CHECK_MODULES(PYTHON_BINDINGS, pygtk-2.0 pycairo)

PYGTK_DEFSDIR=`$PKG_CONFIG --variable=defsdir pygtk-2.0`
AC_SUBST(PYGTK_DEFSDIR)

AC_OUTPUT([
Makefile
hippo-canvas-1.pc
docs/Makefile
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
