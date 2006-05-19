dojo.provide("dh.account");
dojo.require("dh.formtable");
dojo.require("dh.textinput");
dojo.require("dh.fileinput");
dojo.require("dh.photochooser");

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
	
	dh.account.musicbioEntryNode = document.getElementById('dhMusicBioEntry');
	dh.account.musicbioEntry = new dh.textinput.Entry(dh.account.musicbioEntryNode, "If you listen to Coldplay, I want to meet you.", dh.formtable.currentValues['dhMusicBioEntry']);
 
 	dh.formtable.undoValues['dhMusicBioEntry'] = dh.account.musicbioEntry.getValue();
	dh.account.musicbioEntry.onValueChanged = function(value) {
		dh.formtable.onValueChanged(dh.account.musicbioEntry, 'setmusicbio', 'musicbio', value,
		"Saving new music bio...",
		"Your music bio has been saved.");
	}
	
	dh.account.myspaceEntryNode = document.getElementById('dhMyspaceEntry');
	dh.account.myspaceEntry = new dh.textinput.Entry(dh.account.myspaceEntryNode, null, dh.formtable.currentValues['dhMyspaceEntry']);

	dh.formtable.undoValues['dhMyspaceEntry'] = dh.account.myspaceEntry.getValue();
	dh.account.myspaceEntry.onValueChanged = function(value) {
		dh.formtable.onValueChanged(dh.account.myspaceEntry, 'setmyspacename', 'name', value,
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
	
	dh.photochooser.init("user", dh.account.userId)
}

dojo.event.connect(dojo, "loaded", dj_global, "dhAccountInit");
