dojo.provide("dh.lang");

dh.inherits = function(subclass, superclass){
	if(typeof superclass != 'function'){ 
		dh.raise("superclass: "+superclass+" borken");
	}
	subclass.prototype = new superclass();
	subclass.prototype.constructor = subclass;
	subclass.superclass = superclass.prototype;
	// DEPRECATED: super is a reserved word, use 'superclass'
	subclass['super'] = superclass.prototype;
}

dh.lang.mixin = function(obj, props){
	var tobj = {};
	for(var x in props){
		if(typeof tobj[x] == "undefined"){
			obj[x] = props[x];
		}
	}
	return obj;
}

dh.lang.extend = function(ctor, props){
	this.mixin(ctor.prototype, props);
}

dh.lang.extendPrototype = function(obj, props){
	this.extend(obj.constructor, props);
}

dh.lang.hitch = function(obj, meth){
	return function(){ return obj[meth].apply(obj, arguments); }
}

dh.lang.defineClass = function(childConstructor, parentConstructor, childProps) {
	if (!parentConstructor)
		parentConstructor = Object;
	dh.inherits(childConstructor, parentConstructor);
	dh.lang.mixin(childConstructor.prototype, childProps);
}

// Partial implmentation of is* functions from
// http://www.crockford.com/javascript/recommend.html
dh.lang.isObject = function(wh) {
	return typeof wh == "object" || dh.lang.isArray(wh) || dh.lang.isFunction(wh);
}

dh.lang.isArray = function(wh) {
	return (wh instanceof Array || typeof wh == "array");
}

dh.lang.isFunction = function(wh) {
	return (wh instanceof Function || typeof wh == "function");
}

dh.lang.isString = function(wh) {
	return (wh instanceof String || typeof wh == "string");
}

dh.lang.isNumber = function(wh) {
	return (wh instanceof Number || typeof wh == "number");
}

dh.lang.isBoolean = function(wh) {
	return (wh instanceof Boolean || typeof wh == "boolean");
}

dh.lang.isUndefined = function(wh) {
	return ((wh == undefined)&&(typeof wh == "undefined"));
}

// is this the right place for this?
dh.lang.has = function(obj, name){
	return (typeof obj[name] !== 'undefined');
}

dh.lang.shallowCopy = function(obj) {
	var ret = {}, key;
	for(key in obj) {
		if(dh.lang.isUndefined(ret[key])) {
			ret[key] = obj[key];
		}
	}
	return ret;
}
