dh = {}
dh.core = {}
dh.util = {}
dh.util.dom = {}

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

// Parse query parameters, sucked from dh.util in server
dh.util.getParamsFromLocation = function() {
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

dh.util.debug = function (msg) {
	window.external.DebugLog("javascript: " + msg)
}

// Takes a DOM event handler function and fixes
// the IE event object to have the standard DOM 2 functions etc.
// Also wraps it in a try/catch for debugging purposes.
dh.util.dom.stdEventHandler = function(f) {
  return function(e) {
    try {
      if (!e) e = window.event;
      if (!e.stopPropagation) {
        e.stopPropagation = function() { e.cancelBubble = true; }
      }
      e.returnValue = f(e);
      return e.returnValue;
    } catch (e) {
      dh.util.debug("exception in event handler: " + e.message);
      return false;
    }
  }
}

dh.util.dom.appendSpanText = function (elt, text, styleClass) {
	var span = document.createElement("span")
	span.setAttribute("className", styleClass)
	span.appendChild(document.createTextNode(text))
	elt.appendChild(span)	
}

dh.util.dom.joinSpannedText = function (elt, arr, styleClass, sep) {
	for (var i = 0; i < arr.length; i++) {
		dh.util.dom.appendSpanText(elt, arr[i], styleClass)
		if (i < arr.length - 1) {
			elt.appendChild(document.createTextNode(sep))
		}
	}	
}

dh.util.dom.createHrefImg = function (src, target) {
	var a = document.createElement("a")
	a.setAttribute("href", target)
	var img = document.createElement("img")
	a.appendChild(img)
	img.setAttribute("src", src)
	return a;
}

dh.util.dom.clearNode = function (elt) {
	while (elt.firstChild) { elt.removeChild(elt.firstChild); }
}

// Notification implementation

dh.notification = {}

dh.display = null;

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl) {
	var closeButton = document.getElementById("dh-close-button")
	closeButton.onclick = dh.util.dom.stdEventHandler(function (e) {
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
		dh.util.debug('sharing ' + share + " senderName: " + share["senderName"])
		this.notifications.push({notificationType: 'linkShare',
		                         data: share})
		dh.util.debug("position " + this.position + " notifications: " + this.notifications)		                         
		if (this.position < 0) {
			this.setPosition(0)
		}
		this._insertNavigation()		
	}
	
	this.setPosition = function(pos) {
		if (pos < 0) {
			dh.util.debug("negative position specified")
			return
		} else if (pos >= this.notifications.length) {
			dh.util.debug("position " + pos + " is too big")
			return
		}
			
		var notification = this.notifications[pos]
		this.position = pos
		dh.util.debug("switching to position " + pos + ", notification type " + notification.notificationType)		
		if (notification.notificationType == 'linkShare') {	
			this._displayLinkShare(notification.data);
		}
		this._insertNavigation()
	}
	
	this.goPrevious = function () {
		this.setPosition(this.position - 1)
	}
	
	this.goNext = function () {
		this.setPosition(this.position + 1)
	}
	
	this._insertNavigation = function () {
		var node = document.getElementById("dh-notification-position")
		dh.util.dom.clearNode(node)
		var table = document.createElement("table")
		var tbody = document.createElement("tbody")
		table.appendChild(tbody)
		var tr = document.createElement("tr")
		tbody.appendChild(tr)
		var td = document.createElement("td")
		tr.appendChild(td)
		var div = document.createElement("div")
		div.setAttribute("className", "dh-notification-position")
		td.appendChild(div)
		div.appendChild(document.createTextNode((this.position+1) + " of " + this.notifications.length))
		
		tr = document.createElement("tr")
		tbody.appendChild(tr)
		td = document.createElement("td")
		tr.appendChild(td)
		div = document.createElement("div")
		td.appendChild(div)
		div.setAttribute("className", "dh-notification-position")
		var img = dh.util.dom.createHrefImg(this.appletUrl + "activeLeft.png", "")
		img.firstChild.setAttribute("className", "dh-notification-position")
		div.appendChild(img)
		var display = this
		img.onclick = dh.util.dom.stdEventHandler(function (e) {
			display.goPrevious();
			return false;
		})
		img = dh.util.dom.createHrefImg(this.appletUrl + "activeRight.png", "")
		img.firstChild.setAttribute("className", "dh-notification-position")		
		div.appendChild(img)
		img.onclick = dh.util.dom.stdEventHandler(function (e) {
			display.goNext();
			return false;
		})		
		node.appendChild(table)
	}
	
	this._displayLinkShare = function (share) {
	dh.util.debug("displaying " + share + " senderName: " + typeof(share["senderName"])	+ " " + share["senderName"])	
    var parent = document.getElementById("dh-notification")
	dh.util.dom.clearNode(parent)
	
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
	var display = this
	var senderClickHandler = dh.util.dom.stdEventHandler(function(e) {
		e.stopPropagation();
		window.external.OpenExternalURL(display.serverUrl + "viewperson?personId=" + share["senderId"])
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
	a.onclick = dh.util.dom.stdEventHandler(function(e) {
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
	div.setAttribute("id", "dh-notification-position")	
	
	td = document.createElement("td")
	tr.appendChild(td)
	div = document.createElement("div")
	td.appendChild(div)
	div.setAttribute("className", "dh-notification-meta")
	div.appendChild(document.createTextNode("This was sent to "))
	var personRecipients = dh.core.adaptExternalArray(share["personRecipients"])
	var groupRecipients = dh.core.adaptExternalArray(share["groupRecipients"])	
	// FIXME this is all hostile to i18n
	dh.util.dom.joinSpannedText(div, personRecipients, "dh-notification-recipient", ", ")
	if (personRecipients.length > 0 && groupRecipients.length > 0) {
		div.appendChild(document.createTextNode(" and "))
	}
	if (groupRecipients.length > 1) {
		div.appendChild(document.createTextNode("the groups "))
		dh.util.dom.joinSpannedText(div, groupRecipients, "dh-notification-group-recipient", ", ")
	} else if (groupRecipients.length == 1) {
		div.appendChild(document.createTextNode("the "))
		dh.util.dom.appendSpanText(div, groupRecipients[0], "dh-notification-group-recipient")
		div.appendChild(document.createTextNode(" group"))
	}
	parent.appendChild(table)
	}
}

// Global namespace since it's painful to do anything else from C++
dhAddLinkShare = function (senderName, senderId, linkTitle,
                               linkURL, linkDescription, personRecipients, groupRecipients) {
	dh.util.debug("in dhAddLinkShare, senderName: " + senderName)                               
	dh.display.addLinkShare({senderName: senderName,
	  					    senderId: senderId,
						    linkTitle: linkTitle,
						    linkURL: linkURL,
						    linkDescription: linkDescription,
						    personRecipients: personRecipients,
						    groupRecipients: groupRecipients})
}

dhAddSwarmNotice = function (postId, clickerId, postTitle) {
}

