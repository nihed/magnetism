dojo.provide("dh.popup");
dojo.require("dh.util");
dojo.require("dojo.style");
dojo.require("dh.event");

// hash from popupId to bool
dh.popup.showing = {};

dh.popup.isShowing = function(popupId) {
	return !!dh.popup.showing[popupId];
}

dh.popup.show = function(popupId, aboveNode) {
	if (!popupId)
		throw Error("no popup id");

	if (!aboveNode)
		throw Error("no aboveNode");

	if (dh.popup.showing[popupId])
		return;
	
	// we assume that "aboveNode" is positioned, or at least 
	// that we want to be relative to its positioned parent
	
	var popup = document.getElementById(popupId);
	if (!popup)
		throw Error("popupId nonexistent " + popupId);

	// reparent so the popup is relative to first possible
	// parent
	var e = aboveNode;
	while (e.nodeName.toUpperCase() != 'DIV') {
		e = e.parentNode;
	}
	
	if (popup.parentNode != e) {
		popup.parentNode.removeChild(popup);
		e.appendChild(popup);
	}
	
	/* This isn't precision, just "kind of next to"
	 * IE considers a scrollbar click as mouse input ergo it doesn't allow people to scroll horizontally 
	 * and see the rest of the popup under 800x600
	 */
	popup.style.left = "0px";
	popup.style.bottom = "15px";
	popup.style.display = 'block';
	
	document.body.onkeydown = function(ev) {
		if (dh.event.getKeyCode(ev) == ESC) {
			dh.popup.hide(popupId);
			dh.event.cancel(ev);
			return false;
		}
		return true;
	}
	
	document.body.onmousedown = function(ev) {
		var target = dh.event.getNode(ev);
		if (!target) {
			alert("No event node?");
			return;
		}
		var e = target;
		while (e && e != popup) {
			e = e.parentNode;
		}
		if (!e) {
			// we weren't a child of the popup
			dh.popup.hide(popupId);
			// don't activate something else
			dh.event.cancel(ev);
			return false;
		}
		return true;
	}
	
	dh.popup.showing[popupId] = true;
}

dh.popup.hide = function(popupId) {
	if (!popupId)
		throw Error("no popup id");
		
	if (!dh.popup.showing[popupId])
		return;

	var popup = document.getElementById(popupId);
	popup.style.display = 'none';
	document.body.onmousedown = null;
	document.body.onkeydown = null;
	dh.popup.showing[popupId] = false;
}
