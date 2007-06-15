dojo.provide('dh.main');
dojo.require('dh.html');
dojo.require("dh.util")

dh.main._blockArray = new Array();

dh.main.updateWidth = function() {
	var blockHeaders = dh.html.getElementsByClass('dh-stacker-block');
	var i = 0;
	while (i < blockHeaders.length) {
	   var blockIcons = dh.html.getElementsByClass('dh-stacker-block-icon', blockHeaders[i]);	
       if (blockIcons.length != 1)
	       throw "stacker block should contain a single icon element";

	   var blockTitles = dh.html.getElementsByClass('dh-stacker-block-title', blockHeaders[i]);	
       if (blockTitles.length != 1)
	       throw "stacker block should contain a single title element";

	   var blockDetails = dh.html.getElementsByClass('dh-stacker-block-right-container-inner', blockHeaders[i]);	
       if (blockDetails.length != 1)
	       throw "stacker block should contain a single details element";   			
	       		
	   var availableWidth = blockHeaders[i].offsetWidth - blockIcons[0].offsetWidth - blockDetails[0].offsetWidth - 22; 
	   if (dh.main._blockArray[i] == null) {     		
	       dh.main._blockArray[i] = blockTitles[0].cloneNode(true);
	       dh.util.ellipseNodeWithChildrenPlus(blockTitles[0], availableWidth, null);     		
	   } else {
	       dh.util.ellipseNodeWithChildrenPlus(blockTitles[0], availableWidth, dh.main._blockArray[i]);   
	   }    		
	   	   
       i++;     
	}   
}

dhMainInit = function() {
    dh.main.updateWidth();
}
