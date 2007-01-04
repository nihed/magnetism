dojo.provide("dh.groupaccount")
dojo.require("dh.server")
dojo.require("dh.util")
dojo.require("dh.formtable")
dojo.require("dh.textinput")
dojo.require("dh.photochooser")
dojo.require("dh.fileinput")
dojo.require("dh.popup")
dojo.require("dojo.dom");
dojo.require("dh.dom");
dojo.require("dh.feeds");

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
	                        dh.debug("Didn't get group in response to creategroup");
							dh.groupaccount.stopWait("Couldn't create the group")
                		}                    	
                    },
                    function(type, error, http) {
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

dh.groupaccount.hideAllFeedPopups = function() {
	dh.popup.hide('dhFeedPreviewPopup');
	dh.popup.hide('dhFeedFailedPopup');
	dh.popup.hide('dhFeedLoadingPopup');
}

dh.groupaccount.addFeed = function(feedUrl) {
   	dh.server.doXmlMethod("addgroupfeed",
				     { "groupId" : dh.groupaccount.groupId,
				       "url" :  feedUrl },
		  	    	 function(childNodes, http) {
		  	    	 	dh.groupaccount.hideAllFeedPopups();
						dh.util.refresh();
		  	    	 },
		  	    	 function(code, msg, http) {
			  	    	dh.groupaccount.hideAllFeedPopups();
		  	    		dh.formtable.showStatusMessage('dhFeedEntry', "Failed to add feed; try again? (" + msg + ")");
		  	    	 });
}

dh.groupaccount.onFeedPreview = function(childNodes, http) {
	var canceled = !dh.popup.isShowing('dhFeedLoadingPopup');
	dh.groupaccount.hideAllFeedPopups();
    if (canceled)
    	return;
    
    if (childNodes.length != 1 || childNodes.item(0).nodeName != "feedPreview")
    	throw Error("Bad feed preview XML doesn't have single feedPreview node");
	// change child nodes to be children of feedPreview
    childNodes = childNodes.item(0).childNodes;
    
	var title = null;
	var link = null;
	var source = null;
	var items = [];
	var i = 0;
	for (i = 0; i < childNodes.length; ++i) {
		var child = childNodes.item(i);
		if (child.nodeType != dojo.dom.ELEMENT_NODE)
			continue;
		
		//alert("child node name " + child.nodeName + " content: " + dh.dom.textContent(child));
		
		if (child.nodeName == "title") {
			title = dh.dom.textContent(child);
		} else if (child.nodeName == "link") {
			link = dh.dom.textContent(child);
		} else if (child.nodeName == "source") {
			source = dh.dom.textContent(child);
		} else if (child.nodeName == "item") {
			var item = {};
			var j = 0;
			for (j = 0; j < child.childNodes.length; ++j) {			
				var child2 = child.childNodes.item(j);
				
				if (child2.nodeType != dojo.dom.ELEMENT_NODE)
					continue;
				
				if (child2.nodeName == "title") {
					item["title"] = dh.dom.textContent(child2);
				} else if (child2.nodeName == "link") {
					item["link"] = dh.dom.textContent(child2);
				} else {
					alert("unknown node " + child2.nodeName + " with content: " + dh.dom.textContent(child2));
				}
			}
			items.push(item);
		} else {
			//alert("unknown node " + child.nodeName + " with content: " + dh.dom.textContent(child));
		}
	}

	if (!source)
		throw Error("something went wrong parsing feed preview");

	//alert(items.length + " items found");

	var previewNode = document.getElementById('dhFeedPreview');
	if (!previewNode)
		throw Error("no feed preview node!");

	dh.util.clearNode(previewNode);

	var feedTitleNode = document.createElement('a');
	dojo.html.addClass(feedTitleNode, 'dh-feed-title');
	feedTitleNode.setAttribute("target", "_blank");
	feedTitleNode.setAttribute("href", link);
	dh.dom.textContent(feedTitleNode, title);
	previewNode.appendChild(feedTitleNode);

	var feedItemListNode = document.createElement('ol');
	previewNode.appendChild(feedItemListNode);

	for (i = 0; i < items.length; ++i) {
		var itemNode = document.createElement('li');
		dojo.html.addClass(itemNode, 'dh-feed-item');
		dh.dom.textContent(itemNode, items[i]["title"]);
		feedItemListNode.appendChild(itemNode);
	}
    
	dh.feeds.previewOK = function() {
		dh.groupaccount.addFeed(source);
	};
	dh.feeds.previewCancel = function() {
		dh.groupaccount.hideAllFeedPopups();
	};
  	dh.popup.show('dhFeedPreviewPopup', document.getElementById('dhFeedEntry'));
}

dh.groupaccount.tryAddFeed = function() {
	var url = dh.groupaccount.feedEntry.getValue();
	if (url)
		url = dojo.string.trim(url);
	if (url.length == 0) {
		dh.formtable.showStatusMessage('dhFeedEntry', "Enter a web site URL");
		return;
	}
	
	dh.groupaccount.hideAllFeedPopups();
	
	// set up dh.feeds; wait to set up the "preview" 
	// callbacks until we get the preview info though
	dh.feeds.loadingCancel = function() {
		dh.groupaccount.hideAllFeedPopups();
	};
	dh.feeds.failedTryAgain = function() {
		dh.groupaccount.tryAddFeed();
	};
	dh.feeds.failedCancel = function() {
		dh.groupaccount.hideAllFeedPopups();
	};
	
	dh.feeds.setUrl(url);
	
	// now show the first feed popup
	dh.popup.show('dhFeedLoadingPopup', document.getElementById('dhFeedEntry'));
	
   	dh.server.doXmlMethod("feedpreview",
				     { "url" :  url },
				     dh.groupaccount.onFeedPreview,
		  	    	 function(code, msg, http) {
		  	    	 	var canceled = !dh.popup.isShowing('dhFeedLoadingPopup');
			 	    	dh.groupaccount.hideAllFeedPopups();
			 	    	if (!canceled) {
			 	    		var failedMessageNode = document.getElementById('dhFeedFailedMessage');

							dh.util.clearNode(failedMessageNode);
							dojo.html.addClass(failedMessageNode, 'dh-feed-title');
							dh.dom.textContent(failedMessageNode, msg);
							
			  	    	 	dh.popup.show('dhFeedFailedPopup', document.getElementById('dhFeedEntry'));
			  	    	}
		  	    	 });
}

dh.groupaccount.removeFeed = function(feedUrl) {
   	dh.server.doXmlMethod("removegroupfeed",
				     { "groupId" : dh.groupaccount.groupId,
				       "url" :  feedUrl },
		  	    	 function(childNodes, http) {
						dh.util.refresh();
		  	    	 },
		  	    	 function(code, msg, http) {
			  	    	 dh.formtable.showStatusMessage('dhFeedEntry', "Failed to remove feed; try again? (" + msg + ")");
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
