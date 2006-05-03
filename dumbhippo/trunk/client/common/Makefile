HEADERS= \
    hippo/hippo-connection.h    \
    hippo/hippo-cookies.h       \
    hippo/hippo-platform.h

SOURCES= \
    hippo/hippo-connection.c \
    hippo/hippo-cookies.c    \
    hippo/hippo-platform.c

OBJECTS= \
    hippo/hippo-connection.o \
    hippo/hippo-cookies.o    \
    hippo/hippo-platform.o

CFLAGS=-Wall -g -O2 `pkg-config --cflags gobject-2.0 loudmouth-1.0` -I.

all:	testhippo

%.o: %.c $(HEADERS)
	$(CC) -c -o $@ $< $(CFLAGS)

libhippo.a: $(OBJECTS)
	ar rcs libhippo.a $(OBJECTS)

testhippo: hippo/testhippo.c libhippo.a
	$(CC) -o $@ hippo/testhippo.c libhippo.a `pkg-config --libs gobject-2.0 loudmouth-1.0` $(CFLAGS)

clean:
	/bin/rm -f $(OBJECTS) testhippo libhippo.a
