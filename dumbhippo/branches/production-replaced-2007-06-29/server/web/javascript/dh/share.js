dojo.provide("dh.share");

dojo.require("dojo.style");
dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.html");
dojo.require("dh.html");
dojo.require("dojo.widget.RichText");
dojo.require("dojo.widget.html.Button");
dojo.require("dojo.widget.HtmlComboBox");
dojo.require("dh.server");
dojo.require("dh.util");
dojo.require("dh.model");
dojo.require("dh.autosuggest");
dojo.require("dh.suggestutils");
dojo.require("dh.dom");

// whether allKnownIds has successfully been filled in
dh.share.haveLoadedContacts = false;

// hash of all persons/groups we should autocomplete on, keyed by guid
dh.share.allKnownIds = { };

// currently selected recipients, may be group or person objects
dh.share.selectedRecipients = [];

dh.share.recipientComboBox = null;
dh.share.recipientComboBoxButton = null;
dh.share.descriptionRichText = null;
dh.share.autoSuggest = null;

//
// Some callbacks when things happen; really, we should use dojo signal
// connections, but stay simple for now
//

// Called when a recipient node is created, before it is added to the tree
//   recipientCreatedCallback(recipient, newNode)
dh.share.recipientCreatedCallback = null;

// Called after other processing when the set of recipient changes
//   recipientsChangedCallback(newNode)
dh.share.recipientsChangedCallback = null;

// Called to see if we can add a particular recipient
//  canAddRecipientCallback(personOrGroupObj)
dh.share.canAddRecipientCallback = null;

// could probably choose a better color ;-)
dh.share.flash = function(node) {
	var origColor = dojo.html.getBackgroundColor(node);
	var flashColor = [0,200,0];
	//dh.debug("fading from " + origColor + " to " + flashColor);
	dojo.fx.html.colorFade(node, origColor, flashColor, 400,
						function(node, anim) {
							dh.debug("fading from " + flashColor + " to " + origColor);
							dojo.fx.html.colorFade(node, flashColor, origColor, 400, function(node, anim) {
								/* go back to our CSS color */
								node.removeAttribute("style");
							});
						});
}

// merges an XML document into allKnownIds and returns an array
// of the newly-added items
dh.share.mergeObjectsDocument = function(doc) {
	var retval = [];
	var objectsElement = doc.getElementsByTagName("objects").item(0);
	if (!objectsElement) {
		dh.debug("no <objects> element");
		return retval;
	}
	var nodeList = objectsElement.childNodes;
	for (var i = 0; i < nodeList.length; ++i) {
		var element = nodeList.item(i);
		if (element.nodeType != dh.dom.ELEMENT_NODE) {
			continue;
		} else {
			var obj = dh.model.objectFromXmlNode(element);
		    // merge in a new person/group we know about, overwriting any older data
		    dh.share.allKnownIds[obj.id] = obj;
		    retval.push(obj);
		    dh.debug(" saved new obj type = " + obj.kind + " id = " + obj.id + " display = " + obj.displayName);
		}
	}
	
	// update the autocompletions
	dh.share.autoSuggest.checkUpdate(true);
	
	return retval;
}

dh.share.findIdNode = function(id) {
	var list = document.getElementById("dhRecipientListTableRow");
	for (var i = 0; i < list.childNodes.length; ++i) {
		var child = list.childNodes.item(i);
		if (child.nodeType != dh.dom.ELEMENT_NODE)
			continue;
		for (var j = 0; j < child.childNodes.length; j++) {			
			var subchild = child.childNodes.item(j)			
			var childId = subchild.getAttribute("dhId");
			if (childId && id == childId) {
				return subchild;
			}
		}
	}
	return null;
}

dh.share.forEachPossibleGroupMember = function(func) {
	var list = document.getElementById("dhRecipientListTableRow");
	for (var i = 0; i < list.childNodes.length; ++i) {
		var child = list.childNodes.item(i);
		if (child.nodeType != dh.dom.ELEMENT_NODE)
			continue;
			
		for (var j = 0; j < child.childNodes.length; j++) {			
			var subchild = child.childNodes.item(j)
			var id = subchild.getAttribute("dhId");
			var obj = dh.share.allKnownIds[id];
	
			if (obj.isPerson())
				func(subchild);
		}
	}
	return null;
}

dh.share.removeRecipient = function(recipientId, node) {
	if (arguments.length < 2) {
		node = dh.share.findIdNode(recipientId);
	}

	var objIndex = dh.model.findGuid(dh.share.selectedRecipients, recipientId);
	dh.share.selectedRecipients.splice(objIndex, 1);
	
	var tr = node.parentNode	
	if (dh.util.disableOpacityEffects) {
	    tr.parentNode.removeChild(tr)
	} else {
		// remove the HTML representing this recipient
		var anim = dojo.fx.html.fadeOut(node, 800, function(node, anim) {
			tr.parentNode.removeChild(tr)
		});
	}
	
	if (dh.share.recipientsChangedCallback)
		dh.share.recipientsChangedCallback();
}

dhRemoveRecipientClicked = function(event) {
	dh.debug("remove recipient");
	
	// scan up for the dhId node which is the outermost
	// node of the html representing this person/group, and also 
	// has the person/group ID in question
	
	var idToRemove = null;
	var node = event.target;
	while (node != null) {
		idToRemove = node.getAttribute("dhId");
		if (idToRemove)
			break;
		node = node.parentNode;
	}
	
	dh.share.removeRecipient(idToRemove, node);
}

dh.share.isValidEmail = function(email) {
	return email.length > 0 && email.indexOf("@") >= 0 && email.indexOf(" ") < 0 && email.indexOf(",") < 0
}

dh.share.addEmailContactAsync = function(email, onComplete) {
	dh.server.getXmlPOST("createorgetcontact",
			{ "email" : email },
			function(type, data, http) {
				dh.debug("got back a contact " + data);
				dh.debug("text is : " + http.responseText);
						
				var newContacts = dh.share.mergeObjectsDocument(data);
				
				for (var i = 0; i < newContacts.length; ++i) {
					// add someone; this flashes their entry and is a no-op 
					// if they were already added
					dh.debug("adding newly-created contact as recipient");
					dh.share.doAddRecipient(newContacts[i].id);
				}
				
				if (onComplete)
					onComplete()
			},
			function(type, error, http) {
				// FIXME display
			});
}

dh.share.createNewContactFromCombo = function() {
	var email = dh.util.trim(dh.share.autoSuggest.inputText);
	
	if (email.length == 0) { // Silently ignore empty
		dh.debug("ignoring empty email")
		return
	}
	
	if (!dh.share.isValidEmail(email)) {
		alert("invalid email address: '" + email + "'");
		return;
	}
	
	dh.debug("looking up contact " + email);
	
	dh.share.addEmailContactAsync(email)
}
	
dh.share.recipientSelected = function(selectedId) {

	dh.debug("adding recipient since selected = " + selectedId);
	if (selectedId)
		dh.share.doAddRecipient(selectedId, true);
	else
		dh.share.createNewContactFromCombo();
	dh.share.autoSuggest.setText('')	
}

dh.share.doAddRecipient = function(selectedId, noFlash) {	
	
	dh.debug("adding " + selectedId + " as recipient if they aren't already");
	
	var objKey = dh.model.findGuid(dh.share.allKnownIds, selectedId);
	if (!objKey) {
		// user should never get here
		alert("something went wrong adding that person ... (" + selectedId + ")");
		return;
	}
	
	var obj = dh.share.allKnownIds[objKey];
	
	if (!dh.model.findGuid(dh.share.selectedRecipients, obj.id)) {
		
		if (dh.share.canAddRecipientCallback && 
			!dh.share.canAddRecipientCallback(obj)) {
			return;
		}
		
		dh.share.selectedRecipients.push(obj);
		
		var idNode = document.createElement("table");
		idNode.setAttribute("dhId", obj.id);
		idNode.setAttribute("width", "100%")
		idNode.setAttribute("cellspacing", "0")
		idNode.setAttribute("cellpadding", "0")		
		dh.html.addClass(idNode, "dhShareRecipientPerson");
		if (dh.share.recipientCreatedCallback)
			dh.share.recipientCreatedCallback(obj, idNode);
		
		// tbody is required in DOM, even if you can omit it in HTML
		// if you do omit it all sorts of bizarre stuff happens
		var tbody = document.createElement("tbody");
		idNode.appendChild(tbody);
		var tr1 = document.createElement("tr");
		tbody.appendChild(tr1);
		var td = document.createElement("td");
		tr1.appendChild(td);
		
		var imgSrc = obj.photoUrl
	
		td.setAttribute("valign", "top")
		td.setAttribute("align", "left")
		
		var borderDiv = document.createElement("div")
		borderDiv.className = "dh-share-recipient-photo-border"
		td.appendChild(borderDiv)
		
		var div = document.createElement("div")		
		div.style.background = "url(" + imgSrc + ")";		
		borderDiv.appendChild(div)
		div.className = "dh-share-recipient-photo"
		
		var removeImg = document.createElement("img")
		removeImg.setAttribute("src", dhImageRoot2 + "picclose.gif")
		removeImg.setAttribute("width", "12px")
		removeImg.setAttribute("height", "12px")
		removeImg.className = "dh-share-remove-recipient"
		dojo.event.connect(removeImg, "onclick", dj_global, "dhRemoveRecipientClicked");		
		div.appendChild(removeImg)
		
		var tr2 = document.createElement("tr");
		tbody.appendChild(tr2);
		var td = document.createElement("td");
		td.setAttribute("align", "left")
		
		var div = document.createElement("div")
		td.appendChild(div)
		div.className = "dh-share-recipient-person-name"
		td.setAttribute("align", "left")
		td.setAttribute("valign", "top")
		div.appendChild(document.createTextNode(obj.displayName));
		tr2.appendChild(td);		

		var tr3  = document.createElement("tr");
		tbody.appendChild(tr3);
		var td = document.createElement("td");
		td.setAttribute("align", "left")
		td.setAttribute("valign", "top")	
		tr3.appendChild(td);
		div = document.createElement("div")
		td.appendChild(div)
		div.className = "dh-share-recipient-note"
		if (obj.isGroup()) {
			var text = obj.memberCount == 1 ? "1 member" : obj.memberCount + " members";
			div.appendChild(document.createTextNode(text));
		} else {
			if (!obj.hasAccount)
				div.appendChild(document.createTextNode("via email"));
		}

		if (!dh.util.disableOpacityEffects)
			dojo.html.setOpacity(idNode, 0);
		
		var recipientsListNode = document.getElementById("dhRecipientListTableRow");
		var td = document.createElement("td")
		td.appendChild(idNode)
		recipientsListNode.appendChild(td);
	
		if (!dh.util.disableOpacityEffects)	
			var anim = dojo.fx.html.fadeIn(idNode, 800);
			
		if (dh.share.recipientsChangedCallback)
			dh.share.recipientsChangedCallback();
	} else {
		if (!noFlash)
			dh.share.flash(dh.share.findIdNode(obj.id));
	}
	
	// clear the combo again // Our new autosuggest.js does this for us
	//	dh.share.recipientComboBox.value = "";
}

dh.share.loadContacts = function() {
	if (dh.share.haveLoadedContacts)
		return;
	
	dh.server.getXmlGET("contactsandgroups",
			{ },
			function(type, data, http) {
				dh.debug("got back contacts " + data);
				dh.debug("text is : " + http.responseText);
							
				dh.share.mergeObjectsDocument(data);
				document.getElementById("dhShareRecipientsLoading").style.display = "none";
				
				dh.share.haveLoadedContacts = true;
			},
			function(type, error, http) {
				var loadingDiv = document.getElementById("dhShareRecipientsLoading");
				dh.util.clearNode(loadingDiv);
				loadingDiv.appendchild(document.createTextNode("Error loading friends and groups"));
				
				// note that we don't cache an empty result set, we will retry instead...
			});
}

dh.share.getEligibleRecipients = function() {
    if (dh.share.autoSuggest.menuMode) {
  		return dh.suggestutils.getMenuRecipients(dh.share.allKnownIds)
	} else {
   		return dh.suggestutils.getMatchingRecipients(dh.share.allKnownIds, 
   													 dh.share.autoSuggest.inputText,
                                             		 dh.share.selectedRecipients)
	}
}

// Returns the ID of an exact match for 'str', if any. (Case is ignored;
// if there are multiple exact matches differing in case, a random
// one is returned)
dh.share.findExactMatch = function(str) {
	str = str.toLowerCase()
	for (var id in dh.share.allKnownIds) {
		var obj = dh.share.allKnownIds[id];

		if (str == obj.displayName.toLowerCase() ||
			(obj.email && str == obj.email.toLowerCase()) ||
			(obj.aim && str == obj.aim.toLowerCase())) {
				return obj.id
		}
	}
	
	return null
}

dh.share.getRecipients = function() {
	var recipients = []
	for (var i = 0; i < dh.share.selectedRecipients.length; i++) {
		var recip = dh.share.selectedRecipients[i]
		recipients.push(recip)
	}
	return recipients;
}

// Called when the user clicks on the Submit button; enters any
// outstanding text from the recipients field then calls 
// doSubmit. doSubmit may be called asynchronously or 
// synchronously
dh.share.checkAndSubmit = function(doSubmit) {
	var recipient = dh.util.trim(dh.share.autoSuggest.inputText)
	if (recipient.length > 0) {
		// Check for an exact match	
		var eligible = dh.share.getEligibleRecipients()
		var exactMatch = dh.share.findExactMatch(recipient)
		
		if (exactMatch) {
			dh.share.doAddRecipient(exactMatch, true)
		} else if (eligible.length == 1) {
			dh.share.doAddRecipient(eligible[0][1], true)
		} else if (eligible.length > 1) {
			alert("'" + recipient + "' matches multiple recipients")
			return
		} else if (dh.share.isValidEmail(recipient)) {
			// We have to do a round trip to the server to create
			// the new email contact
			dh.share.addEmailContactAsync(recipient, doSubmit)
			return 	
		} else {
			alert("invalid email address: '" + recipient + "'")
			return;
		}
	}
	
	// It might be better to sensitize and desensitize the
	// Send button, but sending a message without any recipients
	// has the bizarre effect of sending it just to yourself
	if (dh.share.selectedRecipients.length == 0) {
		alert("Please enter some recipients")
		return
	}
	
	dh.share.submitButton.style.display = "none"
	dh.share.submitButtonClicked.style.display = "inline"
	
	doSubmit()
}

dh.share.init = function() {
	dh.debug("dh.share.init");
			
	dh.share.recipientComboBox = document.getElementById('dhShareRecipientComboBox');
	dh.share.recipientComboBoxButton = document.getElementById('dhShareRecipientComboBoxButton');
	dh.share.autoSuggest = new dh.autosuggest.AutoSuggest(dh.share.recipientComboBox, dh.share.recipientComboBoxButton);
	dh.share.submitButton = document.getElementById('dhShareShareButton')
	dh.share.submitButtonClicked = document.getElementById('dhShareShareButtonClicked')	
	dh.share.autoSuggest.setOnSelectedFunc(dh.share.recipientSelected);
	dh.share.autoSuggest.setGetEligibleFunc(dh.share.getEligibleRecipients);

	// rich text areas can't exist when display:none, so we have to create it after showing
	dh.share.descriptionRichText = document.getElementById("dhShareDescriptionTextArea");
}
