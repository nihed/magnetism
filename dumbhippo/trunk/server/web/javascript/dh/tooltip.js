dojo.provide('dh.tooltip');
dojo.require('dh.util');

dh.tooltip.Tooltip = function(container, source, tip) {
	var me = this;

	this._container = container;
	this._source = source;
	this._tip = tip;
	
	tip.style.display = "none";
	
	this._source.onmouseover = function (e) {
		if (!e) e = window.event;
		
		// we don't need to do anything for the tooltip if it is already visible			
		if (me._tip.style.display == "block")
			        
	    var width = window.innerWidth ? window.innerWidth : document.body.offsetWidth;
    	var xOffset = window.pageXOffset ? window.pageXOffset : document.body.scrollLeft;
		var pageOuterPos = dh.util.getBodyPosition(me._container);
		var sourcePos = dh.util.getBodyPosition(me._source);	
	
		me._tip.style.display = "block";
	
		if (sourcePos.x + 15 + me._tip.offsetWidth > width) {
 	    	me._tip.style.left = (sourcePos.x - me._tip.offsetWidth + 45 - pageOuterPos.x) + "px"; 
	 	} else {
 		    me._tip.style.left = (sourcePos.x - pageOuterPos.x + 15) + "px";
	 	}
 	    
		me._tip.style.top = (sourcePos.y - pageOuterPos.y + 50) + "px";	
		return;
	}

	this._source.onmouseout = function(e) {
		if (!e) e = window.event;
    	var relTarget = e.relatedTarget || e.toElement;		
	    if (!dh.util.isDescendant(me._tip, relTarget))
		    me._tip.style.display = "none";
	}
	
	this._tip.onmouseout = function(e) {
		if (!e) e = window.event;
    	var relTarget = e.relatedTarget || e.toElement;		
	    if (!dh.util.isDescendant(me._source, relTarget) && !dh.util.isDescendant(me._tip, relTarget))
		    me._tip.style.display = "none";	
	}
}
