DIST_SUBDIRS = pixman src test doc
SUBDIRS = pixman src doc

.PHONY: doc test retest recheck check-valgrind
# We have some generated header files, so we really need to build the
# library before we can build the docs
doc: all
	cd doc && $(MAKE) $(AM_MAKEFLAGS) doc
test: all
	cd test && $(MAKE) $(AM_MAKEFLAGS) test
retest: all
	cd test && $(MAKE) $(AM_MAKEFLAGS) retest
recheck: all
	cd test && $(MAKE) $(AM_MAKEFLAGS) recheck
check-valgrind: all
	cd test && $(MAKE) $(AM_MAKEFLAGS) check-valgrind

# libpng is required for our test programs
if CAIRO_HAS_PNG_FUNCTIONS
SUBDIRS += test
endif

EXTRA_DIST = \
	BUGS \
	CODING_STYLE \
	COPYING \
	COPYING-LGPL-2.1 \
	COPYING-MPL-1.1 \
	PORTING_GUIDE \
	RELEASING \
	ROADMAP \
	TODO
DISTCLEANFILES = config.cache
MAINTAINERCLEANFILES = \
	$(srcdir)/aclocal.m4 \
	$(srcdir)/autoscan.log \
	$(srcdir)/compile \
	$(srcdir)/config.guess \
	$(srcdir)/config.h.in \
	$(srcdir)/config.sub \
	$(srcdir)/configure.scan \
	$(srcdir)/depcomp \
	$(srcdir)/install-sh \
	$(srcdir)/ltmain.sh \
	$(srcdir)/missing \
	$(srcdir)/mkinstalldirs \
	`find "$(srcdir)" -type f -name Makefile.in -print`

DISTCHECK_CONFIGURE_FLAGS=--enable-gtk-doc

include $(srcdir)/ChangeLog.mk

# Some custom targets to make it easier to release things.
# Use either:
#		make release-check
# or		make release-publish

RELEASE_OR_SNAPSHOT = $$(if test "x$(CAIRO_VERSION_MINOR)" = "x$$(echo "$(CAIRO_VERSION_MINOR)/2*2" | bc)" ; then echo release; else echo snapshot; fi)
RELEASE_UPLOAD_HOST =   cairographics.org
RELEASE_UPLOAD_BASE =	/srv/cairo.freedesktop.org/www
RELEASE_UPLOAD_DIR =	$(RELEASE_UPLOAD_BASE)/$(RELEASE_OR_SNAPSHOT)s
RELEASE_URL_BASE = 	http://cairographics.org/$(RELEASE_OR_SNAPSHOT)s
RELEASE_ANNOUNCE_LIST = cairo-announce@cairographics.org (and CC gnome-announce-list@gnome.org)

MANUAL_DATED =		cairo-manual-`date +%Y%m%d`
MANUAL_TAR_FILE = 	$(MANUAL_DATED).tar.gz
MANUAL_UPLOAD_DIR =	$(RELEASE_UPLOAD_BASE)

tar_file = $(PACKAGE)-$(VERSION).tar.gz
sha1_file = $(tar_file).sha1
gpg_file = $(sha1_file).asc

$(sha1_file): $(tar_file)
	sha1sum $^ > $@

$(gpg_file): $(sha1_file)
	@echo "Please enter your GPG password to sign the checksum."
	gpg --armor --sign $^ 

# Win32 package zipfiles
runtime_zip_file = /tmp/$(PACKAGE)-$(VERSION).zip
developer_zip_file = /tmp/$(PACKAGE)-dev-$(VERSION).zip

$(runtime_zip_file): install
	-$(RM) $@
	cd $(prefix); \
	zip $@ bin/libcairo-$(LT_CURRENT_MINUS_AGE).dll

$(developer_zip_file): install
	-$(RM) $@
	cd $(prefix); \
	zip -r $@ include/cairo lib/libcairo.dll.a lib/cairo.lib lib/pkgconfig/cairo.pc lib/pkgconfig/cairo-*.pc share/gtk-doc/html/cairo

zips: $(runtime_zip_file) $(developer_zip_file)

release-verify-soname-major:
	@echo -n "Checking that the cairo soname major number is 2..."
	@test "$(LT_CURRENT_MINUS_AGE)" = "2" \
		|| (echo "Ouch." && echo "The soname major number is $(LT_CURRENT_MINUS_AGE) instead of 2." \
		&& echo "The libtool shared library version numbers in configure.in must be incremented properly." \
		&& false)
	@echo "Good."

release-verify-even-micro:
	@echo -n "Checking that $(VERSION) has an even micro component..."
	@test "$(CAIRO_VERSION_MICRO)" = "`echo $(CAIRO_VERSION_MICRO)/2*2 | bc`" \
		|| (echo "Ouch." && echo "The version micro component '$(CAIRO_VERSION_MICRO)' is not an even number." \
		&& echo "The version in configure.in must be incremented before a new release." \
		&& false)
	@echo "Good."

release-verify-newer:
	@echo -n "Checking that no $(VERSION) release already exists..."
	@ssh $(RELEASE_UPLOAD_HOST) test ! -e $(RELEASE_UPLOAD_DIR)/$(tar_file) \
		|| (echo "Ouch." && echo "Found: $(RELEASE_UPLOAD_HOST):$(RELEASE_UPLOAD_DIR)/$(tar_file)" \
		&& echo "Are you sure you have an updated checkout?" \
		&& echo "This should never happen." \
		&& false)
	@echo "Good."

release-remove-old:
	$(RM) $(tar_file) $(sha1_file) $(gpg_file)

# Maybe it's just my system, but somehow group sticky bits keep
# getting set and this causes failures in un-tarring on some systems.
# Until I figure out where the sticky bit is coming from, just clean
# these up before building a release.
release-cleanup-group-sticky:
	find . -type f | xargs chmod g-s

release-check: release-verify-soname-major release-verify-even-micro release-verify-newer release-remove-old release-cleanup-group-sticky distcheck

release-upload: release-check $(tar_file) $(sha1_file) $(gpg_file)
	mkdir -p releases
	scp $(tar_file) $(sha1_file) $(gpg_file) $(RELEASE_UPLOAD_HOST):$(RELEASE_UPLOAD_DIR)
	mv $(tar_file) $(sha1_file) $(gpg_file) releases
	ssh $(RELEASE_UPLOAD_HOST) "rm -f $(RELEASE_UPLOAD_DIR)/LATEST-$(PACKAGE)-[0-9]* && ln -s $(tar_file) $(RELEASE_UPLOAD_DIR)/LATEST-$(PACKAGE)-$(VERSION)"
	git tag -s $(CAIRO_VERSION_MAJOR).$(CAIRO_VERSION_MINOR).$(CAIRO_VERSION_MICRO)

release-publish-message: releases/$(sha1_file)
	@echo "Pleas push the new tag with a command such as:"
	@echo ""
	@echo "	git push master $(VERSION)"
	@echo ""
	@echo "Please send an announcement to $(RELEASE_ANNOUNCE_LIST)"
	@echo "including the following:"
	@echo ""
	@echo "Subject: $(PACKAGE) $(RELEASE_OR_SNAPSHOT) $(VERSION) now available"
	@echo ""
	@echo "============================== CUT HERE =============================="
	@echo "A new $(PACKAGE) $(RELEASE_OR_SNAPSHOT) $(VERSION) is now available from:"
	@echo ""
	@echo "	$(RELEASE_URL_BASE)/$(tar_file)"
	@echo ""
	@echo "    which can be verified with:"
	@echo ""
	@echo "	$(RELEASE_URL_BASE)/$(sha1_file)"
	@echo -n "	"
	@cat releases/$(sha1_file)
	@echo ""
	@echo "	$(RELEASE_URL_BASE)/$(gpg_file)"
	@echo "	(signed by `getent passwd "$$USER" | cut -d: -f 5 | cut -d, -f 1`)"
	@echo "  Additionally, a git clone of the source tree:"
	@echo ""
	@echo "	git clone git://git.cairographics.org/git/cairo"
	@echo ""
	@echo "    will include a signed $(VERSION) tag which points to a commit named:"
	@echo "	`git cat-file tag $(VERSION) | grep ^object | sed -e 's,object ,,'`"
	@echo ""
	@echo "    which can be verified with:"
	@echo "	git verify-tag $(VERSION)"
	@echo ""
	@echo "    and can be checked out with a command such as:"
	@echo "	git checkout -b build $(VERSION)"
	@echo ""
	@echo "============================== CUT HERE =============================="
	@echo "Also, please add the new entries from the NEWS file as well as the detailed"
	@echo "change log sent to the cairo-announce list when the tag is pushed."
	@echo ""
	@echo "The same NEWS entries should be posted on the cairographics.org wiki as well."
	@echo ""
	@echo "Last but not least, do not forget to bump up the micro"
	@echo "version component to the next (odd) number and commit."

release-publish: release-upload release-publish-message

doc-publish: doc
	cp -a doc/public/html $(MANUAL_DATED)
	tar czf $(MANUAL_TAR_FILE) $(MANUAL_DATED)
	scp $(MANUAL_TAR_FILE) $(RELEASE_UPLOAD_HOST):$(MANUAL_UPLOAD_DIR)
	ssh $(RELEASE_UPLOAD_HOST) "cd $(MANUAL_UPLOAD_DIR) && tar xzf $(MANUAL_TAR_FILE) && rm -f manual && ln -s $(MANUAL_DATED) manual && ln -sf $(MANUAL_TAR_FILE) cairo-manual.tar.gz"

.PHONY: release-verify-even-micro release-verify-newer release-remove-old release-cleanup-group-sticky release-check release-upload release-publish docs-publish
