dojo.provide("dh.server");

dojo.require("dojo.event.*");
// "io.*" really means "XMLHTTPRequest only", which is what we want.
// IframeIO should be avoided unless you really need it, since even
// requiring the module creates an iframe per page. (And requires
// iframe_history.html, which we don't install into place right now)
dojo.require("dojo.io.*");
dojo.require("dh.util");

dh.server._wrapErrorFunc = function(f, name) {
	return function(type, error, http) {
		dh.logError("server", "Got error invoking method " + name);
		dh.logError("server", "   " + error.message);
		if (f != null)
			f();
	}
}

dh.server.get = function(name, params, loadFunc, errorFunc, how, what) {
	var root = null;
	
	if (what == "text/plain")
		root = dhTextRoot;
	else if (what == "text/xml")
		root = dhXmlRoot;
	else if (how == "POST" && what == null)
		root = dhPostRoot;
	else {
		dh.debug("don't know how to get that: " + what);
		return;
	}
	
	dj_debug("launching IO request to " + root + name);
	dojo.io.bind({
		 method: how,
		 url: root + name,
		 load: loadFunc,
		 error: dh.server._wrapErrorFunc(errorFunc, name),
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

// omit the serverErrorFunc if you want to treat it the same as an XML error reply.
// (serverErrorFunc = http error, normalErrorFunc = error returned as xml document)
dh.server.doXmlMethod = function(name, params, loadFunc, normalErrorFunc, serverErrorFunc, method) {
	if (!serverErrorFunc) {
		serverErrorFunc = function(type, error, http) {
			normalErrorFunc("red", "Something went wrong! We're supposed to keep that from happening... trying again might help (we hope).", http);
		}
	}
	
	if (!method)
		method = "POST";
		
	dh.server.get(name, params,
		function(type, retDoc, http) {		
			var respElt = retDoc.documentElement
			var stat = respElt.getAttribute("stat")
			if (stat == "ok") {
				loadFunc(respElt.childNodes, http)
			} else {
				var code = null
				var msg = null
				for (var i = 0; i < respElt.childNodes.length; ++i) {
					var kid = respElt.childNodes.item(i)
					if (kid.nodeName == "err") {
						code = kid.getAttribute("code")
						msg = kid.getAttribute("msg")
						break
					}
				}
				if (!msg) {
					code = "red"
					msg = "Unknown problem"		
				}
				normalErrorFunc(code, msg, http)					
			}
		},
		serverErrorFunc, method, "text/xml");
}

dh.server.doXmlMethodGET = function(name, params, loadFunc, normalErrorFunc, serverErrorFunc) {
	dh.server.doXmlMethod(name, params, loadFunc, normalErrorFunc, serverErrorFunc, "GET");
}

// a POST with no expected data back, it just does some operation
dh.server.doPOST = function(name, params, loadFunc, errorFunc) {
	dh.server.get(name, params, loadFunc, errorFunc, "POST", null);
}
