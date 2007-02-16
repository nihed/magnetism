bignative_la_CPPFLAGS =			\
	$(AM_CPPFLAGS)			\
	$(PYTHON_INCLUDES)		\
	$(BIGBOARD_CFLAGS)

hippocoredir = $(pythondir)
pkgpyexecdir = $(hippocoredir)

pkgpyexec_LTLIBRARIES=bignative.la

bignative_la_LDFLAGS = -module -avoid-version
bignative_la_LIBADD =					\
	$(BIGBOARD_LIBS)

nodist_bignative_la_SOURCES = src/bignative.c src/bigboard-native.h src/bigboard-native.c

#bin_SCRIPTS = bigboard

noinst_SCRIPTS = bigboard-uninst

bigboard-uninst: src/bigboard-uninst.in
	sed \
	    -e s!\@PYTHON\@!@PYTHON@! \
	    -e s!\@SRCDIR\@!$(srcdir)! \
	    < $< > $@
	chmod a+x $@
