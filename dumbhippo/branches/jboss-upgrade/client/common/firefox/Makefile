MOZDIR=/home/otaylor/src/redhat/BUILD/mozilla
DISTDIR=$(MOZDIR)/dist
IDLDIR=$(DISTDIR)/idl

INCLUDES = 						\
	$(shell pkg-config --cflags glib-2.0 nspr) 	\
	-I include					\
	-I $(DISTDIR)/include/caps			\
	-I $(DISTDIR)/include/dom			\
	-I $(DISTDIR)/include/js			\
	-I $(DISTDIR)/include/necko			\
	-I $(DISTDIR)/include/string			\
	-I $(DISTDIR)/include/xpcom			\
	-I $(DISTDIR)/include/xpconnect

LIBS =							\
	$(shell pkg-config --libs nspr)

CPPFLAGS = -g -Wall -Wno-non-virtual-dtor -O2 $(INCLUDES)

XPIDL=$(DISTDIR)/bin/xpidl -I $(IDLDIR) -I public/

IDLFILES = 				\
	hippoIService.idl		\
	hippoIServiceListener.idl

BUILT_HEADERS = $(IDLFILES:%.idl=include/%.h) 
HEADERS = $(BUILT_HEADERS) src/hippoService.h
TYPELIBS = $(IDLFILES:%.idl=$(COMPONENTDIR)/%.xpt)

SOURCES = 			\
	hippoService.cpp	\
	hippoModule.cpp

OBJECTS = $(SOURCES:%.cpp=bin/%.o)

COMPONENTDIR = extension/components

all: $(TYPELIBS) $(COMPONENTDIR)/libmugshotfirefox.so

$(OBJECTS): $(HEADERS)

bin/%.o: src/%.cpp
	$(CXX) -c $(CPPFLAGS) $(CXXFLAGS) -o $@ $< 

include/%.h: public/%.idl
	$(XPIDL) -m header -e $@ $<

$(COMPONENTDIR)/%.xpt: public/%.idl
	$(XPIDL) -m typelib -e $@ $<

$(COMPONENTDIR)/libmugshotfirefox.so: $(OBJECTS)
	$(CXX) -shared -o $@ $(OBJECTS)

clean:
	rm -f $(COMPONENTDIR)/*
	rm -f include/*
	rm -f bin/*
