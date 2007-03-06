dojo.provide("dh.actions");

dojo.require("dh.server");
dojo.require("dh.util");
dojo.require("dh.asyncActionLink")
dojo.require("dh.control");

dh.actions.addContact = function(contactId, cb, errcb) {
   	dh.server.doPOST("addcontactperson",
				     { "contactId" : contactId },
		  	    	 function(type, data, http) {
		  	    	 	if (cb)
		  	    	 		cb()
		  	    	 	else
		  	    	 		dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	 	if (errcb)
		  	    	 		errcb()
		  	    	 	else
		  	    	    	alert("Couldn't add user to contact list");
		  	    	 });
}

dh.actions.removeContactConfirmation = function(name, resourceId, invited) {
    if (invited) {
        var idSuffix = 'InvList';
        var removeFunction = function() {
	        dh.actions.removeInvitedContact(resourceId);
	    };
	} else {
        var idSuffix = 'ContactsList';
        var removeFunction = function() {
	        dh.actions.removeContact(resourceId)
	    };	
	}                   
	                        
	dh.util.showMessage('Remove ' + name + ' from your contacts?', idSuffix,
	                    removeFunction,
	                    function() {
	                        dh.util.showMessage(null, idSuffix);
	                    });
}

dh.actions.removeContact = function(contactObjectId) {
	dh.server.doPOST("removecontactobject",
				     { "contactObjectId" : contactObjectId },
		  	    	 function(type, data, http) {
		  	    	 	 dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't remove contact from contact list");
		  	    	 });
}

dh.actions.removeInvitedContact = function(resourceId) {
	dh.server.doPOST("removeinvitedcontact",
				     { "resourceId" : resourceId },
		  	    	 function(type, data, http) {
		  	    	 	 dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't remove invited contact");
		  	    	 });
}

dh.actions.joinGroup = function(groupId, cb, errcb) {
  	dh.server.doPOST("joingroup",
			 	     { "groupId" : groupId },
  					 function(type, data, http) {
  					 	if (!cb)
			  		 		dh.util.refresh();
			  		 	else
			  		 		cb();
					 },
					 function(type, error, http) {
					 	if (!errcb)
							alert("Couldn't join group");
						else
							errcb();
					 });
}

dh.actions.leaveGroup = function(groupId, cb, errcb) {
  	dh.server.doPOST("leavegroup",
			 	     { "groupId" : groupId },
  					 function(type, data, http) {
  					 	if (!cb)
			  		 		dh.util.refresh();
			  		 	else
			  		 		cb()
					 },
					 function(type, error, http) {
					 	if (!errcb)
							alert("Couldn't leave group");
						else
							errcb()
					 });
}

dh.actions.addMember = function(groupId, id, onSuccess) {
	dh.server.getXmlPOST("addmembers",
		{ 
			"groupId" : groupId, 
			"members" : id
		},
		function(type, data, http) {
			onSuccess();
		},					
		function(type, error, http) {	
		});
}

dh.actions.removeGroupInvitee = function(groupId, email) {
    dh.server.doPOST("suggestgroups",
		   		     { "address" : email, 
					   "suggestedGroupIds" : "",
					   "desuggestedGroupIds" : groupId },
		  	    	 function(type, data, http) {
		  	    	       dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't remove a group invitee.");
		  	    	 });
}
		  	    	     
dh.actions.signOut = function() {
   	dh.server.doPOST("signout", { },
		  	    	 function(type, data, http) {
		  	    	 	// don't reload the current page since often it will require signin
		  	    	 	// phony signout parameter prevents IE from showing cached (e.g. logged in) page
			  	    	 window.open("/?signout=true", "_self");
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't sign out");
		  	    	 });
}

dh.actions.enableAccount = function(next) {
   	dh.server.doPOST("setaccountdisabled", 
   					 { "disabled": "false" },
		  	    	 function(type, data, http) {
		  	    	 	 if (next)
			  	    	 	 dh.util.goToNextPage(next)
			  	    	 else
				  	    	 dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Error enabling account");
		  	    	 });
}

dh.actions.disableAccount = function() {
   	dh.server.doPOST("setaccountdisabled",
   					{ "disabled" : "true" },
		  	    	 function(type, data, http) {
			  	    	 dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't disable account");
		  	    	 });
}

dh.actions.updateGetStarted = function() {
	var link = document.getElementById("dhGetStarted");
	var image = document.getElementById("dhGetStartedButton");
	if (document.getElementById("dhAcceptTerms").checked) {
		image.src = dhImageRoot3 + "get_started_button.gif";
		link.href= "javascript:dh.actions.acceptTerms()";
	} else {
		image.src = dhImageRoot3 + "get_started_disabled.gif"
		link.removeAttribute("href");
	}
}

dh.actions.acceptTerms = function() {
	if (!document.getElementById("dhAcceptTerms").checked) { // paranoia
		alert("You must first accept the Mugshot Terms of Use");
		return;
	}
   	dh.server.doPOST("acceptterms",
   					 {},
		  	    	 function(type, data, http) {
			  	    	 dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't accept the terms of use");
		  	    	 });
}

dh.actions.removeDownloadMessage = function() {
   	document.getElementById("dhAccountStatus").style.display = "none";
   	dh.server.doPOST("setneedsdownload",
   					 { "needsDownload" : "false" },
		  	    	 function(type, data, http) {
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't remove the download message");
		  	    	 });
}

dh.actions.setMusicSharingEnabled = function(enabled) {
   	dh.server.doPOST("setmusicsharingenabled",
   					{ "enabled" : enabled ? "true" : "false" },
		  	    	 function(type, data, http) {
			  	    	 dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't toggle music sharing");
		  	    	 });
}

dh.actions.setNotifyPublicShares = function(notify) {
   	dh.server.doPOST("setnotifypublicshares",
   					{ "notify" : notify ? "true" : "false" },
		  	    	 function(type, data, http) {
			  	    	 dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't toggle default public share");
		  	    	 });
}

dh.actions.setPersonQuip = function(entity, isLove, text) {
   	dh.server.doPOST("setquip",
   					{ entityType: "person", entity : entity, isLove: isLove, text: text },
		  	    	 function(type, data, http) {
			  	    	 dh.util.refresh();
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
	  	    	 	 dh.util.refresh();
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
		  	    	 	 dh.util.refresh();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't change favoriteness of post");
		  	    	 });
}

dh.actions.switchPage = function (name, anchor, newPage) {
	var params = dh.util.getParamsFromLocation()
	var positions = {}
	var oldPosString = params["pos"]
	if (oldPosString) {
		var settings = oldPosString.split(/!/)
		for (var i = 0; i < settings.length; i++) {
			var colon = settings[i].indexOf("-")
			if (colon > 0) {
				var page = parseInt(settings[i].substring(colon + 1))
				if (!isNaN(page)) {
					positions[settings[i].substring(0, colon)] = page
				}
			}
		}
	}
	
	positions[name] = newPage
	
	var newPosString = ""
	for (var key in positions) {
		if (positions[key] > 0) {
			if (newPosString != "") {
				newPosString += "!"
			}
			newPosString += key + "-" + positions[key]
		}
	}
	
	if (newPosString != "")
		params["pos"] = newPosString
	else
		delete params["pos"]
	
	var newQuery = dh.util.encodeQueryString(params)
	var newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname +
	            newQuery
	if (anchor != null && anchor != "")
		newUrl += "#" + anchor
	window.location.replace(newUrl)
	
	return false
}

dh.actions.validateEmailInput = function(emailInputId) {
	var emailInput = document.getElementById(emailInputId)
	return dh.util.validateEmail(emailInput.value)
}

// This is a wrapper around dh.control.control.showChatWindow(chatId)
// that lazily tries loading the control
dh.actions.joinChat = function(chatId) {
	dh.control.createControl();

	if (dh.control.control.haveLiveChat()) {
		dh.control.control.showChatWindow(chatId);
	} else if (dh.browser.linux) {
		var url = dhBaseUrl.replace(/^http:/, "mugshot:");
		url += "/joinChat?id=" + chatId;
    	window.open(url, "_self");
    } else {
		window.open("/download", '_NEW');
	}
}

dh.actions.setApplicationUsageEnabled = function(enabled) {
   	dh.server.doPOST("setapplicationusageenabled",
				     { "enabled" : enabled },
		  	    	 function(type, data, http) {
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't change application usage sharing preference.");
		  	    	 });
}
