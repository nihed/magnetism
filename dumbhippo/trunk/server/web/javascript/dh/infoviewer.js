dojo.provide('dh.infoviewer');
dojo.require('dh.util');

dh.infoviewer.onMouseOver = function(e) {
    if (!e) e = window.event;
	var infoDiv = document.getElementById("dhEntity" + this.dhImageId);
    var width = window.innerWidth ? window.innerWidth : document.body.offsetWidth;
    var height = window.innerHeight? window.innerHeight : document.body.offsetHeight;
    var xOffset = window.pageXOffset ? window.pageXOffset : document.body.scrollLeft;
	var yOffset = window.pageYOffset ? window.pageYOffset : document.body.scrollTop;
	if (e.clientX + 350 > width) {
  	    infoDiv.style.left = (xOffset + width - 350) + "px"; 
  	} else {
  	    infoDiv.style.left = (xOffset + e.clientX) + "px"; 
  	}
  	    
	infoDiv.style.top = (yOffset + e.clientY) + "px"
	
	infoDiv.style.display = "block";				
}

dh.infoviewer.onMouseOut = function(e) {
	var infoDiv = document.getElementById("dhEntity" + this.dhImageId);
	infoDiv.style.display = "none";
}