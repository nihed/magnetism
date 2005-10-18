dojo.provide("dh.server");

dojo.require("dojo.event.*");
dojo.require("dojo.io.*");

dh.server.get = function(name, params, loadFunc, errorFunc, how, what) {
	var root = null;
	
	if (what == "text/plain")
		root = dhTextRoot;
	else if (what == "text/xml")
		root = dhXmlRoot;
	else {
		dojo.debug("don't know how to get that: " + what);
		return;
	}
	
	dj_debug("launching IO request to " + root + name);
	dojo.io.bind({
		 method: how,
		 url: root + name,
		 load: loadFunc,     // (type, data, event)
		 error: errorFunc,   // (type, error)
		 content: params,
		 mimetype: what,
		 async: true,
		 transport: "XMLHTTPTransport"
		 });
	dj_debug("...launched, waiting");
}

// note that none of these is like a <form> post which is type
// "application/x-www-form-urlencoded" or something and has 
// special response stuff maybe, I don't know

dh.server.getTextGET = function(name, params, loadFunc, errorFunc) {
	dh.server.get(name, params, loadFunc, errorFunc, "GET", "text/plain");
}

dh.server.getTextPOST = function(name, params, loadFunc, errorFunc) {
	dh.server.get(name, params, loadFunc, errorFunc, "POST", "text/plain");
}

dh.server.getXmlGET = function(name, params, loadFunc, errorFunc) {
	dh.server.get(name, params, loadFunc, errorFunc, "GET", "text/xml");
}

dh.server.getXmlPOST = function(name, params, loadFunc, errorFunc) {
	dh.server.get(name, params, loadFunc, errorFunc, "POST", "text/xml");
}
