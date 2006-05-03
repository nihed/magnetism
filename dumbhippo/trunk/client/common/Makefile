HEADERS= \
    hippo/hippo-connection.h \
    hippo/hippo-cookies.h

SOURCES= \
    hippo/hippo-connection.c \
    hippo/hippo-cookies.c

OBJECTS= \
    hippo/hippo-connection.o \
    hippo/hippo-cookies.o

CFLAGS=-Wall -g -O2

all:	testhippo

%.o: %.c $(HEADERS)
	$(CC) -c -o $@ $< $(CFLAGS) `pkg-config --cflags gobject-2.0 loudmouth-1.0`

libhippo.a: $(OBJECTS)
	ar rcs libhippo.a $(OBJECTS)

testhippo: hippo/testhippo.c libhippo.a
	$(CC) -I. -o $@ hippo/testhippo.c libhippo.a `pkg-config --cflags --libs gobject-2.0 loudmouth-1.0`

clean:
	/bin/rm -f $(OBJECTS) testhippo libhippo.a
