all: clean
	xcopy /s /e /i ..\..\common\firefox\extension\* $(OUTDIR)\firefox
	mkdir $(OUTDIR)\firefox\components
	copy $(OUTDIR)\*.xpt $(OUTDIR)\firefox\components
	copy $(OUTDIR)\HippoFirefoxStub.dll $(OUTDIR)\firefox\components

clean:
	-rmdir /q /s $(OUTDIR)\firefox
