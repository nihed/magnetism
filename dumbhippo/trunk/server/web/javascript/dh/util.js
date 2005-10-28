dojo.provide("dh.util");

dh.util.getParamsFromLocation = function() {
	var query = window.location.search.substring(1);
	dojo.debug("query: " + query);
	var map = {};
	var params = query.split("&");
   	for (var i = 0; i < params.length; i++) {
   		var eqpos = params[i].indexOf('=')
   		if (eqpos > 0) {
   		    var key = params[i].substring(0, eqpos);
   		    var val = params[i].substring(eqpos+1);
   			map[key] = decodeURIComponent(val);
   			dojo.debug("mapping query key " + key + " to " + map[key]);
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
	dojo.html.prependClass(node, "dhInvisible");
}

dh.util.show = function(node) {
	dojo.html.removeClass(node, "dhInvisible");
}

dh.util.toggleShowing = function(node) {
	if (dh.util.isShowing(node))
		dh.util.hide(node);
	else 
		dh.util.show(node);
}

dh.util.isShowing = function(node) {
	return !dojo.html.hasClass(node, "dhInvisible");
}

dh.util.closeWindow = function() {
	// Use our ActiveX control to close this dialog; the reason
	// for checking the readyState is to see if the object was 
	// actually loaded.
	var embed = document.getElementById("dhEmbedObject");
	if (embed && embed.readyState && embed.readyState >= 3)
		embed.CloseWindow();
}

// could probably choose a better color ;-)
dh.util.flash = function(node) {
	var origColor = dojo.html.getBackgroundColor(node);
	var flashColor = [0,200,0];
	dojo.debug("fading from " + origColor + " to " + flashColor);
	dojo.fx.html.colorFade(node, origColor, flashColor, 400,
						function(node, anim) {
							dojo.debug("fading from " + flashColor + " to " + origColor);
							dojo.fx.html.colorFade(node, flashColor, origColor, 400, function(node, anim) {
								/* go back to our CSS color */
								node.removeAttribute("style");
							});
						});
}

dh.util.join = function(array, separator, elemProp) {
	var joined = "";
	for (var i = 0; i < array.length; ++i) {
		if (i != 0) {
			joined = joined + separator;
		}
		if (arguments.length > 2)
			joined = joined + array[i][elemProp];
		else
			joined = joined + array[i];
	}
	return joined;
}

dh.util.disableOpacityEffects = dojo.render.html.mozilla && dojo.render.html.geckoVersion < 20051001;
