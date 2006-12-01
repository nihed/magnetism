dojo.provide("dh.account");
dojo.require("dh.formtable");
dojo.require("dh.textinput");
dojo.require("dh.fileinput");
dojo.require("dh.photochooser");
dojo.require("dh.lovehate");
dojo.require('dh.password');

dh.account.generatingRandomBio = false;
dh.account.generateRandomBio = function() {
	if (dh.account.generatingRandomBio) {
		dh.formtable.showStatusMessage('dhBioEntry', "Working on it - be patient!");
		return;
	}

	dh.formtable.showStatus('dhBioEntry', "Generating random bio...", null,
			  	    	 null, null);
	dh.account.generatingRandomBio = true;
	dh.server.getTextGET("randombio", 
						{ },
						function(type, data, http) {
							dh.formtable.showStatusMessage('dhBioEntry', "Tada! Random bio!");
							dh.account.generatingRandomBio = false;
							// focus and set the new text
							dh.account.bioEntryNode.select();
							// don't emit changed until user causes it
							dh.account.bioEntry.setValue(data, true);
						},
						function(type, error, http) {
							dh.formtable.showStatusMessage('dhBioEntry', "Failed to generate random bio - we suck, sorry! Try again soon.");
		  	    	 		dh.account.generatingRandomBio = false;
						});
}


dh.account.verifyEmail = function() {
	var emailEntryNode = document.getElementById('dhEmailEntry');
	if (emailEntryNode.value.indexOf("@") < 0) {
		dh.formtable.showStatusMessage('dhEmailEntry', "Enter an email address, then click Verify");
		return;
	}
	var address = emailEntryNode.value;
  	dh.server.doPOST("sendclaimlinkemail",
			 	     { "address" : address },
  					 function(type, data, http) {
	  					 dh.formtable.showStatusMessage('dhEmailEntry', "We sent mail to '" + address + "', click on the link in that mail.");
	  					 emailEntryNode.value = "";
					 },
					 function(type, error, http) {
						 dh.formtable.showStatusMessage('dhEmailEntry', "Failed to send mail - check the address, or just try again...");
					 });
}

dh.account.removeClaimEmail = function(address) {
  	dh.server.doPOST("removeclaimemail",
			 	     { "address" : address },
  					 function(type, data, http) {
  					 	document.location.reload();
					 },
					 function(type, error, http) {
						 alert("Couldn't remove this address.");
					 });
}

dh.account.removeClaimAim = function(address) {
  	dh.server.doPOST("removeclaimaim",
			 	     { "address" : address },
  					 function(type, data, http) {
  					 	document.location.reload();
					 },
					 function(type, error, http) {
						 alert("Couldn't remove this address.");
					 });
}

dh.account.hateExternalAccount = function(type, quip, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("hateexternalaccount",
				     { "type" : type,
				       "quip" :  quip },
					loadFunc, errorFunc);
}

dh.account.removeExternalAccount = function(type, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("removeexternalaccount",
				     { "type" : type },
						loadFunc, errorFunc);
}
dh.account.findFlickrAccount = function(email, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("findflickraccount",
				     { "email" : email },
						loadFunc, errorFunc);
}

dh.account.setFlickrAccount = function(nsid, email, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setflickraccount",
				     { "nsid" : nsid, "email" : email },
				     	loadFunc, errorFunc);
}
dh.account.setLinkedInProfile = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setlinkedinprofile",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setMyspaceName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setmyspacename",
				     { "name" : name },
						loadFunc, errorFunc);
}
dh.account.setYouTubeName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setyoutubename",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setLastFmName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setlastfmname",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}

dh.account.createExternalAccountOnHateSavedFunc = function(entry, accountType) {
	return function(value) {
		var oldMode = entry.getMode();
		entry.setBusy();
		dh.account.hateExternalAccount(accountType, value,
		 	    	 function(childNodes, http) {
		 	    	 	entry.setMode('hate');
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	alert(msg);
		  	    	 	entry.setMode(oldMode);
		  	    	 });
	}
}

dh.account.createExternalAccountOnCanceledFunc = function(entry, accountType) {
	return function(value) {
		var oldMode = entry.getMode();
		entry.setBusy();
		dh.account.removeExternalAccount(accountType, 
		 	    	 function(childNodes, http) {
		 	    	 	entry.setMode('indifferent');
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	alert(msg);
		  	    	 	entry.setMode(oldMode);
		  	    	 });
	}
}

dh.account.onFlickrLoveSaved = function(value) {
	var entry = dh.account.flickrEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
	dh.account.findFlickrAccount(value,
			function(childNodes, http) {

				// change child nodes to be children of flickrUser
			    childNodes = childNodes.item(0).childNodes;
			    var nsid = null;
			    var username = null;
				var i = 0;
				for (i = 0; i < childNodes.length; ++i) {
					var child = childNodes.item(i);
					if (child.nodeType != dojo.dom.ELEMENT_NODE)
						continue;
		
					if (child.nodeName == "nsid") {
						nsid = dojo.dom.textContent(child);
					} else if (child.nodeName == "username") {
						username = dojo.dom.textContent(child);
					}
				}
				
				dh.account.setFlickrAccount(nsid, value,
					function(childNodes, http) {
						entry.setMode('love');
					},
					function(code, msg, http) {
						alert(msg);
			  	     	entry.setMode(oldMode);			
					});
			},
	  	    function(code, msg, http) {
	  	     	alert(msg);
	  	     	entry.setMode(oldMode);
	  	    });
}

dh.account.onMyspaceLoveSaved = function(value) {
	var entry = dh.account.myspaceEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setMyspaceName(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onYouTubeLoveSaved = function(value) {
	var entry = dh.account.youTubeEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setYouTubeName(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onLastFmLoveSaved = function(value) {
	var entry = dh.account.lastFmEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setLastFmName(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onLinkedInLoveSaved = function(value) {
	var entry = dh.account.linkedInEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setLinkedInProfile(value,
	 	    	 function(childNodes, http) {
				    var username = null;
					var i = 0;
					for (i = 0; i < childNodes.length; ++i) {
						var child = childNodes.item(i);
						if (child.nodeType != dojo.dom.ELEMENT_NODE)
							continue;
			
						if (child.nodeName == "username") {
							username = dojo.dom.textContent(child);
						}
	 	    	 	}
					entry.setLoveValueAlreadySaved(username);
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dhAccountInit = function() {
	dh.account.usernameEntryNode = document.getElementById('dhUsernameEntry');
	dh.account.usernameEntry = new dh.textinput.Entry(dh.account.usernameEntryNode, "J. Doe", dh.formtable.currentValues['dhUsernameEntry']);
	
	dh.formtable.undoValues['dhUsernameEntry'] = dh.account.usernameEntry.getValue();
	dh.account.usernameEntry.onValueChanged = function(value) {
		dh.formtable.onValueChanged(dh.account.usernameEntry, 'renameperson', 'name', value,
		"Saving user name...",
		"Your user name has been saved.");
	}
	
	dh.account.bioEntryNode = document.getElementById('dhBioEntry');
	dh.account.bioEntry = new dh.textinput.Entry(dh.account.bioEntryNode, "I grew up in Kansas.", dh.formtable.currentValues['dhBioEntry']);

	dh.formtable.undoValues['dhBioEntry'] = dh.account.bioEntry.getValue();
	dh.account.bioEntry.onValueChanged = function(value) {
		dh.formtable.onValueChanged(dh.account.bioEntry, 'setbio', 'bio', value,
		"Saving new bio...",
		"Your bio has been saved.");
	}
	
//	dh.account.musicbioEntryNode = document.getElementById('dhMusicBioEntry');
//	dh.account.musicbioEntry = new dh.textinput.Entry(dh.account.musicbioEntryNode, "If you listen to Coldplay, I want to meet you.", dh.formtable.currentValues['dhMusicBioEntry']);
 
// 	dh.formtable.undoValues['dhMusicBioEntry'] = dh.account.musicbioEntry.getValue();
//	dh.account.musicbioEntry.onValueChanged = function(value) {
//		dh.formtable.onValueChanged(dh.account.musicbioEntry, 'setmusicbio', 'musicbio', value,
//		"Saving new music bio...",
//		"Your music bio has been saved.");
//	}

	dh.account.rhapsodyListeningHistoryEntryNode = document.getElementById('dhRhapsodyListeningHistoryEntry');
	dh.account.rhapsodyListeningHistoryEntry = new dh.textinput.Entry(dh.account.rhapsodyListeningHistoryEntryNode, "Rhapsody recent plays RSS URL", dh.formtable.currentValues['dhRhapsodyListeningHistoryEntry']);

	dh.formtable.undoValues['dhRhapsodyListeningHistoryEntry'] = dh.account.rhapsodyListeningHistoryEntry.getValue();
	dh.account.rhapsodyListeningHistoryEntry.onValueChanged = function(value) {
		dh.formtable.onValueChangedXmlMethod(dh.account.rhapsodyListeningHistoryEntry, 'setrhapsodyhistoryfeed', 'url', value,
		"Saving new Rhapsody recent plays RSS feed...",
		"Your Rhapsody recent plays RSS feed has been updated."); // phrasing "updated" is because it could also be removed
	}
	
	dh.account.websiteEntryNode = document.getElementById('dhWebsiteEntry');
	dh.account.websiteEntry = new dh.textinput.Entry(dh.account.websiteEntryNode, "Your website URL", dh.formtable.currentValues['dhWebsiteEntry']);

	dh.formtable.undoValues['dhWebsiteEntry'] = dh.account.websiteEntry.getValue();
	dh.account.websiteEntry.onValueChanged = function(value) {
		dh.formtable.onValueChangedXmlMethod(dh.account.websiteEntry, 'setwebsite', 'url', value,
		"Saving your website address...",
		"Your website link has been updated.");  // phrasing "updated" is because it could also be removed
	}

	dh.account.blogEntryNode = document.getElementById('dhBlogEntry');
	dh.account.blogEntry = new dh.textinput.Entry(dh.account.blogEntryNode, "Your blog URL", dh.formtable.currentValues['dhBlogEntry']);

	dh.formtable.undoValues['dhBlogEntry'] = dh.account.blogEntry.getValue();
	dh.account.blogEntry.onValueChanged = function(value) {
		dh.formtable.onValueChangedXmlMethod(dh.account.blogEntry, 'setblog', 'url', value,
		"Saving your blog address...",
		"Your blog link has been updated.");  // phrasing "updated" is because it could also be removed
	}
	
	// add some event handlers on the file input
	dh.account.photoEntry = new dh.fileinput.Entry(document.getElementById('dhPictureEntry'));
	
	// make pressing enter submit the email verify
	var emailEntryNode = document.getElementById('dhEmailEntry');
	emailEntryNode.onkeydown = function(ev) {
		var key = dh.util.getKeyCode(ev);
		if (key == ENTER) {
			dh.account.verifyEmail();
		}
	}
	
	dh.photochooser.init("user", dh.account.userId)

	dh.account.myspaceEntry = new dh.lovehate.Entry('dhMyspace', 'Enter your Myspace name', dh.account.initialMyspaceName,
							'I despise Tom and his space', dh.account.initialMyspaceHateQuip);
	dh.account.myspaceEntry.onLoveSaved = dh.account.onMyspaceLoveSaved;
	dh.account.myspaceEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.myspaceEntry, 'MYSPACE');
	dh.account.myspaceEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.myspaceEntry, 'MYSPACE');

	dh.account.youTubeEntry = new dh.lovehate.Entry('dhYouTube', 'YouTube username or profile URL', dh.account.initialYouTubeName,
							'Video should kill the internet geeks', dh.account.initialYouTubeHateQuip);
	dh.account.youTubeEntry.onLoveSaved = dh.account.onYouTubeLoveSaved;
	dh.account.youTubeEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.youTubeEntry, 'YOUTUBE');
	dh.account.youTubeEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.youTubeEntry, 'YOUTUBE');
	
	dh.account.lastFmEntry = new dh.lovehate.Entry('dhLastFm', 'Last.fm username', dh.account.initialLastFmName,
					'Uhh...what\'s Last.fm?', dh.account.initialLastFmHateQuip);
	dh.account.lastFmEntry.onLoveSaved = dh.account.onLastFmLoveSaved;
	dh.account.lastFmEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.lastFmEntry, 'LASTFM');
	dh.account.lastFmEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.lastFmEntry, 'LASTFM');		
		
	dh.account.flickrEntry = new dh.lovehate.Entry('dhFlickr', 'Email used for Flickr account', dh.account.initialFlickrEmail,
					'Flickr doesn\'t do it for me', dh.account.initialFlickrHateQuip);
	dh.account.flickrEntry.onLoveSaved = dh.account.onFlickrLoveSaved;
	dh.account.flickrEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.flickrEntry, 'FLICKR');
	dh.account.flickrEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.flickrEntry, 'FLICKR');
	
	dh.account.linkedInEntry = new dh.lovehate.Entry('dhLinkedIn', 'LinkedIn profile URL or username', dh.account.initialLinkedInName,
					'LinkedIn is for nerds', dh.account.initialLinkedInHateQuip);
	dh.account.linkedInEntry.onLoveSaved = dh.account.onLinkedInLoveSaved;
	dh.account.linkedInEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.linkedInEntry, 'LINKED_IN');
	dh.account.linkedInEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.linkedInEntry, 'LINKED_IN');	
}

dojo.event.connect(dojo, "loaded", dj_global, "dhAccountInit");
