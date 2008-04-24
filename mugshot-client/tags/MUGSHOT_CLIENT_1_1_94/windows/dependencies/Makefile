SOURCEDIR=..\..\..\mugshot-dependencies\windows-binary

all:
	-mkdir $(OUTDIR)
	copy $(SOURCEDIR)\COPYING.txt $(OUTDIR)\COPYING-LIBS.txt
	copy $(SOURCEDIR)\charset.dll $(OUTDIR)
	copy $(SOURCEDIR)\iconv.dll $(OUTDIR)
	copy $(SOURCEDIR)\intl.dll $(OUTDIR)
	copy $(SOURCEDIR)\jpeg62.dll $(OUTDIR)
	copy $(SOURCEDIR)\libpng13.dll $(OUTDIR)
	copy $(SOURCEDIR)\zlib1.dll $(OUTDIR)
	-mkdir $(OUTDIR)\etc
	-mkdir $(OUTDIR)\etc\pango
	copy $(SOURCEDIR)\pango\etc\pango\pango.aliases $(OUTDIR)\etc\pango

clean:
	-del $(OUTDIR)\COPYING-LIBS.txt
	-del $(OUTDIR)\charset.dll
	-del $(OUTDIR)\iconv.dll
	-del $(OUTDIR)\intl.dll
	-del $(OUTDIR)\jpeg62.dll
	-del $(OUTDIR)\libpng13.dll
	-del $(OUTDIR)\zlib1.dll
	-rmdir /q /s $(OUTDIR)\etc
