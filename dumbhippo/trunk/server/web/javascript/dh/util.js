dojo.provide("dh.util");
dojo.require("dojo.html");

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
	// We have a CloseWindow object in our ActiveX control that
	// we can use to close arbitrary windows, but as it happens,
	// the only windows we want to close can be closed with
	// window.close. Don't check window.opener here since we
	// can close the windows opened by the client even when
	// window.opener isn't set.
	
	window.close();
	return true;
}

// could probably choose a better color ;-)
dh.util.flash = function(node) {
	var origColor = dojo.html.getBackgroundColor(node);
	var flashColor = [0,200,0];
	//dojo.debug("fading from " + origColor + " to " + flashColor);
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

dh.util.getMainNode = function() {
	var node = document.getElementById("dhMain");
	if (!node)
		node = document.body;
	return node;
}

// arg is the default page to go to if none was specified
// "close" and "here" are magic pseudo-pages for close the window
// and stay on this page
//
// Note that we don't validate the default, just what we retrieve from paremeters,
// so you must not pass user input as 'def' to this function.
dh.util.goToNextPage = function(def, flashMessage) {
	if (flashMessage) {
		// delete the whole page
		var main = dh.util.getMainNode();
		dh.util.hide(main);
		while (main.firstChild) {
			main.removeChild(main.firstChild);
		}
		
		// insert the message
		var messageNode = document.createElement("div");
		dojo.html.addClass(messageNode, "dh-closing-message");
		messageNode.appendChild(document.createTextNode(flashMessage));
		main.appendChild(messageNode);
		dh.util.show(main);
		
		setTimeout("dh.util.goToNextPage(\"" + def + "\");", 1000); // in 1 second, go to next page
		return;
	}

	var params=dh.util.getParamsFromLocation();
	var where = params.next;
	
	// We want to handle params.next="close" / def="main" and also
	// params.next="main" / def="close", and in the first case 
	// we want to fall back to "main" if the close fails
	
	if (where == "close") {
		if (dh.util.closeWindow()) {
			return; // never reached I think
		} else {
			dojo.debug("close window failed, trying default " + def);
			delete where;
		}
	}
	
	if (!where)
		where = def;
		
	if (!where) {
		dojo.debug("no next page specified");	
	} else if (where == "close") {
		dh.util.closeWindow();
	} else if (where == "here") {
		dojo.debug("staying put");
	} else if (where == def || where.match(/^[a-zA-Z]+$/)) {
		dojo.debug("opening " + where);
    	window.open(where, "_self");
	} else {
		dojo.debug("invalid next page target " + where);
	}
}

// loosely based on dojo.html.renderedTextContent
dh.util.getTextFromHtmlNode = function(node) {
	var result = "";
	if (node == null) { return result; }
	
	switch (node.nodeType) {
		case dojo.dom.ELEMENT_NODE: // ELEMENT_NODE
			if (node.nodeName.toLowerCase() == "br") {
				result += "\n";
			} else {
				//dojo.debug("element = " + node.nodeName);
			}
			break;
		case 5: // ENTITY_REFERENCE_NODE
			result += node.nodeValue;
			break;
		case 2: // ATTRIBUTE_NODE
			break;
		case 3: // TEXT_NODE
		case 4: // CDATA_SECTION_NODE
			result += node.nodeValue;
			break;
		default:
			break;
	}
	
	for (var i = 0; i < node.childNodes.length; i++) {
		result += dh.util.getTextFromHtmlNode(node.childNodes[i]);
	}

	return result;
}

dh.util.getTextFromRichText = function(richtext) {
	// dojo has dojo.html.renderedTextContent() but it isn't 
	// finished and doesn't work well enough for our purposes here
	// yet (probably overkill too since we offer no styled text toolbar)
	return dh.util.getTextFromHtmlNode(richtext.editNode);
}

dh.util.toggleCheckBox = function(boxNameOrNode) {
	var node = boxNameOrNode;
	if (dojo.lang.isString(boxNameOrNode)) {
		node = document.getElementById(boxNameOrNode);
	}
	node.checked = !node.checked;
	
	// fixup the radio group
	if (node.type == "radio") {
		var allInputs = document.getElementsByTagName("input");
		for (var i = 0; i < allInputs.length; ++i) {
			var n = allInputs[i];
			if (n != node && n.name == node.name) {
				n.checked = !node.checked;
			}
		}
	}
}

// Yes, this is IE specific.  It's used on pages
// which can only be viewed from IE currently.
dh.util.getMSXML = function (text) {
	var domDoc = new ActiveXObject("Microsoft.XMLDOM");
	domDoc.async = false;
	domDoc.loadXML(text);
	domDoc.setProperty("SelectionLanguage", "XPath")
	return domDoc;
}

dh.util.createPngElement = function(src, width, height) {
	// don't try to use <img> or <span>, it won't work; the <div> is why you have to provide width/height
	var img = document.createElement("div");
	if (dojo.render.html.ie) {
		// don't try to use setAttribute(), it won't work
		img.style.width = width;
		img.style.height = height;
		img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale');"
	} else {
		img.setAttribute("style", "background-image: url( " + src + " ); width: " + width + "; height: " + height + ";");
	}
	return img;
}

dh.util.clearNode = function (node) {
	while (node.firstChild) { node.removeChild(node.firstChild) }
}

dh.util.openShareGroupWindow = function(groupId) {
	var url = dhServerUri + 'sharegroup?groupId=' + groupId + '&v=1&next=close';
	window.open(url,
	'_NEW',
	'menubar=no,location=no,toolbar=no,scrollbars=yes,status=no,resizable=yes,height=450,width=550,top='+((screen.availHeight-450)/2)+',left='+((screen.availWidth-550)/2));
}

dh.util.openFrameSet = function(window, event, obj, postID) {
	top.window.location.href = "visit?post=" + postID;
	return false;
}
