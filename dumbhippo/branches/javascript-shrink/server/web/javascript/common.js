// common.js - adds requires on the dojo bootstrap files
// our dh:script tag forces it to load on all pages that use any javascript
dojo.provide('common');
dojo.provide('dh'); // so the "dh" hash exists

// these modules are invented in the jscompress script.
// bootstrap1 has to be included already, making it work 
// like a module was too annoying because it defines global
// variables
dojo.require('dojo.hostenv_browser');
dojo.require('dojo.bootstrap2');

// This is to catch bugs
dojo.require = function(module) {
	throw new Error("dojo.require should not still exist at runtime, jscompress should have replaced it");
}
dojo.provide = function(module) {
	throw new Error("dojo.provide should not still exist at runtime, jscompress should have replaced it");
}

// these two functions are here instead of a module since they are 
// debug-oriented and useful to have "fewer dependencies" for
function dhAddScriptToHead(url) {
	var script = document.createElement('script');
	script.type = 'text/javascript';
	script.src = url;
	document.getElementsByTagName('head')[0].appendChild(script); 
}

function dhAllPropsAsString(obj) {
	var s = "{";
	for (var prop in obj) {
		s = s + prop + " : " + obj[prop] + ",";
	}
	s = s + "}";
	return s;
}
