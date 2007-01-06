dojo.provide("dh.event");
dojo.require("dh.util");
dojo.require("dh.lang");

// get the node an event happened on - this is always the leaf node,
// so if an event "bubbles" to a parent, the target is still the 
// leaf.
dh.event.getNode = function(ev)
{
 	// this is the W3C model
	if (ev)
		return ev.target;
	
	// this is IE
	if (window.event)
		return window.event.srcElement;
		
	// FIXME we should throw here, but need to audit broken code
	// throw Error("no event to get dom node from");
	return null;
};

// cancel an event. This combines two things; "stop bubbling up to 
// parent nodes" and "do not run the default action such as form submit"
// a false return from the event handler is also needed to prevent 
// the default action, but I believe not to stop bubbling.
dh.event.cancel = function(ev)
{
	if (!ev)
		var ev = window.event;

	// stop the default action such as form submit		
	if (ev.preventDefault)
		ev.preventDefault();
	else
		ev.returnValue = false;
		
	// This stops "bubbling up" from leaf nodes to 
	// parents; stopPropagation() is the W3C model
	// and the cancelBubble member variable is IE.
	// http://www.quirksmode.org/js/events_order.html
	if (ev.stopPropagation)
		ev.stopPropagation();
	else
		ev.cancelBubble = true;		
	
	// FIXME we should throw here, but need to audit broken code; the block expansion code
	// may be an example of something that has this hosed	
	// throw Error("no event to cancel");
	return null;
};

// Define common keycodes - do not put "var" in front of these
// or they'd be in local instead of global scope
TAB = 9;
ESC = 27;
KEYUP = 38;
KEYDN = 40;
ENTER = 13;
SHIFT = 16;
CTRL = 17;
ALT = 18;
CAPS_LOCK = 20;

dh.event.getKeyCode = function(ev)
{
	if (ev)
		return ev.keyCode;
	if (window.event)
		return window.event.keyCode;
		
	// some code relies on this by e.g. connecting the same handler 
	// to a key event and an onchange event, the dh.password module does
	// for example. If that were fixed I'd prefer to throw here.
	return null;
};

// is alt held down?
dh.event.getAltKey = function(ev)
{
	if (ev)
		return ev.altKey;
	if (window.event)
		return window.event.altKey;
	
	// FIXME we should throw here, but need to audit broken code
	// throw Error("no event to get alt key state from");
	return null;
};

/* 
 * Rules for event handlers:
 * - they should have one "ev" argument if they are a button/key event 
 *   and intend to try to look at the event object
 * - if they use dh.event.cancel() they should also return false
 * - "this" in the event handler is not set to anything consistent
 *   across different browsers (it may be the window or the dom node
 *   currently being bubbled to, I believe)
 */

dh.event.addEventListener = function(node, eventName, func) {
	if (node.addEventListener) {
		// false = "bubbling phase" i.e. handle leaves first
		// true = "capturing phase" i.e. handle outermost nodes first
		// IE only supports "bubbling up" with attachEvent, so 
		// "capturing" is not possible to do portably.
		// "node.onclick = function()" assignments do bubbling.
		// See http://www.quirksmode.org/js/events_order.html
		node.addEventListener(eventName, func, false);
	} else if (node.attachEvent) {
		node.attachEvent("on" + eventName, func);
	} else {
		throw Error("browser does not support addEventListener or attachEvent");
	}
}

dh.event.connectAfterMethod = function(object, methodName, func) {
	var oldHandler = object[methodName];
	object[methodName] = function() {
		oldHandler.apply(object, arguments);
		return ourHandler.apply(object, arguments);
	}
}

dh.event._onWindowLoadHandlers = [];

dh.event._onWindowLoaded = function() {
	
	var i;
	for (i = 0; i < dh.event._onWindowLoadHandlers.length; ++i) {
		var f = dh.event._onWindowLoadHandlers[i];
		f.apply(window);
	}
	
	// drop the listeners, as gc paranoia
	dh.event._onWindowLoadHandlers = [];
	
	// afaik this does not matter
	return true;
}

// If dojo has been initialized because something is using it, we 
// need to keep the dojo onload handler.
if (typeof window.onload == 'function') {
	var oldHandler = window.onload;
	var ourHandler = dh.event._onWindowLoaded;
	dh.event._onWindowLoaded = function(){
		oldHandler.apply(window, arguments);
		return ourHandler.apply(window, arguments);
	}
}

window.onload = dh.event._onWindowLoaded;

dh.event.addPageLoadListener = function(func) {
	// using addEventListener appears to work in both IE and Firefox, but 
	// they appear to run the listeners in opposite order. Also, this 
	// way we can drop the listeners after we run them, which might help 
	// with gc.
	//dh.event.addEventListener(window, "load", func);
	dh.event._onWindowLoadHandlers.push(func);
}
