PATH=$(PATH);C:\Program Files\Windows Installer XML\bin

.SUFFIXES : .wxs

OBJECTS	= \
    $(INTDIR)\Components.wixobj		    \
    $(INTDIR)\DumbHippo.wixobj		    \
    $(INTDIR)\ErrorDialog.wixobj	    \
    $(INTDIR)\FatalErrorPage.wixobj	    \
    $(INTDIR)\FilesInUseDialog.wixobj	    \
    $(INTDIR)\NotAdminPage.wixobj	    \
    $(INTDIR)\ProgressPage.wixobj	    \
    $(INTDIR)\UpgradeProgressPage.wixobj    \
    $(INTDIR)\WelcomePage.wixobj
    
INPUTS =		\
    fog.jpg		\
    license.rtf		\
    $(OUTDIR)\*.dll	\
    $(OUTDIR)\*.exe	\
    $(OUTDIR)\applets\*   

all: dirs $(OUTDIR)\Mugshot.msi

{.}.wxs{$(INTDIR)}.wixobj:
    candle -out $@ $<

$(OUTDIR)\Mugshot.msi: $(OBJECTS) $(INPUTS)
    light -b $(OUTDIR) -out $@ $(OBJECTS)

dirs:
    -mkdir $(INTDIR)
    -mkdir $(OUTDIR)

clean:
    -del $(OUTDIR)\Mugshot.msi
    -del $(INTDIR)\*.wixobj
