dojo.provide("dh.fileinput");
dojo.require("dh.util");

// Firefox doesn't seem to activate the form a file input is 
// inside on pressing Enter.
// IE on the other hand does do it on pressing Enter.
// Right now all this class does is add the Enter handler
// and the activate() convenience method.

dh.fileinput.Entry = function(entryNode)
{
	// for closures
	var me = this;
	
	this.elem = entryNode;
	
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
	}

	// with the onchange, this isn't needed
	/*
	this.elem.onkeydown = function(ev) {
		var key = dh.util.getKeyCode(ev);
		if (key == ENTER) {
			dh.util.cancelEvent(ev);
			me.activate();
			return false;
		}
	}
	*/
}
