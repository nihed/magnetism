SUBDIRS = src

PKGNAME = fedora-imagebuilder

all: subdirs

clean:
	rm -f *.pyc *.pyo *~ *.bak
	for d in $(SUBDIRS); do make -C $$d clean ; done

distclean: clean
	rm -rf dist build
	rm *.tar.gz

subdirs:
	for d in $(SUBDIRS); do make -C $$d; [ $$? = 0 ] || exit 1 ; done

install:
	mkdir -p $(DESTDIR)/usr/bin/
	mkdir -p $(DESTDIR)/usr/libexec
	install -m 755 fedora-imagebuilder.py $(DESTDIR)/usr/bin/fedora-imagebuilder
	for d in $(SUBDIRS); do make  DESTDIR=`cd $(DESTDIR); pwd` -C $$d install; [ $$? = 0 ] || exit 1; done
