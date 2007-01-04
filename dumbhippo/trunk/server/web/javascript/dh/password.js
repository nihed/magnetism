dojo.provide("dh.password");

dojo.require("dojo.event.*");
dojo.require("dh.util");
dojo.require("dh.server");

dh.password.passwordEntry = null;
dh.password.againEntry = null;
dh.password.setButton = null;

dh.password.inProgress = false;

// We display the validation messages in a timeout, so they only show 
// up if you pause for a minute (if you aren't sure what's going on)
// Don't make the timeout too long or the new problem will be "stale"
// validation messages

dh.password.validationTimeout = null;
dh.password.validationMessage = null;

dh.password.unqueueValidationMessage = function() {
	if (dh.password.validationTimeout) {
		clearTimeout(dh.password.validationTimeout);
		dh.password.validationTimeout = null;
	}
}

dh.password.showValidationMessageNow = function() {
	if (dh.password.validationTimeout) {
		dh.password.unqueueValidationMessage();
		dh.formtable.showStatusMessage('dhPasswordEntry', dh.password.validationMessage, true);
	}
}

dh.password.queueValidationMessage = function(message) {
	if (dh.password.inProgress)
		return;
	dh.password.unqueueValidationMessage();
	dh.password.validationMessage = message;
	dh.password.validationTimeout = setTimeout(function() {
		dh.formtable.showStatusMessage('dhPasswordEntry', dh.password.validationMessage, true);
	}, 1500);
}

// this is called both onkeyup and onchange, ev won't exist for onchange (or in IE of course)
dhPasswordFormUpdate = function(ev) {	
	var first = dh.password.passwordEntry.value;
	var second = dh.password.againEntry.value;
	
	// keep spaces out of the entry
	first = dh.util.trim(first);
	dh.password.passwordEntry.value = first;
	second = dh.util.trim(second);
	dh.password.againEntry.value = second;
	
	var invalid = false;
	if (first != second) {
		dh.password.queueValidationMessage("The two passwords don't match.");
		invalid = true;
	} else if (first.length < 6) {
		if (first.length > 0 || second.length > 0)
			dh.password.queueValidationMessage("Your password should have at least 6 letters or numbers in it.");
		else {
			dh.password.unqueueValidationMessage();
			dh.formtable.hideStatus('dhPasswordEntry');
		}
		invalid = true;
	} else {
		dh.password.queueValidationMessage("Your password looks good! Click 'Set Password' when you're done.");
	}
	
	if (invalid) {
		dh.password.setButton.src = dhImageRoot3 + "setpassword_disabled.gif";
	} else {
		dh.password.setButton.src = dhImageRoot3 + "setpassword.gif";
	}
				
	var key = dh.util.getKeyCode(ev);
	if (key && key == ENTER) {
		dhPasswordFormSubmit();
	}
}

// set to "" to unset the password
dh.password.setPassword = function(password) {
	if (dh.password.inProgress)
		return;
	
	dh.password.unqueueValidationMessage();
	
	dh.password.inProgress = true;
	dh.server.doPOST("setpassword",
					{ 
						"password" : password
					},
					function(type, data, http) {
						dh.password.passwordEntry.value = "";							
						dh.password.againEntry.value = "";
						dhPasswordFormUpdate();
						
						var removeNode = document.getElementById('dhRemovePasswordLink');
						if (password && password.length > 0) {
							dh.formtable.showStatusMessage('dhPasswordEntry', "Your new password has been saved.");
							removeNode.style.display = 'inline';
						} else {
							dh.formtable.showStatusMessage('dhPasswordEntry', "Your password has been removed.");
							removeNode.style.display = 'none';
						}
						
						dh.password.inProgress = false;
					},
					function(type, error, http) {
						dh.password.inProgress = false;
						dh.formtable.showStatusMessage('dhPasswordEntry', "Failed to set your password... try again?");
					});
}

dhPasswordFormSubmit = function() {
	if (dh.password.setButton.disabled) {
		// can happen when pressing enter in text box
		dh.password.showValidationMessageNow();
		return;
	}

	var password = dh.util.trim(dh.password.passwordEntry.value);
	dh.password.setPassword(password);
}

dh.password.unsetPassword = function() {
	dh.password.setPassword("");
}

dhPasswordInit = function() {
	dh.password.passwordEntry = document.getElementById('dhPasswordEntry');
	dh.password.againEntry = document.getElementById('dhPasswordAgainEntry');
	dh.password.setButton = document.getElementById('dhSetPasswordButton');
	
	// this happens when your account is disabled
	if (!dh.password.passwordEntry)
		return;

	dojo.event.connect(dh.password.passwordEntry, "onchange", dj_global, "dhPasswordFormUpdate");
	dojo.event.connect(dh.password.againEntry, "onchange", dj_global, "dhPasswordFormUpdate");
	dojo.event.connect(dh.password.passwordEntry, "onkeyup", dj_global, "dhPasswordFormUpdate");
	dojo.event.connect(dh.password.againEntry, "onkeyup", dj_global, "dhPasswordFormUpdate");

	dojo.event.connect(dh.password.setButton, "onclick", dj_global, "dhPasswordFormSubmit");
	
	// init sensitivity of button, etc.
	dhPasswordFormUpdate();
}

dojo.event.connect(dojo, "loaded", dj_global, "dhPasswordInit");
