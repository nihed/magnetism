dojo.provide('dh.main');
dojo.require('dojo.html');
dojo.require("dh.util")

dhMainInit = function() {
	var blockHeaders = dojo.html.getElementsByClass('dh-stacker-block');
	var i = 0;
	while (i < blockHeaders.length) {
	   var blockIcons = dojo.html.getElementsByClass('dh-stacker-block-icon', blockHeaders[i]);	
       if (blockIcons.length != 1)
	       throw "stacker block should contain a single icon element";

	   var blockTitles = dojo.html.getElementsByClass('dh-stacker-block-title', blockHeaders[i]);	
       if (blockTitles.length != 1)
	       throw "stacker block should contain a single title element";

	   var blockDetails = dojo.html.getElementsByClass('dh-stacker-block-right-container-inner', blockHeaders[i]);	
       if (blockDetails.length != 1)
	       throw "stacker block should contain a single details element";   			
	       				       			
	   // 36 is for the icon on the left    				       			
	   var newTitleWidth = blockHeaders[i].offsetWidth * .75 - 36;   
	   	     
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
       
       blockIcons[0].style.display = "block";  			
	   blockTitles[0].style.display = "block"; 	
       blockDetails[0].style.display = "block"; 	    
	   	
       i++;     
	}   
}
