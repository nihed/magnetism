dojo.provide("dh.actions");

dojo.require("dh.server");

dh.actions.requestJoinRoom = function(userId, chatId) {
    // Check readyState is to see if the object was actually loaded.
    var embed = document.getElementById("dhEmbedObject");
    if (embed && embed.readyState && embed.readyState >= 3) {
		embed.showChatWindow(userId, chatId);
	} else {
		// should only show up when we suck and don't remove the "join chat" option
		// in advance, but here as a fallback
		alert("Chat requires the DumbHippo software to be installed on Windows Internet Explorer; " + 
		"we're working on a web-only version, stay tuned");
	}
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

dh.actions.setMusicSharingEnabled = function(enabled) {
   	dh.server.doPOST("setmusicsharingenabled",
   					{ "enabled" : enabled ? "true" : "false" },
		  	    	 function(type, data, http) {
			  	    	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't toggle music sharing");
		  	    	 });
}

dh.actions.setNotifyPublicShares = function(notify) {
   	dh.server.doPOST("setnotifypublicshares",
   					{ "notify" : notify ? "true" : "false" },
		  	    	 function(type, data, http) {
			  	    	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't toggle default public share");
		  	    	 });
}

dh.actions.setPersonQuip = function(entity, isLove, text) {
   	dh.server.doPOST("setquip",
   					{ entityType: "person", entity : entity, isLove: isLove, text: text },
		  	    	 function(type, data, http) {
			  	    	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't set quip");
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

dh.actions.showChangePhoto = function(n) {
	dh.util.hideId("dhChangePhotoLink" + n)
	dh.util.showId("dhPhotoUploadFileEntry" + n)
	// We set the image here to start it loading so that we hopefully
	// have it already loaded by the time we need to display progress
	var progress = document.getElementById("dhPhotoUploadProgress" + n)
	if (Math.random() < 0.5)
		progress.src = dhImageRoot + "HulaHippo.gif"
	else
		progress.src = dhImageRoot + "HulaPhotographer.gif"
}

dh.actions.doChangePhoto = function(n) {
	dh.util.hideId("dhPhotoUploadFileEntry" + n)
	document.forms["dhPhotoUploadForm" + n].submit()
	var progress = document.getElementById("dhPhotoUploadProgress" + n)
	progress.src = progress.src // Restarts the animation after post in IE
	dh.util.hideId("dhPhoto-192")
	dh.util.show(progress)
	document.getElementById("dhChangePhotoLink" + n).disabled = true
}

dh.actions.setPostFavorite = function(postId, favorite) {
   	dh.server.doPOST("setfavoritepost",
				     { "postId" : postId,
				     	"favorite" : favorite },
		  	    	 function(type, data, http) {
		  	    	 	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't change favoriteness of post");
		  	    	 });
}
