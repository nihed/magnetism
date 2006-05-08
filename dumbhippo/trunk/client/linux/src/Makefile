CFLAGS = -I ../../common `pkg-config --cflags loudmouth-1.0 gobject-2.0 gthread-2.0` -Wall -Wdeclaration-after-statement -Wmissing-prototypes -Wchar-subscripts -Wmissing-declarations -Wnested-externs -Wpointer-arith -Wcast-align -Wfloat-equal -Wsign-compare -g -ggdb
LIBHIPPO = ../../common/libhippo.a
LIBS = `pkg-config --libs loudmouth-1.0 gobject-2.0 gthread-2.0` $(LIBHIPPO)

OBJECTS = 			\
	hippo-cookies-linux.o	\
	hippo-platform-impl.o	\
	main.o

hippo-ui: $(OBJECTS)
	gcc -o $@ $(OBJECTS) $(LIBS)

clean:
	rm -rf $(OBJECTS) hippo-ui


