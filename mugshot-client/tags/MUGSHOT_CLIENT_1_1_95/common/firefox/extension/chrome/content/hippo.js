var Hippo = {}

Hippo.mixin = function(obj, props){
	var tobj = {};
	for(var x in props){
		if(typeof tobj[x] == "undefined"){
			obj[x] = props[x];
		}
	}
	return obj;
};

Hippo.extend = function(ctor, props){
	this.mixin(ctor.prototype, props);
};

Hippo.getParamsFromSearch = function(search) {
	var query = search.substring(1);
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
};

Hippo.trim = function(s){
	if (!s.length)
		return s;

	return s.replace(/^\s*/, "").replace(/\s*$/, "");
};

// This check is to make sure that the framer content can't redirect the 
// main window to javascript:, chrome:, file:, etc. The result is the
// resolved URI using the fromUrl as the base (if url is relative),
// or null if the security check failed.
Hippo.checkLoadUri = function(fromUrl, url) {
    try {
	const nsIIOService = Components.interfaces.nsIIOService;
	var ioServ = Components.classes["@mozilla.org/network/io-service;1"]
	    .getService(nsIIOService);
	var fromUri = ioServ.newURI(fromUrl, null /* charset */, null /* baseURI */ );
	var uri = ioServ.newURI(url, null, fromUri);
	
	const nsIScriptSecurityManager = Components.interfaces.nsIScriptSecurityManager;
	var secMan = Components.classes["@mozilla.org/scriptsecuritymanager;1"]
	    .getService(nsIScriptSecurityManager);
	secMan.checkLoadURI(fromUri, uri, nsIScriptSecurityManager.DISALLOW_SCRIPT_OR_DATA);
	
	return uri.spec;
    } catch (e) {
	alert(e);
	return null;
    }
}

Hippo.uriSchemeIs = function(baseUrl, url, scheme) {
    const nsIIOService = Components.interfaces.nsIIOService;
    var ioServ = Components.classes["@mozilla.org/network/io-service;1"]
        .getService(nsIIOService);
    var baseUri = ioServ.newURI(url, null /* charset */, null /* baseURI */ );
    var uri = ioServ.newURI(url, null, baseUri);

    return uri.schemeIs(scheme)
}
