all:
	-mkdir $(OUTDIR)
	copy COPYING.txt $(OUTDIR)\COPYING-LIBS.txt
	copy charset.dll $(OUTDIR)
	copy iconv.dll $(OUTDIR)
	copy intl.dll $(OUTDIR)
	copy jpeg62.dll $(OUTDIR)
	copy libpng13.dll $(OUTDIR)
	copy zlib1.dll $(OUTDIR)
	-mkdir $(OUTDIR)\etc
	-mkdir $(OUTDIR)\etc\pango
	copy pango\etc\pango\pango.aliases $(OUTDIR)\etc\pango

clean:
	-del $(OUTDIR)\COPYING-LIBS.txt
	-del $(OUTDIR)\charset.dll
	-del $(OUTDIR)\iconv.dll
	-del $(OUTDIR)\intl.dll
	-del $(OUTDIR)\jpeg62.dll
	-del $(OUTDIR)\libpng13.dll
	-del $(OUTDIR)\zlib1.dll
	-rmdir /q /s $(OUTDIR)\etc
