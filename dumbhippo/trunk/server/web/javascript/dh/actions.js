dojo.provide("dh.actions");

dojo.require("dh.server");

dh.actions.requestJoinRoom = function(chatRoomName) {
   	dh.server.doPOST("requestjoinroom",
				     { "chatRoomName" : chatRoomName },
		  	    	 function(type, data, http) {
		  	    	 	 window.setTimeout('document.location.reload()', 5000);
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't request to join room");
		  	    	 });
}

dh.actions.addContact = function(contactId) {
   	dh.server.doPOST("addcontactperson",
				     { "contactId" : contactId },
		  	    	 function(type, data, http) {
		  	    	 	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't add user to contact list");
		  	    	 });
}

dh.actions.removeContact = function(contactId) {
	dh.server.doPOST("removecontactperson",
				     { "contactId" : contactId },
		  	    	 function(type, data, http) {
		  	    	 	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't remove user from contact list");
		  	    	 });
}

dh.actions.joinGroup = function(groupId) {
  	dh.server.doPOST("joingroup",
			 	     { "groupId" : groupId },
  					 function(type, data, http) {
			  		 	 document.location.reload();
					 },
					 function(type, error, http) {
						 alert("Couldn't join group");
					 });
}

dh.actions.leaveGroup = function(groupId) {
  	dh.server.doPOST("leavegroup",
			 	     { "groupId" : groupId },
  					 function(type, data, http) {
			  		 	 document.location.reload();
					 },
					 function(type, error, http) {
						 alert("Couldn't leave group");
					 });
}

dh.actions.signOut = function() {
   	dh.server.doPOST("signout", { },
		  	    	 function(type, data, http) {
		  	    	 	// don't reload the current page since often it will require signin
			  	    	 window.open("/main", "_self");
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't sign out");
		  	    	 });
}

dh.actions.setAccountDisabled = function(disabled) {
   	dh.server.doPOST("setaccountdisabled",
   					{ "disabled" : disabled ? "true" : "false" },
		  	    	 function(type, data, http) {
			  	    	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't disable account");
		  	    	 });
}

// This handler function gets stuffed as the a member function of
// a dojo.widget.HtmlInlineEditBox
dh.actions.renamePersonHandler = function(value, oldValue) {
	this.setText("Please wait...");
   	dh.server.doPOST("renameperson",
				     { "name" : value },
			  	    	 function(type, data, http) {
	  	    	 	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't rename user");
		  	    	     this.setText(oldValue);
		  	    	 });
	    }

dh.actions.fillAlphaPng = function(image) {
    var span = image.parentNode
    var src = image.src
    span.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale');"
    span.style.background = "transparent";
}
