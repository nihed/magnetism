var browser;

function getParamsFromLocation() {
	var query = window.location.search.substring(1);
	var map = {};
	var params = query.split("&");
   	for (var i = 0; i < params.length; i++) {
   		var eqpos = params[i].indexOf('=')
   		if (eqpos > 0) {
   		    var key = params[i].substring(0, eqpos);
   		    var val = params[i].substring(eqpos+1);
   		    // Java encodes spaces as +'s, we need to change that
   		    // into something that decodeURIComponent can understand
   		    val = val.replace(/\+/g, "%20");
   		    map[key] = decodeURIComponent(val);
   		}
    }
    return map;
}

function init() {
    browser = document.getElementById("content");

    var params = getParamsFromLocation();

    if (params["src"]) {
	const nsIWebNavigation = Components.interfaces.nsIWebNavigation;
        browser.webNavigation.loadURI(params["src"], nsIWebNavigation.LOAD_FLAGS_NONE, null, null, null);
    }
}