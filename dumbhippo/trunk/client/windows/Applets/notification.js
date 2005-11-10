dh = {}
dh.core = {}
dh.util = {}

// Parse query parameters, sucked from dh.util.
dh.core.getParamsFromLocation = function() {
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

// Takes a DOM event handler function and fixes
// the IE event object to have the standard DOM 2 functions etc.
// Also wraps it in a try/catch for debugging purposes.
dh.core.stdEventHandler = function(f) {
  return function(e) {
    try {
      if (!e) e = window.event;
      if (!e.stopPropagation) {
        e.stopPropagation = function() { e.cancelBubble = true; }
      }
      e.returnValue = f(e);
      return e.returnValue;
    } catch (e) {
      alert(e.message);
      return false;
    }
  }
}

dh.core.inherits = function(klass, superKlass) {
	klass.prototype = new superKlass();
	klass.prototype.constructor = klass;
	klass.superclass = superKlass.prototype;
}

dh.core.adaptExternalArray = function (arr) {
	// FIXME IE specific
	if (true)  {
		return new VBArray(arr).toArray()
	}
	return arr;
}

dh.util.debug = function (msg) {
	window.external.DebugLog("javascript: " + msg)
}

dh.util.appendSpanText = function (elt, text, styleClass) {
	var span = document.createElement("span")
	span.setAttribute("className", styleClass)
	span.appendChild(document.createTextNode(text))
	elt.appendChild(span)	
}

dh.util.joinSpannedText = function (elt, arr, styleClass, sep) {
	for (var i = 0; i < arr.length; i++) {
		dh.util.appendSpanText(elt, arr[i], styleClass)
		if (i < arr.length - 1) {
			elt.appendChild(document.createTextNode(sep))
		}
	}	
}

dh.util.clearNode = function (elt) {
	while (elt.firstChild) { elt.removeChild(elt.firstChild); }
}


dh.notification = {}

dh.display = null;

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl) {
	var closeButton = document.getElementById("dh-close-button")
	closeButton.onclick = dh.core.stdEventHandler(function (e) {
			e.stopPropagation();
			window.external.Close();
			return false;
	})
	dh.display = new dh.notification.Display(serverUrl, appletUrl);	
}

dh.notification.Display = function (serverUrl, appletUrl) {
	this.notifications = []
	this.position = -1
	
	this.serverUrl = serverUrl
	this.appletUrl = appletUrl
	
	this.addLinkShare = function (share) {
		dh.util.debug('sharing ' + share)
		this.notifications.push({notificationType: 'linkShare',
		                         data: share})
		dh.util.debug("position " + this.position + " notifications: " + this.notifications)		                         
		if (this.position < 0) {
			this.setPosition(0)
		}
	}
	
	this.setPosition = function(pos) {
		var notification = this.notifications[pos]
		this.position = pos
		dh.util.debug("switching to position " + pos + ", notification type " + notification.notificationType)		
		if (notification.notificationType == 'linkShare') {
			this.displayLinkShare(notification.data);
		}
	}
	
	this.displayLinkShare = function (share) {
	dh.util.debug("displaying " + share)	
    var parent = document.getElementById("dh-notification")
	dh.util.clearNode(parent)
	
	var table = document.createElement("table")
	var tbody = document.createElement("tbody")
	table.appendChild(tbody)
	table.setAttribute("className", "dh-notification")
	var tr = document.createElement("tr")
	tbody.appendChild(tr)
	var td = document.createElement("td")
	tr.appendChild(td)
	td.setAttribute("className", "dh-notification-sender")
	var img = document.createElement("img")
	img.setAttribute("src", this.serverUrl + "files/headshots/" + share["senderId"])
	img.setAttribute("className", "dh-notification-sender")
	var senderClickHandler = dh.core.stdEventHandler(function(e) {
		e.stopPropagation();
		window.external.OpenExternalURL(this.serverUrl + "viewperson?personId=" + share["senderId"])
		return false;
	})
	img.onclick = senderClickHandler
	td.appendChild(img)	
	td.appendChild(document.createElement("br"))
	var a = document.createElement("a")
	a.setAttribute("href", "")
	td.appendChild(a)
	a.onclick = senderClickHandler
	var span = document.createElement("span")
	span.setAttribute("className", "dh-notification-sender")
	span.appendChild(document.createTextNode(share["senderName"]))
	a.appendChild(span)	
	
	td = document.createElement("td")
	tr.appendChild(td)
	var div = document.createElement("div")
	div.setAttribute("className", "dh-link-title")
	td.appendChild(div)
	a = document.createElement("a")
	div.appendChild(a)
	a.onclick = dh.core.stdEventHandler(function(e) {
			e.stopPropagation();
			window.external.OpenExternalURL(share["linkURL"])
			return false;
	})
	a.setAttribute("href", "")
	a.appendChild(document.createTextNode(share["linkTitle"]))
	div = document.createElement("div")
	td.appendChild(div)
	div.appendChild(document.createTextNode(share["linkDescription"]))

	parent.appendChild(table)
	
	table = document.createElement("table")
	tbody = document.createElement("tbody")
	table.appendChild(tbody)	
	tr = document.createElement("tr")
	tbody.appendChild(tr)
	td = document.createElement("td")
	tr.appendChild(td)
	div = document.createElement("div")	
	td.appendChild(div)
	div.setAttribute("className", "dh-notification-position")
	var img = document.createElement("img")
	div.appendChild(img)
	img.setAttribute("src", this.appletUrl + "activeLeft.png")
	var img = document.createElement("img")
	div.appendChild(img)
	img.setAttribute("src", this.appletUrl + "activeRight.png")	
	
	td = document.createElement("td")
	tr.appendChild(td)
	div = document.createElement("div")
	td.appendChild(div)
	div.setAttribute("className", "dh-notification-meta")
	div.appendChild(document.createTextNode("This was sent to "))
	var personRecipients = dh.core.adaptExternalArray(share["personRecipients"])
	var groupRecipients = dh.core.adaptExternalArray(share["groupRecipients"])	
	// FIXME this is all hostile to i18n
	dh.util.joinSpannedText(div, personRecipients, "dh-notification-recipient", ", ")
	if (personRecipients.length > 0 && groupRecipients.length > 0) {
		div.appendChild(document.createTextNode(" and "))
	}
	if (groupRecipients.length > 1) {
		div.appendChild(document.createTextNode("the groups "))
		dh.util.joinSpannedText(div, groupRecipients, "dh-notification-group-recipient", ", ")
	} else if (groupRecipients.length == 1) {
		div.appendChild(document.createTextNode("the "))
		dh.util.appendSpanText(div, groupRecipients[0], "dh-notification-group-recipient")
		div.appendChild(document.createTextNode(" group"))
	}
	parent.appendChild(table)
	}
}

// Global namespace since it's painful to do anything else from C++
dhAddLinkShare = function (senderName, senderId, linkTitle,
                               linkURL, linkDescription, personRecipients, groupRecipients) {
	dh.util.debug("in dhAddLinkShare")                               
	dh.display.addLinkShare({senderName: senderName,
	  					    senderId: senderId,
						    linkTitle: linkTitle,
						    linkURL: linkURL,
						    linkDescription: linkDescription,
						    personRecipients: personRecipients,
						    groupRecipients: groupRecipients})
}

