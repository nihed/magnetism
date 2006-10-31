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
   		    // Java encodes spaces as +'s, we need to change that
   		    // into something that decodeURIComponent can understand
   		    val = val.replace(/\+/g, "%20");
   			map[key] = decodeURIComponent(val);
   			dojo.debug("mapping query key " + key + " to " + map[key]);
   		}
    }
    return map;
}

dh.util.encodeQueryString = function(params) {
	var result = ""
	for (key in params) {
		if (result == "")
			result = "?"
		else
			result += "&"
		result += key
		result += "="
		result += encodeURIComponent(params[key])
	}
	return result
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
		node = document.getElementById("dhPage");
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
	if (node == null) 
	    return result;

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

dh.util.truncateTextInHtmlNode = function(node, length) {
	if (node == null)
	    return length;
	    
	switch (node.nodeType) {
		case dojo.dom.ELEMENT_NODE: // ELEMENT_NODE
			if (node.nodeName.toLowerCase() == "br") {
		        // TODO: not sure if can remove <br> on the fly here, 
		        // but that would be a good thing to do
		        // this is a corner case, we want some other node to 
		        // take care of inserting "..."
		        if (length > 1)
				    length--;				    
			} else {
				//dojo.debug("element = " + node.nodeName);
			}
			break;
		case 5: // ENTITY_REFERENCE_NODE
		    if (length <= 0) {
		        node.nodeValue = "";
		    } else if (length <= node.nodeValue.length) {
		        var newText = node.nodeValue.substring(0, length) + "...";
		        node.nodeValue = newText; 
		        length = 0;     
		    } else {
		        length = length - node.nodeValue.length;
		    }
			break;
		case 2: // ATTRIBUTE_NODE
			break;
		case 3: // TEXT_NODE
		case 4: // CDATA_SECTION_NODE
		    if (length <= 0) {
		        node.nodeValue = "";
		    } else if (length <= node.nodeValue.length) {
		        var newText = node.nodeValue.substring(0, length) + "...";
		        node.nodeValue = newText; 
		        length = 0;     
		    } else {
		        length = length - node.nodeValue.length;
		    }
		    break;
		default:
			break;
	}
	
	for (var i = 0; i < node.childNodes.length; i++) {       
	   length = dh.util.truncateTextInHtmlNode(node.childNodes[i], length);
	}

	return length;
}

dh.util.toggleCheckBox = function(boxNameOrNode) {
	var node = boxNameOrNode;
	if (dojo.lang.isString(boxNameOrNode)) {
		node = document.getElementById(boxNameOrNode);
	}
	node.checked = !node.checked;
	
	// fixup the radio group
	// if a button in a radio group got deselected, we want to select the next
	// button in the group after it, or if it was the last button in a radio group
	// that got deselected, we want to select the first one 
	// if a button in a radio group got selected, we want to make sure that all
	// other buttons are deselected
	if (node.type == "radio") {
		var deselectedNodeIndex = -1;
		var newNodeToSelectIndex = -1;
		var firstNodeIndex = -1;
		var allInputs = document.getElementsByTagName("input");
		for (var i = 0; i < allInputs.length; ++i) {
			var n = allInputs[i];
			if (n != node && n.name == node.name) {
			    // whether we just selected the radio button or deselected it, 
			    // set all other ones to be deselected first 
				n.checked = false;
				
				// because there might be other elements with the "input" tag, 
				// we want to know what is the first node and the node following 
				// the deselected node in this specific group of radio buttons
				if (firstNodeIndex == -1) {
				    firstNodeIndex = i;
				}
				if (deselectedNodeIndex >= 0 && newNodeToSelectIndex == -1) {
				    newNodeToSelectIndex = i;
				}
			} else if (n == node && !node.checked) {
			    // we just deselected a radio button, we need to select 
			    // a new one, so memorize the deselected node index
			    deselectedNodeIndex = i;
			}		
		}
		
	    if (deselectedNodeIndex >= 0) {
	        if (newNodeToSelectIndex >= 0) {
	            allInputs[newNodeToSelectIndex].checked = true;
	        } else {
		        // last button got deselected, so select the first one
	            allInputs[firstNodeIndex].checked = true;
	        }
		}	
	}
}

dh.util.selectCheckBox = function(boxNameOrNode) {
	var node = boxNameOrNode;
	if (dojo.lang.isString(boxNameOrNode)) {
		node = document.getElementById(boxNameOrNode);
	}
	node.checked = true;
	
	// fixup the radio group
	if (node.type == "radio") {
		var allInputs = document.getElementsByTagName("input");
		for (var i = 0; i < allInputs.length; ++i) {
			var n = allInputs[i];
			if (n != node && n.name == node.name) {
				n.checked = false;
			}
		}
	}
}

// disable the button if the textbox is blank, enable otherwise
dh.util.updateButton = function(textboxName, buttonName) {
    var textbox = document.getElementById(textboxName);
    var button = document.getElementById(buttonName);
    button.disabled = (dojo.string.trim(textbox.value)=='');
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

// keep this in sync with the javascript on /bookmark
dh.util.openShareWindow = function(url) {
	window.open(url,
	'_NEW',
	'menubar=no,location=no,toolbar=no,scrollbars=yes,status=no,resizable=yes,height=400,width=550,top='+((screen.availHeight-400)/2)+',left='+((screen.availWidth-550)/2));
}

dh.util.openShareGroupWindow = function(groupId) {
	var url = dhServerUri + 'sharegroup?groupId=' + groupId + '&v=1&next=close';
	dh.util.openShareWindow(url);
}

dh.util.openShareLinkWindow = function(link, title) {
	var url = dhServerUri + 'sharelink?url=' + encodeURIComponent(link) + '&title=' + encodeURIComponent(title) + '&v=1&next=close';
	dh.util.openShareWindow(url);
}

dh.util.useFrameSet = function(window, event, obj, postID) {
	obj.href="/visit?post=" + postID;
}

dh.util.getTextWidth = function(text, fontFamily, fontSize, fontStyle, fontVariant, fontWeight) {
     // Only elements that are rendered have the offsetWidth property set. 
     // So we add the text to the page, measure it, and then remove it.
     var textSpan = document.createElement("span")
     textSpan.innerHTML = text;
     if (fontFamily)
         textSpan.style.fontFamily = fontFamily;
     if (fontSize)
         textSpan.style.fontSize = fontSize;
     if (fontStyle)
         textSpan.style.fontStyle = fontStyle;
     if (fontVariant)
         textSpan.style.fontSize = fontVariant;
     if (fontWeight)
         textSpan.style.fontWeight = fontWeight;                     
     document.body.appendChild(textSpan)
     var width = textSpan.offsetWidth
     document.body.removeChild(textSpan)
     return width;       
}

// parses text into elements containing links and plain text, appends
// them as children to the textElement
dh.util.insertTextWithLinks = function(textElement, text) {
    var done = false
    var i = 0

    var urlArray = dh.util.getNextUrl(text, i)
    if (urlArray == null) {    
        var textNode = document.createTextNode(text)
        textElement.appendChild(textNode)           
        return
    }
    
    var url = urlArray[0]
    var validUrl = urlArray[1]     
    var urlStart = text.indexOf(url, i)
    var textNode = document.createTextNode(text.substring(0, urlStart))
    textElement.appendChild(textNode)          

    while (urlArray != null) {
        dh.util.addLinkElement(textElement, validUrl, url)  
        var urlEnd = urlStart + url.length          
        urlArray = dh.util.getNextUrl(text, urlEnd)      
        var moreText = text.substring(urlEnd, text.length)  
        if (urlArray != null) {
            url = urlArray[0]
            validUrl = urlArray[1]               
            urlStart = text.indexOf(url, urlEnd) 
            moreText = text.substring(urlEnd, urlStart)
        }
        var textNode = document.createTextNode(moreText)
        textElement.appendChild(textNode)         
    }    
}

dh.util.urlRegex = null;

// finds the next possible url in the text, starting at position i
// if one is found, returns an array of two strings, one containing the
// url as it appears in the text, and another one containing a valid
// url that can be linked to; otherwise, returns null
dh.util.getNextUrl = function(text, i) {
	if (!dh.util.urlRegex) {
		// we mainly identify a url by it containing a dot and two or three letters after it, which
		// can then be followed by a slash and more letters and acceptable characters 
		// this should superset almost all possibly ways to type in a url
		// we also use http://, https://, www, web, ftp, and ftp:// to identify urls like www.amazon, which
		// are also accepted by the browers
		// WARNING Firefox 1.0 can't parse this so keep it as a string, not a regex literal 
		dh.util.urlRegex = new RegExp('([^\\s"\'<>[\\]][\\w._%-:/]*\\.[a-z]{2,3}(/[\\w._%-:/&=?]*)?(["\'<>[\\]\\s]|$))|(https?://)|((www|web)(\\.))|(ftp(\\.))|(ftp://)', 'i');	
	}
    var reg = dh.util.urlRegex;

    var regArray = reg.exec(text.substring(i, text.length))
    var urlStart = -1
    if (regArray)
        urlStart = i + regArray.index

    if (urlStart >= 0) {          
        var urlEndReg = /(["'<>[\]\s$])/    
        var urlEndRegArray = urlEndReg.exec(text.substring(urlStart, text.length))      
        var urlEnd = text.length
        // normally, urlEndRegArray should not be null because at the very least we should get the end of string      
        if (urlEndRegArray)
            urlEnd = urlStart + urlEndRegArray.index          
     
        var url = text.substring(urlStart, urlEnd)      
        var validUrl = url
        
        if ((url.indexOf("http") != 0) && (url.indexOf("ftp") != 0)) {
            validUrl = "http://" + url    
        } else if (url.indexOf("ftp.") == 0) {
            validUrl = "ftp://" + url            
        }
      
        var urlArray = new Array(url, validUrl)    
        return urlArray    
    }
    return null
}

// creates a link element with the given url and text, 
// appends it as a child to the parentElement
// the link will be excluded from a tabbing order,
// would not have a focus border show up around it when 
// selected and open in a new browser window
dh.util.addLinkElement = function(parentElement, url, text) {
    var linkElement = dh.util.createLinkElement(url, text)
    parentElement.appendChild(linkElement)
}

// creates a link element with the given url and text
// the link will be excluded from a tabbing order,
// would not have a focus border show up around it when 
// selected and open in a new browser window
dh.util.createLinkElement = function(url, text) {
    var linkElement = document.createElement("a")
    linkElement.href = dh.util.getPreparedUrl(url)
    linkElement.target = "_blank"
    linkElement.hideFocus = "true"
    linkElement.tabIndex = -1
    var linkTextNode = document.createTextNode(text)
    linkElement.appendChild(linkTextNode)
    return linkElement
}

// creates a link element with the given url and appends the
// child to it; the child can be an image that needs to be clickable
// the link will be excluded from a tabbing order,
// would not have a focus border show up around it when 
// selected and open in a new browser window
dh.util.createLinkElementWithChild = function(url, linkChild) {
    var linkElement = document.createElement("a")
    linkElement.href = dh.util.getPreparedUrl(url)
    linkElement.target = "_blank"    
    linkElement.hideFocus = "true"
    linkElement.tabIndex = -1
    linkElement.appendChild(linkChild)
    return linkElement
}

// right now just replaces spaces with "+" to
// make the url look nicer in the browser window,
// spaces show up as "%20" otherwise 
dh.util.getPreparedUrl = function(url) {
    var preparedUrl = url.replace(/\s/g, "+")
    return preparedUrl
}

dh.util.stdEventHandler = function(f) {
    return function(e) {
        try {
            if (!e) e = window.event;
            e.returnValue = f(e);
            return e.returnValue;
        } catch (ex) {
            alert("exception in event handler: " + ex.message);
            return false;
        }
    }
}

// get the node an event happened on
dh.util.getEventNode = function(ev)
{
	if (ev)
		return ev.target;
	if (window.event)
		return window.event.srcElement;
};

// cancel an event
dh.util.cancelEvent = function(ev)
{
	if (ev) {
		ev.preventDefault();
		ev.stopPropagation();
	}
	
	if (window.event) {
		window.event.returnValue = false;
		window.event.cancelBubble = true;
	}
};

// Define common keycodes
TAB = 9;
ESC = 27;
KEYUP = 38;
KEYDN = 40;
ENTER = 13;
SHIFT = 16;
CTRL = 17;
ALT = 18;
CAPS_LOCK = 20;

dh.util.getKeyCode = function(ev)
{
	if (ev)
		return ev.keyCode;
	if (window.event)
		return window.event.keyCode;
};

// is alt held down?
dh.util.getAltKey = function(ev)
{
	if (ev)
		return ev.altKey;
	if (window.event)
		return window.event.altKey;
};

dh.util.getBodyPosition = function(el) {
	var point = { "x" : 0, "y" : 0 };
	
	while (el.offsetParent && el.tagName.toUpperCase() != 'BODY') {
		point.x += el.offsetLeft;
		point.y += el.offsetTop;
		el = el.offsetParent;
	}

	point.x += el.offsetLeft;
	point.y += el.offsetTop;
	
	return point;
}

dh.util.showMessage = function(message) {
	var div = document.getElementById("dhMessageDiv")

	if (message) {	
		dh.util.clearNode(div)
		div.appendChild(document.createTextNode(message))
		div.style.display = "block"
	} else {
		div.style.display = "none"
	}
};

dh.util.contains = function(items, item) { 
    for (var i = 0; i < items.length; i++){
        if (items[i] == item) {
            return true
        }
    }
    return false
};

// This function only works if you have the client running, on Windows
dh.util.clientDebug = function(text) {
	try {
		window.external.DebugLog(text);
	} catch (e) {
	}
}

dh.util.timeString = function(timestamp) {
	var date = new Date();
	date.setTime(timestamp);
    return dh.util.zeroPad(date.getMonth()+1, 2) + "/" + dh.util.zeroPad(date.getDate(), 2) + "/" 
           + date.getFullYear() + " " + dh.util.zeroPad(date.getHours(), 2) + ":" 
           + dh.util.zeroPad(date.getMinutes(), 2) + ":" + dh.util.zeroPad(date.getSeconds(), 2);
}

dh.util.zeroPad = function(number, len) {
    var str = number + "";
    while (str.length < len) {
        str = "0" + str;
    }
    return str;
}

dh.util.addEventListener = function(node, eventName, func) {
	if (node.addEventListener) {
		node.addEventListener(eventName, func, false);
	} else if (node.attachEvent) {
		node.attachEvent("on" + eventName, func);
	} else {
		throw new Error("browser does not support addEventListener or attachEvent");
	}
}
