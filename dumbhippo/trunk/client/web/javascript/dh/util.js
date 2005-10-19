dojo.provide("dh.util");

dh.util.getParamsFromLocation = function() {
	var loc = document.location.toString();
	var locSplitOnQuest = loc.split("?");
	if (locSplitOnQuest.length < 2) {
		dojo.debug("no params parsed");
		return {};
	}
	var paramsString = locSplitOnQuest[locSplitOnQuest.length - 1];
	dojo.debug("loc = " + loc + " locSplitOnQuest = " + locSplitOnQuest + " paramsString = " + paramsString);
	var map = {};
	var params = paramsString.split("&");
   	for (var i in params) {
   		if (params[i].length == 0) {
   			continue;
   		}
   		var keyvalue = params[i].split("=");
   		if (keyvalue.length != 2) {
   			dojo.debug("Malformed keyvalue pair '" + keyvalue + "'");
   		} else {
   			map[keyvalue[0]] = decodeURIComponent(keyvalue[1]);
   		}
    }
    return map;
}

dh.util.showId = function(nodeId) {
	var node = document.getElementById(nodeId);
	if (!node)
		dojo.raise("can't find node " + nodeId);
	dh.util.show(node);
}

dh.util.hideId = function(nodeId) {
	var node = document.getElementById(nodeId);
	if (!node)
		dojo.raise("can't find node " + nodeId);
	dh.util.hide(node);
}

dh.util.hide = function(node) {
	dojo.html.addClass(node, "dhInvisible");
}

dh.util.show = function(node) {
	dojo.html.removeClass(node, "dhInvisible");
}
