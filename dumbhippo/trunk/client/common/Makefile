HEADERS= \
    hippo/hippo-basics.h        \
    hippo/hippo-chat-room.h     \
    hippo/hippo-common.h        \
    hippo/hippo-connection.h    \
    hippo/hippo-cookies.h       \
    hippo/hippo-data-cache.h    \
    hippo/hippo-data-cache-internal.h \
    hippo/hippo-entity.h        \
    hippo/hippo-entity-protected.h  \
    hippo/hippo-myspace.h	\
    hippo/hippo-person.h        \
    hippo/hippo-platform.h      \
    hippo/hippo-post.h

SOURCES= \
    hippo/hippo-basics.c        \
    hippo/hippo-chat-room.c     \
    hippo/hippo-connection.c    \
    hippo/hippo-cookies.c       \
    hippo/hippo-data-cache.c    \
    hippo/hippo-entity.c        \
    hippo/hippo-myspace.c	\
    hippo/hippo-person.c        \
    hippo/hippo-platform.c      \
    hippo/hippo-post.c

OBJECTS= \
    hippo/hippo-basics.o     \
    hippo/hippo-chat-room.o  \
    hippo/hippo-connection.o \
    hippo/hippo-cookies.o    \
    hippo/hippo-data-cache.o \
    hippo/hippo-entity.o     \
    hippo/hippo-myspace.o    \
    hippo/hippo-person.o     \
    hippo/hippo-platform.o   \
    hippo/hippo-post.o

CFLAGS=-Wall -Wdeclaration-after-statement -Wmissing-prototypes -Wchar-subscripts -Wmissing-declarations -Wnested-externs -Wpointer-arith -Wcast-align -Wfloat-equal -Wsign-compare -g -ggdb -O `pkg-config --cflags gobject-2.0 gthread-2.0 loudmouth-1.0` -I.

all:	libhippo.a

%.o: %.c $(HEADERS)
	$(CC) -c -o $@ $< $(CFLAGS)

libhippo.a: $(OBJECTS)
	ar rcs libhippo.a $(OBJECTS)

clean:
	/bin/rm -f $(OBJECTS) testhippo libhippo.a
