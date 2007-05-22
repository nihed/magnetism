include $(top_srcdir)/Makefile-include.am

SUBDIRS = keybinder

bignative_la_CPPFLAGS =			\
	$(AM_CPPFLAGS)			\
	$(PYTHON_INCLUDES)		\
	$(BIGBOARD_CFLAGS)

pkgpyexecdir = $(pyexecdir)/bigboard
pkgpyexec_LTLIBRARIES=bignative.la

bignative_la_LDFLAGS = -module -avoid-version
bignative_la_LIBADD =					\
	$(BIGBOARD_LIBS)

bignative_la_SOURCES = bignative.c bigboard-native.h bigboard-native.c

bigboarddir = $(pythondir)/bigboard
bigboard_PYTHON = __init__.py \
                  big_widgets.py \
                  deskbar_embed.py \
                  google.py \
                  keyring.py \
                  libgimmie.py \
                  mugshot.py \
                  presence.py \
                  profile.py \
                  slideout.py \
		  stock.py

libbigdir=$(bigboarddir)/libbig
libbig_PYTHON = libbig/__init__.py \
	       	libbig/dbusutil.py \
	       	libbig/httpcache.py \
	       	libbig/http.py \
	       	libbig/imagecache.py \
	       	libbig/logutil.py \
	       	libbig/polling.py \
	       	libbig/singletonmixin.py \
	       	libbig/state.py \
	       	libbig/struct.py \
	       	libbig/xmlquery.py

httplibdir=$(bigboarddir)/httplib2
httplib_PYTHON = httplib2/__init__.py \
                 httplib2/iri2uri.py

appstockdir=$(stocksdir)/apps
appstock_PYTHON = \
                stocks/apps/appbrowser.py \
                stocks/apps/apps_directory.py \
                stocks/apps/AppsStock.py \
                stocks/apps/apps_widgets.py \
                stocks/apps/listing.xml

calendarstockdir=$(stocksdir)/calendar
calendarstock_PYTHON = \
                stocks/calendar/CalendarStock.py \
                stocks/calendar/listing.xml

photosstockdir=$(stocksdir)/photos
photosstock_PYTHON = \
                stocks/photos/PhotosStock.py \
                stocks/photos/listing.xml

searchstockdir=$(stocksdir)/search
searchstock_PYTHON = \
                stocks/search/SearchStock.py \
                stocks/search/listing.xml

selfstockdir=$(stocksdir)/self
selfstock_PYTHON = \
                stocks/self/SelfStock.py \
                stocks/self/listing.xml
