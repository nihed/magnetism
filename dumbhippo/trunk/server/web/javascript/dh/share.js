dojo.provide("dh.share");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.html");
dojo.require("dojo.string");
dojo.require("dojo.widget.RichText");
dojo.require("dojo.widget.html.Button");
dojo.require("dojo.widget.HtmlComboBox");
dojo.require("dh.server");
dojo.require("dh.util");
dojo.require("dh.model");
dojo.require("dh.autosuggest");

// whether allKnownIds has successfully been filled in
dh.share.haveLoadedContacts = false;
// hash of all persons/groups we should autocomplete on, keyed by guid
dh.share.allKnownIds = {};
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

dh.share.findGuid = function(set, id) {
	// set can be an array or a hash
	for (var prop in set) {
		if (dojo.lang.has(set[prop], "id")) {
			if (id == set[prop]["id"]) {
				return prop;
			}
		}
	}
	return null;
}

// merges an XML document into allKnownIds and returns an array
// of the newly-added items
dh.share.mergeObjectsDocument = function(doc) {
	var retval = [];
	var objectsElement = doc.getElementsByTagName("objects").item(0);
	if (!objectsElement) {
		dojo.debug("no <objects> element");
		return retval;
	}
	var nodeList = objectsElement.childNodes;
	for (var i = 0; i < nodeList.length; ++i) {
		var element = nodeList.item(i);
		if (element.nodeType != dojo.dom.ELEMENT_NODE) {
			continue;
		} else {
			var obj = dh.model.objectFromXmlNode(element);
		    // merge in a new person/group we know about, overwriting any older data
		    dh.share.allKnownIds[obj.id] = obj;
		    retval.push(obj);
		    dojo.debug(" saved new obj type = " + obj.kind + " id = " + obj.id + " display = " + obj.displayName);
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
		if (child.nodeType != dojo.dom.ELEMENT_NODE)
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
		if (child.nodeType != dojo.dom.ELEMENT_NODE)
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

	var objIndex = dh.share.findGuid(dh.share.selectedRecipients, recipientId);
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
	dojo.debug("remove recipient");
	
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
				dojo.debug("got back a contact " + data);
				dojo.debug("text is : " + http.responseText);
						
				var newContacts = dh.share.mergeObjectsDocument(data);
				
				for (var i = 0; i < newContacts.length; ++i) {
					// add someone; this flashes their entry and is a no-op 
					// if they were already added
					dojo.debug("adding newly-created contact as recipient");
					dh.share.doAddRecipient(newContacts[i].id);
				}
				
				if (onComplete)
					onComplete()
			},
			function(type, error, http) {
				dojo.debug(" got back error " + dhAllPropsAsString(error));
				// FIXME display
			});
}

dh.share.createNewContactFromCombo = function() {
	var email = dojo.string.trim(dh.share.autoSuggest.inputText);
	
	if (email.length == 0) // Silently ignore empty
		return
	
	if (!dh.share.isValidEmail(email)) {
		alert("invalid email address: '" + email + "'");
		return;
	}
	
	dojo.debug("looking up contact " + email);
	
	dh.share.addEmailContactAsync(email)
}
	
dh.share.recipientSelected = function(selectedId) {

	dojo.debug("adding recipient since selected = " + selectedId);
	if (selectedId)
		dh.share.doAddRecipient(selectedId, true);
	else
		dh.share.createNewContactFromCombo();
}

dh.share.doAddRecipient = function(selectedId, noFlash) {	
	
	dojo.debug("adding " + selectedId + " as recipient if they aren't already");
	
	var objKey = dh.share.findGuid(dh.share.allKnownIds, selectedId);
	if (!objKey) {
		// user should never get here
		alert("something went wrong adding that person ... (" + selectedId + ")");
		return;
	}
	
	var obj = dh.share.allKnownIds[objKey];
	
	if (!dh.share.findGuid(dh.share.selectedRecipients, obj.id)) {
		
		if (dh.share.canAddRecipientCallback && 
			!dh.share.canAddRecipientCallback(obj)) {
			return;
		}
		
		dh.share.selectedRecipients.push(obj);
		
		var idNode = document.createElement("table");
		idNode.setAttribute("dhId", obj.id);
		idNode.setAttribute("width", "55px")
		dojo.html.addClass(idNode, "dhShareRecipientPerson");
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
		
		var imgSrc;
		if (obj.isPerson()) {
			if (obj.userId) {
				imgSrc = dhHeadshotsRoot + obj.userId;
			} else {
				imgSrc = dhHeadshotsRoot + "default";
			}
		} else {
			imgSrc = dhGroupshotsRoot + obj.id;
		}
		td.setAttribute("valign", "top")
		td.setAttribute("align", "right")
		var div = document.createElement("div")		
		div.style.background = "url(" + imgSrc + ")";		
		td.appendChild(div)
		dojo.html.addClass(div, "dhShareRecipientPersonPhoto");				
		
		/*
		var img = dh.util.createPngElement(imgSrc, 48, 48);
		dojo.html.addClass(img, "dhShareRecipientPersonPhoto");		
		td.appendChild(img);
		*/
		
		var removeImg = document.createElement("img")
		removeImg.setAttribute("src", "/images/xblue.gif")
		removeImg.setAttribute("width", "13px")
		removeImg.setAttribute("height", "13px")
		dojo.event.connect(removeImg, "onclick", dj_global, "dhRemoveRecipientClicked");		
		div.appendChild(removeImg)
		
		var tr2 = document.createElement("tr");
		tbody.appendChild(tr2);
		var td = document.createElement("td");
		td.setAttribute("align", "left")
		td.setAttribute("width", "55px")
		
		var div = document.createElement("div")
		td.appendChild(div)
		dojo.html.addClass(div, "dhShareRecipientPersonName");
		td.setAttribute("height", "50%")
		td.setAttribute("valign", "bottom")
		tr2.appendChild(td);
		div.appendChild(document.createTextNode(obj.displayName));
		td.appendChild(document.createTextNode("\u00A0"))

		var tr3  = document.createElement("tr");
		tbody.appendChild(tr3);
		var td = document.createElement("td");
		td.setAttribute("align", "left")
		td.setAttribute("valign", "top")
		td.setAttribute("height", "50%")		
		dojo.html.addClass(td, "dhRecipientNote");
		td.setAttribute("width", "55px");		
		tr3.appendChild(td);
		if (obj.isGroup()) {
			td.appendChild(document.createTextNode(obj.sampleMembers));
		} else {
			if (!obj.hasAccount)
				td.appendChild(document.createTextNode("via email"));
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
			dh.util.flash(dh.share.findIdNode(obj.id));
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
				dojo.debug("got back contacts " + data);
				dojo.debug("text is : " + http.responseText);
							
				dh.share.mergeObjectsDocument(data);
				
				dh.share.haveLoadedContacts = true;
			},
			function(type, error, http) {
				dojo.debug("getting contacts, got back error " + dhAllPropsAsString(error));
				
				// note that we don't cache an empty result set, we will retry instead...
			});
}

dh.share.makeHighlightNode = function(word, match, type) {
	var index = word.toLowerCase().indexOf(match.toLowerCase());

	var elem = type || 'li';

	var obj = document.createElement(elem);

	var preText = word.substring(0,index);
	var preSpan = document.createElement("span");
	preSpan.appendChild(document.createTextNode(preText));

	var matchText = word.substring(index, index + match.length);
	var matchStrong = document.createElement("strong");
	matchStrong.appendChild(document.createTextNode(matchText));

	var postText = word.substring(index + match.length,word.length);
	var postSpan = document.createElement("span");
	postSpan.appendChild(document.createTextNode(postText));

	obj.appendChild(preSpan);
	obj.appendChild(matchStrong);
	obj.appendChild(postSpan);

	return obj;
}

dh.share.findInStringArray = function(strings, func, data) {
	for (var i = 0; i < strings.length; ++i) {
		if (func(strings[i], data))
			return dh.share.makeHighlightNode(strings[i], data);
	}
	return null;
}

dh.share.sortEligibleCompare = function(a, b) {
	aText = dojo.dom.textContent(a[0])
	bText = dojo.dom.textContent(b[0])	
	if (aText.localeCompare) // don't trust this to exist...
		return aText.localeCompare(bText);
	else {
		if (aText < bText)
			return -1;
		else if (aText > bText)
			return 1;
		else
			return 0;
	}
}

dh.share.sortEligible = function(array) {
	array.sort(dh.share.sortEligibleCompare);
	return array;
}

dh.share.getEligibleRecipients = function() {

	var results = new Array();

	if (dh.share.autoSuggest.menuMode) {
		// in menu mode, we just show everyone according to their name
		for (var id in dh.share.allKnownIds) {
			var obj = dh.share.allKnownIds[id];
			var node = document.createElement("li");
			node.appendChild(document.createTextNode(obj.displayName));
			results.push([node, obj.id]);			
		}
		
		return dh.share.sortEligible(results);
	}

	var matchNameFunc = function (word, match) {
		// Check whole word first
		if (word.toLowerCase().indexOf(match.toLowerCase()) == 0) {
			return true;
		} else {
			// Now split the word up on spaces so we match
			// second half of the word just like the first half
			var splitSuggestion = word.split(' ');
			for (j in splitSuggestion) {
				var sug = splitSuggestion[j].toLowerCase();
				if (sug.indexOf(match.toLowerCase()) == 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	var matchEmailFunc = function (word, match) {
		//Check whole email first
		if (word.toLowerCase().indexOf(match.toLowerCase()) == 0) {
			return true;
		} else {
			// Now split the email up on the @ so we match
			// second half of the word just like the first half
			var splitEmail = word.split('@')[0].split('.');
			for (j in splitEmail) {
				var sug = splitEmail[j].toLowerCase();
				if(sug.indexOf(match.toLowerCase()) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	var searchStr = dojo.string.trim(dh.share.autoSuggest.inputText);

	if (searchStr.length == 0)
		return results; // no eligible when the input is empty

	for (var id in dh.share.allKnownIds) {
		var obj = dh.share.allKnownIds[id];
		
		var found = null;
		var matchedNode = null;
		if (matchNameFunc(obj.displayName, searchStr)) {
			found = obj;
			matchedNode = dh.share.makeHighlightNode(obj.displayName, searchStr);
		} else if (obj.email && matchEmailFunc(obj.email, searchStr)) {
			found = obj;
			matchedNode = dh.share.makeHighlightNode(obj.email, searchStr);
		} else if (obj.aim && matchNameFunc(obj.aim, searchStr)) {
			found = obj;
			matchedNode = dh.share.makeHighlightNode(obj.aim, searchStr);
		} else {
			// look in all emails and aims; but checking primary
			// email and aim first was deliberate, even though 
			// we'll check them again here
			if (obj.emails) {
				var n = dh.share.findInStringArray(obj.emails, matchEmailFunc, searchStr);
				if (n) {
					found = obj;
					matchedNode = n;
				}
			}
			if (!found && obj.aims) {
				var n = dh.share.findInStringArray(obj.aims, matchNameFunc, searchStr);
				if (n) {
					found = obj;
					matchedNode = n;
				}
			}
		}
		
		if (found && found.isPerson() && !found.hasAccount && !found.email) {
			// we can't share with someone who is only an aim address
			found = null;
			matchedNode = null;
		}
		
		if (found) {
			if (!dh.share.findGuid(dh.share.selectedRecipients,
						               found.id)) {
				results.push([matchedNode, found.id]);
			}
		}
	}
	
	return dh.share.sortEligible(results);
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

// Called when the user clicks on the Submit button; enters any
// outstanding text from the recipients field then calls 
// doSubmit. doSubmit may be called asynchronously or 
// synchronously
dh.share.checkAndSubmit = function(doSubmit) {
	var recipient = dojo.string.trim(dh.share.autoSuggest.inputText)
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
	
	doSubmit()
}

dh.share.init = function() {
	dojo.debug("dh.share.init");
			
	dh.share.recipientComboBox = document.getElementById('dhShareRecipientComboBox');
	dh.share.recipientComboBoxButton = document.getElementById('dhShareRecipientComboBoxButton');
	dh.share.autoSuggest = new dh.autosuggest.AutoSuggest(dh.share.recipientComboBox, dh.share.recipientComboBoxButton);
	dh.share.autoSuggest.setOnSelectedFunc(dh.share.recipientSelected);
	dh.share.autoSuggest.setGetEligibleFunc(dh.share.getEligibleRecipients);

	// rich text areas can't exist when display:none, so we have to create it after showing
	dh.share.descriptionRichText = document.getElementById("dhShareDescriptionTextArea");
}
