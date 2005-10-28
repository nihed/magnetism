dojo.provide("dh.server");

dojo.require("dojo.event.*");
// "io.*" really means "XMLHTTPRequest only"
dojo.require("dojo.io.*");
dojo.require("dojo.io.IframeIO");

dh.server.get = function(name, params, loadFunc, errorFunc, how, what) {
	var root = null;
	
	if (what == "text/plain")
		root = dhTextRoot;
	else if (what == "text/xml")
		root = dhXmlRoot;
	else if (how == "POST" && what == null)
		root = dhPostRoot;
	else {
		dojo.debug("don't know how to get that: " + what);
		return;
	}
	
	dj_debug("launching IO request to " + root + name);
	dojo.io.bind({
		 method: how,
		 url: root + name,
		 load: loadFunc,
		 error: errorFunc,
		 content: params,
		 mimetype: what,
		 async: true,
		 transport: "XMLHTTPTransport"
		 });
	dj_debug("...launched, waiting");
}

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

// a POST with no expected data back, it just does some operation
dh.server.doPOST = function(name, params, loadFunc, errorFunc) {
	dh.server.get(name, params, loadFunc, errorFunc, "POST", null);
}

dh.server.submitUploadForm = function(node, name, loadFunc, errorFunc) {
	
	var action = dhUploadRoot + name;
	
	dojo.debug("launching upload form submit to " + action + " ...");
	dojo.io.bind({
		formNode: node,
		method: "POST",
		url: action,
		load: loadFunc,
		error: errorFunc,
		async: true,
		transport: "IframeTransport"
		});
	dojo.debug("...launched, waiting");
}
