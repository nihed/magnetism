dojo.provide("dh.groupinvitation")
dojo.require("dh.autosuggest")
dojo.require("dh.model")
dojo.require("dh.server")
dojo.require("dh.suggestutils")
dojo.require("dh.textinput")
dojo.require("dojo.string")

// hash of all persons we should autocomplete on, keyed by guid
dh.groupinvitation.allKnownIds = { };

dh.groupinvitation.reloadWithMessage = function(message) {
	// We do this as a POST to avoid including the message in the URL
	var body = dojo.string.trim(dh.groupinvitation.messageEntry.getValue())
	
	document.getElementById("dhReloadMessage").value = message
	document.getElementById("dhReloadBody").value = body
	document.forms["dhReloadForm"].submit()
}

// merges an XML document into allKnownIds
dh.groupinvitation.mergeObjectsDocument = function(doc) {
	var objectsElement = doc.getElementsByTagName("objects").item(0);
	if (!objectsElement) {
		dojo.debug("no <objects> element");
		return
	}
	var nodeList = objectsElement.childNodes;
	for (var i = 0; i < nodeList.length; ++i) {
		var element = nodeList.item(i);
		if (element.nodeType != dojo.dom.ELEMENT_NODE) {
			continue;
		} else {
			var obj = dh.model.objectFromXmlNode(element);
		    dh.groupinvitation.allKnownIds[obj.id] = obj;
		}
	}
	
	// update the autocompletions
	dh.groupinvitation.autoSuggest.checkUpdate(true);
}

dh.groupinvitation.loadContacts = function() {
	dh.server.getXmlGET("addablecontacts",
			{ 
				"groupId" : dh.groupinvitation.groupId
			},
			function(type, data, http) {
				dh.groupinvitation.mergeObjectsDocument(data);
			},
			function(type, error, http) {
				dojo.debug("getting contacts, got back error " + dhAllPropsAsString(error));
			});
}

dh.groupinvitation.getEligibleRecipients = function() {
    if (dh.groupinvitation.autoSuggest.menuMode) {
  		return dh.suggestutils.getMenuRecipients(dh.groupinvitation.allKnownIds)
	} else {
   		return dh.suggestutils.getMatchingRecipients(dh.groupinvitation.allKnownIds, 
   													 dh.groupinvitation.autoSuggest.inputText)
	}
}

dh.groupinvitation.recipientSelected = function(selectedId) {
	var obj = dh.groupinvitation.allKnownIds[selectedId]
	
	dh.groupinvitation.autoSuggest.setText(obj.displayName)
	
	// Save both the text and the ID, so we can tell when the user subsequently edits
	dh.groupinvitation.selectedText = obj.displayName
	dh.groupinvitation.selectedId = selectedId
}

dh.groupinvitation.send = function() {
	var subject = dojo.string.trim(dh.groupinvitation.subjectEntry.getValue())
	var message = dojo.string.trim(dh.groupinvitation.messageEntry.getValue())
	
	var params = { 
        "groupId" : dh.groupinvitation.groupId,
        "subject" : subject,
	    "message" : message
    }

	var currentText = dh.groupinvitation.autoSuggest.elem.value
    
    if (dh.groupinvitation.selectedId != null && dh.groupinvitation.selectedText == currentText) {
    	params["inviteeId"] = dh.groupinvitation.selectedId
    } else {
		var address = dojo.string.trim(currentText)
		if (address == "" || address.indexOf("@") < 0) {
			alert("Please enter a valid email address")
			return
		}
		params["inviteeAddress"] = address
    }
    
    dh.server.getXmlPOST("sendgroupinvitation",
    				params,
                    function(type, document, http) {
                    	var messages = document.getElementsByTagName("message")
                    	if (messages.length > 0) {
                    		dh.groupinvitation.reloadWithMessage(dojo.dom.textContent(messages[0]))
                		} else {
	                        dojo.debug("Didn't get message in response to sendgroupinvitation");
                		}
                    },
                    function(type, error, http) {
                        dojo.debug("sendgroupinvitation got back error " + dhAllPropsAsString(error));
  	                })
}

dh.groupinvitation.fillValues = function(values) {
	dh.groupinvitation.subjectEntry.setValue(values["dhSubjectEntry"])
	dh.groupinvitation.messageEntry.setValue(values["dhMessageEntry"])
}

dhGroupInvitationInit = function() {
	// The AutoSuggest class needs a div
    //   <div id="dhAutoSuggest" class="dhInvisible"><ul></ul></div>
    // With our page layout, it has to be a direct child of <body> to
    // get positioned correctly, which is hard with our tag structure,
    // so we create it dynamically. It seems to work fine, despite
    // the comments in autosuggest.js.
	var div = document.createElement("div")
	div.id = "dhAutoSuggest"
	div.className = "dhInvisible"
	div.appendChild(document.createElement("ul"))
	document.body.appendChild(div)
	
	dh.groupinvitation.autoSuggest = new dh.autosuggest.AutoSuggest(document.getElementById("dhAddressEntry"),
                                                               document.getElementById("dhAddressButton"))
	dh.groupinvitation.autoSuggest.setOnSelectedFunc(dh.groupinvitation.recipientSelected);
	dh.groupinvitation.autoSuggest.setGetEligibleFunc(dh.groupinvitation.getEligibleRecipients);

	dh.groupinvitation.subjectEntry = new dh.textinput.Entry(document.getElementById("dhSubjectEntry"))
	dh.groupinvitation.messageEntry = new dh.textinput.Entry(document.getElementById("dhMessageEntry"))
	
	dh.groupinvitation.fillValues(dh.groupinvitation.initialValues)
	dh.groupinvitation.loadContacts()
}
