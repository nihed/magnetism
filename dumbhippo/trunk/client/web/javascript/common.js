// common.js runs after we load dojo.js
// all real code should be in dojo modules really.

//dj_debug("common.js start");

// set location of "dh" relative to location of "dojo.js"
dojo.hostenv.setModulePrefix("dh", "../dh");

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

//dj_debug(dhAllPropsAsString(dojo));
//dj_debug("common.js end");
