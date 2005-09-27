all:
	-mkdir $(OUTDIR)
	copy libiconv\bin\charset.dll $(OUTDIR)
	copy libiconv\bin\iconv.dll $(OUTDIR)
	copy gettext\bin\asprintf.dll $(OUTDIR)
	copy gettext\bin\intl.dll $(OUTDIR)
	copy glib\bin\libglib-2.0-0.dll $(OUTDIR)

clean:
	del $(OUTDIR)\charset.dll
	del $(OUTDIR)\iconv.dll
	del $(OUTDIR)\asprintf.dll
	del $(OUTDIR)\intl.dll
	del $(OUTDIR)\libglib-2.0-0.dll

