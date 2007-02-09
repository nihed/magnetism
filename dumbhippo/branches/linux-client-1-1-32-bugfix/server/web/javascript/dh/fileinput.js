dojo.provide("dh.fileinput");
dojo.require("dh.util");
dojo.require("dh.event");

// Firefox doesn't seem to activate the form a file input is 
// inside on pressing Enter.
// IE on the other hand does do it on pressing Enter.
// Right now all this class does is add the Enter handler
// and the activate() convenience method, and an onchange 
// handler that activates

dh.fileinput.Entry = function(entryNode)
{
	// for closures
	var me = this;
	
	this.elem = entryNode;
	this.styledFileUpload = null;
	
	this.activate = function() {
		var e = this.elem.parentNode;
		while (e && e.nodeName.toLowerCase() != 'form')
			e = e.parentNode;
		if (!e)
			return;
		e.submit();
	}
	
	this.elem.onchange = function() {
		me.think();
		me.activate();
		return false;
	}

	this.think = function() {
		var img = document.createElement("img");
		img.src = dhImageRoot2 + "feedspinner.gif";
		this.elem.parentNode.appendChild(img);
		this.elem.style.display = "none";
		if (this.styledFileUpload != null) {	    
		    img.className = "dh-styled-file-upload-think";
		    this.styledFileUpload.style.display = "none";	
		} else {
		    img.className = "dh-file-upload-think";
		}	  
	}
   
	// with the onchange, this isn't needed
	/*
	this.elem.onkeydown = function(ev) {
		var key = dh.event.getKeyCode(ev);
		if (key == ENTER) {
			dh.event.cancel(ev);
			me.activate();
			return false;
		}
	}
	*/
	
	this.setBrowseButtonDiv = function(value) {
	    // the passed in value can be null
		this.styledFileUpload = value;
	    
	    // this image mapping worked for IE, but not for Firefox;
	    // it could have been used along with 
	    // this.elem.style.visibility = "hidden"
	    // and would not have required any extra css
	    // this.styledFileUpload.onclick = function() {
        //   me.elem.click();
        // } 
	}
}
