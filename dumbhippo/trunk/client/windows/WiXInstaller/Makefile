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
    
SYMBOL_FILES =							\
	$(OUTDIR)\HippoUI.sym				\
	$(OUTDIR)\HippoUtil.sym				\
	$(OUTDIR)\libcairo-2.sym			\
	$(OUTDIR)\libglib-2.0-0.sym			\
	$(OUTDIR)\libgmodule-2.0-0.sym		\
	$(OUTDIR)\libgobject-2.0-0.sym		\
	$(OUTDIR)\libgthread-2.0-0.sym		\
	$(OUTDIR)\libpango-1.0-0.sym		\
	$(OUTDIR)\libpangocairo-1.0-0.sym	\
	$(OUTDIR)\libpangowin32-1.0-0.sym	\
	$(OUTDIR)\loudmouth-1.sym
	

all: dirs $(OUTDIR)\Mugshot.msi $(OUTDIR)\Mugshot-symbols.zip

{.}.wxs{$(INTDIR)}.wixobj:
    candle -out $@ $<

$(OUTDIR)\Mugshot.msi: $(OBJECTS) $(INPUTS)
    light -b $(OUTDIR) -out $@ $(OBJECTS)
    
$(OUTDIR)\Mugshot-symbols.zip: $(SYMBOL_FILES)
	-del $@
	..\util\zip -j $@ $(SYMBOL_FILES) 

dirs:
    -mkdir $(INTDIR)
    -mkdir $(OUTDIR)

clean:
    -del $(OUTDIR)\Mugshot.msi
    -del $(INTDIR)\*.wixobj
