dojo.provide("dh.sharelink");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.html");
dojo.require("dojo.widget.RichText");
dojo.require("dojo.widget.html.Button");
dojo.require("dojo.widget.HtmlComboBox");
dojo.require("dojo.widget.HtmlInlineEditBox");
dojo.require("dh.server");
dojo.require("dh.util");
dojo.require("dh.model");

// whether allKnownIds has successfully been filled in
dh.sharelink.haveLoadedContacts = false;
// hash of all persons/groups we should autocomplete on, keyed by guid
dh.sharelink.allKnownIds = {};
// currently selected recipients, may be group or person objects
dh.sharelink.selectedRecipients = [];

dh.sharelink.urlToShareEditBox = null;
dh.sharelink.urlTitleToShareEditBox = null;
dh.sharelink.secretCheckbox = null;
dh.sharelink.recipientComboBox = null;
dh.sharelink.descriptionRichText = null;
dh.sharelink.createGroupPopup = null;
dh.sharelink.createGroupNameEntry = null;

dh.sharelink.findGuid = function(set, id) {
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
dh.sharelink.mergeObjectsDocument = function(doc) {
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
		    dh.sharelink.allKnownIds[obj.id] = obj;
		    retval.push(obj);
		    dojo.debug(" saved new obj type = " + obj.kind + " id = " + obj.id + " display = " + obj.displayName);
		}
	}
	return retval;
}

dh.sharelink.findIdNode = function(id) {
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

dh.sharelink.forEachPossibleGroupMember = function(func) {
	var list = document.getElementById("dhRecipientList");
	for (var i = 0; i < list.childNodes.length; ++i) {
		var child = list.childNodes.item(i);
		if (child.nodeType != dojo.dom.ELEMENT_NODE)
			continue;
			
		var id = child.getAttribute("dhId");
		var obj = dh.sharelink.allKnownIds[id];

		if (obj.isPerson())
			func(child);
	}
	return null;
}

dh.sharelink.highlightPossibleGroup = function() {
	dh.sharelink.forEachPossibleGroupMember(function(node) {
		dojo.html.addClass(node, "dhCouldBeInGroup");
	});
}

dh.sharelink.unhighlightPossibleGroup = function() {
	// keep highlighted if the popup is showing
	var popup = dh.sharelink.createGroupPopup;
	if (dh.util.isShowing(popup))
		return;
	
	dh.sharelink.forEachPossibleGroupMember(function(node) {
		dojo.html.removeClass(node, "dhCouldBeInGroup");
	});
}

dh.sharelink.toggleCreateGroup = function() {
	dh.sharelink.highlightPossibleGroup();
	var popup = dh.sharelink.createGroupPopup;
	dh.util.toggleShowing(popup);
	
	if (!dh.util.isShowing(popup))
		dh.sharelink.unhighlightPossibleGroup();
}

dh.sharelink.creatingGroup = false;
dh.sharelink.doCreateGroup = function() {
	var name = dh.sharelink.createGroupNameEntry.value;

	// don't get two of these in flight at once
	if (dh.sharelink.creatingGroup)
		return;
	dh.sharelink.creatingGroup = true;
		
	var statusNode = document.getElementById("dhCreateGroupStatus");
	dojo.dom.textContent(statusNode, "Please wait...");
	dh.util.show(statusNode);
	
	var groupMembers = [];
	for (var i = 0; i < dh.sharelink.selectedRecipients.length; ++i) {
		var r = dh.sharelink.selectedRecipients[i];
		if (r.isPerson()) {
			groupMembers.push(r);
		}
	}
	var commaMembers = dh.util.join(groupMembers, ",", "id");
		
	dh.server.getXmlPOST("creategroup",
					{ 
						"name" : name, 
						"members" : commaMembers
					},
					function(type, data, http) {
						dojo.debug("got back a new group " + data);
						dojo.debug("text is : " + http.responseText);
					
						var newGroups = dh.sharelink.mergeObjectsDocument(data);

						for (var i = 0; i < newGroups.length; ++i) {							    	
							// add the group as a recipient
					    	dh.sharelink.doAddRecipient(newGroups[i].id);
						}
						
						// remove the individual members as recipients
						for (var i = 0; i < groupMembers.length; ++i) {
							dh.sharelink.removeRecipient(groupMembers[i].id);
						}
						
						dh.util.hide(dh.sharelink.createGroupPopup);
						dh.sharelink.createGroupNameEntry.value = "";
						dh.util.hide(statusNode);
						dh.sharelink.unhighlightPossibleGroup();
						dh.sharelink.creatingGroup = false;
					},
					function(type, error, http) {
						//dojo.debug("creategroup got back error " + dhAllPropsAsString(error));
						// FIXME display the error, don't hide status
						dh.util.hide(statusNode);
						dh.sharelink.creatingGroup = false;		
					});
}

dhDoCreateGroupKeyUp = function(event) {
	if (event.keyCode == 13) {
		dh.sharelink.doCreateGroup();
	}
}

dh.sharelink.removeRecipient = function(recipientId, node) {
	if (arguments.length < 2) {
		node = dh.sharelink.findIdNode(recipientId);
	}

	var objIndex = dh.sharelink.findGuid(dh.sharelink.selectedRecipients, recipientId);
	dh.sharelink.selectedRecipients.splice(objIndex, 1);
	
	if (dh.util.disableOpacityEffects) {
		node.parentNode.removeChild(node);
	} else {
		// remove the HTML representing this recipient
		var anim = dojo.fx.html.fadeOut(node, 800, function(node, anim) {
			node.parentNode.removeChild(node);
		});
	}
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
	
	dh.sharelink.removeRecipient(idToRemove, node);
}

// called when user presses Enter or the Add button
dh.sharelink.doAddRecipientFromCombo = function(createContact) {
	var cb = dh.sharelink.recipientComboBox;
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
						
				var newContacts = dh.sharelink.mergeObjectsDocument(data);
				
				for (var i = 0; i < newContacts.length; ++i) {
					// add someone; this flashes their entry and is a no-op 
					// if they were already added
					dh.sharelink.doAddRecipient(newContacts[i].id);
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
	if (!dh.sharelink.recipientComboBox.selectedResult) {
		dojo.debug("no result selected");
		return;
	}
	var id = dh.sharelink.recipientComboBox.selectedResult[1];
	dojo.debug("selected = " + id);
	dh.sharelink.doAddRecipient(id);
}

dh.sharelink.doAddRecipient = function(selectedId) {	
	
	dojo.debug("adding " + selectedId + " as recipient if they aren't already");
	
	var objKey = dh.sharelink.findGuid(dh.sharelink.allKnownIds, selectedId);
	if (!objKey) {
		// user should never get here
		alert("something went wrong adding that person ... (" + selectedId + ")");
		return;
	}
	
	var obj = dh.sharelink.allKnownIds[objKey];
	
	if (!dh.sharelink.findGuid(dh.sharelink.selectedRecipients, obj.id)) {
		
		dh.sharelink.selectedRecipients.push(obj);
		
		var idNode = document.createElement("table");
		idNode.setAttribute("dhId", obj.id);
		dojo.html.addClass(idNode, "dhRecipient");
		dojo.html.addClass(idNode, "dhItemBox");
		if (dh.util.isShowing(dh.sharelink.createGroupPopup) && obj.isPerson())
			dojo.html.addClass(idNode, "dhCouldBeInGroup");
		
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
	} else {
		dh.util.flash(dh.sharelink.findIdNode(obj.id));
	}
	
	// clear the combo again
	dh.sharelink.recipientComboBox.textInputNode.value = "";
	if (dh.sharelink.recipientComboBox._result_list_open)
		dh.sharelink.recipientComboBox.hideResultList();
	dh.sharelink.recipientComboBox.dataProvider.lastSearchProvided = null;
	dh.sharelink.recipientComboBox.dataProvider.singleCompletionId = null;
}

dhDoAddRecipientKeyUp = function(event) {
	if (event.keyCode == 13) {
		dh.sharelink.doAddRecipientFromCombo(true);
	}
}

dh.sharelink.submitButtonClicked = function() {
	dojo.debug("clicked share link button");
	
	var title = dh.sharelink.urlTitleToShareEditBox.textValue;
	
	var url = dh.sharelink.urlToShareEditBox.value;
	
	var descriptionHtml = dh.util.getTextFromRichText(dh.sharelink.descriptionRichText);
	
	var commaRecipients = dh.util.join(dh.sharelink.selectedRecipients, ",", "id");
	
	var secret = dh.sharelink.secretCheckbox.checked ? "true" : "false";
	
	dojo.debug("url = " + url);
	dojo.debug("title = " + title);
	dojo.debug("desc = " + descriptionHtml);
	dojo.debug("rcpts = " + commaRecipients);
	dojo.debug("secret = " + secret);
	
	// double-check that we're logged in
	dh.server.doPOST("sharelink",
						{ 
							"url" : url,
							"title" : title, 
						  	"description" : descriptionHtml,
						  	"recipients" : commaRecipients,
						  	"secret" : secret
						},
						function(type, data, http) {
							dojo.debug("sharelink got back data " + dhAllPropsAsString(data));
							dh.util.goToNextPage("home");
						},
						function(type, error, http) {
							dojo.debug("sharelink got back error " + dhAllPropsAsString(error));

						});
}

dh.sharelink.loadContacts = function() {
	if (dh.sharelink.haveLoadedContacts)
		return;
	
	dh.server.getXmlGET("contactsandgroups",
			{ },
			function(type, data, http) {
				dojo.debug("got back contacts " + data);
				dojo.debug("text is : " + http.responseText);
							
				dh.sharelink.mergeObjectsDocument(data);
				
				dh.sharelink.haveLoadedContacts = true;
			},
			function(type, error, http) {
				dojo.debug("getting contacts, got back error " + dhAllPropsAsString(error));
				
				// note that we don't cache an empty result set, we will retry instead...
			});
}

dh.sharelink.copyCompletions = function(completions, filterSelected) {
	// second arg is optional
	if (arguments.length < 2) {
		arguments.push(false);
	}

	var copy = [];
	for (var i = 0; i < completions.length; ++i) {
		if (filterSelected && dh.sharelink.findGuid(dh.sharelink.selectedRecipients,
							                         completions[i][1])) {
			continue;
		}
		
		copy.push([ completions[i][0], completions[i][1] ]);
	}
	return copy;
}

dh.sharelink.FriendListProvider = function() {

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
		
		for (var id in dh.sharelink.allKnownIds) {
			var obj = dh.sharelink.allKnownIds[id];
			
			var found = null;
			var matchedStr = null;
			if (matchFunc(obj.displayName, searchStr)) {
				found = obj;
				matchedStr = obj.displayName;
			} else if (obj.email && matchFunc(obj.email, searchStr)) {
				found = obj;
				matchedStr = obj.email;
			}
			
			if (found) {
				if (!dh.sharelink.findGuid(dh.sharelink.selectedRecipients,
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
			dh.sharelink.recipientComboBox.showResultList(); // is a no-op if already showing
		} else {
			dojo.debug("providing search results to the combo for '" + forSearchStr + "' results are " + resultsDataPairs);
			this.lastSearchProvided = forSearchStr;
			this.provideSearchResults(resultsDataPairs);
		}
		
		// maybe not the best place to do this
		if (this.singleCompletionId && forSearchStr.length > 0) {
			dojo.debug("adding single completion " + this.singleCompletionId);
			dh.sharelink.doAddRecipient(this.singleCompletionId);
		} else {
			dojo.debug("don't have single completion");
		}
	}
}
dojo.inherits(dh.sharelink.FriendListProvider, Object);

dh.sharelink.HtmlFriendComboBox = function(){
	// dojo.debug("creating HtmlFriendComboBox");
	dojo.widget.HtmlComboBox.call(this);
	
	this.widgetType = "FriendComboBox";
	
	this.fillInTemplate = function(args, frag){
		// override the default provider
		this.dataProvider = new dh.sharelink.FriendListProvider();
		// DEBUG - put data in the default provider
		//this.dataProvider = new dojo.widget.ComboBoxDataProvider();
		//this.dataProvider.setData(dh.sharelink.stateNames);
    }
}

dojo.inherits(dh.sharelink.HtmlFriendComboBox, dojo.widget.HtmlComboBox);

dojo.widget.manager.registerWidgetPackage("dh.sharelink");
dojo.widget.tags.addParseTreeHandler("dojo:friendcombobox");

dh.sharelink.init = function() {
	dojo.debug("dh.sharelink.init");
			
	var params = dh.util.getParamsFromLocation();
	
	dh.sharelink.urlToShareEditBox = document.getElementById("dhUrlToShare");
	
	var urlParam = params["url"]
	if (urlParam) {
		dh.sharelink.urlToShareEditBox.value = urlParam;
	} else {
		dh.sharelink.urlToShareEditBox.value = "(enter link to share)";
		var urlDiv = document.getElementById("dhUrlToShareDiv");
		dh.util.show(urlDiv);
	}
	
	dh.sharelink.urlTitleToShareEditBox = dojo.widget.manager.getWidgetById("dhUrlTitleToShare");
	var params = dh.util.getParamsFromLocation();
	if (dojo.lang.has(params, "title")) {
		dh.sharelink.urlTitleToShareEditBox.setText(params["title"]);
	}

	dh.sharelink.secretCheckbox = document.getElementById("dhSecretCheckbox");

	dh.sharelink.recipientComboBox = dojo.widget.manager.getWidgetById("dhRecipientComboBox");
	dojo.event.connect(dh.sharelink.recipientComboBox.textInputNode, "onkeyup", dj_global, "dhDoAddRecipientKeyUp");
	dojo.event.connect(dh.sharelink.recipientComboBox, "selectOption", dj_global, "dhComboBoxOptionSelected");
	
	// most of the dojo is set up now, so show the widgets
	dh.util.showId("dhShareLinkForm");
	
	// rich text areas can't exist when display:none, so we have to create it after showing
	dh.sharelink.descriptionRichText = dojo.widget.fromScript("richtext", 
															 {}, // props,
															 document.getElementById("dhShareLinkDescription"));
															 
	dh.sharelink.createGroupPopup = document.getElementById("dhCreateGroupPopup");					 
	dh.sharelink.createGroupNameEntry = document.getElementById("dhCreateGroupName");
	dojo.event.connect(dh.sharelink.createGroupNameEntry, "onkeyup",
						dj_global, "dhDoCreateGroupKeyUp");
						
	// set default focus
	dh.sharelink.recipientComboBox.textInputNode.focus();
	
	// load up your contacts
	dh.sharelink.loadContacts();
}

dhShareLinkInit = dh.sharelink.init; // connect doesn't like namespaced things
dojo.event.connect(dojo, "loaded", dj_global, "dhShareLinkInit");
