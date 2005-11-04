getParamsFromLocation = function() {
	var query = window.location.search.substring(1);
	var map = {};
	var params = query.split("&");
   	for (var i = 0; i < params.length; i++) {
   		var eqpos = params[i].indexOf('=')
   		if (eqpos > 0) {
   		    var key = params[i].substring(0, eqpos);
   		    var val = params[i].substring(eqpos+1);
   			map[key] = decodeURIComponent(val);
   		}
    }
    return map;
}