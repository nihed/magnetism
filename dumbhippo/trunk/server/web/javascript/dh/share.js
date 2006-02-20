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
	var list = document.getElementById("dhRecipientList");
	for (var i = 0; i < list.childNodes.length; ++i) {
		var child = list.childNodes.item(i);
		if (child.nodeType != dojo.dom.ELEMENT_NODE)
			continue;
		var childId = child.getAttribute("dhId");
		if (childId && id == childId) {
			return child;
		}
	}
	return null;
}

dh.share.forEachPossibleGroupMember = function(func) {
	var list = document.getElementById("dhRecipientList");
	for (var i = 0; i < list.childNodes.length; ++i) {
		var child = list.childNodes.item(i);
		if (child.nodeType != dojo.dom.ELEMENT_NODE)
			continue;
			
		var id = child.getAttribute("dhId");
		var obj = dh.share.allKnownIds[id];

		if (obj.isPerson())
			func(child);
	}
	return null;
}

dh.share.removeRecipient = function(recipientId, node) {
	if (arguments.length < 2) {
		node = dh.share.findIdNode(recipientId);
	}

	var objIndex = dh.share.findGuid(dh.share.selectedRecipients, recipientId);
	dh.share.selectedRecipients.splice(objIndex, 1);
	
	if (dh.util.disableOpacityEffects) {
		node.parentNode.removeChild(node);
	} else {
		// remove the HTML representing this recipient
		var anim = dojo.fx.html.fadeOut(node, 800, function(node, anim) {
			node.parentNode.removeChild(node);
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

dh.share.createNewContactFromCombo = function() {
	var email = dojo.string.trim(dh.share.autoSuggest.inputText);
	
	if (email.length == 0 || email.indexOf("@") < 0 || email.indexOf(" ") >= 0 || email.indexOf(",") >= 0) {
		alert("invalid email address: '" + email + "'");
		return;
	}
	
	dojo.debug("looking up contact " + email);

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
			},
			function(type, error, http) {
				dojo.debug(" got back error " + dhAllPropsAsString(error));
				// FIXME display
			});
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
		dojo.html.addClass(idNode, "dhRecipient");
		dojo.html.addClass(idNode, "dhItemBox");
		if (dh.share.recipientCreatedCallback)
			dh.share.recipientCreatedCallback(obj, idNode);
		
		// don't think tbody is used anymore?
		var tbody = document.createElement("tbody");
		idNode.appendChild(tbody);
		var tr1 = document.createElement("tr");
		tbody.appendChild(tr1);
		var td = document.createElement("td");
		dojo.html.addClass(td, "dhHeadShot");
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
		
		var img = dh.util.createPngElement(imgSrc, 48, 48);
		td.appendChild(img);
		
		var td = document.createElement("td");
		dojo.html.addClass(td, "dhRemoveRecipient");
		tr1.appendChild(td);
		var removeLink = document.createElement("a");
		removeLink.appendChild(document.createTextNode("[X]"));
		removeLink.setAttribute("href", "javascript:void(0);");
		dojo.html.addClass(removeLink, "dhRemoveRecipient");
		removeLink.setAttribute("rowSpan", "2");
		dojo.event.connect(removeLink, "onclick", dj_global, "dhRemoveRecipientClicked");
		td.appendChild(removeLink);
		
		var tr2  = document.createElement("tr");
		tbody.appendChild(tr2);
		var td = document.createElement("td");
		dojo.html.addClass(td, "dhRecipientName");
		td.setAttribute("colSpan","2");
		tr2.appendChild(td);
		td.appendChild(document.createTextNode(obj.displayName));

		var tr3  = document.createElement("tr");
		tbody.appendChild(tr3);
		var td = document.createElement("td");
		dojo.html.addClass(td, "dhRecipientNote");
		td.setAttribute("colSpan","2");
		tr3.appendChild(td);
		if (obj.isGroup()) {
			td.appendChild(document.createTextNode(obj.sampleMembers));
		} else {
			if (!obj.hasAccount)
				td.appendChild(document.createTextNode("via email"));
		}

		if (!dh.util.disableOpacityEffects)
			dojo.html.setOpacity(idNode, 0);
		
		var recipientsListNode = document.getElementById("dhRecipientList");
		recipientsListNode.appendChild(idNode);
	
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

dh.share.init = function() {
	dojo.debug("dh.share.init");
			
	dh.share.recipientComboBox = document.getElementById('dhRecipientComboBox');
	dh.share.recipientComboBoxButton = document.getElementById('dhRecipientComboBoxButton');
	dh.share.autoSuggest = new dh.autosuggest.AutoSuggest(dh.share.recipientComboBox, dh.share.recipientComboBoxButton);
	dh.share.autoSuggest.setOnSelectedFunc(dh.share.recipientSelected);
	dh.share.autoSuggest.setGetEligibleFunc(dh.share.getEligibleRecipients);
	
	// rich text areas can't exist when display:none, so we have to create it after showing
	dh.share.descriptionRichText = document.getElementById("dhShareDescription");
}
