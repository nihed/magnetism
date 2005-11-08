dh = {}
dh.client = {}
dh.notification = {}

// Parse query parameters, sucked from dh.util.
dh.client.getParamsFromLocation = function() {
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
dh.client.stdEventHandler = function(f) {
  return function(e) {
    try {
      if (!e) e = window.event;
      if (!e.stopPropagation) {
        e.stopPropagation = function() { e.cancelBubble = true; }
      }
      e.returnValue = f(e);
      return e.returnValue;
    } catch (e) {
      alert(e);
      return false;
    }
  }
}

// Called when the user clicks on the shared link
dh.notification.handleLinkClicked = dh.client.stdEventHandler(function(e) {
	e.stopPropagation();
	window.external.LinkClicked();
	window.external.Close();
	return false;
})

// Called when the user clicks on the sender name
dh.notification.handleSenderLinkClicked = dh.client.stdEventHandler(function(e) {
	e.stopPropagation();
	window.external.SenderLinkClicked();
	window.external.Close();
	return false;
})

// Called when user clicks on close button
dh.notification.handleCloseClicked = dh.client.stdEventHandler(function(e) {
	e.stopPropagation();
	window.external.Close();
	return false;
})



dhSetRecipients = function (personRecipients, groupRecipients) {
	var appendSpanText = function (elt, text, styleClass) {
		var span = document.createElement("span")
		span.setAttribute("className", styleClass)
		span.appendChild(document.createTextNode(text))
		elt.appendChild(span)	
	}
	var joinSpannedText = function (elt, arr, styleClass, sep) {
		for (var i = 0; i < arr.length; i++) {
			appendSpanText(elt, arr[i], styleClass)
			if (i < arr.length - 1) {
				elt.appendChild(document.createTextNode(sep))
			}
		}	
	}
    var personRecipients = (new VBArray(personRecipients).toArray())
    var groupRecipients = (new VBArray(groupRecipients).toArray())
 
	// FIXME this is all hostile to i18n   
    var elt = document.getElementById("dh-notification-recipients")
	while (elt.firstChild) { elt.removeChild(elt.firstChild); }
	
	joinSpannedText(elt, personRecipients, "dh-notification-recipient", ", ")
	if (personRecipients.length > 0 && groupRecipients.length > 0) {
		elt.appendChild(document.createTextNode(" and "))
	}
	if (groupRecipients.length > 1) {
		elt.appendChild(document.createTextNode("the groups "))
		joinSpannedText(elt, groupRecipients, "dh-notification-group-recipient", ", ")
	} else if (groupRecipients.length == 1) {
		elt.appendChild(document.createTextNode("the "))
		appendSpanText(elt, groupRecipients[0], "dh-notification-group-recipient")
		elt.appendChild(document.createTextNode(" group"))
	}
}