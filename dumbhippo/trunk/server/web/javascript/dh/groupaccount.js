dojo.provide("dh.groupaccount")
dojo.require("dh.server")
dojo.require("dh.util")
dojo.require("dh.formtable")
dojo.require("dh.textinput")
dojo.require("dh.photochooser")
dojo.require("dh.fileinput")
dojo.require("dojo.dom")
dojo.require("dh.popup")

dh.groupaccount.startWait = function() {
	dh.util.showMessage("Please wait...")
	document.getElementById("dhCreateGroupSave").disabled = true
	document.getElementById("dhCreateGroupCancel").disabled = true
}

dh.groupaccount.stopWait = function(message) {
	dh.util.showMessage(message)
	document.getElementById("dhCreateGroupSave").disabled = false
	document.getElementById("dhCreateGroupCancel").disabled = false
}

dh.groupaccount.createGroup = function() {
	var secret = document.getElementById("dhGroupVisibilityPrivate").checked
	var groupName = dojo.string.trim(dh.groupaccount.groupNameEntry.getValue())
	var description = dojo.string.trim(dh.groupaccount.aboutGroupEntry.getValue())
	
	if (groupName == "") {
		alert("Please enter a group name")
		return
	}

	dh.groupaccount.startWait()
	dh.server.getXmlPOST("creategroup",
					{
						"name" : groupName,
						"secret" : secret,
						"description" : description
					},
                    function(type, doc, http) {
                    	var groups = doc.getElementsByTagName("group")
                    	if (groups.length > 0 && groups[0].getAttribute("id") != null) {
							dh.groupaccount.stopWait()
							document.location.href = "/group-invitation?group=" + groups[0].getAttribute("id")
                		} else {
	                        dojo.debug("Didn't get group in response to creategroup");
							dh.groupaccount.stopWait("Couldn't create the group")
                		}                    	
                    },
                    function(type, error, http) {
                        dojo.debug("creategroup got back error " + dhAllPropsAsString(error));
						dh.groupaccount.stopWait("Couldn't create the group")
  	                })
}

dh.groupaccount.updateName = function(name) {
	document.title = "Settings for " + name
	var nameDiv = document.getElementById("dhSidebarBoxProfileGroupName")
	dh.util.clearNode(nameDiv)
	nameDiv.appendChild(document.createTextNode(name))
}

dh.groupaccount.updateDescription = function(description) {
	var nameDiv = document.getElementById("dhSidebarBoxProfileGroupDescription")
	dh.util.clearNode(nameDiv)
	nameDiv.appendChild(document.createTextNode(description))
}

dh.groupaccount.addFeed = function(feedUrl) {
   	dh.server.doXmlMethod("addgroupfeed",
				     { "groupId" : dh.groupaccount.groupId,
				       "url" :  url },
		  	    	 function(childNodes, http) {
						// FIXME
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	// FIXME
		  	    	 });
}

dh.groupaccount.tryAddFeed = function() {
	var url = dh.groupaccount.feedEntry.getValue();
	if (url)
		url = dojo.string.trim(url);
	if (url.length == 0) {
		dh.formtable.showStatusMessage('dhFeedEntry', "Enter a web site URL");
		return;
	}
	
	dh.popup.show('dhFeedLoadingPopup', document.getElementById('dhFeedEntry'));
	
   	dh.server.doXmlMethod("feedpreview",
				     { "url" :  url },
		  	    	 function(childNodes, http) {
						// FIXME
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	// FIXME
		  	    	 });
}

dh.groupaccount.removeFeed = function(feedUrl) {
   	dh.server.doXmlMethod("removegroupfeed",
				     { "groupId" : dh.groupaccount.groupId,
				       "url" :  url },
		  	    	 function(childNodes, http) {
						// FIXME
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	// FIXME
		  	    	 });
}

dhCreateGroupInit = function() {
	dh.groupaccount.groupNameEntry = new dh.textinput.Entry(document.getElementById("dhGroupNameEntry"), "Mugshot Fans")
	dh.groupaccount.aboutGroupEntry = new dh.textinput.Entry(document.getElementById("dhAboutGroupEntry"))
}

dhGroupAccountInit = function() {
	dh.groupaccount.groupNameEntry = new dh.textinput.Entry(document.getElementById("dhGroupNameEntry"), "Mugshot Fans", dh.formtable.currentValues['dhGroupNameEntry'])

	dh.formtable.undoValues['dhGroupNameEntry'] = dh.groupaccount.groupNameEntry.getValue();
	dh.groupaccount.groupNameEntry.onValueChanged = function(value) {
		dh.formtable.onValueChanged(dh.groupaccount.groupNameEntry, 'renamegroup', 'name', value,
			"Saving group name...",
			"The group name has been saved.",
			{ "groupId" : dh.groupaccount.groupId },
			dh.groupaccount.updateName);
	}
	
	dh.groupaccount.aboutGroupEntry = new dh.textinput.Entry(document.getElementById("dhAboutGroupEntry"), "", dh.formtable.currentValues['dhAboutGroupEntry'])

	dh.formtable.undoValues['dhAboutGroupEntry'] = dh.groupaccount.aboutGroupEntry.getValue();
	dh.groupaccount.aboutGroupEntry.onValueChanged = function(value) {
		dh.formtable.onValueChanged(dh.groupaccount.aboutGroupEntry, 'setgroupdescription', 'description', value,
			"Saving group description...",
			"The group description has been saved.",
			{ "groupId" : dh.groupaccount.groupId },
			dh.groupaccount.updateDescription);
	}

	// add some event handlers on the file input (onchange)
	new dh.fileinput.Entry(document.getElementById('dhPictureEntry'));
	
	dh.photochooser.init("group", dh.groupaccount.groupId);
	
	dh.groupaccount.feedEntry = new dh.textinput.Entry(document.getElementById("dhFeedEntry"), 'Enter feed URL', null);
}
