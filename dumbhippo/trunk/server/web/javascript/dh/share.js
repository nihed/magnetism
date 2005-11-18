dojo.provide("dh.share");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.html");
dojo.require("dojo.widget.RichText");
dojo.require("dojo.widget.html.Button");
dojo.require("dojo.widget.HtmlComboBox");
dojo.require("dh.server");
dojo.require("dh.util");
dojo.require("dh.model");

// whether allKnownIds has successfully been filled in
dh.share.haveLoadedContacts = false;
// hash of all persons/groups we should autocomplete on, keyed by guid
dh.share.allKnownIds = {};
// currently selected recipients, may be group or person objects
dh.share.selectedRecipients = [];

dh.share.recipientComboBox = null;
dh.share.descriptionRichText = null;

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

// called when user presses Enter or the Add button
dh.share.doAddRecipientFromCombo = function(createContact) {
	var cb = dh.share.recipientComboBox;
	var email = cb.textInputNode.value;
	
	if (email.length == 0 || email.indexOf("@") < 0) {
		dojo.debug("invalid email address: " + email);
		// FIXME display something
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

dhComboBoxOptionSelected = function() {
	// combo box does not reliably fill in selectedResult I 
	// don't think, but it should in selectOption which we are the handler
	// for.
	if (!dh.share.recipientComboBox.selectedResult) {
		dojo.debug("no result selected");
		return;
	}
	var id = dh.share.recipientComboBox.selectedResult[1];

	// Unfortunately dojo calls this 
	// callback twice when you click a recipient, and we don't
	// want to flash the recipient on the second time. 
	// Conveniently, a selected recipient won't be in the dropdown
	// if it's already added normally, so no effect on normal behavior
	// if we do noFlash = true

	dojo.debug("adding recipient since selected = " + id);
	dh.share.doAddRecipient(id, true);
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
		var img = document.createElement("img");
		if (obj.isPerson())
			img.setAttribute("src", dhHeadshotsRoot + obj.id);
		else
			img.setAttribute("src", dhGroupshotsRoot + obj.id);
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
	
	// clear the combo again
	dh.share.recipientComboBox.textInputNode.value = "";
	if (dh.share.recipientComboBox._result_list_open)
		dh.share.recipientComboBox.hideResultList();
	dh.share.recipientComboBox.dataProvider.lastSearchProvided = null;
	dh.share.recipientComboBox.dataProvider.singleCompletionId = null;
}

dhDoAddRecipientKeyUp = function(event) {
	if (event.keyCode == 13) {
		dh.share.doAddRecipientFromCombo(true);
	}
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

dh.share.findInStringArray = function(strings, func, data) {
	for (var i = 0; i < strings.length; ++i) {
		if (func(strings[i], data))
			return strings[i];
	}
	return null;
}

dh.share.FriendListProvider = function() {

	this.lastSearchProvided = null;
	this.singleCompletionId = null;

	// type is a string "STARTSTRING", "SUBSTRING", "STARTWORD" which we ignore for now
	this.startSearch = function(searchStr, type, ignoreLimit) {
		//dojo.debug("friend startSearch");
		
		var st = type || "startstring";
		
		// not case-sensitive
		searchStr = searchStr.toLowerCase();
		
		var matchFunc;
		if (st.toLowerCase() == "substring") {
			matchFunc = function(text, searchStr) {
				return text.toLowerCase().indexOf(searchStr) >= 0;
			}
		} else {
			matchFunc = function(text, searchStr) {
				return text.toLowerCase().substr(0, searchStr.length) == searchStr;
			}
		}
		
		var completions = [];
		
		for (var id in dh.share.allKnownIds) {
			var obj = dh.share.allKnownIds[id];
			
			var found = null;
			var matchedStr = null;
			if (matchFunc(obj.displayName, searchStr)) {
				found = obj;
				matchedStr = obj.displayName;
			} else if (obj.email && matchFunc(obj.email, searchStr)) {
				found = obj;
				matchedStr = obj.email;
			} else if (obj.aim && matchFunc(obj.aim, searchStr)) {
				found = obj;
				matchedStr = obj.aim;
			} else {
				// look in all emails and aims; but checking primary
				// email and aim first was deliberate, even though 
				// we'll check them again here
				if (obj.emails) {
					var s = dh.share.findInStringArray(obj.emails, matchFunc, searchStr);
					if (s) {
						found = obj;
						matchedStr = s;
					}
				}
				if (!found && obj.aims) {
					var s = dh.share.findInStringArray(obj.aims, matchFunc, searchStr);
					if (s) {
						found = obj;
						matchedStr = s;
					}
				}
			}
			
			if (found && found.isPerson() && !found.hasAccount && !found.email) {
				// we can't share with someone who is only an aim address
				found = null;
				matchedStr = null;
			}
			
			if (found) {
				if (!dh.share.findGuid(dh.share.selectedRecipients,
							               found.id)) {
					completions.push([matchedStr, found.id]);
				}
			}
		}
		
		this.emitProvideSearchResults(completions, searchStr);
	}

	// a "signal", pass it an array of 2-item arrays, where the pairs
	// are usercompletion+ourselectionid ; BEWARE dojo destroys this array so pass it a copy
	// if you are also keeping a reference
	this.provideSearchResults = function(resultsDataPairs) {
		dojo.debug("friend provideSearchResults results = " + resultsDataPairs);
	}
	
	this.emitProvideSearchResults = function(resultsDataPairs, forSearchStr) {
	
		// HtmlComboBox should probably do this itself... working around it
	
		dojo.debug("lastSearchProvided -" + this.lastSearchProvided + "- forSearchStr -" + forSearchStr + "-");
	
		if (resultsDataPairs.length == 1) {
			this.singleCompletionId = resultsDataPairs[0][1];
			dojo.debug("single completion is " + this.singleCompletionId);
		} else {
			this.singleCompletionId = null;
			dojo.debug("there are " + resultsDataPairs.length + " completions");
		}
	
		if (this.lastSearchProvided == forSearchStr) {
			// just show the list.
			dh.share.recipientComboBox.showResultList(); // is a no-op if already showing
		} else {
			dojo.debug("providing search results to the combo for '" + forSearchStr + "' results are " + resultsDataPairs);
			this.lastSearchProvided = forSearchStr;
			this.provideSearchResults(resultsDataPairs);
		}

		// This code adds the recipient if there is only a single one. It's
		// from experience, a really bad idea to do this; it's unpredictable
		// for the user, and often does something they don't want.

//		if (this.singleCompletionId && forSearchStr.length > 0) {
//			dojo.debug("adding single completion as recipient " + this.singleCompletionId);
//			dh.share.doAddRecipient(this.singleCompletionId);
//		} else {
//			dojo.debug("don't have single completion");
//		}
	}
}
dojo.inherits(dh.share.FriendListProvider, Object);

dh.share.HtmlFriendComboBox = function(){
	// dojo.debug("creating HtmlFriendComboBox");
	dojo.widget.HtmlComboBox.call(this);
	
	this.widgetType = "FriendComboBox";
	
	this.fillInTemplate = function(args, frag){
		// override the default provider
		this.dataProvider = new dh.share.FriendListProvider();
		// DEBUG - put data in the default provider
		//this.dataProvider = new dojo.widget.ComboBoxDataProvider();
		//this.dataProvider.setData(dh.share.stateNames);
    }
}

dojo.inherits(dh.share.HtmlFriendComboBox, dojo.widget.HtmlComboBox);

dojo.widget.manager.registerWidgetPackage("dh.share");
dojo.widget.tags.addParseTreeHandler("dojo:friendcombobox");

dh.share.init = function() {
	dojo.debug("dh.share.init");
			
	dh.share.recipientComboBox = dojo.widget.manager.getWidgetById("dhRecipientComboBox");
	dojo.event.connect(dh.share.recipientComboBox.textInputNode, "onkeyup", dj_global, "dhDoAddRecipientKeyUp");
	dojo.event.connect(dh.share.recipientComboBox, "selectOption", dj_global, "dhComboBoxOptionSelected");
	
	// rich text areas can't exist when display:none, so we have to create it after showing
	dh.share.descriptionRichText = dojo.widget.fromScript("richtext", 
															 {}, // props,
															 document.getElementById("dhShareLinkDescription"));
}
