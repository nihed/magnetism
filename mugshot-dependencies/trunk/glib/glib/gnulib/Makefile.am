## Process this file with automake to produce Makefile.in

INCLUDES = -I$(top_srcdir) -DG_LOG_DOMAIN=\"GLib\" \
	$(GLIB_DEBUG_FLAGS) -DG_DISABLE_DEPRECATED -DGLIB_COMPILATION 

noinst_LTLIBRARIES = libgnulib.la

libgnulib_la_SOURCES = 	\
	asnprintf.c \
	printf-args.c \
	printf-args.h \
	printf-parse.c \
	printf-parse.h \
	vasnprintf.c \
	vasnprintf.h \
	printf.c \
	printf.h \
	g-gnulib.h


EXTRA_DIST = makefile.msc







