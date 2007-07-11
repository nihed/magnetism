include $(top_srcdir)/Makefile-include.am

bin_SCRIPTS = bigboard

noinst_SCRIPTS = bigboard-uninst 

bigboard: bigboard.in
	sed \
	    -e s!\@PYTHON\@!@PYTHON@!g \
	    -e s!\@BB_PYDIR\@!$(mainscriptdir)!g \
	    -e s!\@BB_STOCKDIR\@!$(stocksdir)!g \
	    < $< > $@
	chmod a+x $@

bigboard-uninst: bigboard-uninst.in
	sed \
	    -e s!\@PYTHON\@!@PYTHON@!g \
	    -e s!\@BB_TOPSRCDIR\@!$(top_srcdir)!g \
	    -e s!\@BB_PYDIR\@!$(top_srcdir)/!g \
	    -e s!\@BB_STOCKDIR\@!$(srcdir)/bigboard/stocks!g \
	    < $< > $@
	chmod a+x $@

gconf-online-sync: gconf-online-sync.in
	sed \
	    -e s!\@PYTHON\@!@PYTHON@!g \
	    -e s!\@BB_PYDIR\@!$(mainscriptdir)!g \
	    -e s!\@BB_STOCKDIR\@!$(stocksdir)!g \
	    < $< > $@
	chmod a+x $@

EXTRA_DIST = bigboard.in bigboard-uninst.in gconf-online-sync.in

CLEANFILES = bigboard bigboard-uninst gconf-online-sync
