CFLAGS = -I ../../common -I .. `pkg-config --cflags loudmouth-1.0 gobject-2.0 gthread-2.0 gtk+-2.0` -Wall -Wdeclaration-after-statement -Wmissing-prototypes -Wchar-subscripts -Wmissing-declarations -Wnested-externs -Wpointer-arith -Wcast-align -Wfloat-equal -Wsign-compare -g -ggdb
COMMONDIR=../../common
GTKDIR=../gtk
LIBHIPPO = $(COMMONDIR)/libhippo.a
LIBS = `pkg-config --libs loudmouth-1.0 gobject-2.0 gthread-2.0 gtk+-2.0` $(LIBHIPPO)

## help make find our gtk cut-and-paste stuff
vpath gtk%.h $(GTKDIR)
vpath gtk%.c $(GTKDIR)

OBJECTS = 	         		\
    gtkstatusicon.o         \
    gtktrayicon-x11.o       \
	hippo-cookies-linux.o	\
	hippo-platform-impl.o	\
	hippo-status-icon.o	    \
	main.o

mugshot: $(OBJECTS) $(LIBHIPPO)
	gcc -o $@ $(OBJECTS) $(LIBS)

## so not right at all
HEADERS := $(wildcard *.h) $(wildcard $(GTKDIR)/*.h) $(wildcard $(COMMONDIR)/hippo/*.h)
$(OBJECTS): $(HEADERS)

$(LIBHIPPO):
	cd $(COMMONDIR) && $(MAKE)

.PHONY: clean
clean:
	rm -rf $(OBJECTS) mugshot


