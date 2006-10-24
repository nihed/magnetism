dojo.provide('dh.main');
dojo.require('dojo.html');
dojo.require("dh.util")

dhMainInit = function() {
	var blockHeaders = dojo.html.getElementsByClass('dh-stacker-block-header');
	var i = 0;
	while (i < blockHeaders.length) {
	   var blockIcons = dojo.html.getElementsByClass('dh-stacker-block-icon', blockHeaders[i]);	
       if (blockIcons.length != 1)
	       throw "stacker block should contain a single icon element";

	   var blockTitles = dojo.html.getElementsByClass('dh-stacker-block-title', blockHeaders[i]);	
       if (blockTitles.length != 1)
	       throw "stacker block should contain a single title element";

	   var blockDetails = dojo.html.getElementsByClass('dh-stacker-block-right', blockHeaders[i]);	
       if (blockDetails.length != 1)
	       throw "stacker block should contain a single details element";

	   var detailsText = dh.util.getTextFromHtmlNode(blockDetails[0]);	    
	   var newDetailsWidth = dh.util.getTextWidth(detailsText, "Arial, sans", "11px", null, null, "normal") + 2;	       	
	   var oldDetailsWidth = blockDetails[0].offsetWidth;
	   // blockDetails[0].style.width = newDetailsWidth + "px"    			
	       				       			
	   var oldTitleWidth = blockTitles[0].offsetWidth;
	   // 20 is the width of the icon cell and 4 is for the extra cell between 
	   // the left and right sections of the block
	   var newTitleWidth = blockHeaders[i].offsetWidth - 24 - newDetailsWidth;
	   // blockTitles[0].style.width = newTitleWidth + "px" 	   
	   	     
	   var titleText = dh.util.getTextFromHtmlNode(blockTitles[0]);	
	   // this is for figuring out how many characters we can fit     
	   var titleWidth = dh.util.getTextWidth(titleText, "Arial, sans", "14px", null, null, "bold");	   
	   var charToDisplay = Math.floor((titleText.length * newTitleWidth) / titleWidth);  
	   // let's make sure the width of the particular characters we'll display is not more than 
	   // the intended width of the title div 
	   var charWidth = dh.util.getTextWidth(titleText.substring(0, charToDisplay), "Arial, sans", "14px", null, null, "bold");
	   while (charWidth > newTitleWidth) {
	       charToDisplay = Math.floor((charToDisplay * newTitleWidth) / charWidth);
	       var charWidth = dh.util.getTextWidth(titleText.substring(0, charToDisplay), "Arial, sans", "14px", null, null, "bold");     
	   }
	   
       if (charToDisplay < titleText.length) {
           // we know we are truncating anyway, so - 3 is for fitting in the "..."
           dh.util.truncateTextInHtmlNode(blockTitles[0], charToDisplay - 3);
       }
       
       blockIcons[0].style.display = "inline";  			
	   blockTitles[0].style.display = "inline"; 	
       blockDetails[0].style.display = "inline"; 	    
	   	
       i++;     
	}   
}
