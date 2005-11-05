dojo.provide("dh.sharegroup");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.html");
dojo.require("dh.share");
dojo.require("dh.server");
dojo.require("dh.util");

dh.sharegroup.submitButtonClicked = function() {
	dojo.debug("clicked share link button");
	
	var descriptionHtml = dh.util.getTextFromRichText(dh.share.descriptionRichText);
	
	var commaRecipients = dh.util.join(dh.share.selectedRecipients, ",", "id");
	
	var secret = false;
	
	dojo.debug("groupId = " + dhShareGroupId);
	dojo.debug("desc = " + descriptionHtml);
	dojo.debug("rcpts = " + commaRecipients);
	
	// double-check that we're logged in
	dh.server.doPOST("sharegroup",
						{ 
							"groupId" : dhShareGroupId,
						  	"description" : descriptionHtml,
						  	"recipients" : commaRecipients,
						},
						function(type, data, http) {
							dojo.debug("sharegroup got back data " + dhAllPropsAsString(data));
							dh.util.goToNextPage("viewgroup?groupId=" + dhShareGroupId, 
							                     "The group has been shared!");
						},
						function(type, error, http) {
							dojo.debug("sharegroup got back error " + dhAllPropsAsString(error));
						});
}

dh.sharegroup.loadContacts = function() {
	if (dh.share.haveLoadedContacts)
		return;
	
	dh.server.getXmlGET("addablecontacts",
			{ 
				"groupId" : dhShareGroupId
			},
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


dh.sharegroup.init = function() {
	dojo.debug("dh.sharegroup.init");
			
	// most of the dojo is set up now, so show the widgets
	dh.util.showId("dhShareGroupForm");
	
	dh.share.init();
	
	// set default focus
	dh.share.recipientComboBox.textInputNode.focus();
	
	// load up your contacts
	dh.sharegroup.loadContacts();
}

dhShareGroupInit = dh.sharegroup.init; // connect doesn't like namespaced things
dojo.event.connect(dojo, "loaded", dj_global, "dhShareGroupInit");
