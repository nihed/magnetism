INCLUDES = 				\
	-DHIPPOHERD_COMPILATION		\
	-I$(top_srcdir)			\
	$(HIPPOHERD_CFLAGS)

lib_LTLIBRARIES = libhippoherd-1.la

libhippoherd_1_la_SOURCES =		\
	hh-client.c

libhippoherdinclude_HEADERS =		\
	hh-client.h			\
	hippoherd.h

libhippoherd_1_la_LIBADD = 		\
	$(HIPPOHERD_LIBS)

## don't export symbols that start with "_" (we use this 
## convention for internal symbols)
libhippoherd_1_la_LDFLAGS= -export-symbols-regex "^[^_].*" -version-info $(LT_CURRENT):$(LT_REVISION):$(LT_AGE) -no-undefined

libhippoherdincludedir = $(includedir)/hippoherd-1.0/hh
