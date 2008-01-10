dojo.provide("dojo.string");
dojo.require("dojo.lang");

dojo.string.trim = function(iString){
	if(arguments.length == 0){ // allow String.prototyp-ing
		iString = this; 
	}
	if(typeof iString != "string"){ return iString; }
	if(!iString.length){ return iString; }
	return iString.replace(/^\s*/, "").replace(/\s*$/, "");
}

// Parameterized string function
//  str - formatted string with %{values} to be replaces
//  pairs - object of name: "value" value pairs
//  killExtra - remove all remaining %{values} after pairs are inserted
dojo.string.paramString = function(str, pairs, killExtra) {
	if(typeof str != "string") { // allow String.prototype-ing
		pairs = str;
		killExtra = pairs;
		str = this;
	}

	for(var name in pairs) {
		var re = new RegExp("\\%\\{" + name + "\\}", "g");
		str = str.replace(re, pairs[name]);
	}

	if(killExtra) { str = str.replace(/%\{([^\}\s]+)\}/g, ""); }
	return str;
}

/** Uppercases the first letter of each word */
dojo.string.capitalize = function (str) {
	if (typeof str != "string" || str == null)
		return "";
	if (arguments.length == 0) { str = this; }
	var words = str.split(' ');
	var retval = "";
	var len = words.length;
	for (var i=0; i<len; i++) {
		var word = words[i];
		word = word.charAt(0).toUpperCase() + word.substring(1, word.length);
		retval += word;
		if (i < len-1)
			retval += " ";
	}
	
	return new String(retval);
}

dojo.string.isBlank = function (str) {
	if(!dojo.lang.isString(str)) { return true; }
	return (dojo.string.trim(str).length == 0);
}

dojo.string.encodeAscii = function(str) {
	if(!dojo.lang.isString(str)) { return str; }
	var ret = "";
	var value = escape(str);
	var match, re = /%u([0-9A-F]{4})/i;
	while((match = value.match(re))) {
		var num = Number("0x"+match[1]);
		var newVal = escape("&#" + num + ";");
		ret += value.substring(0, match.index) + newVal;
		value = value.substring(match.index+match[0].length);
	}
	ret += value.replace(/\+/g, "%2B");
	return ret;
}
	
