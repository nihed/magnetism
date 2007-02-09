dojo.provide("dojo.lang");
dojo.provide("dojo.lang.Lang");

dojo.lang.mixin = function(obj, props){
	var tobj = {};
	for(var x in props){
		if(typeof tobj[x] == "undefined"){
			obj[x] = props[x];
		}
	}
	return obj;
}

dojo.lang.extend = function(ctor, props){
	this.mixin(ctor.prototype, props);
}

dojo.lang.extendPrototype = function(obj, props){
	this.extend(obj.constructor, props);
}

dojo.lang.hitch = function(obj, meth){
	return function(){ return obj[meth].apply(obj, arguments); }
}


/**
 * Sets a timeout in milliseconds to execute a function in a given context
 * with optional arguments.
 *
 * setTimeout (Object context, function func, number delay[, arg1[, ...]]);
 * setTimeout (function func, number delay[, arg1[, ...]]);
 */
dojo.lang.setTimeout = function (func, delay) {
	var context = window, argsStart = 2;
	if (typeof delay == "function") {
		context = func;
		func = delay;
		delay = arguments[2];
		argsStart++;
	}
	
	var args = [];
	for (var i = argsStart; i < arguments.length; i++) {
		args.push(arguments[i]);
	}
	return setTimeout(function () { func.apply(context, args); }, delay);
}

// Partial implmentation of is* functions from
// http://www.crockford.com/javascript/recommend.html
dojo.lang.isObject = function(wh) {
	return typeof wh == "object" || dojo.lang.isArray(wh) || dojo.lang.isFunction(wh);
}

dojo.lang.isArray = function(wh) {
	return (wh instanceof Array || typeof wh == "array");
}

dojo.lang.isFunction = function(wh) {
	return (wh instanceof Function || typeof wh == "function");
}

dojo.lang.isString = function(wh) {
	return (wh instanceof String || typeof wh == "string");
}

dojo.lang.isNumber = function(wh) {
	return (wh instanceof Number || typeof wh == "number");
}

dojo.lang.isBoolean = function(wh) {
	return (wh instanceof Boolean || typeof wh == "boolean");
}

dojo.lang.isUndefined = function(wh) {
	return ((wh == undefined)&&(typeof wh == "undefined"));
}

dojo.lang.isAlien = function(wh) {
	return !dojo.lang.isFunction() && /\{\s*\[native code\]\s*\}/.test(String(wh));
}

dojo.lang.find = function(arr, val, identity){
	if(identity){
		for(var i=0;i<arr.length;++i){
			if(arr[i] === val){ return i; }
		}
	}else{
		for(var i=0;i<arr.length;++i){
			if(arr[i] == val){ return i; }
		}
	}
	return -1;
}

dojo.lang.inArray = function(arr, val){
	// support both (arr, val) and (val, arr)
	if( (!arr || arr.constructor != Array) && (val && val.constructor == Array) ) {
		var a = arr;
		arr = val;
		val = a;
	}
	return dojo.lang.find(arr, val) > -1;
}

dojo.lang.getNameInObj = function(ns, item){
	if(!ns){ ns = dj_global; }

	for(var x in ns){
		if(ns[x] === item){
			return new String(x);
		}
	}
	return null;
}

// is this the right place for this?
dojo.lang.has = function(obj, name){
	return (typeof obj[name] !== 'undefined');
}

dojo.lang.isEmpty = function(obj) {
	var tmp = {};
	var count = 0;
	for(var x in obj){
		if(obj[x] && (!tmp[x])){
			count++;
			break;
		} 
	}
	return (count == 0);
}

dojo.lang.forEach = function(arr, unary_func, fix_length){
	var il = arr.length;
	for(var i=0; i< ((fix_length) ? il : arr.length); i++){
		if(unary_func(arr[i]) == "break"){
			break;
		}
	}
}

dojo.lang.map = function(arr, obj, unary_func){
	if((typeof obj == "function")&&(!unary_func)){
		unary_func = obj;
		obj = dj_global;
	}
	for(var i=0;i<arr.length;++i){
		unary_func.call(obj, arr[i]);
	}
}

dojo.lang.tryThese = function(){
	for(var x=0; x<arguments.length; x++){
		try{
			if(typeof arguments[x] == "function"){
				var ret = (arguments[x]());
				if(ret){
					return ret;
				}
			}
		}catch(e){
			dojo.debug(e);
		}
	}
}

dojo.lang.delayThese = function(farr, cb, delay, onend){
	/**
	 * alternate: (array funcArray, function callback, function onend)
	 * alternate: (array funcArray, function callback)
	 * alternate: (array funcArray)
	 */
	if(!farr.length){ 
		if(typeof onend == "function"){
			onend();
		}
		return;
	}
	if((typeof delay == "undefined")&&(typeof cb == "number")){
		delay = cb;
		cb = function(){};
	}else if(!cb){
		cb = function(){};
		if(!delay){ delay = 0; }
	}
	setTimeout(function(){
		(farr.shift())();
		cb();
		dojo.lang.delayThese(farr, cb, delay, onend);
	}, delay);
}

dojo.lang.shallowCopy = function(obj) {
	var ret = {}, key;
	for(key in obj) {
		if(dojo.lang.isUndefined(ret[key])) {
			ret[key] = obj[key];
		}
	}
	return ret;
}
