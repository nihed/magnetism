all: *.xml *.css *.png *.js
    -mkdir $(OUTDIR)\applets
    copy notification.xml $(OUTDIR)\applets
    copy clientstyle.xml $(OUTDIR)\applets
    copy dh.js $(OUTDIR)\applets
    copy notification.js $(OUTDIR)\applets
    copy flickr.js $(OUTDIR)\applets	
    copy notification.css $(OUTDIR)\applets	
    copy close.png $(OUTDIR)\applets
    copy activeLeft.png $(OUTDIR)\applets
    copy activeRight.png $(OUTDIR)\applets

clean:
	deltree $(OUTDIR)\applets
