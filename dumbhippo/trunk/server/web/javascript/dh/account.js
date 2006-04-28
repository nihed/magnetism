dojo.provide("dh.account");
dojo.require("dh.textinput");

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

dh.account.undo = function(entryObject, oldValue) {
	dh.account.showStatus(entryObject.elem.id, "Undoing...", null, null, null);
   	dh.server.doPOST("renameperson",
			     		{ "name" : oldValue },
		  	    		function(type, data, http) {
		  	    			entryObject.setValue(oldValue, true); // true = doesn't emit the changed signal
		  	    	 		dh.account.showStatus('dhUsernameEntry', "Undone!", "Close",
		  	    	 		"javascript:dh.account.hideStatus('dhUsernameEntry');", "I've read this already, go away");
	 					  	// this saves what we'll undo to after the NEXT change
							dh.account.undoValues['dhUsernameEntry'] = oldValue;
			  	    	},
			  	    	function(type, error, http) {
							dh.account.showStatus('dhUsernameEntry', "Failed to undo!", null, null, null);
			  	    	});	
}

dh.account.usernameEntryNode = null;
dh.account.usernameEntry = null;
dh.account.undoValues = {};
dh.account.currentValues = null; // gets filled in as part of the jsp

dhAccountInit = function() {
	dh.account.usernameEntryNode = document.getElementById('dhUsernameEntry');
	dh.account.usernameEntry = new dh.textinput.Entry(dh.account.usernameEntryNode, "J. Doe", dh.account.currentValues['dhUsernameEntry']);
	dh.account.undoValues['dhUsernameEntry'] = dh.account.usernameEntry.getValue();
	dh.account.usernameEntry.onValueChanged = function(value) {
		if (!value || value.length == 0) {
			return;
		}
		dh.account.showStatus('dhUsernameEntry', "Saving user name...", null, null);
	   	dh.server.doPOST("renameperson",
				     		{ "name" : value },
			  	    		function(type, data, http) {
			  	    			var oldValue = dh.account.undoValues['dhUsernameEntry'];
			  	    	 		dh.account.showStatus('dhUsernameEntry', "Your user name has been saved.", oldValue ? "Undo" : null,
			  	    	 		oldValue ?
			  	    	 		dh.account.makeLinkClosure(function() {
			  	    	 			dh.account.undo(dh.account.usernameEntry, oldValue);
			  	    	 		}) : null, oldValue ? "Change back to '" + oldValue + "'" : null);
			  	    	 		// this saves what we'll undo to after the NEXT change
								dh.account.undoValues['dhUsernameEntry'] = value;
				  	    	},
				  	    	function(type, error, http) {
								dh.account.showStatus('dhUsernameEntry', "Failed to save your new user name.", "Close",
								"javascript:dh.account.hideStatus('dhUsernameEntry');", "");
				  	    	});
	}
}

dojo.event.connect(dojo, "loaded", dj_global, "dhAccountInit");
