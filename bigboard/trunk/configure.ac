AC_INIT(bigboard, 0.5.11, feedback@mugshot.org)
AC_CONFIG_SRCDIR(main.py)
AC_CONFIG_MACRO_DIR([m4])

AM_INIT_AUTOMAKE
GNOME_COMMON_INIT

AM_CONFIG_HEADER(config.h)

AM_MAINTAINER_MODE
AM_DISABLE_STATIC
AM_PROG_LIBTOOL
AC_SUBST(ACLOCAL_AMFLAGS, "$ACLOCAL_FLAGS -I m4")
AC_PROG_CC
AC_PROG_LIBTOOL
AC_ISC_POSIX
AC_HEADER_STDC
AM_PROG_CC_C_O

AC_CHECK_PROG(HAVE_PKGCONFIG, pkg-config, yes, no)

GETTEXT_PACKAGE=bigboard
IT_PROG_INTLTOOL([0.35.0])
AC_SUBST(GETTEXT_PACKAGE)
AC_DEFINE_UNQUOTED(GETTEXT_PACKAGE,"$GETTEXT_PACKAGE",[The name of the gettext domain])
AM_GLIB_GNU_GETTEXT

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

## whether to do the applet as a shared lib, which it probably should be eventually, 
## it just isn't doing enough stuff to be another process
AM_CONDITIONAL(APPLET_INPROCESS, false)

PKG_CHECK_MODULES(BIGBOARD, gtk+-2.0 >= 2.8 pygtk-2.0 >= 2.8 gnome-keyring-1)
AC_SUBST(BIGBOARD_LIBS)
AC_SUBST(BIGBOARD_CFLAGS)

PKG_CHECK_MODULES(BIGBOARD_APPLET, libpanelapplet-2.0 gtk+-2.0 >= 2.8 dbus-1 )
AC_SUBST(BIGBOARD_APPLET_LIBS)
AC_SUBST(BIGBOARD_APPLET_CFLAGS)

AC_PATH_PROG(GCONFTOOL, gconftool-2)
AM_GCONF_SOURCE_2

## Python bindings
AM_PATH_PYTHON
AM_CHECK_PYTHON_HEADERS(,[AC_MSG_ERROR(could not find Python headers)])

AC_ARG_ENABLE(pymod_checks, 
              AC_HELP_STRING([--disable-pymod-checks], [Disable checks for Python modules]),
              enable_pymod_checks=$enableval,
	      enable_pymod_checks=yes)
              
if test x${enable_pymod_checks} = xyes; then
  AM_CHECK_PYMOD(hippo,,,AC_MSG_ERROR([hippo module required (hippo-canvas)]))
  AM_CHECK_PYMOD(gconf,,,AC_MSG_ERROR([gconf module required]))
  AM_CHECK_PYMOD(gnome.ui,,,AC_MSG_ERROR([gnome.ui module required]))
  AM_CHECK_PYMOD(dbus,,,AC_MSG_ERROR([dbus module required]))
fi

AC_PATH_PROG(PYGTK_CODEGEN, pygtk-codegen-2.0, no)

PYGTK_DEFSDIR=`$PKG_CONFIG --variable=defsdir pygtk-2.0`
AC_SUBST(PYGTK_DEFSDIR)

AC_OUTPUT([
Makefile
applet/Makefile
bigboard/Makefile
bigboard/keybinder/Makefile
data/Makefile
scripts/Makefile
po/Makefile.in
version
])
