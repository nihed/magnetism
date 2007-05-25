dnl -*- mode: m4 -*-
AC_PREREQ(2.59)

AC_INIT(apps/gmail/gmail.desktop.in)
AC_CONFIG_AUX_DIR(config)

AC_CANONICAL_TARGET

AM_INIT_AUTOMAKE(online-desktop, 0.2)

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


AC_PROG_CC
# no libs for now anyway but this is here for future safety
AM_DISABLE_STATIC
# no C++ for the moment
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


## used for .desktop file in theory, disabled until we have actual translations
## IT_PROG_INTLTOOL([0.34.90])

GTK2_REQUIRED=2.6.0

PKG_CHECK_MODULES(MAILTO_HANDLER, gtk+-2.0 >= $GTK2_REQUIRED)

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
        compiler:                 ${CC}
        MAILTO_HANDLER_CFLAGS:    ${MAILTO_HANDLER_CFLAGS}
        MAILTO_HANDLER_LIBS:      ${MAILTO_HANDLER_LIBS}

        Now type 'make' to build $PACKAGE
"
