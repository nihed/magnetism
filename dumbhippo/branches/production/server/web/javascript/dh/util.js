dojo.provide("dh.util");
dojo.provide("dh.logger");

dojo.require("dh.lang");
dojo.require("dh.dom");
dojo.require("dh.html");

dh.browser = tmp_dhBrowser;

// for dynamically loading a script
dh.util.addScriptToHead = function(url) {
	var script = document.createElement('script');
	script.type = 'text/javascript';
	script.src = url;
	document.getElementsByTagName('head')[0].appendChild(script); 
}

// for debug-dumping an object
dh.util.allPropsAsString = function(obj) {
	var s = "{";
	for (var prop in obj) {
		s = s + prop + " : " + obj[prop] + ", ";
	}
	s = s + "}";
	return s;
}

dh.logger.LogEntry = function(category, text, level) {
	this.category = category;
	this.text = text;
	this.date = new Date();
	this.level = level ? level : "info";	
}

dh.logger.logWindow = null;
dh.logger.maxLogEntries = 40;
dh.logger.logEntries = [];

dh.logger.isActive = function() {
	return dh.logger.logWindow && !dh.logger.logWindow.closed;
}

dh.logger.show = function() {
	if (!dh.logger.isActive()) {
		dh.logger.logWindow = window.open();
		
		var doc = dh.logger.logWindow.document;
		
		var log = doc.createElement("table");
		var tbody = doc.createElement("tbody");
		log.appendChild(tbody);
		tbody.id = "dhErrorLog";

		var controlsDiv = doc.createElement("div")

		var clearButton = doc.createElement("input")
		clearButton.type = "button";
		clearButton.value = "Clear"
		clearButton.onclick = function (e) {
			tbody.innerHTML = "";
		}		
		controlsDiv.appendChild(clearButton)

		doc.body.appendChild(controlsDiv)
		doc.body.appendChild(log);		
		var i;
		for (i = 0; i < dh.logger.logEntries.length; i++) {
			dh.logger.append(dh.logger.logEntries[i]);
		}
		dh.logger.logEntries = []
	}
}

dh.logger.append = function(entry) {
	var doc = dh.logger.logWindow.document;
	var logTable = doc.getElementById("dhErrorLog");
	
	var tr = doc.createElement("tr");
	var td = doc.createElement("td");
	tr.appendChild(td);
	td.style.fontSize = "smaller";
	td.appendChild(doc.createTextNode(entry.date));
	var td = doc.createElement("td");
	tr.appendChild(td);
	td.style.fontWeight = "bold";
	td.appendChild(doc.createTextNode(entry.category));
	var td = doc.createElement("td");
	tr.appendChild(td);
	var span = doc.createElement("span");
	if (entry.level == "error") {
		span.style.color = "red";
	}
	span.style.marginLeft = "10px"
	span.appendChild(doc.createTextNode(entry.text));
	td.appendChild(span);
	
	logTable.appendChild(tr);	
}

dh.logger.log = function(category, text, level) {
	if (!text) {
		text = category;
		category = "unknown";
	}
	var entry = new dh.logger.LogEntry(category, text, level);
	if (dh.logger.isActive()) {
		dh.logger.append(entry);
	} else {
		dh.logger.logEntries.push(entry);
		if (dh.logger.logEntries.length >= dh.logger.maxLogEntries) {
			dh.logger.logEntries.shift();
		}
	}
}

dh.logger.logError = function(category, text) {
	dh.logger.log(category, text, "error");
}

// Convenience aliases
dh.log = dh.logger.log;
dh.logError = dh.logger.logError;

// deprecated, replaces dojo.debug
dh.debug = function(text) {
	dh.log("debug", text, "debug");
}

// dojo.raise compatibility; no real reason to use this, 
// the idea in Dojo is that it allows hooking in a log 
// whenever an exception is thrown. A downside of using
// this is that javascript console will show this location
// instead of where you threw from
dh.raise = function(message, excep){
	throw Error(message);
}

// from dojo.string.trim
dh.util.trim = function(s){
	// some code (notably on sharelink there was something) seems to rely 
	// on being able to trim() a null string and get null back (or maybe
	// empty string is false in javascript? would not surprise me)
	if (!s) {
		return s;
	}
	if (!s.length) {
		return s;
	}
	return s.replace(/^\s*/, "").replace(/\s*$/, "");
}

dh.util.getParamsFromLocation = function() {
	var query = window.location.search.substring(1);
	dh.debug("query: " + query);
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
   			dh.debug("mapping query key " + key + " to " + map[key]);
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
		dh.raise("can't find node " + nodeId);
	dh.util.show(node);
}

dh.util.hideId = function(nodeId) {
	var node = document.getElementById(nodeId);
	if (!node)
		dh.raise("can't find node " + nodeId);
	dh.util.hide(node);
}

dh.util.hide = function(node) {
	dh.html.prependClass(node, "dhInvisible");
}

dh.util.show = function(node) {
	dh.html.removeClass(node, "dhInvisible");
}

dh.util.toggleShowing = function(node) {
	if (dh.util.isShowing(node))
		dh.util.hide(node);
	else 
		dh.util.show(node);
}

dh.util.isShowing = function(node) {
	return !dh.html.hasClass(node, "dhInvisible");
}

dh.util.isDescendant = function (possibleParent, child) {
	while (child && child != possibleParent) {
		child = child.parentNode;
	}
	return child == possibleParent;
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

dh.util.disableOpacityEffects = dh.browser.gecko && !dh.browser.geckoAtLeast15;

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
		dh.html.addClass(messageNode, "dh-closing-message");
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
			dh.debug("close window failed, trying default " + def);
			delete where;
		}
	}
	
	if (!where)
		where = def;
		
	if (!where) {
		dh.debug("no next page specified");	
	} else if (where == "close") {
		dh.util.closeWindow();
	} else if (where == "here") {
		dh.debug("staying put");
	} else if (where == def || where.match(/^[a-zA-Z]+$/)) {
		dh.debug("opening " + where);
    	window.open(where, "_self");
	} else {
		dh.debug("invalid next page target " + where);
	}
}

// loosely based on dojo.html.renderedTextContent
dh.util.getTextFromHtmlNode = function(node) {
	var result = "";
	if (node == null) 
	    return result;

	switch (node.nodeType) {
		case dh.dom.ELEMENT_NODE: // ELEMENT_NODE
			if (node.nodeName.toLowerCase() == "br") {
				result += "\n";
			} else {
				//dh.debug("element = " + node.nodeName);
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
		case dh.dom.ELEMENT_NODE: // ELEMENT_NODE
			if (node.nodeName.toLowerCase() == "br") {
		        // TODO: not sure if can remove <br> on the fly here, 
		        // but that would be a good thing to do
		        // this is a corner case, we want some other node to 
		        // take care of inserting "..."
		        if (length > 1)
				    length--;				    
			} else {
				//dh.debug("element = " + node.nodeName);
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
	if (dh.lang.isString(boxNameOrNode)) {
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
	if (dh.lang.isString(boxNameOrNode)) {
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
    button.disabled = (dh.util.trim(textbox.value)=='');
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
	if (dh.browser.ie) {
		// don't try to use setAttribute(), it won't work
		img.style.width = width;
		img.style.height = height;
		img.style.overflow = "hidden";
		img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale');"
	} else {
		img.setAttribute("style", "background-image: url( " + src + " ); width: " + width + "; height: " + height + "; overflow: hidden;");
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

dh.util.hasClass = function(node, className) {
	return dh.html.hasClass(node, className)
}

dh.util.prependClass = function(node, className) {
	dh.html.prependClass(node, className)
}

dh.util.removeClass = function(node, className) {
	dh.html.removeClass(node, className)
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
    return linkElement;
}

dh.util.createActionLinkElement = function(text, onclick, className) {
    var linkElement = document.createElement("a");
    linkTextNode = document.createTextNode(text);
    linkElement.appendChild(linkTextNode);
    linkElement.onclick = onclick;
    linkElement.className = className;
    return linkElement;
}

dh.util.foreachChildElements = function(startNode, func) {
	var foreachRecurse = function(currentNode) {
		if (currentNode.nodeType != 1)
			return;
		func(currentNode);
		for (var i = 0; i < currentNode.childNodes.length; i++) {
			arguments.callee(currentNode.childNodes.item(i));
		}
	};
	foreachRecurse(startNode);
}

// right now just replaces spaces with "+" to
// make the url look nicer in the browser window,
// spaces show up as "%20" otherwise 
dh.util.getPreparedUrl = function(url) {
    var preparedUrl = url.replace(/\s/g, "+");
    return preparedUrl;
}

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

dh.util.showMessage = function(message, idSuffix, confirmAction, cancelAction) {
    if (!idSuffix) {
        idSuffix = ""
    }
        
	var div = document.getElementById("dhMessageDiv" + idSuffix)

	if (message) {	
		dh.util.clearNode(div)
		div.appendChild(document.createTextNode(message))
		if (confirmAction && cancelAction) {
		    div.appendChild(dh.util.createActionLinkElement("Confirm", confirmAction, "dh-confirm-link"))         
		    div.appendChild(dh.util.createActionLinkElement("Cancel", cancelAction, "dh-cancel-link"))      
		}
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

dh.util.validateEmail = function(address) {
	address = dh.util.trim(address)

	if (address == "" || address.indexOf("@") < 0) {
		alert("Please enter a valid email address")
		return false;
	}
	return true;
}

// Reload the content of the page, without trigger revalidation as document.location.reload()
dh.util.refresh = function() {
	window.open(document.location.href, "_self", null, true);
}


dh.util.sizePhoto = function(baseUrl, size) {
	if (baseUrl.lastIndexOf("?") >= 0)
		return baseUrl + "&size=" + size;
	else
		return baseUrl + "?size=" + size;
}
	

dh.util.formatTimeAgo = function(date) {
	var time = date.getTime();
	if (time <= 0x80000000) // Unknown / bogus
		return "";
	
	var now = new Date();

	var deltaSeconds = (now.getTime() - time) / 1000;
	
	if (deltaSeconds < 30)
		return "";
	
	if (deltaSeconds < 90)
		return "a minute ago";
		
	if (deltaSeconds < 60*60) {
		var deltaMinutes = deltaSeconds / 60;
		if (deltaMinutes < 5) {
			return Math.round(deltaMinutes) + " min. ago";
		} else {
			deltaMinutes = deltaMinutes - (deltaMinutes % 5);
			return Math.round(deltaMinutes) + " min. ago";
		}
	}

	var deltaHours = deltaSeconds / (60 * 60);
	
	if (deltaHours < 1.55)
		return "1 hr. ago";

	if (deltaHours < 24)
		return Math.round(deltaHours) + " hrs. ago";

	if (deltaHours < 48)
		return "Yesterday";
	
	if (deltaHours < 24*15)
		return Math.round(deltaHours / 24) + " days ago";
	
	var deltaWeeks = deltaHours / (24*7);
	
	if (deltaWeeks < 6)
		return Math.round(deltaWeeks) + " weeks ago";
	
	if (deltaWeeks < 50)
		return Math.round(deltaWeeks / 4) + " months ago";
	
	var deltaYears = deltaWeeks / 52;
	
	if (deltaYears < 1.55)
		return "1 year ago";
	else
		return Math.round(deltaYears) + " years ago";
}