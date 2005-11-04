all: *.xml *.css
	-mkdir $(OUTDIR)\applets
	copy notification.xml $(OUTDIR)\applets
	copy clientstyle.xml $(OUTDIR)\applets
	copy notification.js $(OUTDIR)\applets
	copy notification.css $(OUTDIR)\applets	

clean:
	deltree $(OUTDIR)\applets
