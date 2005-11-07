dh = {}
dh.client = {}

// Parse query parameters, sucked from dh.util.
dh.client.getParamsFromLocation = function() {
	var query = window.location.search.substring(1);
	var map = {};
	var params = query.split("&");
   	for (var i = 0; i < params.length; i++) {
   		var eqpos = params[i].indexOf('=')
   		if (eqpos > 0) {
   		    var key = params[i].substring(0, eqpos);
   		    var val = params[i].substring(eqpos+1);
   			map[key] = decodeURIComponent(val);
   		}
    }
    return map;
}

// Takes a DOM event handler function and fixes
// the IE event object to have the standard DOM 2 functions etc.
// Also wraps it in a try/catch for debugging purposes.
dh.client.stdEventHandler = function(f) {
  return function(e) {
    try {
      if (!e) e = window.event;
      if (!e.stopPropagation) {
        e.stopPropagation = function() { e.cancelBubble = true; }
      }
      e.returnValue = f(e);
      return e.returnValue;
    } catch (e) {
      alert(e);
      return false;
    }
  }
}

// Called when the user clicks on the shared link
dh.client.handleLinkClicked = dh.client.stdEventHandler(function(e) {
	e.stopPropagation();
	window.external.LinkClicked();
	return false;
})