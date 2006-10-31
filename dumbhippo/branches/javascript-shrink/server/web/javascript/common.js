// common.js - included after dojo bootstrap and before all our other javascript
dojo.provide('common');
dojo.provide('dh'); // so the "dh" hash exists

// dojo hackarounds - it never does an explicit "dojo.provide" on these
// things so they don't exist when we need them to without these hacks
dojo.provide('dojo.graphics')
dojo.provide('dojo.io')
dojo.provide('dojo.uri')

// This is to catch bugs
dojo.requires = function(module) {
	throw new Error("dojo.requires should not still exist at runtime, jscompress should have replaced it");
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
