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

bigboarddir = $(pyexecdir)/bigboard
bigboard_PYTHON = *.py

libbigdir=$(bigboarddir)/libbig
libbig_PYTHON = libbig/*.py

httplibdir=$(bigboarddir)/httplib2
httplib_PYTHON = httplib2/__init__.py \
                 httplib2/iri2uri.py

appstockdir=$(stocksdir)/apps
appstock_PYTHON = \
                stocks/apps/*.py \
                stocks/apps/listing.xml

calendarstockdir=$(stocksdir)/calendar
calendarstock_PYTHON = \
                stocks/calendar/*.py \
                stocks/calendar/listing.xml

photosstockdir=$(stocksdir)/photos
photosstock_PYTHON = \
                stocks/photos/*.py \
                stocks/photos/listing.xml

searchstockdir=$(stocksdir)/search
searchstock_PYTHON = \
                stocks/search/*.py \
                stocks/search/listing.xml

selfstockdir=$(stocksdir)/self
selfstock_PYTHON = \
                stocks/self/*.py \
                stocks/self/listing.xml
                
peoplestockdir=$(stocksdir)/people
peoplestock_PYTHON = \
                stocks/people/*.py \
                stocks/people/peoplewidgets.py \
                stocks/people/listing.xml

filestockdir=$(stocksdir)/files
filestock_PYTHON = \
                stocks/files/*.py \
                stocks/files/filebrowser.py \
                stocks/files/listing.xml

