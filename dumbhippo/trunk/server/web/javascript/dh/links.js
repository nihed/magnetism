dojo.provide("dh.links");

dojo.require("dh.util");

dh.links.doPagerIndex = function (param, i, anchor) {
	var params = dh.util.getParamsFromLocation();
	params[param] = ""+i
	var newQuery = dh.util.encodeQueryString(params)
	var newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname +
	            newQuery + "#" + anchor
	window.location.replace(newUrl)
}