bignative_la_CPPFLAGS =			\
	$(AM_CPPFLAGS)			\
	$(PYTHON_INCLUDES)		\
	$(BIGBOARD_CFLAGS)

pkgpyexecdir = $(pythondir)/bigboard
pkgpyexec_LTLIBRARIES=bignative.la

bignative_la_LDFLAGS = -module -avoid-version
bignative_la_LIBADD =					\
	$(BIGBOARD_LIBS)

bignative_la_SOURCES = src/bignative.c src/bigboard-native.h src/bigboard-native.c

bin_SCRIPTS = bigboard

noinst_SCRIPTS = bigboard-uninst 

mainscriptdir = $(libexecdir)/bigboard
mainscript_PYTHON = src/main.py

bigboarddir = $(pythondir)/bigboard
bigboard_PYTHON = src/bigboard.py \
                  src/big_widgets.py \
                  src/google.py \
                  src/keyring.py \
                  src/libbig.py \
                  src/libgimmie.py \
                  src/mugshot.py \
                  src/profile.py \
                  src/singletonmixin.py \
                  src/slideout.py 

httplibdir=$(bigboarddir)/httplib2
httplib_PYTHON = src/httplib2/__init__.py \
                 src/httplib2/iri2uri.py

stocksdir = $(datadir)/bigboard/stocks
appstockdir=$(stocksdir)/apps
appstock_PYTHON = \
                src/stocks/apps/appbrowser.py \
                src/stocks/apps/apps_directory.py \
                src/stocks/apps/AppsStock.py \
                src/stocks/apps/apps_widgets.py \
                src/stocks/apps/listing.xml

calendarstockdir=$(stocksdir)/calendar
calendarstock_PYTHON = \
                src/stocks/calendar/CalendarStock.py \
                src/stocks/calendar/listing.xml

photosstockdir=$(stocksdir)/photos
photosstock_PYTHON = \
                src/stocks/photos/PhotosStock.py \
                src/stocks/photos/listing.xml

searchstockdir=$(stocksdir)/search
searchstock_PYTHON = \
                src/stocks/search/SearchStock.py \
                src/stocks/search/listing.xml

selfstockdir=$(stocksdir)/self
selfstock_PYTHON = \
                src/stocks/self/SelfStock.py \
                src/stocks/self/listing.xml

bigboard: src/bigboard.in
	sed \
	    -e s!\@PYTHON\@!@PYTHON@! \
	    -e s!\@BB_PYDIR\@!$(mainscriptdir)! \
	    -e s!\@BB_STOCKDIR\@!$(stocksdir)! \
	    < $< > $@
	chmod a+x $@

bigboard-uninst: src/bigboard-uninst.in
	sed \
	    -e s!\@PYTHON\@!@PYTHON@! \
	    -e s!\@BB_PYDIR\@!$(srcdir)/src! \
	    -e s!\@BB_STOCKDIR\@!$(srcdir)/src/stocks! \
	    < $< > $@
	chmod a+x $@


EXTRA_DIST = src/bigboard.in src/bigboard-uninst.in

CLEANFILES = bigboard bigboard-uninst
