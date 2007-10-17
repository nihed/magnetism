include $(top_srcdir)/Makefile-include.am

bin_SCRIPTS = bigboard

noinst_SCRIPTS = bigboard-uninst 

bigboard: bigboard.in Makefile
	sed \
	    -e s!\@PYTHON\@!@PYTHON@!g \
	    -e s!\@BB_PYDIR\@!$(mainscriptdir)!g \
	    -e s!\@BB_STOCKDIR\@!$(stocksdir)!g \
	    -e s!\@pyexecdir\@!$(pyexecdir)!g \
	    < $< > $@
	chmod a+x $@

bigboard-uninst: bigboard-uninst.in Makefile
	sed \
	    -e s!\@PYTHON\@!@PYTHON@!g \
	    -e s!\@BB_TOPSRCDIR\@!$(top_srcdir)!g \
	    -e s!\@BB_PYDIR\@!$(top_srcdir)/!g \
	    -e s!\@BB_STOCKDIR\@!$(srcdir)/bigboard/stocks!g \
	    < $< > $@
	chmod a+x $@

EXTRA_DIST = bigboard.in bigboard-uninst.in

CLEANFILES = bigboard bigboard-uninst
