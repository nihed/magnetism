dojo.provide("dojo.validate");
dojo.require("dojo.lang");

// currently a stub for dojo.validate

dojo.validate.isText = function(value) {
	return (dojo.lang.isString(value) && value.search(/\S/) != -1);
}

dojo.validate.isInteger = function(value) {
	if (!dojo.lang.isNumber(value)) {
		return false;
	}
	return Math.round(value) == value;
}

dojo.validate.isValidNumber = function(value) {
	return dojo.lang.isNumber(value);
}

// FIXME: may be too basic
dojo.validate.isEmailAddress = function(value) {
	value = value.replace(/mailto:/i, "");
	return /^<?([\w\.\-]+)@([\w\.\-]+)>?$/i.test(value);
}

// FIXME: should this biggyback on isEmailAddress or just have its own RegExp?
dojo.validate.isEmailAddressList = function(value) {
	var values = value.split(/\s*[\s;,]\s*/gi);
	for(var i = 0; i < values.length; i++) {
		if(!dojo.validate.isEmailAddress(values[i])) {
			return false;
		}
	}
	return true;
}
