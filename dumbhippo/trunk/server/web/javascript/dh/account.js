dojo.provide("dh.account");
dojo.require("dh.textinput");
dojo.require("dh.fileinput");
dojo.require("dh.photochooser");

dh.account.linkClosures = {};
dh.account.invokeLinkClosure = function(which) {
	var f = dh.account.linkClosures[which];
	f.call(this);
}

dh.account.linkClosuresCount = 0;

dh.account.makeLinkClosure = function(f) {
	dh.account.linkClosures[dh.account.linkClosuresCount] = f;
	var link = "javascript:dh.account.invokeLinkClosure(" + dh.account.linkClosuresCount + ");";
	dh.account.linkClosuresCount = dh.account.linkClosuresCount + 1;
	return link;
}

dh.account.getStatusRow = function(controlId) {
	return document.getElementById(controlId + 'StatusRow');
}

dh.account.getStatusSpacer = function(controlId) {
	return document.getElementById(controlId + 'StatusSpacer');
}

dh.account.getStatusText = function(controlId) {
	return document.getElementById(controlId + 'StatusText');
}

dh.account.getStatusLink = function(controlId) {
	return document.getElementById(controlId + 'StatusLink');
}

dh.account.hideStatus = function(controlId) {
	var statusRow = dh.account.getStatusRow(controlId);
	var statusSpacer = dh.account.getStatusSpacer(controlId);
	statusRow.style.display = 'none';
	statusSpacer.style.display = 'none';
}

dh.account.showStatus = function(controlId, statusText, linkText, linkHref, linkTitle) {
	var statusRow = dh.account.getStatusRow(controlId);
	var statusSpacer = dh.account.getStatusSpacer(controlId);
	var statusTextNode = dh.account.getStatusText(controlId);
	var statusLinkNode = dh.account.getStatusLink(controlId);
	dojo.dom.textContent(statusTextNode, statusText);
	
	if (linkText) {
		dojo.dom.textContent(statusLinkNode, linkText);
		statusLinkNode.href = linkHref;
		statusLinkNode.title = linkTitle;
		statusLinkNode.style.display = 'inline';
	} else {
		statusLinkNode.style.display = 'none';
	}
	
	// Show everything
	try {
		// Firefox
		statusRow.style.display = 'table-row';
		statusSpacer.style.display = 'table-row';
	} catch (e) {
		// IE
		statusRow.style.display = 'block';
		statusSpacer.style.display = 'block';
	}
}

dh.account.showStatusMessage = function(controlId, message, hideClose) {
	dh.account.showStatus(controlId, message, hideClose ? null : "Close",
		hideClose ? null : dh.account.makeLinkClosure(function() {
			dh.account.hideStatus(controlId);
		}),
		hideClose ? null : "I've read this already, go away");
}

dh.account.onValueChanged = function(entryObject, postMethod, argName, value,
										pendingMessage, successMessage) {
	var controlId = entryObject.elem.id;										
	if (!value || value.length == 0) {
		return;
	}
	var args = {};
	args[argName] = value;
	dh.account.showStatus(controlId, pendingMessage, null, null);
   	dh.server.doPOST(postMethod,
			     		args,
		  	    		function(type, data, http) {
		  	    			var oldValue = dh.account.undoValues[controlId];
		  	    	 		dh.account.showStatus(controlId, successMessage,
		  	    	 			oldValue ? "Undo" : null,
		  	    	 			oldValue ?
				  	    	 		dh.account.makeLinkClosure(function() {
				  	    	 			dh.account.undo(entryObject, oldValue, postMethod, argName);
				  	    	 		}) : null,
			  	    	 		oldValue ? "Change back to the previous setting" : null);
		  	    	 		// this saves what we'll undo to after the NEXT change
							dh.account.undoValues[controlId] = value;
			  	    	},
			  	    	function(type, error, http) {
							dh.account.showStatusMessage(controlId, "Failed to save this setting.");
			  	    	});
}

dh.account.undo = function(entryObject, oldValue, postMethod, argName) {
	var controlId = entryObject.elem.id;
	dh.account.showStatus(controlId, "Undoing...", null, null, null);
	var args = {};
	args[argName] = oldValue;
   	dh.server.doPOST(postMethod,
			     		args,
		  	    		function(type, data, http) {
		  	    			entryObject.setValue(oldValue, true); // true = doesn't emit the changed signal
		  	    	 		dh.account.showStatusMessage(controlId, "Undone!");
	 					  	// this saves what we'll undo to after the NEXT change
							dh.account.undoValues[controlId] = oldValue;
			  	    	},
			  	    	function(type, error, http) {
							dh.account.showStatus(controlId, "Failed to undo!", null, null, null);
			  	    	});	
}

dh.account.undoValues = {};
dh.account.currentValues = null; // gets filled in as part of the jsp

dh.account.generatingRandomBio = false;
dh.account.generateRandomBio = function() {
	if (dh.account.generatingRandomBio) {
		dh.account.showStatusMessage('dhBioEntry', "Working on it - be patient!");
		return;
	}

	dh.account.showStatus('dhBioEntry', "Generating random bio...", null,
			  	    	 null, null);
	dh.account.generatingRandomBio = true;
	dh.server.getTextGET("randombio", 
						{ },
						function(type, data, http) {
							dh.account.showStatusMessage('dhBioEntry', "Tada! Random bio!");
							dh.account.generatingRandomBio = false;
							// focus and set the new text
							dh.account.bioEntryNode.select();
							// don't emit changed until user causes it
							dh.account.bioEntry.setValue(data, true);
						},
						function(type, error, http) {
							dh.account.showStatusMessage('dhBioEntry', "Failed to generate random bio - we suck, sorry! Try again soon.");
		  	    	 		dh.account.generatingRandomBio = false;
						});
}


dh.account.verifyEmail = function() {
	var emailEntryNode = document.getElementById('dhEmailEntry');
	if (emailEntryNode.value.indexOf("@") < 0) {
		dh.account.showStatusMessage('dhEmailEntry', "Enter an email address, then click Verify");
		return;
	}
	var address = emailEntryNode.value;
  	dh.server.doPOST("sendclaimlinkemail",
			 	     { "address" : address },
  					 function(type, data, http) {
	  					 dh.account.showStatusMessage('dhEmailEntry', "We sent mail to '" + address + "', click on the link in that mail.");
	  					 emailEntryNode.value = "";
					 },
					 function(type, error, http) {
						 dh.account.showStatusMessage('dhEmailEntry', "Failed to send mail - check the address, or just try again...");
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

dhAccountInit = function() {
	dh.account.usernameEntryNode = document.getElementById('dhUsernameEntry');
	dh.account.usernameEntry = new dh.textinput.Entry(dh.account.usernameEntryNode, "J. Doe", dh.account.currentValues['dhUsernameEntry']);
	
	dh.account.undoValues['dhUsernameEntry'] = dh.account.usernameEntry.getValue();
	dh.account.usernameEntry.onValueChanged = function(value) {
		dh.account.onValueChanged(dh.account.usernameEntry, 'renameperson', 'name', value,
		"Saving user name...",
		"Your user name has been saved.");
	}
		
	dh.account.bioEntryNode = document.getElementById('dhBioEntry');
	dh.account.bioEntry = new dh.textinput.Entry(dh.account.bioEntryNode, "I grew up in Kansas. If you listen to Coldplay, I want to meet you.", dh.account.currentValues['dhBioEntry']);

	dh.account.undoValues['dhBioEntry'] = dh.account.bioEntry.getValue();
	dh.account.bioEntry.onValueChanged = function(value) {
		dh.account.onValueChanged(dh.account.bioEntry, 'setbio', 'bio', value,
		"Saving new bio...",
		"Your bio has been saved.");
	}
	
	dh.account.myspaceEntryNode = document.getElementById('dhMyspaceEntry');
	dh.account.myspaceEntry = new dh.textinput.Entry(dh.account.myspaceEntryNode, null, dh.account.currentValues['dhMyspaceEntry']);

	dh.account.undoValues['dhMyspaceEntry'] = dh.account.myspaceEntry.getValue();
	dh.account.myspaceEntry.onValueChanged = function(value) {
		dh.account.onValueChanged(dh.account.myspaceEntry, 'setmyspacename', 'name', value,
		"Saving new MySpace name...",
		"Your MySpace name has been saved.");
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
}

dojo.event.connect(dojo, "loaded", dj_global, "dhAccountInit");
