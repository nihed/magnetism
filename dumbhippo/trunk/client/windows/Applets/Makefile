all: *.xml *.css *.png *.js *.gif images
    -mkdir $(OUTDIR)\applets
    copy add.png $(OUTDIR)\applets    
    copy blue*arrow.gif $(OUTDIR)\applets
    copy bubble.css $(OUTDIR)\applets
    copy bubble.js $(OUTDIR)\applets
    copy bubbleList.css $(OUTDIR)\applets	
    copy bubbleList.js $(OUTDIR)\applets
    copy bubbleList.xml $(OUTDIR)\applets
    copy chaticon.gif $(OUTDIR)\applets    
    copy clientstyle.xml $(OUTDIR)\applets
    copy bubclose.png $(OUTDIR)\applets
    copy bubclose_purple.png $(OUTDIR)\applets    
    copy bubedge_*.png $(OUTDIR)\applets
    copy bubgroupupdate.gif $(OUTDIR)\applets
    copy bublinkswarm.gif $(OUTDIR)\applets
    copy dh.css $(OUTDIR)\applets	    
    copy dh.js $(OUTDIR)\applets
    copy clientstyle.xml $(OUTDIR)\applets
    copy flickr.js $(OUTDIR)\applets	
    copy gray*arrow.gif $(OUTDIR)\applets
    copy groupChat.png $(OUTDIR)\applets
    copy ignoreicon.png $(OUTDIR)\applets    
    copy linkswarm.gif $(OUTDIR)\applets
    copy ls_bubshadow.png $(OUTDIR)\applets
    copy lsorange_*.png $(OUTDIR)\applets
    copy lspurple_*.png $(OUTDIR)\applets    
    copy lswhite_*.png $(OUTDIR)\applets
    copy menu.css $(OUTDIR)\applets	
    copy menu.js $(OUTDIR)\applets
    copy menu.xml $(OUTDIR)\applets
    copy notification.css $(OUTDIR)\applets	
    copy notification.js $(OUTDIR)\applets
    copy notification.xml $(OUTDIR)\applets
    copy wbubcnr_*.png $(OUTDIR)\applets


## Also edit Linux Makefile.am if changing this
IMGDIR=..\..\common\images
images:
	-mkdir $(OUTDIR)\images
	copy $(IMGDIR)\add_icon.png $(OUTDIR)\images
	copy $(IMGDIR)\arrow.png $(OUTDIR)\images
	copy $(IMGDIR)\arrow2.png $(OUTDIR)\images
	copy $(IMGDIR)\bar_middle.png $(OUTDIR)\images
	copy $(IMGDIR)\bar_pipe.png $(OUTDIR)\images
	copy $(IMGDIR)\bar_x2.png $(OUTDIR)\images
	copy $(IMGDIR)\bar_x.png $(OUTDIR)\images
	copy $(IMGDIR)\blue_x.png $(OUTDIR)\images
	copy $(IMGDIR)\chat.png $(OUTDIR)\images
	copy $(IMGDIR)\closetip.png $(OUTDIR)\images
	copy $(IMGDIR)\expandtip.png $(OUTDIR)\images
	copy $(IMGDIR)\find.png $(OUTDIR)\images
	copy $(IMGDIR)\home2.png $(OUTDIR)\images
	copy $(IMGDIR)\home.png $(OUTDIR)\images
	copy $(IMGDIR)\hush2.png $(OUTDIR)\images
	copy $(IMGDIR)\hush.png $(OUTDIR)\images
	copy $(IMGDIR)\lid.png $(OUTDIR)\images
	copy $(IMGDIR)\lock_icon.png $(OUTDIR)\images
	copy $(IMGDIR)\mugshotstacker.png $(OUTDIR)\images
	copy $(IMGDIR)\noart.png $(OUTDIR)\images
	copy $(IMGDIR)\nophoto.png $(OUTDIR)\images
	copy $(IMGDIR)\photo_icon.png $(OUTDIR)\images
	copy $(IMGDIR)\quiphate_icon.png $(OUTDIR)\images
	copy $(IMGDIR)\quiplove_icon.png $(OUTDIR)\images
	copy $(IMGDIR)\search_x.png $(OUTDIR)\images
	copy $(IMGDIR)\unhush2.png $(OUTDIR)\images
	copy $(IMGDIR)\unhush.png $(OUTDIR)\images

clean:
	-deltree $(OUTDIR)\applets
	-deltree $(OUTDIR)\images
