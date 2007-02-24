dojo.provide('dh.tooltip');
dojo.require('dh.util');

dh.tooltip.Tooltip = function(container, source, tip) {
	var me = this;

	this._container = container;
	this._source = source;
	this._tip = tip;
	
    tip.style.visibility = "hidden";

	this._alignBottom = false;	
	this._yOffset = 50;
	this._closeOnSourceOut = false;
	
	this.setYOffset = function (offset) {
		this._yOffset = offset;
	}
	
	this.setAlignBottom = function(alignBottom) {
		this._alignBottom = alignBottom;
	}
	
	this.setCloseOnClick = function (closeOnClick) {
		if (!closeOnClick)
			return;
		this._tip.onclick = function (e) {
			me._hide();	
		}
	}
	
	this.setCloseOnSourceOut = function(closeOnSourceOut) {
		me._closeOnSourceOut = closeOnSourceOut;
	}
	
	this._hide = function() {
		me._tip.style.visibility = "hidden";
	}
	
	this._source.onmouseover = function (e) {
		if (!e) e = window.event;
		
		// we don't need to do anything for the tooltip if it is already visible			
	    if (me._tip.style.visibility == "visible")
			return;		
			        
	    var width = window.innerWidth ? window.innerWidth : document.body.offsetWidth;
    	var xOffset = window.pageXOffset ? window.pageXOffset : document.body.scrollLeft;
		var pageOuterPos = dh.util.getBodyPosition(me._container);
		var sourcePos = dh.util.getBodyPosition(me._source);	
	
		if (sourcePos.x + 15 + me._tip.offsetWidth > width) {
 	    	me._tip.style.left = (sourcePos.x - me._tip.offsetWidth + 45 - pageOuterPos.x) + "px"; 
	 	} else {
 		    me._tip.style.left = (sourcePos.x - pageOuterPos.x + 15) + "px";
	 	}
 	    var offset = me._alignBottom ? me._source.offsetHeight : me._yOffset;
 	    
		me._tip.style.top = (sourcePos.y - pageOuterPos.y + offset) + "px";	
		
	    me._tip.style.visibility = "visible";
		
		return;
	}

	this._source.onmouseout = function(e) {
		if (!e) e = window.event;
    	var relTarget = e.relatedTarget || e.toElement;	
		if (me._closeOnSourceOut && !dh.util.isDescendant(me._source, relTarget))
			me._hide()
	    if (!dh.util.isDescendant(me._tip, relTarget))
		    me._hide();
	}
	
	this._tip.onmouseout = function(e) {
		if (!e) e = window.event;
    	var relTarget = e.relatedTarget || e.toElement;		
	    if (!dh.util.isDescendant(me._source, relTarget) && !dh.util.isDescendant(me._tip, relTarget))
		    me._hide();
	}
}
