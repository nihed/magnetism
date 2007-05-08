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
