include $(top_srcdir)/Makefile-include.am

SUBDIRS = keybinder
EXTRA_DIST =

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

libbigbardir=$(bigboarddir)/bigbar
libbigbar_PYTHON = bigbar/*.py

httplibdir=$(bigboarddir)/httplib2
httplib_PYTHON = httplib2/__init__.py \
                 httplib2/iri2uri.py

stocks_DATA = stocks/*.xml
EXTRA_DIST += $(stocks_DATA)

appstockdir=$(stocksdir)/apps
appstock_PYTHON = stocks/apps/*.py

calendarstockdir=$(stocksdir)/google_calendar
calendarstock_PYTHON = stocks/google_calendar/*.py

photosstockdir=$(stocksdir)/mugshot_photos
photosstock_PYTHON = stocks/mugshot_photos/*.py

searchstockdir=$(stocksdir)/search
searchstock_PYTHON = stocks/search/*.py

selfstockdir=$(stocksdir)/self
selfstock_PYTHON = stocks/self/*.py
                
peoplestockdir=$(stocksdir)/people
peoplestock_PYTHON = stocks/people/*.py

filestockdir=$(stocksdir)/files
filestock_PYTHON = stocks/files/*.py

